package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Unit;
import rubble.data.Token;
import rubble.data.Types;


public final class Binding extends Parser<AST.Binding<Unit, Types.Parsed, String>> {
    
    public Binding(ParseContext context) {
        super(context, "a variable binding", ";");
    }
    
    public Binding(Location loc, ArrayList<Token> tokens) {
        super(loc, tokens, "a variable binding", ";");
    }
    
    protected LeftDenotation<AST.Binding<Unit, Types.Parsed, String>> leftDenotation(Token token) throws CompilerError {
        return null;
    }
    
    protected AST.Binding<Unit, Types.Parsed, String> nullDenotation(Token token) throws CompilerError {
        switch(token.tag) {
        case Identifier:
        case Reserved:
            context.index -= 1;
            ArrayList<AST.Reference<Types.Parsed, String>> names = Reference.parse(context);
            if (names.size() == 0) {
                throw ParseContext.errorUnexpected(token.loc, "a variable binding", "did not find one");
            }
            context.requireToken("=");
            AST.Expression<Unit, Types.Parsed, String> value = new Expression(context).parseOpenTuple();
            Location loc = new Location(token.loc, value.loc);
            return new AST.Binding<Unit, Types.Parsed, String>(loc, names, value);
        default: throw errorUnexpectedToken(token.loc, token.source);
        }
    }
}
