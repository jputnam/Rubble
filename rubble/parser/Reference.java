package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Types;


public final class Reference<Name, Phase> {
    
    private static final class Notation<Name, Phase> {
        
        final Location loc;
        final Name name;
        final boolean isDeclaredMutable;
        final Types.Type<Name, Phase> declaredType;
        
        public Notation(Location loc, Name name, boolean isDeclaredMutable, Types.Type<Name, Phase> declaredType) {
            this.loc = loc;
            this.name = name;
            this.isDeclaredMutable = isDeclaredMutable;
            this.declaredType = declaredType;
        }
    }
    
    private static final class NotationParser extends Parser<Notation<String, Types.Parsed>> {
        
        public NotationParser(ParseContext context) {
            super(context, "a variable name", ",");
        }
        
        public NotationParser(Location loc, ArrayList<Token> tokens) {
            super(loc, tokens, "a variable name", ",");
        }
        
        protected LeftDenotation<Notation<String, Types.Parsed>> leftDenotation(Token token) throws CompilerError {
            return null;
        }
        
        protected Notation<String, Types.Parsed> nullDenotation(Token token) throws CompilerError {
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
                    Types.Type<String, Types.Parsed> type = new Type(context).parse(0);
                    return new Notation<String, Types.Parsed>(token.loc, token.source, isDeclaredMutable, type);
                }
                return new Notation<String, Types.Parsed>(token.loc, token.source, isDeclaredMutable, null);
            default: throw errorUnexpectedToken(token.loc, token.source);
            }
        }
    }
    
    private static Types.Type<String, Types.Parsed> varVarRule(Location loc, Types.Type<String, Types.Parsed> type, boolean isDeclaredMutable) throws CompilerError {
        if (isDeclaredMutable) {
            if (type.isMutable) {
                throw CompilerError.parse(loc, "A variable may not be both marked as mutable and declared as having a mutable type.");
            } else {
                return type.mutable();
            }
        }
        return type;
    }
    
    public static ArrayList<AST.Reference<String, Types.Parsed>> parse(ParseContext context) throws CompilerError {
        ArrayList<Notation<String, Types.Parsed>> names = (new NotationParser(context)).parseList();
        
        ArrayList<AST.Reference<String, Types.Parsed>> result = new ArrayList<AST.Reference<String, Types.Parsed>>(); 
        if (names.size() == 0) {
            return new ArrayList<AST.Reference<String, Types.Parsed>>();
        }
        
        Notation<String, Types.Parsed> last = names.get(names.size() - 1);
        Types.Type<String, Types.Parsed> declared = last.declaredType;
        if (declared == null) {
            declared = Types.UNKNOWN_IMMUTABLE;
        }
        result.add(0, new AST.Reference<String, Types.Parsed>(last.name, varVarRule(last.loc, declared, last.isDeclaredMutable)));
        
        
        for (int i = names.size() - 2; i >= 0; i--) {
            Notation<String, Types.Parsed> notation = names.get(i);
            if (notation.declaredType == null) {
                Types.Type<String, Types.Parsed> finalType = varVarRule(notation.loc, declared, notation.isDeclaredMutable);
                result.add(0, new AST.Reference<String, Types.Parsed>(notation.name, finalType));
            } else {
                result.add(0, new AST.Reference<String, Types.Parsed>(notation.name, notation.declaredType));
                declared = names.get(i).declaredType;
            }
        }
        return result;
    }
}
