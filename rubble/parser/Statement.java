package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Unit;


public final class Statement extends Parser<AST.Statement<Unit>> {
    
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
    /*
    public Statement(Location loc, ArrayList<Token> tokens) {
        super(new ParseContext(loc, tokens), "a statement", ";");
        scopeStack = null;
    }
    */
    
    private Statement(ParseContext context, StringStack scopeStack) {
        super(context, "a statement", ";");
        this.scopeStack = scopeStack;
    }
    
    private Statement(Location loc, ArrayList<Token> tokens, StringStack scopeStack) {
        super(new ParseContext(loc, tokens), "a statement", ";");
        this.scopeStack = scopeStack;
    }
    
    private static AST.LValue<Unit> certifyLValue(AST.Expression<Unit> ast) throws CompilerError {
        switch (ast.tag) {
        case Index:
            AST.Index<Unit> ix = (AST.Index<Unit>)ast;
            return new AST.IndexL<Unit>(ast.loc, Unit.Unit, certifyLValue(ix.base), ix.offset);
        case Tuple:
            AST.Tuple<Unit> tuple = (AST.Tuple<Unit>)ast;
            ArrayList<AST.LValue<Unit>> ls = new ArrayList<AST.LValue<Unit>>();
            for (AST.Expression<Unit> e: tuple.es){
                ls.add(certifyLValue(e));
            }
            return new AST.TupleL<Unit>(ast.loc, Unit.Unit, ls);
        case ValueAt:
            return new AST.Indirect<Unit>(ast.loc, Unit.Unit, ast);
        case Variable:
            return new AST.Direct<Unit>(ast.loc, Unit.Unit, ((AST.Variable<Unit>)ast).name);
        default:
            throw ParseContext.errorUnexpected(ast.loc, "an lvalue", "another kind of expression");
        }
    }
    
    protected LeftDenotation<rubble.data.AST.Statement<Unit>> leftDenotation(Token token) throws CompilerError {
        return null;
    }

    protected AST.Statement<Unit> nullDenotation(Token token) throws CompilerError {
        Token lookahead;
        switch(token.tag) {
        case Block:
            if (token.source.equals("{") || token.source.equals(Token.IMPLICIT_BRACE)) {
                return new AST.Nested<Unit>(token.loc, ((new Statement(token.loc, token.subtokens, scopeStack)).parseListFull("}")));
            } else if (token.source.equals("(")) {
                return parseCallOrAssignment(token);
            }
            throw errorUnexpectedToken(token.loc, token.source);
        case Identifier:
            lookahead = context.lookahead();
            if (lookahead == null) {
                throw errorUnexpectedToken(token.loc, "an incomplete statement");
            } else if (lookahead.source.equals("forever")) {
                context.index += 1;
                return new AST.Forever<Unit>(token.loc, token.source, ((new Statement(context.inBraces(), new StringStack(token.source, scopeStack))).parseListFull("}")));
            }
            return parseCallOrAssignment(token);
        case Reserved:
            if (token.source.equals("break")) {
                lookahead = context.lookahead();
                if (lookahead == null || lookahead.tag == Token.Tag.Semicolon) {
                    if (scopeStack.size == 0) {
                        throw CompilerError.parse(token.loc, "There are is no enclosing loop to break out of.");
                    }
                    return new AST.Break<Unit>(token.loc, 0);
                } else if (lookahead.tag == Token.Tag.Identifier) {
                    int target = scopeStack.find(lookahead.source);
                    if (target >= scopeStack.size) {
                        throw CompilerError.parse(token.loc, "The break target was not found.");
                    }
                    return new AST.Break<Unit>(token.loc, target);
                }
                else throw ParseContext.errorUnexpected(token.loc, "the end of the statement or a label", token.source);
            } else if (token.source.equals("if")) {
                AST.Expression<Unit> cond = (new Expression(context)).parse(0);
                context.requireToken("then");
                ArrayList<AST.Statement<Unit>> trueBranch = (new Statement(context.inBraces(), scopeStack)).parseListFull("}");
                ArrayList<AST.Statement<Unit>> falseBranch = new ArrayList<AST.Statement<Unit>>();
                lookahead = context.lookahead();
                if (lookahead != null && lookahead.source.equals("else")) {
                    context.index += 1;
                    falseBranch = (new Statement(context.inBraces(), scopeStack)).parseListFull("}");
                }
                return new AST.IfS<Unit>(token.loc, cond, trueBranch, falseBranch);
            } else if (token.source.equals("forever")) {
                return new AST.Forever<Unit>(token.loc, "", (new Statement(context.inBraces(), new StringStack("", scopeStack)).parseListFull("}")));
            } else if (token.source.equals("let")) {
                return parseLet(token.loc);
            } else if (token.source.equals("return")) {
                return new AST.Return<Unit>(token.loc, (new Expression(context).parseOpenTuple()));
            } else if (token.source.equals("valueAt")) {
                return parseCallOrAssignment(token);
            }
            throw errorUnexpectedToken(token.loc, token.source);
        default:
            throw errorUnexpectedToken(token.loc, token.source);
        }
    }
    
    private AST.Statement<Unit> parseCallOrAssignment(Token token) throws CompilerError {
        context.index -= 1;
        AST.Expression<Unit> ast = (new Expression(context)).parseOpenTuple();
        Token lookahead = context.lookahead();
        if (lookahead == null || lookahead.tag == Token.Tag.Semicolon) {
            if (ast.tag == AST.ExpressionTag.Apply) {
                return new AST.Call<Unit>(token.loc, ((AST.Apply<Unit>)ast).function, ((AST.Apply<Unit>)ast).argument);
            }
            throw errorUnexpectedToken(token.loc, token.source);
        } else if (lookahead.source.equals("=")) {
            AST.LValue<Unit> lValue = certifyLValue(ast);
            context.index += 1;
            return new AST.Assign<Unit>(token.loc, lValue, (new Expression(context)).parseOpenTuple());
        }
        throw errorUnexpectedToken(token.loc, token.source);
    }
    
    public AST.Let<Unit> parseLet(Location loc) throws CompilerError {
        Token lookahead = context.lookahead();
        if (lookahead == null) {
            throw ParseContext.errorUnexpected(loc, "a binding", "ran out of input");
        } else if (lookahead.tag == Token.Tag.Block) {
            return new AST.Let<Unit>(loc, (new Binding(lookahead.loc, lookahead.subtokens)).parseListFull("}"));
        } else if (lookahead.tag == Token.Tag.Identifier) {
            ArrayList<AST.Binding<Unit>> bindings = new ArrayList<AST.Binding<Unit>>();
            bindings.add((new Binding(lookahead.loc, lookahead.subtokens)).parse(0));
            return new AST.Let<Unit>(loc, bindings);
        }
        throw ParseContext.errorUnexpected(loc, "an identifier or binding block", lookahead.source);
    }
}
