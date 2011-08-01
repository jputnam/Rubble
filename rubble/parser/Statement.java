package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Types;
import rubble.data.Unit;


public final class Statement extends Parser<AST.Statement<Unit, Types.Parsed, String>> {
    
    private static final class StringStack {
        
        public final String head;
        public final StringStack tail;
        public final int size;
        
        public final static StringStack NIL = new StringStack("", null);
        
        public StringStack(String head, StringStack tail) {
            this.head = head;
            this.tail = tail;
            this.size = tail == null ? 0 : tail.size + 1;
        }
        
        public int find(String target) {
            return head.equals(target) ? 0 : tail == null ? 1 : 1 + tail.find(target);
        }
    }
    
    private StringStack scopeStack;
    
    public Statement(ParseContext context) {
        super(context, "a statement", ";");
        scopeStack = StringStack.NIL;
    }
    
    public Statement(Location loc, ArrayList<Token> tokens) {
        super(new ParseContext(loc, tokens), "a statement", ";");
        scopeStack = StringStack.NIL;
    }
    
    private Statement(ParseContext context, StringStack scopeStack) {
        super(context, "a statement", ";");
        this.scopeStack = scopeStack;
    }
    
    private Statement(Location loc, ArrayList<Token> tokens, StringStack scopeStack) {
        super(new ParseContext(loc, tokens), "a statement", ";");
        this.scopeStack = scopeStack;
    }
    
    private static AST.LValue<Unit, Types.Parsed, String> certifyLValue(AST.Expression<Unit, Types.Parsed, String> ast) throws CompilerError {
        switch (ast.tag) {
        case Index:
            AST.Index<Unit, Types.Parsed, String> ix = (AST.Index<Unit, Types.Parsed, String>)ast;
            return new AST.IndexL<Unit, Types.Parsed, String>(ast.loc, Unit.Unit, certifyLValue(ix.base), ix.offset);
        case Tuple:
            AST.Tuple<Unit, Types.Parsed, String> tuple = (AST.Tuple<Unit, Types.Parsed, String>)ast;
            ArrayList<AST.LValue<Unit, Types.Parsed, String>> ls = new ArrayList<AST.LValue<Unit, Types.Parsed, String>>();
            for (AST.Expression<Unit, Types.Parsed, String> e: tuple.es){
                ls.add(certifyLValue(e));
            }
            return new AST.TupleL<Unit, Types.Parsed, String>(ast.loc, Unit.Unit, ls);
        case ValueAt:
            return new AST.Indirect<Unit, Types.Parsed, String>(ast.loc, Unit.Unit, ((AST.ValueAt<Unit, Types.Parsed, String>)ast).value);
        case Variable:
            return new AST.Direct<Unit, Types.Parsed, String>(ast.loc, Unit.Unit, ((AST.Variable<Unit, Types.Parsed, String>)ast).name);
        default:
            throw ParseContext.errorUnexpected(ast.loc, "an lvalue", "found another kind of expression");
        }
    }
    
    protected LeftDenotation<rubble.data.AST.Statement<Unit, Types.Parsed, String>> leftDenotation(Token token) throws CompilerError {
        return null;
    }

    protected AST.Statement<Unit, Types.Parsed, String> nullDenotation(Token token) throws CompilerError {
        Token lookahead;
        switch(token.tag) {
        case Block:
            if (token.source.equals("{") || token.source.equals(Token.IMPLICIT_BRACE)) {
                return new AST.Nested<Unit, Types.Parsed, String>(token.loc, ((new Statement(token.loc, token.subtokens, scopeStack)).parseListFull("}")));
            } else if (token.source.equals("(")) {
                return parseCallOrAssignment(token);
            }
            throw errorUnexpectedToken(token.loc, token.source);
        case Identifier:
            lookahead = context.lookahead();
            if (lookahead == null) {
                throw errorUnexpectedToken(token.loc, "an incomplete statement");
            } else if (lookahead.source.equals("forever")) {
                context.index++;
                return new AST.Forever<Unit, Types.Parsed, String>(token.loc, token.source, ((new Statement(context.inBraces(), new StringStack(token.source, scopeStack))).parseListFull("}")));
            }
            return parseCallOrAssignment(token);
        case Reserved:
            if (token.source.equals("break")) {
                if (scopeStack.size == 0) {
                    throw CompilerError.parse(token.loc, "There is no enclosing loop to break out of.");
                }
                lookahead = context.lookahead();
                if (lookahead == null || lookahead.tag == Token.Tag.Semicolon) {
                    return new AST.Break<Unit, Types.Parsed, String>(token.loc, 0);
                } else if (lookahead.tag == Token.Tag.Identifier) {
                    int target = scopeStack.find(lookahead.source);
                    if (target >= scopeStack.size) {
                        throw CompilerError.parse(token.loc, "The break target was not found.");
                    }
                    context.index++;
                    return new AST.Break<Unit, Types.Parsed, String>(token.loc, target);
                }
                else throw ParseContext.errorUnexpected(token.loc, "the end of the statement or a label", "found " + lookahead.source);
            } else if (token.source.equals("if")) {
                AST.Expression<Unit, Types.Parsed, String> cond = (new Expression(context)).parse(0);
                context.requireToken("then");
                ArrayList<AST.Statement<Unit, Types.Parsed, String>> trueBranch = (new Statement(context.inBraces(), scopeStack)).parseListFull("}");
                ArrayList<AST.Statement<Unit, Types.Parsed, String>> falseBranch = new ArrayList<AST.Statement<Unit, Types.Parsed, String>>();
                lookahead = context.lookahead();
                if (lookahead != null && lookahead.source.equals("else")) {
                    context.index++;
                    falseBranch = (new Statement(context.inBraces(), scopeStack)).parseListFull("}");
                }
                return new AST.IfS<Unit, Types.Parsed, String>(token.loc, cond, trueBranch, falseBranch);
            } else if (token.source.equals("forever")) {
                return new AST.Forever<Unit, Types.Parsed, String>(token.loc, "", (new Statement(context.inBraces(), new StringStack("", scopeStack)).parseListFull("}")));
            } else if (token.source.equals("let")) {
                return parseLet(token.loc);
            } else if (token.source.equals("return")) {
                return new AST.Return<Unit, Types.Parsed, String>(token.loc, (new Expression(context).parseOpenTuple()));
            } else if (token.source.equals("valueAt")) {
                return parseCallOrAssignment(token);
            }
            throw errorUnexpectedToken(token.loc, token.source);
        default:
            throw errorUnexpectedToken(token.loc, token.source);
        }
    }
    
    private AST.Statement<Unit, Types.Parsed, String> parseCallOrAssignment(Token token) throws CompilerError {
        context.index -= 1;
        AST.Expression<Unit, Types.Parsed, String> ast = (new Expression(context)).parseOpenTuple();
        Token lookahead = context.lookahead();
        if (lookahead == null || lookahead.tag == Token.Tag.Semicolon) {
            if (ast.tag == AST.ExpressionTag.Apply) {
                return new AST.Call<Unit, Types.Parsed, String>(token.loc, ((AST.Apply<Unit, Types.Parsed, String>)ast).function, ((AST.Apply<Unit, Types.Parsed, String>)ast).argument);
            }
            throw errorUnexpectedToken(token.loc, token.source);
        } else if (lookahead.source.equals("=")) {
            AST.LValue<Unit, Types.Parsed, String> lValue = certifyLValue(ast);
            context.index++;
            return new AST.Assign<Unit, Types.Parsed, String>(token.loc, lValue, (new Expression(context)).parseOpenTuple());
        }
        throw errorUnexpectedToken(token.loc, token.source);
    }
    
    public AST.Let<Unit, Types.Parsed, String> parseLet(Location loc) throws CompilerError {
        Token lookahead = context.lookahead();
        if (lookahead == null) {
            throw ParseContext.errorUnexpected(loc, "a binding", "ran out of input");
        }
        Location letLoc;
        ArrayList<AST.Binding<Unit, Types.Parsed, String>> bs;
        switch (lookahead.tag) {
        case Block:
            bs = new Binding(lookahead.loc, lookahead.subtokens).parseListFull("}");
            if (bs.size() == 0) {
                throw CompilerError.parse(loc, "You cannot have an empty let block.");
            }
            letLoc = new Location(loc, bs.get(bs.size() - 1).loc);
            return new AST.Let<Unit, Types.Parsed, String>(letLoc, bs);
        case Identifier:
        case Reserved:
            bs = new ArrayList<AST.Binding<Unit, Types.Parsed, String>>();
            bs.add((new Binding(context)).parse(0));
            letLoc = new Location(loc, bs.get(bs.size() - 1).loc);
            return new AST.Let<Unit, Types.Parsed, String>(letLoc, bs);
        }
        throw ParseContext.errorUnexpected(loc, "an identifier or binding block", "found " + lookahead.source);
    }
}
