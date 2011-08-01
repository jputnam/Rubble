package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Types;
import rubble.data.Unit;
import rubble.data.AST.ExpressionTag;


public final class Expression extends Parser<AST.Expression<Unit, Types.Parsed, String>> {
    
    public Expression(ParseContext context) {
        super(context, "an expression", ",");
    }
    
    public Expression(Location loc, ArrayList<Token> tokens) {
        super(loc, tokens, "an expression", ",");
    }
    
    private LeftDenotation<AST.Expression<Unit, Types.Parsed, String>> application(final AST.Expression<Unit, Types.Parsed, String> ast) throws CompilerError {
        return new LeftDenotation<AST.Expression<Unit, Types.Parsed, String>>() {
            
            public int lbp() { return 11; }
            private final int rbp = 10;
            
            public AST.Expression<Unit, Types.Parsed, String> apply(AST.Expression<Unit, Types.Parsed, String> left) throws CompilerError {
                return new AST.Apply<Unit, Types.Parsed, String>(left.loc, Unit.Unit, left, parseLeft(ast, rbp));
            }
        };
    }
    
    private LeftDenotation<AST.Expression<Unit, Types.Parsed, String>> infixExpression(final int precedence, final AST.Expression<Unit, Types.Parsed, String> center) throws CompilerError {
        return new LeftDenotation<AST.Expression<Unit, Types.Parsed, String>>() {
            
            public int lbp() { return precedence; }
            
            public AST.Expression<Unit, Types.Parsed, String> apply(AST.Expression<Unit, Types.Parsed, String> left) throws CompilerError {
                AST.Expression<Unit, Types.Parsed, String> right = parse(precedence);
                switch (center.tag) {
                case Apply:
                    AST.Apply<Unit, Types.Parsed, String> result = (AST.Apply<Unit, Types.Parsed, String>)center;
                    ArrayList<AST.Expression<Unit, Types.Parsed, String>> aArguments;
                    if (result.argument.tag == ExpressionTag.Tuple) {
                        aArguments = ((AST.Tuple<Unit, Types.Parsed, String>)result.argument).es;
                    } else {
                        aArguments = new ArrayList<AST.Expression<Unit, Types.Parsed, String>>();
                        aArguments.add(result.argument);
                    }
                    aArguments.add(left);
                    aArguments.add(right);
                    return new AST.Apply<Unit, Types.Parsed, String>(result.loc, Unit.Unit, result.function, new AST.Tuple<Unit, Types.Parsed, String>(result.argument.loc, Unit.Unit, aArguments));
                    
                default:
                    ArrayList<AST.Expression<Unit, Types.Parsed, String>> bArguments = new ArrayList<AST.Expression<Unit, Types.Parsed, String>>();
                    bArguments.add(left);
                    bArguments.add(right);
                    return new AST.Apply<Unit, Types.Parsed, String>(center.loc, Unit.Unit, center, new AST.Tuple<Unit, Types.Parsed, String>(left.loc, Unit.Unit, bArguments));
                }
            }
        };
    }
    
    private LeftDenotation<AST.Expression<Unit, Types.Parsed, String>> infixOperator(final int precedence, Token center) throws CompilerError {
        return infixExpression(precedence, new AST.Variable<Unit, Types.Parsed, String>(center.loc, Unit.Unit, center.source));
    }
    
    protected LeftDenotation<AST.Expression<Unit, Types.Parsed, String>> leftDenotation(final Token token) throws CompilerError {
        switch (token.tag) {
        case Block:
            if (token.source.equals("`")) {
                return infixExpression(5, (new Expression(token.loc, token.subtokens)).parseFull("`"));
            } else if (token.source.equals("(")) {
                return application(parseTuple(token.loc, token.subtokens));
            } else if (token.source.equals("[")) {
                return new LeftDenotation<AST.Expression<Unit, Types.Parsed, String>>() {
                    
                    public int lbp() { return 14; }
                    
                    public AST.Expression<Unit, Types.Parsed, String> apply(AST.Expression<Unit, Types.Parsed, String> left) throws CompilerError {
                        return new AST.Index<Unit, Types.Parsed, String>(left.loc, Unit.Unit, left, (new Expression(token.loc, token.subtokens)).parseFull("]"));
                    }
                };
            }
            return null;
        case Identifier:
            return application(new AST.Variable<Unit, Types.Parsed, String>(token.loc, Unit.Unit, token.source));
        case Number:
            return application(new AST.Number<Unit, Types.Parsed, String>(token.loc, Unit.Unit, token.source));
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
                return new LeftDenotation<AST.Expression<Unit, Types.Parsed, String>>() {
                    
                    public int lbp() { return 12; }
                    
                    public AST.Expression<Unit, Types.Parsed, String> apply(AST.Expression<Unit, Types.Parsed, String> left) throws CompilerError {
                        final Types.Type<Types.Parsed> tau = (new Type(context)).parse(11);
                        return new AST.AsType<Unit, Types.Parsed, String>(left.loc, Unit.Unit, left, tau);
                    }
                };
            }
            return null;
        default:
            return null;
        }
    }
    
    protected AST.Expression<Unit, Types.Parsed, String> nullDenotation(Token token) throws CompilerError {
        switch (token.tag) {
        case Block:
            if (token.source.equals("(")) {
                return parseTuple(token.loc, token.subtokens);
            } else if (token.source.equals("[")) {
                return new AST.BufferLiteral<Unit, Types.Parsed, String>(token.loc, Unit.Unit, (new Expression(token.loc, token.subtokens)).parseListFull("]"));
            } else if (token.source.equals("`")) {
                throw errorUnexpectedToken(token.loc, "a backtick sequence");
            } else {
                throw errorUnexpectedToken(token.loc, "a code block");
            }
        case Comma:
            throw errorUnexpectedToken(token.loc, "a comma");
        case Identifier:
            return new AST.Variable<Unit, Types.Parsed, String>(token.loc, Unit.Unit, token.source);
        case Number:
            return new AST.Number<Unit, Types.Parsed, String>(token.loc, Unit.Unit, token.source);
        case Operator:
            throw errorUnexpectedToken(token.loc, "an operator");
        case Reserved:
            if (token.source.equals("addressOf")) {
                return new AST.AddressOf<Unit, Types.Parsed, String>(token.loc, Unit.Unit, parse(12));
            } else if (token.source.equals("if")) {
                AST.Expression<Unit, Types.Parsed, String> cond = parse(0);
                context.requireToken("then");
                AST.Expression<Unit, Types.Parsed, String> trueBranch = parse(0);
                context.requireToken("else");
                AST.Expression<Unit, Types.Parsed, String> falseBranch = parse(0);
                return new AST.IfE<Unit, Types.Parsed, String>(token.loc, Unit.Unit, cond, trueBranch, falseBranch);
            } else if (token.source.equals("negate")) {
                return new AST.Apply<Unit, Types.Parsed, String>(token.loc, Unit.Unit, new AST.Variable<Unit, Types.Parsed, String>(token.loc, Unit.Unit, "negate"), parse(12));
            } else if (token.source.equals("valueAt")) {
                return new AST.ValueAt<Unit, Types.Parsed, String>(token.loc, Unit.Unit, parse(12));
            }
            throw errorUnexpectedToken(token.loc, token.source);
        case Semicolon:
            throw errorUnexpectedToken(token.loc, "a semicolon");
        default:
            throw CompilerError.ice(token.loc, "Nonexhaustive pattern match in rubble.parser.Expression.nullDenotation().");
        }
    }
    
    public AST.Expression<Unit, Types.Parsed, String> parseOpenTuple() throws CompilerError {
        ArrayList<AST.Expression<Unit, Types.Parsed, String>> result = parseList();
        switch (result.size()) {
        case 0:
            throw ParseContext.errorUnexpected(new Location(context.loc.endRow, context.loc.endColumn), "an expression", "ran out of tokens");
        case 1:
            return result.get(0);
        default:
            Location loc = new Location(result.get(0).loc, result.get(result.size() - 1).loc);
            return new AST.Tuple<Unit, Types.Parsed, String>(loc, Unit.Unit, result);
        }
    }
    
    public static AST.Expression<Unit, Types.Parsed, String> parseTuple(Location loc, ArrayList<Token> tokens) throws CompilerError {
        ArrayList<AST.Expression<Unit, Types.Parsed, String>> result = (new Expression(loc, tokens)).parseListFull(")");
        switch (result.size()) {
        case 0:
            return new AST.Variable<Unit, Types.Parsed, String>(loc, Unit.Unit, "()");
        case 1:
            return result.get(0);
        default:
            return new AST.Tuple<Unit, Types.Parsed, String>(loc, Unit.Unit, result);
        }
    }
}
