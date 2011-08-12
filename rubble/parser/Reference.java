package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Mode;
import rubble.data.Token;
import rubble.data.Types;

/**
 * For lack of a better name, these are called references.  They are what come
 * after the keyword "let" and before the equals sign in a let statement.
 * They are also used in parameter lists.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class Reference {
    
    private static final class NotationParser extends Parser<AST.Reference<String, Types.Parsed>> {
        
        public NotationParser(ParseContext context) {
            super(context, "a variable name", ",");
        }
        
        public NotationParser(Location loc, ArrayList<Token> tokens) {
            super(loc, tokens, "a variable name", ",");
        }
        
        protected LeftDenotation<AST.Reference<String, Types.Parsed>> leftDenotation(Token token) throws CompilerError {
            return null;
        }
        
        protected AST.Reference<String, Types.Parsed> nullDenotation(Token token) throws CompilerError {
            Mode mode = Mode.Immutable;
            if (token.source.equals("var")) {
                mode = Mode.Mutable;
                token = nextToken();
            }
            switch(token.tag) {
            case Identifier:
                Types.Type<String, Types.Parsed> type = Types.UNKNOWN;
                Token t = context.lookahead();
                if (t != null && t.source.equals("asType")) {
                    context.index++;
                    type = new Type(context).parse(0);
                }
                return new AST.Reference<String, Types.Parsed>(token.loc, mode, token.source, type);
            default: throw errorUnexpectedToken(token.loc, token.source);
            }
        }
    }
    
    public static ArrayList<AST.Reference<String, Types.Parsed>> parse(ParseContext context) throws CompilerError {
        ArrayList<AST.Reference<String, Types.Parsed>> result = (new NotationParser(context)).parseList();
        
        if (result.size() == 0) {
            return result;
        }
        
        AST.Reference<String, Types.Parsed> last = result.get(result.size() - 1);
        Types.Type<String, Types.Parsed> declared = last.type;
        
        for (int i = result.size() - 2; i >= 0; i--) {
            AST.Reference<String, Types.Parsed> current = result.get(i);
            if (current.type == Types.UNKNOWN) {
                result.set(i, new AST.Reference<String, Types.Parsed>(current.loc, current.mode, current.name, declared));
            } else {
                declared = current.type;
            }
        }
        return result;
    }
}
