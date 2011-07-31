package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Types;


public final class Reference {
    
    private static final class Notation {
        
        final Location loc;
        final String name;
        final boolean isDeclaredMutable;
        final Types.Type declaredType;
        
        public Notation(Location loc, String name, boolean isDeclaredMutable, Types.Type declaredType) {
            this.loc = loc;
            this.name = name;
            this.isDeclaredMutable = isDeclaredMutable;
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
                    context.index += 1;
                    Types.Type type = varVarRule(token.loc, (new Type(context)).parse(0), isDeclaredMutable);
                    return new Notation(token.loc, token.source, isDeclaredMutable || type.isMutable, type);
                }
                return new Notation(token.loc, token.source, isDeclaredMutable, null);
            default: throw errorUnexpectedToken(token.loc, token.source);
            }
        }
    }
    
    private static Types.Type varVarRule(Location loc, Types.Type type, boolean isDeclaredMutable) throws CompilerError {
        if (isDeclaredMutable) {
            if (type.isMutable) {
                throw CompilerError.parse(loc, "A variable may not be both marked as mutable and declared as having a mutable type.");
            } else {
                return type.mutable();
            }
        }
        return type;
    }
    
    public static ArrayList<AST.Reference> parse(ParseContext context) throws CompilerError {
        ArrayList<Notation> names = (new NotationParser(context)).parseList();
        
        ArrayList<AST.Reference> result = new ArrayList<AST.Reference>(); 
        if (names.size() == 0) {
            return new ArrayList<AST.Reference>();
        }
        
        Types.Type declared = names.get(names.size() - 1).declaredType;
        if (declared == null) {
            declared = new Types.TypeVar(-1, false);
        }
        for (int i = 0; i < names.size() - 1; i++) {
            Notation notation = names.get(i);
            if (notation.declaredType == null) {
                Types.Type finalType = varVarRule(notation.loc, declared, notation.isDeclaredMutable);
                result.add(0, new AST.Reference(notation.name, finalType));
            } else {
                result.add(0, new AST.Reference(notation.name, notation.declaredType));
                declared = names.get(i).declaredType;
            }
        }
        return result;
    }
}
