package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Types;
import rubble.data.AST.ExpressionTag;


public final class Expression extends Parser<AST.Expression<Types.Parsed>> {
    
    public Expression(ParseContext context) {
        super(context, "an expression", ",");
    }
    
    public Expression(Location loc, ArrayList<Token> tokens) {
        super(loc, tokens, "an expression", ",");
    }
    
    private LeftDenotation<AST.Expression<Types.Parsed>> application(final AST.Expression<Types.Parsed> ast) throws CompilerError {
        return new LeftDenotation<AST.Expression<Types.Parsed>>() {
            
            public int lbp() { return 11; }
            private final int rbp = 10;
            
            public AST.Expression<Types.Parsed> apply(AST.Expression<Types.Parsed> left) throws CompilerError {
                return new AST.Apply<Types.Parsed>(left.loc, Types.UNKNOWN_NEUTRAL, left, parseLeft(ast, rbp));
            }
        };
    }
    
    private LeftDenotation<AST.Expression<Types.Parsed>> infixExpression(final int precedence, final AST.Expression<Types.Parsed> center) throws CompilerError {
        return new LeftDenotation<AST.Expression<Types.Parsed>>() {
            
            public int lbp() { return precedence; }
            
            public AST.Expression<Types.Parsed> apply(AST.Expression<Types.Parsed> left) throws CompilerError {
                AST.Expression<Types.Parsed> right = parse(precedence);
                switch (center.tag) {
                case Apply:
                    AST.Apply<Types.Parsed> result = (AST.Apply<Types.Parsed>)center;
                    ArrayList<AST.Expression<Types.Parsed>> aArguments;
                    if (result.argument.tag == ExpressionTag.Tuple) {
                        aArguments = ((AST.Tuple<Types.Parsed>)result.argument).es;
                    } else {
                        aArguments = new ArrayList<AST.Expression<Types.Parsed>>();
                        aArguments.add(result.argument);
                    }
                    aArguments.add(left);
                    aArguments.add(right);
                    return new AST.Apply<Types.Parsed>(result.loc, Types.UNKNOWN_NEUTRAL, result.function, new AST.Tuple<Types.Parsed>(result.argument.loc, Types.UNKNOWN_NEUTRAL, aArguments));
                    
                default:
                    ArrayList<AST.Expression<Types.Parsed>> bArguments = new ArrayList<AST.Expression<Types.Parsed>>();
                    bArguments.add(left);
                    bArguments.add(right);
                    return new AST.Apply<Types.Parsed>(center.loc, Types.UNKNOWN_NEUTRAL, center, new AST.Tuple<Types.Parsed>(left.loc, Types.UNKNOWN_NEUTRAL, bArguments));
                }
            }
        };
    }
    
    private LeftDenotation<AST.Expression<Types.Parsed>> infixOperator(final int precedence, Token center) throws CompilerError {
        return infixExpression(precedence, new AST.Variable<Types.Parsed>(center.loc, Types.UNKNOWN_NEUTRAL, center.source));
    }
    
    protected LeftDenotation<AST.Expression<Types.Parsed>> leftDenotation(final Token token) throws CompilerError {
        switch (token.tag) {
        case Block:
            if (token.source.equals("`")) {
                return infixExpression(5, (new Expression(token.loc, token.subtokens)).parseFull("`"));
            } else if (token.source.equals("(")) {
                return application(parseTuple(token.loc, token.subtokens));
            } else if (token.source.equals("[")) {
                return new LeftDenotation<AST.Expression<Types.Parsed>>() {
                    
                    public int lbp() { return 14; }
                    
                    public AST.Expression<Types.Parsed> apply(AST.Expression<Types.Parsed> left) throws CompilerError {
                        return new AST.Index<Types.Parsed>(left.loc, Types.UNKNOWN_NEUTRAL, left, (new Expression(token.loc, token.subtokens)).parseFull("]"));
                    }
                };
            }
            return null;
        case Identifier:
            return application(new AST.Variable<Types.Parsed>(token.loc, Types.UNKNOWN_NEUTRAL, token.source));
        case Number:
            return application(new AST.Number<Types.Parsed>(token.loc, Types.UNKNOWN_NEUTRAL, token.source));
        case Operator:
            if (token.source.equals("+")) {
                return infixOperator(6, token);
            } else if (token.source.equals("-")) {
                return infixOperator(6, token);
            } else if (token.source.equals("*")) {
                return infixOperator(7, token);
            } else if (token.source.equals("/")) {
                return infixOperator(7, token);
            } else if (token.source.equals("<")) {
                return infixOperator(3, token);
            } else if (token.source.equals(">")) {
                return infixOperator(3, token);
            } else if (token.source.equals("<=")) {
                return infixOperator(3, token);
            } else if (token.source.equals(">=")) {
                return infixOperator(3, token);
            } else if (token.source.equals("==")) {
                return infixOperator(2, token);
            } else if (token.source.equals("!=")) {
                return infixOperator(2, token);
            } else if (token.source.equals("&&")) {
                return infixOperator(1, token);
            } else if (token.source.equals("||")) {
                return infixOperator(1, token);
            }
            throw errorUnexpectedToken(token.loc, "an unrecognized operator");
        case Reserved:
            if (token.source.equals("asType")) {
                return new LeftDenotation<AST.Expression<Types.Parsed>>() {
                    
                    public int lbp() { return 12; }
                    
                    public AST.Expression<Types.Parsed> apply(AST.Expression<Types.Parsed> left) throws CompilerError {
                        final Types.Type<Types.Parsed> tau = (new Type(context)).parse(11);
                        return new AST.AsType<Types.Parsed>(left.loc, tau, left);
                    }
                };
            }
            return null;
        default:
            return null;
        }
    }
    
    protected AST.Expression<Types.Parsed> nullDenotation(Token token) throws CompilerError {
        switch (token.tag) {
        case Block:
            if (token.source.equals("(")) {
                return parseTuple(token.loc, token.subtokens);
            } else if (token.source.equals("[")) {
                return new AST.BufferLiteral<Types.Parsed>(token.loc, Types.UNKNOWN_NEUTRAL, (new Expression(token.loc, token.subtokens)).parseListFull("]"));
            } else if (token.source.equals("`")) {
                throw errorUnexpectedToken(token.loc, "a backtick sequence");
            } else {
                throw errorUnexpectedToken(token.loc, "a code block");
            }
        case Comma:
            throw errorUnexpectedToken(token.loc, "a comma");
        case Identifier:
            return new AST.Variable<Types.Parsed>(token.loc, Types.UNKNOWN_NEUTRAL, token.source);
        case Number:
            return new AST.Number<Types.Parsed>(token.loc, Types.UNKNOWN_NEUTRAL, token.source);
        case Operator:
            throw errorUnexpectedToken(token.loc, "an operator");
        case Reserved:
            if (token.source.equals("addressOf")) {
                return new AST.AddressOf<Types.Parsed>(token.loc, Types.UNKNOWN_NEUTRAL, parse(12));
            } else if (token.source.equals("if")) {
                AST.Expression<Types.Parsed> cond = parse(0);
                context.requireToken("then");
                AST.Expression<Types.Parsed> trueBranch = parse(0);
                context.requireToken("else");
                AST.Expression<Types.Parsed> falseBranch = parse(0);
                return new AST.IfE<Types.Parsed>(token.loc, Types.UNKNOWN_NEUTRAL, cond, trueBranch, falseBranch);
            } else if (token.source.equals("negate")) {
                return new AST.Apply<Types.Parsed>(token.loc, Types.UNKNOWN_NEUTRAL, new AST.Variable<Types.Parsed>(token.loc, Types.UNKNOWN_NEUTRAL, "negate"), parse(12));
            } else if (token.source.equals("valueAt")) {
                return new AST.ValueAt<Types.Parsed>(token.loc, Types.UNKNOWN_NEUTRAL, parse(12));
            }
            throw errorUnexpectedToken(token.loc, token.source);
        case Semicolon:
            throw errorUnexpectedToken(token.loc, "a semicolon");
        default:
            throw CompilerError.ice(token.loc, "Nonexhaustive pattern match in rubble.parser.Expression.nullDenotation().");
        }
    }
    
    public AST.Expression<Types.Parsed> parseOpenTuple() throws CompilerError {
        ArrayList<AST.Expression<Types.Parsed>> result = parseList();
        switch (result.size()) {
        case 0:
            throw ParseContext.errorUnexpected(new Location(context.loc.endRow, context.loc.endColumn), "an expression", "ran out of tokens");
        case 1:
            return result.get(0);
        default:
            Location loc = new Location(result.get(0).loc, result.get(result.size() - 1).loc);
            return new AST.Tuple<Types.Parsed>(loc, Types.UNKNOWN_NEUTRAL, result);
        }
    }
    
    public static AST.Expression<Types.Parsed> parseTuple(Location loc, ArrayList<Token> tokens) throws CompilerError {
        ArrayList<AST.Expression<Types.Parsed>> result = (new Expression(loc, tokens)).parseListFull(")");
        switch (result.size()) {
        case 0:
            return new AST.Variable<Types.Parsed>(loc, Types.UNKNOWN_NEUTRAL, "()");
        case 1:
            return result.get(0);
        default:
            return new AST.Tuple<Types.Parsed>(loc, Types.UNKNOWN_NEUTRAL, result);
        }
    }
}
