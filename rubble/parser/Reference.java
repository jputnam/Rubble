package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Types;


public final class Reference<Phase, N> {
    
    private static final class Notation<Phase, N> {
        
        final Location loc;
        final N name;
        final boolean isDeclaredMutable;
        final Types.Type<Phase> declaredType;
        
        public Notation(Location loc, N name, boolean isDeclaredMutable, Types.Type<Phase> declaredType) {
            this.loc = loc;
            this.name = name;
            this.isDeclaredMutable = isDeclaredMutable;
            this.declaredType = declaredType;
        }
    }
    
    private static final class NotationParser extends Parser<Notation<Types.Parsed, String>> {
        
        public NotationParser(ParseContext context) {
            super(context, "a variable name", ",");
        }
        
        public NotationParser(Location loc, ArrayList<Token> tokens) {
            super(loc, tokens, "a variable name", ",");
        }
        
        protected LeftDenotation<Notation<Types.Parsed, String>> leftDenotation(Token token) throws CompilerError {
            return null;
        }
        
        protected Notation<Types.Parsed, String> nullDenotation(Token token) throws CompilerError {
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
                    return new Notation<Types.Parsed, String>(token.loc, token.source, isDeclaredMutable, type);
                }
                return new Notation<Types.Parsed, String>(token.loc, token.source, isDeclaredMutable, null);
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
    
    public static ArrayList<AST.Reference<Types.Parsed, String>> parse(ParseContext context) throws CompilerError {
        ArrayList<Notation<Types.Parsed, String>> names = (new NotationParser(context)).parseList();
        
        ArrayList<AST.Reference<Types.Parsed, String>> result = new ArrayList<AST.Reference<Types.Parsed, String>>(); 
        if (names.size() == 0) {
            return new ArrayList<AST.Reference<Types.Parsed, String>>();
        }
        
        Notation<Types.Parsed, String> last = names.get(names.size() - 1);
        Types.Type<Types.Parsed> declared = last.declaredType;
        if (declared == null) {
            declared = new Types.Unknown(false);
        }
        result.add(0, new AST.Reference<Types.Parsed, String>(last.name, varVarRule(last.loc, declared, last.isDeclaredMutable)));
        
        
        for (int i = names.size() - 2; i >= 0; i--) {
            Notation<Types.Parsed, String> notation = names.get(i);
            if (notation.declaredType == null) {
                Types.Type<Types.Parsed> finalType = varVarRule(notation.loc, declared, notation.isDeclaredMutable);
                result.add(0, new AST.Reference<Types.Parsed, String>(notation.name, finalType));
            } else {
                result.add(0, new AST.Reference<Types.Parsed, String>(notation.name, notation.declaredType));
                declared = names.get(i).declaredType;
            }
        }
        return result;
    }
}
