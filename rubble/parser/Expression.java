package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Types;
import rubble.data.Unit;
import rubble.data.Token;


public final class Expression extends Parser<AST.Expression<Unit>> {
    
    public Expression(ParseContext context) {
        super(context, "an expression", ",");
    }
    
    public Expression(Location loc, ArrayList<Token> tokens) {
        super(loc, tokens, "an expression", ",");
    }
    
    private LeftDenotation<AST.Expression<Unit>> application(final Token token, final boolean untupleArgument) throws CompilerError {
        return new LeftDenotation<AST.Expression<Unit>>() {
            
            public int lbp() { return 12; }
            private final int rbp = 11;
            
            public AST.Expression<Unit> apply(AST.Expression<Unit> left) throws CompilerError {
                LeftDenotation<AST.Expression<Unit>> ld = (context.isLive()) ? leftDenotation(context.tokens.get(context.index)) : null;
                if (ld != null && rbp < ld.lbp()) {
                    ArrayList<AST.Expression<Unit>> arguments = new ArrayList<AST.Expression<Unit>>();
                    arguments.add(ld.apply(nullDenotation(token)));
                    return new AST.Apply<Unit>(left.loc, Unit.Unit, left, arguments, untupleArgument);
                } else {
                    return new AST.Apply<Unit>(left.loc, Unit.Unit, left, (new Expression(token.loc, token.subtokens)).parseListFull(")"), false);
                }
            }
        };
    }
    
    private LeftDenotation<AST.Expression<Unit>> infixExpression(final int precedence, final AST.Expression<Unit> center) throws CompilerError {
        return new LeftDenotation<AST.Expression<Unit>>() {
            
            public int lbp() { return precedence; }
            
            public AST.Expression<Unit> apply(AST.Expression<Unit> left) throws CompilerError {
                AST.Expression<Unit> right = parse(precedence);
                switch (center.tag) {
                case Apply:
                    AST.Apply<Unit> result = (AST.Apply<Unit>)center;
                    result.arguments.add(left);
                    result.arguments.add(right);
                    return result;
                default:
                    ArrayList<AST.Expression<Unit>> arguments = new ArrayList<AST.Expression<Unit>>();
                    arguments.add(left);
                    arguments.add(right);
                    return new AST.Apply<Unit>(center.loc, Unit.Unit, center, arguments, false);
                }
            }
        };
    }
    
    private LeftDenotation<AST.Expression<Unit>> infixOperator(final int precedence, Token center) throws CompilerError {
        return infixExpression(precedence, new AST.Variable<Unit>(center.loc, Unit.Unit, center.source));
    }
    
    protected LeftDenotation<AST.Expression<Unit>> leftDenotation(final Token token) throws CompilerError {
        switch (token.tag) {
        case Block:
            if (token.source.equals("`")) {
                return infixExpression(5, (new Expression(token.loc, token.subtokens)).parseFull("`"));
            } else if (token.source.equals("(")) {
                return application(token, false);
            } else if (token.source.equals(".(")) {
                return application(token, true);
            } else if (token.source.equals("[")) {
                return new LeftDenotation<AST.Expression<Unit>>() {
                    
                    public int lbp() { return 13; }
                    
                    public AST.Expression<Unit> apply(AST.Expression<Unit> left) throws CompilerError {
                        return new AST.Index<Unit>(left.loc, Unit.Unit, left, (new Expression(token.loc, token.subtokens)).parseFull("]"));
                    }
                };
            }
            return null;
        case Identifier:
            return application(token, false);
        case Number:
            return application(token, false);
        case Operator:
            if (token.source.equals("+")) {
                return infixOperator(5, token);
            } else if (token.source.equals("-")) {
                return infixOperator(5, token);
            } else if (token.source.equals("*")) {
                return infixOperator(6, token);
            } else if (token.source.equals("/")) {
                return infixOperator(6, token);
            } else if (token.source.equals("<")) {
                return infixOperator(3, token);
            } else if (token.source.equals(">")) {
                return infixOperator(3, token);
            } else if (token.source.equals("<=")) {
                return infixOperator(3, token);
            } else if (token.source.equals(">=")) {
                return infixOperator(3, token);
            } else if (token.source.equals("==")) {
                return infixOperator(3, token);
            } else if (token.source.equals("!=")) {
                return infixOperator(3, token);
            } else if (token.source.equals("&&")) {
                return infixOperator(2, token);
            } else if (token.source.equals("||")) {
                return infixOperator(2, token);
            }
            throw errorUnexpectedToken(token.loc, "an unrecognized operator");
        case Reserved:
            if (token.source.equals("asType")) {
                final Types.Type tau = (new Type(context)).parse(11);
                return new LeftDenotation<AST.Expression<Unit>>() {
                    
                    public int lbp() { return 11; }
                    
                    public AST.Expression<Unit> apply(AST.Expression<Unit> left) throws CompilerError {
                        return new AST.AsType<Unit>(left.loc, Unit.Unit, left, tau);
                    }
                };
            }
            return null;
        default:
            return null;
        }
    }
    
    protected AST.Expression<Unit> nullDenotation(Token token) throws CompilerError {
        switch (token.tag) {
        case Block:
            if (token.source.equals("(")) {
                return parseTuple(token.loc, token.subtokens);
            } else if (token.source.equals("[")) {
                return new AST.BufferLiteral<Unit>(token.loc, Unit.Unit, (new Expression(token.loc, token.subtokens)).parseListFull("]"));
            } else if (token.source.equals("`")) {
                throw errorUnexpectedToken(token.loc, "a backtick sequence");
            } else {
                throw errorUnexpectedToken(token.loc, "a code block");
            }
        case Comma:
            throw errorUnexpectedToken(token.loc, "a comma");
        case Identifier:
            return new AST.Variable<Unit>(token.loc, Unit.Unit, token.source);
        case Number:
            return new AST.Number<Unit>(token.loc, Unit.Unit, token.source);
        case Operator:
            throw errorUnexpectedToken(token.loc, "an operator");
        case Reserved:
            if (token.source.equals("addressOf")) {
                return new AST.AddressOf<Unit>(token.loc, Unit.Unit, parse(10));
            } else if (token.source.equals("if")) {
                AST.Expression<Unit> cond = parse(0);
                context.requireToken("then");
                AST.Expression<Unit> trueBranch = parse(0);
                context.requireToken("else");
                AST.Expression<Unit> falseBranch = parse(0);
                return new AST.IfE<Unit>(token.loc, Unit.Unit, cond, trueBranch, falseBranch);
            } else if (token.source.equals("negate")) {
                ArrayList<AST.Expression<Unit>> argument = new ArrayList<AST.Expression<Unit>>();
                argument.add(parse(10));
                return new AST.Apply<Unit>(token.loc, Unit.Unit, new AST.Variable<Unit>(token.loc, Unit.Unit, "negate"), argument, false);
            } else if (token.source.equals("valueAt")) {
                return new AST.ValueAt<Unit>(token.loc, Unit.Unit, parse(10));
            }
            throw errorUnexpectedToken(token.loc, token.source);
        case Semicolon:
            throw errorUnexpectedToken(token.loc, "a semicolon");
        default:
            throw CompilerError.ice(token.loc, "Nonexhaustive pattern match in rubble.parser.Expression.nullDenotation().");
        }
    }
    
    public AST.Expression<Unit> parseOpenTuple() throws CompilerError {
        ArrayList<AST.Expression<Unit>> result = parseList();
        switch (result.size()) {
        case 0:
            throw ParseContext.errorUnexpected(new Location(context.loc.endRow, context.loc.endColumn), "an expression", "ran out of tokens");
        case 1:
            return result.get(0);
        default:
            return new AST.Tuple<Unit>(result.get(0).loc, Unit.Unit, result);
        }
    }
    
    public AST.Expression<Unit> parseTuple(Location loc, ArrayList<Token> tokens) throws CompilerError {
        ArrayList<AST.Expression<Unit>> result = (new Expression(loc, tokens)).parseListFull(")");
        switch (result.size()) {
        case 0:
            return new AST.Variable<Unit>(loc, Unit.Unit, "()");
        case 1:
            return result.get(0);
        default:
            return new AST.Tuple<Unit>(loc, Unit.Unit, result);
        }
    }
}
