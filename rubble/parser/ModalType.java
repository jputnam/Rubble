package rubble.parser;

import java.util.ArrayList;

import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Types;


public final class ModalType extends Parser<Types.ModalType> {

    public ModalType(ParseContext context) {
        super(context, "a type reference", ",");
    }
    
    public ModalType(Location loc, ArrayList<Token> tokens) {
        super(loc, tokens, "a type reference", ",");
    }
    
    protected LeftDenotation<Types.ModalType> leftDenotation(Token token) throws CompilerError {
        return null;
    }
    
    protected Types.ModalType nullDenotation(Token token) throws CompilerError {
        boolean isMutable = false;
        if (token.source.equals("var")) {
            isMutable = true;
        } else {
            context.index -= 1;
        }
        Types.Type tau = new Type(context).parse(0);
        return new Types.ModalType(isMutable, tau);
    }
}
