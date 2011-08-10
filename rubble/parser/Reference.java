package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Types;


public final class Reference<Phase> {
    
    private static final class Notation<Phase> {
        
        final Location loc;
        final String source;
        final boolean isDeclaredMutable;
        final Types.Type<Phase> declaredType;
        
        public Notation(Location loc, boolean isDeclaredMutable, String source, Types.Type<Phase> declaredType) {
            this.loc = loc;
            this.isDeclaredMutable = isDeclaredMutable;
            this.source = source;
            this.declaredType = declaredType;
        }
    }
    
    private static final class NotationParser extends Parser<Notation<Types.Parsed>> {
        
        public NotationParser(ParseContext context) {
            super(context, "a variable name", ",");
        }
        
        public NotationParser(Location loc, ArrayList<Token> tokens) {
            super(loc, tokens, "a variable name", ",");
        }
        
        protected LeftDenotation<Notation<Types.Parsed>> leftDenotation(Token token) throws CompilerError {
            return null;
        }
        
        protected Notation<Types.Parsed> nullDenotation(Token token) throws CompilerError {
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
                    return new Notation<Types.Parsed>(token.loc, isDeclaredMutable, token.source, type);
                }
                return new Notation<Types.Parsed>(token.loc, isDeclaredMutable, token.source, null);
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
        ArrayList<Notation<Types.Parsed>> names = (new NotationParser(context)).parseList();
        
        ArrayList<AST.Reference<Types.Parsed>> result = new ArrayList<AST.Reference<Types.Parsed>>(); 
        if (names.size() == 0) {
            return new ArrayList<AST.Reference<Types.Parsed>>();
        }
        
        Notation<Types.Parsed> last = names.get(names.size() - 1);
        Types.Type<Types.Parsed> declared = last.declaredType;
        if (declared == null) {
            declared = Types.UNKNOWN_IMMUTABLE;
        }
        result.add(0, new AST.Reference<Types.Parsed>(last.source, varVarRule(last.loc, declared, last.isDeclaredMutable)));
        
        
        for (int i = names.size() - 2; i >= 0; i--) {
            Notation<Types.Parsed> notation = names.get(i);
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
