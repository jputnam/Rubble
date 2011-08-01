package rubble.parser;

import java.util.ArrayList;

import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Types;
import rubble.data.Types.GroundTag;
import rubble.data.Types.Parsed;


public final class Type extends Parser<Types.Type<Parsed>> {
    
    public Type(ParseContext context) {
        super(context, "a type", ",");
    }

    public Type(Location loc, ArrayList<Token> tokens) {
        super(loc, tokens, "a type", ",");
    }
    
    private Types.Type<Types.Parsed> groundType(Types.GroundTag tag) {
        return new Types.Known<Types.Parsed>(new Types.Ground(tag, false));
    }
    
    protected LeftDenotation<Types.Type<Parsed>> leftDenotation(Token token) throws CompilerError {
        return null;
    }

    protected Types.Type<Parsed> nullDenotation(Token token) throws CompilerError {
        switch (token.tag){
        case Block:
            if (!token.source.equals("(")) {
                throw errorUnexpectedToken(token.loc, token.source);
            }
            ArrayList<Types.Type<Parsed>> domainList = (new Type(token.loc, token.subtokens)).parseListFull(")");
            Types.Type<Parsed> domain;
            
            switch (domainList.size()) {
            case 0:
                domain = groundType(GroundTag.Unit);
                break;
            case 1:
                domain = domainList.get(0);
                break;
            default:
                domain = new Types.Tuple<Parsed>(domainList, false);
            }
            
            if (context.isLive() && context.lookahead().source.equals("->")) {
                context.index++;
                return new Types.Arrow<Parsed>(domain, parse(0), false);
            } else {
                return domain;
            }
        case Identifier:
            if (token.source.equals("_")) {
                return new Types.Unknown(false);
            } else if (token.source.equals("Boolean")) {
                return groundType(GroundTag.Boolean);
            
            } else if (token.source.equals("Buffer")) {
                Token block = nextToken();
                if (!block.source.equals("[")) {
                    throw ParseContext.errorUnexpected(block.loc, "[", "found " + block.source);
                }
                Type parser = new Type(block.loc, block.subtokens);
                
                Types.Nat<Parsed, String> size;
                Token sizeToken = parser.context.nextTokenExpecting("a buffer size");
                switch (sizeToken.tag) {
                case Identifier:
                    if (sizeToken.source.equals("_")) {
                        size = new Types.NatUnknown();
                    } else {
                        size = new Types.NatExternal<Parsed, String>(sizeToken.source);
                    }
                    break;
                case Number:
                    if (sizeToken.source.charAt(0) == '-' || sizeToken.source.equals("0")) {
                        throw ParseContext.errorUnexpected(sizeToken.loc, "a positive integer", "found " + sizeToken.source);
                    }
                    size = new Types.NatKnown<Parsed, String>(new Types.NatLiteral(Long.parseLong(sizeToken.source)));
                    break;
                default:
                    throw ParseContext.errorUnexpected(sizeToken.loc, "the buffer's size", "found " + sizeToken.source);
                }
                parser.context.requireToken(",");
                return new Types.Buffer<Parsed, String>(size, parser.parseFull("]"), false);
            
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
                return new Types.Ptr<Parsed>((new Type(block.loc, block.subtokens)).parseFull("]"), false);
            
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
        case Reserved:
            if (token.source.equals("var")) {
                Types.Type<Parsed> t = parse(5);
                return t.mutable();
            }
            throw errorUnexpectedToken(token.loc, token.source);
        default: throw errorUnexpectedToken(token.loc, token.source);
        }
    }
}
