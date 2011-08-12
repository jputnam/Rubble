package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Types;

/**
 * The parser for declarations.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class Declaration extends Parser<AST.Declaration<Types.Parsed>> {
    
    public Declaration(ParseContext context) {
        super(context, "a declaration", ";");
    }

    public Declaration(Location loc, ArrayList<Token> tokens) {
        super(new ParseContext(loc, tokens), "a declaration", ";");
    }

    protected LeftDenotation<AST.Declaration<Types.Parsed>> leftDenotation(Token token) throws CompilerError {
        return null;
    }

    protected AST.Declaration<Types.Parsed> nullDenotation(Token token) throws CompilerError {
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
            ArrayList<AST.Reference<Types.Parsed>> arguments = Reference.parse(new ParseContext(argumentToken.loc, argumentToken.subtokens));
            
            // Function types always have at least one argument, so empty
            // argument lists are special cases to implicitly have a Unit
            // typed argument with an unreachable name.
            if (arguments.size() == 0) {
                arguments.add(new AST.Reference<Types.Parsed>("# Implicit argument", new Types.Known<Types.Parsed>(new Types.Ground(Types.GroundTag.Unit, false))));
            }
            
            
            // The return type
            Types.Type<Types.Parsed> returnType = (new Type(context)).parse(0);
            
            // The body
            Token bodyLookahead = context.lookahead();
            ArrayList<AST.Statement<Types.Parsed>> body = (new Statement(context.inBraces())).parseListFull("}");
            
            Location defLoc = new Location(token.loc, bodyLookahead.loc);
            return new AST.Def<Types.Parsed>(defLoc, name.source, arguments, returnType, body);
            
        } if (token.source.equals("let")) {
            AST.Let<Types.Parsed> let = (new Statement(context)).parseLet(token.loc);
            return new AST.GlobalLet<Types.Parsed>(let.loc, let.bindings);
        }
        throw errorUnexpectedToken(token.loc, token.source);
    }

}
