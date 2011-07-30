package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Types;



public final class Reference extends Parser<AST.Reference> {

    public Reference(ParseContext context) {
        super(context, "a variable name", ",");
    }
    
    public Reference(Location loc, ArrayList<Token> tokens) {
        super(loc, tokens, "a variable name", ",");
    }
    
    protected LeftDenotation<AST.Reference> leftDenotation(Token token) throws CompilerError {
        return null;
    }
    
    protected AST.Reference nullDenotation(Token token) throws CompilerError {
        switch(token.tag) {
        case Identifier:
            Token t = context.lookahead();
            if (t != null && t.source.equals("asType")) {
                context.index += 1;
                return new AST.Reference(token.source, (new ModalType(context)).parse(0));
            }
            return new AST.Reference(token.source, null);
        default: throw errorUnexpectedToken(token.loc, token.source);
        }
    }
    
    public static ArrayList<AST.Reference> propagateTypes(ArrayList<AST.Reference> names) {
        if (names.size() == 0) { return names; }
        
        Types.ModalType declared = names.get(names.size() - 1).modalType;
        if (declared == null) {
            declared = new Types.ModalType(false, new Types.TypeVar(-1));
        }
        for (int i = 0; i < names.size() - 1; i++) {
            if (names.get(i).modalType == null) {
                names.set(i, new AST.Reference(names.get(i).name, declared));
            } else {
                declared = names.get(i).modalType;
            }
        }
        return names;
    }
}
