package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Types;
import rubble.data.Variable;

/**
 * The parser for variable bindings.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class Binding extends Parser<AST.Binding<String, Types.Parsed>> {
    
    public Binding(ParseContext context) {
        super(context, "a variable binding", ";");
    }
    
    public Binding(Location loc, ArrayList<Token> tokens) {
        super(loc, tokens, "a variable binding", ";");
    }
    
    protected LeftDenotation<AST.Binding<String, Types.Parsed>> leftDenotation(Token token) throws CompilerError {
        return null;
    }
    
    protected AST.Binding<String, Types.Parsed> nullDenotation(Token token) throws CompilerError {
        switch(token.tag) {
        case Identifier:
        case Reserved:
            context.index -= 1;
            ArrayList<Variable<String, Types.Parsed>> names = VariableDeclaration.parse(context);
            if (names.size() == 0) {
                throw ParseContext.errorUnexpected(token.loc, "a variable binding", "did not find one");
            }
            context.requireToken("=");
            AST.Expression<String, Types.Parsed> value = new Expression(context).parseOpenTuple();
            Location loc = new Location(token.loc, value.loc);
            return new AST.Binding<String, Types.Parsed>(loc, names, value);
        default: throw errorUnexpectedToken(token.loc, token.source);
        }
    }
}
