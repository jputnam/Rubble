package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
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
    
    private static final class Notation {
        
        final Location loc;
        final String source;
        final boolean isDeclaredMutable;
        final Types.Type<Types.Parsed> declaredType;
        
        public Notation(Location loc, boolean isDeclaredMutable, String source, Types.Type<Types.Parsed> declaredType) {
            this.loc = loc;
            this.isDeclaredMutable = isDeclaredMutable;
            this.source = source;
            this.declaredType = declaredType;
        }
    }
    
    private static final class NotationParser extends Parser<Notation> {
        
        public NotationParser(ParseContext context) {
            super(context, "a variable name", ",");
        }
        
        public NotationParser(Location loc, ArrayList<Token> tokens) {
            super(loc, tokens, "a variable name", ",");
        }
        
        protected LeftDenotation<Notation> leftDenotation(Token token) throws CompilerError {
            return null;
        }
        
        protected Notation nullDenotation(Token token) throws CompilerError {
            boolean isDeclaredMutable = false;
            if (token.source.equals("var")) {
                isDeclaredMutable = true;
                token = nextToken();
            }
            switch(token.tag) {
            case Identifier:
                Token t = context.lookahead();
                if (t != null && t.source.equals("asType")) {
                    context.index++;
                    Types.Type<Types.Parsed> type = new Type(context).parse(0);
                    return new Notation(token.loc, isDeclaredMutable, token.source, type);
                }
                return new Notation(token.loc, isDeclaredMutable, token.source, null);
            default: throw errorUnexpectedToken(token.loc, token.source);
            }
        }
    }
    
    private static Types.Type<Types.Parsed> varVarRule(Location loc, Types.Type<Types.Parsed> type, boolean isDeclaredMutable) throws CompilerError {
        if (isDeclaredMutable) {
            if (type.isMutable) {
                throw CompilerError.parse(loc, "A variable may not be both marked as mutable and declared as having a mutable type.");
            } else {
                return type.mutable();
            }
        }
        return type;
    }
    
    public static ArrayList<AST.Reference<Types.Parsed>> parse(ParseContext context) throws CompilerError {
        ArrayList<Notation> names = (new NotationParser(context)).parseList();
        
        ArrayList<AST.Reference<Types.Parsed>> result = new ArrayList<AST.Reference<Types.Parsed>>(); 
        if (names.size() == 0) {
            return new ArrayList<AST.Reference<Types.Parsed>>();
        }
        
        Notation last = names.get(names.size() - 1);
        Types.Type<Types.Parsed> declared = last.declaredType;
        if (declared == null) {
            declared = Types.UNKNOWN_IMMUTABLE;
        }
        result.add(0, new AST.Reference<Types.Parsed>(last.source, varVarRule(last.loc, declared, last.isDeclaredMutable)));
        
        
        for (int i = names.size() - 2; i >= 0; i--) {
            Notation notation = names.get(i);
            if (notation.declaredType == null) {
                Types.Type<Types.Parsed> finalType = varVarRule(notation.loc, declared, notation.isDeclaredMutable);
                result.add(0, new AST.Reference<Types.Parsed>(notation.source, finalType));
            } else {
                result.add(0, new AST.Reference<Types.Parsed>(notation.source, notation.declaredType));
                declared = names.get(i).declaredType;
            }
        }
        return result;
    }
}
