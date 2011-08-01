package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Types;
import rubble.data.Unit;


public final class Declaration extends Parser<AST.Declaration<Unit, Types.Parsed, String>> {
    
    public Declaration(ParseContext context) {
        super(context, "a declaration", ";");
    }

    public Declaration(Location loc, ArrayList<Token> tokens) {
        super(new ParseContext(loc, tokens), "a declaration", ";");
    }

    protected LeftDenotation<AST.Declaration<Unit, Types.Parsed, String>> leftDenotation(Token token) throws CompilerError {
        return null;
    }

    protected AST.Declaration<Unit, Types.Parsed, String> nullDenotation(Token token) throws CompilerError {
        if (token.source.equals("def")) {
            // The function name
            Token name = nextToken();
            if (name.tag != Token.Tag.Identifier) {
                throw ParseContext.errorUnexpected(name.loc, "a function name", "found " + name.source);
            }
            
            // The arguments
            Token argumentToken = nextToken();
            if (!argumentToken.source.equals("(")) {
                throw ParseContext.errorUnexpected(argumentToken.loc, "an argument list", "found " + argumentToken.source);
            }
            ArrayList<AST.Reference<Types.Parsed, String>> arguments = Reference.parse(new ParseContext(argumentToken.loc, argumentToken.subtokens));
            
            // The return type
            Types.Type<Types.Parsed> returnType = (new Type(context)).parse(0);
            
            // The body
            Token bodyLookahead = context.lookahead();
            ArrayList<AST.Statement<Unit, Types.Parsed, String>> body = (new Statement(context.inBraces())).parseListFull("}");
            
            Location defLoc = new Location(token.loc, bodyLookahead.loc);
            return new AST.Def<Unit, Types.Parsed, String>(defLoc, name.source, arguments, returnType, body);
            
        } if (token.source.equals("let")) {
            AST.Let<Unit, Types.Parsed, String> let = (new Statement(context)).parseLet(token.loc);
            return new AST.GlobalLet<Unit, Types.Parsed, String>(let.loc, let.bindings);
        }
        throw errorUnexpectedToken(token.loc, token.source);
    }

}
