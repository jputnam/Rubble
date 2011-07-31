package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Unit;
import rubble.data.Token;


public final class Binding extends Parser<AST.Binding<Unit>> {
    
    public Binding(ParseContext context) {
        super(context, "a variable binding", ",");
    }
    
    public Binding(Location loc, ArrayList<Token> tokens) {
        super(loc, tokens, "a variable binding", ",");
    }
    
    protected LeftDenotation<AST.Binding<Unit>> leftDenotation(Token token) throws CompilerError {
        return null;
    }
    
    protected AST.Binding<Unit> nullDenotation(Token token) throws CompilerError {
        switch(token.tag) {
        case Identifier:
            context.index -= 1;
            ArrayList<AST.Reference> names = Reference.parse(context);
            if (names.size() == 0) {
                throw ParseContext.errorUnexpected(token.loc, "a variable binding", "did not find one");
            }
            context.requireToken("=");
            return new AST.Binding<Unit>(token.loc, names, (new Expression(context)).parseOpenTuple());
        default: throw errorUnexpectedToken(token.loc, token.source);
        }
    }
}
