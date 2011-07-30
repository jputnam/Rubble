package rubble.parser;

import java.util.ArrayList;

import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Types;
import rubble.data.Types.GroundTag;


// FIXME: Don't forget to figure out what to do about buffers of variable size.
public final class Type extends Parser<Types.Type> {
    
    public Type(ParseContext context) {
        super(context, "a type", ",");
    }

    public Type(Location loc, ArrayList<Token> tokens) {
        super(loc, tokens, "a type", ",");
    }

    protected LeftDenotation<Types.Type> leftDenotation(Token token) throws CompilerError {
        return null;
    }

    protected Types.Type nullDenotation(Token token) throws CompilerError {
        switch (token.tag){
        case Block:
            if (!token.source.equals("(")) {
                throw errorUnexpectedToken(token.loc, token.source);
            }
            if (context.isLive() && context.lookahead().source.equals("->")) {
                ArrayList<Types.ModalType> domain = (new ModalType(token.loc, token.subtokens)).parseListFull(")");
                context.index += 1;
                return new Types.Arrow(domain, parse(0));
            } else {
                ArrayList<Types.Type> types = new Type(token.loc, token.subtokens).parseListFull("]");
                switch (types.size()) {
                case 0:
                    return new Types.Ground(GroundTag.Unit);
                case 1:
                    return types.get(0);
                default:
                    return new Types.Tuple(types);
                }
            }
        case Identifier:
            if (token.source.equals("_")) {
                return new Types.TypeVar(-1);
            } else if (token.source.equals("Boolean")) {
                return new Types.Ground(GroundTag.Boolean);
            
            } else if (token.source.equals("Buffer")) {
                Token block = nextToken();
                if (!block.source.equals("[")) {
                    throw ParseContext.errorUnexpected(block.loc, "[", "found " + block.source);
                }
                ModalType parser = new ModalType(block.loc, block.subtokens);
                
                Types.Nat size;
                Token sizeToken = parser.context.nextTokenExpecting("a buffer size");
                switch (sizeToken.tag) {
                case Identifier:
                    size = new Types.NatVar(sizeToken.source);
                    break;
                case Number:
                    if (sizeToken.source.charAt(0) == '-' || sizeToken.source.equals("0")) {
                        throw ParseContext.errorUnexpected(sizeToken.loc, "a positive integer", sizeToken.source);
                    }
                    size = new Types.NatLiteral(Long.parseLong(sizeToken.source));
                default:
                    throw ParseContext.errorUnexpected(sizeToken.loc, "the buffer's size", sizeToken.source);
                }
                parser.context.requireToken(",");
                return new Types.Buffer(size, parser.parseFull("]"));
            
            } else if (token.source.equals("Int8")) {
                return new Types.Ground(GroundTag.Int8);
            } else if (token.source.equals("Int16")) {
                return new Types.Ground(GroundTag.Int16);
            } else if (token.source.equals("Int32")) {
                return new Types.Ground(GroundTag.Int32);
            } else if (token.source.equals("Int64")) {
                return new Types.Ground(GroundTag.Int64);
            
            } else if (token.source.equals("Ptr")) {
                Token block = nextToken();
                if (!block.source.equals("[")) {
                    throw ParseContext.errorUnexpected(block.loc, "[", "found " + block.source);
                }
                return new Types.Ptr((new ModalType(block.loc, block.subtokens)).parseFull("]"));
            
            } else if (token.source.equals("UInt8")) {
                return new Types.Ground(GroundTag.UInt8);
            } else if (token.source.equals("UInt16")) {
                return new Types.Ground(GroundTag.UInt16);
            } else if (token.source.equals("UInt32")) {
                return new Types.Ground(GroundTag.UInt32);
            } else if (token.source.equals("UInt64")) {
                return new Types.Ground(GroundTag.UInt64);
            
            } else {
                throw errorUnexpectedToken(token.loc, token.source);
            }
        default: throw errorUnexpectedToken(token.loc, token.source);
        }
    }
}
