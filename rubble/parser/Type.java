package rubble.parser;

import java.util.ArrayList;

import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Mode;
import rubble.data.Token;
import rubble.data.Types;
import rubble.data.Types.GroundTag;
import rubble.data.Variable;

/**
 * The type parser.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class Type extends Parser<Types.Type<String, Types.Parsed>> {
    
    public Type(ParseContext context) {
        super(context, "a type", ",");
    }

    public Type(Location loc, ArrayList<Token> tokens) {
        super(loc, tokens, "a type", ",");
    }
    
    private Types.Type<String, Types.Parsed> groundType(Types.GroundTag tag) {
        return new Types.Known<String, Types.Parsed>(new Types.Ground(tag));
    }
    
    protected LeftDenotation<Types.Type<String, Types.Parsed>> leftDenotation(Token token) throws CompilerError {
        return null;
    }
    
    protected Types.Type<String, Types.Parsed> nullDenotation(Token token) throws CompilerError {
        switch (token.tag){
        case Block:
            if (!token.source.equals("(")) {
                throw errorUnexpectedToken(token.loc, token.source);
            }
            ArrayList<Variable<String, Types.Parsed>> domainList = VariableDeclaration.parseTypes(context);
            if (context.isLive() && context.lookahead().source.equals("->")) {
                context.index++;
                return new Types.Arrow<String, Types.Parsed>(domainList, parse(0));
            }
            
            switch (domainList.size()) {
            case 0:
                return groundType(GroundTag.Unit);
            case 1:
            	Variable<String, Types.Parsed> var = domainList.get(0);
            	if (var.name.equals("") && var.mode == Mode.Const) {
            		return var.type;
            	}
            	// Intentional fallthrough.
            default:
                return new Types.Tuple<String, Types.Parsed>(domainList);
            }
        case Identifier:
            if (token.source.equals("_")) {
                return Types.UNKNOWN;
            } else if (token.source.equals("Boolean")) {
                return groundType(GroundTag.Boolean);
            
            } else if (token.source.equals("Buffer")) {
                Token block = nextToken();
                if (!block.source.equals("[")) {
                    throw ParseContext.errorUnexpected(block.loc, "[", "found " + block.source);
                }
                Type parser = new Type(block.loc, block.subtokens);
                
                Types.Nat<String, Types.Parsed> size;
                Token sizeToken = parser.context.nextTokenExpecting("a buffer size");
                switch (sizeToken.tag) {
                case Identifier:
                    if (sizeToken.source.equals("_")) {
                        size = new Types.NatUnknown();
                    } else {
                        size = new Types.NatExternal<String, Types.Parsed>(sizeToken.loc, sizeToken.source);
                    }
                    break;
                case Number:
                    if (sizeToken.source.charAt(0) == '-' || sizeToken.source.equals("0")) {
                        throw ParseContext.errorUnexpected(sizeToken.loc, "a positive integer", "found " + sizeToken.source);
                    }
                    size = new Types.NatKnown<String, Types.Parsed>(new Types.NatLiteral(Long.parseLong(sizeToken.source)));
                    break;
                default:
                    throw ParseContext.errorUnexpected(sizeToken.loc, "the buffer's size", "found " + sizeToken.source);
                }
                parser.context.requireToken(",");
                
                Mode mode = Mode.Const;
                if ("var".equals(parser.context.lookahead())) {
                    parser.context.index++;
                    mode = Mode.Var;
                }
                return new Types.Buffer<String, Types.Parsed>(size, mode, parser.parseFull("]"));
            
            } else if (token.source.equals("Int8")) {
                return groundType(GroundTag.Int8);
            } else if (token.source.equals("Int16")) {
                return groundType(GroundTag.Int16);
            } else if (token.source.equals("Int32")) {
                return groundType(GroundTag.Int32);
            } else if (token.source.equals("Int64")) {
                return groundType(GroundTag.Int64);
            
            } else if (token.source.equals("Ptr")) {
                Token block = nextToken();
                if (!block.source.equals("[")) {
                    throw ParseContext.errorUnexpected(block.loc, "[", "found " + block.source);
                }
                Type parser = new Type(block.loc, block.subtokens);
                
                Mode mode = Mode.Const;
                if ("var".equals(parser.context.lookahead())) {
                    parser.context.index++;
                    mode = Mode.Var;
                }
                return new Types.Ptr<String, Types.Parsed>(mode, parser.parseFull("]"));
            
            } else if (token.source.equals("UInt8")) {
                return groundType(GroundTag.UInt8);
            } else if (token.source.equals("UInt16")) {
                return groundType(GroundTag.UInt16);
            } else if (token.source.equals("UInt32")) {
                return groundType(GroundTag.UInt32);
            } else if (token.source.equals("UInt64")) {
                return groundType(GroundTag.UInt64);
            
            } else {
                throw errorUnexpectedToken(token.loc, token.source);
            }
        default: throw errorUnexpectedToken(token.loc, token.source);
        }
    }
}
