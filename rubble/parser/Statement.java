package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Types;

/**
 * The statement parser.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class Statement extends Parser<AST.Statement<Types.Parsed>> {
    
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
    
    private static AST.LValue<Types.Parsed> certifyLValue(AST.Expression<Types.Parsed> ast) throws CompilerError {
        switch (ast.tag) {
        case Index:
            AST.Index<Types.Parsed> ix = (AST.Index<Types.Parsed>)ast;
            return new AST.IndexL<Types.Parsed>(ast.loc, Types.UNKNOWN_NEUTRAL, certifyLValue(ix.base), ix.offset);
        case Tuple:
            AST.Tuple<Types.Parsed> tuple = (AST.Tuple<Types.Parsed>)ast;
            ArrayList<AST.LValue<Types.Parsed>> ls = new ArrayList<AST.LValue<Types.Parsed>>();
            for (AST.Expression<Types.Parsed> e: tuple.es){
                ls.add(certifyLValue(e));
            }
            return new AST.TupleL<Types.Parsed>(ast.loc, Types.UNKNOWN_NEUTRAL, ls);
        case ValueAt:
            return new AST.Indirect<Types.Parsed>(ast.loc, Types.UNKNOWN_NEUTRAL, ((AST.ValueAt<Types.Parsed>)ast).value);
        case Variable:
            return new AST.Direct<Types.Parsed>(ast.loc, Types.UNKNOWN_NEUTRAL, ((AST.Variable<Types.Parsed>)ast).source);
        default:
            throw ParseContext.errorUnexpected(ast.loc, "an lvalue", "found another kind of expression");
        }
    }
    
    protected LeftDenotation<rubble.data.AST.Statement<Types.Parsed>> leftDenotation(Token token) throws CompilerError {
        return null;
    }

    protected AST.Statement<Types.Parsed> nullDenotation(Token token) throws CompilerError {
        Token lookahead;
        switch(token.tag) {
        case Block:
            if (token.source.equals("{") || token.source.equals(Token.IMPLICIT_BRACE)) {
                return new AST.Nested<Types.Parsed>(token.loc, ((new Statement(token.loc, token.subtokens, scopeStack)).parseListFull("}")));
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
                return new AST.Forever<Types.Parsed>(token.loc, token.source, ((new Statement(context.inBraces(), new StringStack(token.source, scopeStack))).parseListFull("}")));
            }
            return parseCallOrAssignment(token);
        case Reserved:
            if (token.source.equals("break")) {
                if (scopeStack.size == 0) {
                    throw CompilerError.parse(token.loc, "There is no enclosing loop to break out of.");
                }
                lookahead = context.lookahead();
                if (lookahead == null || lookahead.tag == Token.Tag.Semicolon) {
                    return new AST.Break<Types.Parsed>(token.loc, 0);
                } else if (lookahead.tag == Token.Tag.Identifier) {
                    int target = scopeStack.find(lookahead.source);
                    if (target >= scopeStack.size) {
                        throw CompilerError.parse(token.loc, "The break target was not found.");
                    }
                    context.index++;
                    return new AST.Break<Types.Parsed>(token.loc, target);
                }
                else throw ParseContext.errorUnexpected(token.loc, "the end of the statement or a label", "found " + lookahead.source);
            } else if (token.source.equals("if")) {
                AST.Expression<Types.Parsed> cond = (new Expression(context)).parse(0);
                context.requireToken("then");
                ArrayList<AST.Statement<Types.Parsed>> trueBranch = (new Statement(context.inBraces(), scopeStack)).parseListFull("}");
                ArrayList<AST.Statement<Types.Parsed>> falseBranch = new ArrayList<AST.Statement<Types.Parsed>>();
                lookahead = context.lookahead();
                if (lookahead != null && lookahead.source.equals("else")) {
                    context.index++;
                    falseBranch = (new Statement(context.inBraces(), scopeStack)).parseListFull("}");
                }
                return new AST.IfS<Types.Parsed>(token.loc, cond, trueBranch, falseBranch);
            } else if (token.source.equals("forever")) {
                return new AST.Forever<Types.Parsed>(token.loc, "", (new Statement(context.inBraces(), new StringStack("", scopeStack)).parseListFull("}")));
            } else if (token.source.equals("let")) {
                return parseLet(token.loc);
            } else if (token.source.equals("return")) {
                return new AST.Return<Types.Parsed>(token.loc, (new Expression(context).parseOpenTuple()));
            } else if (token.source.equals("valueAt")) {
                return parseCallOrAssignment(token);
            }
            throw errorUnexpectedToken(token.loc, token.source);
        default:
            throw errorUnexpectedToken(token.loc, token.source);
        }
    }
    
    private AST.Statement<Types.Parsed> parseCallOrAssignment(Token token) throws CompilerError {
        context.index -= 1;
        AST.Expression<Types.Parsed> ast = (new Expression(context)).parseOpenTuple();
        Token lookahead = context.lookahead();
        if (lookahead == null || lookahead.tag == Token.Tag.Semicolon) {
            if (ast.tag == AST.ExpressionTag.Apply) {
                return new AST.Call<Types.Parsed>(token.loc, ((AST.Apply<Types.Parsed>)ast).function, ((AST.Apply<Types.Parsed>)ast).argument);
            }
            throw errorUnexpectedToken(token.loc, token.source);
        } else if (lookahead.source.equals("=")) {
            AST.LValue<Types.Parsed> lValue = certifyLValue(ast);
            context.index++;
            return new AST.Assign<Types.Parsed>(token.loc, lValue, (new Expression(context)).parseOpenTuple());
        }
        throw errorUnexpectedToken(token.loc, token.source);
    }
    
    public AST.Let<Types.Parsed> parseLet(Location loc) throws CompilerError {
        Token lookahead = context.lookahead();
        if (lookahead == null) {
            throw ParseContext.errorUnexpected(loc, "a binding", "ran out of input");
        }
        Location letLoc;
        ArrayList<AST.Binding<Types.Parsed>> bs;
        switch (lookahead.tag) {
        case Block:
            bs = new Binding(lookahead.loc, lookahead.subtokens).parseListFull("}");
            if (bs.size() == 0) {
                throw CompilerError.parse(loc, "You cannot have an empty let block.");
            }
            letLoc = new Location(loc, bs.get(bs.size() - 1).loc);
            return new AST.Let<Types.Parsed>(letLoc, bs);
        case Identifier:
        case Reserved:
            bs = new ArrayList<AST.Binding<Types.Parsed>>();
            bs.add((new Binding(context)).parse(0));
            letLoc = new Location(loc, bs.get(bs.size() - 1).loc);
            return new AST.Let<Types.Parsed>(letLoc, bs);
        }
        throw ParseContext.errorUnexpected(loc, "an identifier or binding block", "found " + lookahead.source);
    }
}
