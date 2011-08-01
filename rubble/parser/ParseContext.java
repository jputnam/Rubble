package rubble.parser;

import java.util.ArrayList;

import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;


public final class ParseContext {
    
    public final Location loc;
    public final ArrayList<Token> tokens;
    public int index;
    
    public ParseContext(Location loc, ArrayList<Token> tokens) {
        this.loc = loc;
        this.tokens = tokens;
        this.index = 0;
    }
    
    public static CompilerError errorUnexpected(Location loc, String expected, String message) {
        return CompilerError.parse(loc, "The parser expected " + expected + " but " + message + ".");
    }
    
    public ParseContext inBraces() throws CompilerError {
        Token t = nextTokenExpecting("{");
        if (t.source.equals("{") || (t.source.equals(Token.IMPLICIT_BRACE))) {
            return new ParseContext(t.loc, t.subtokens);
        }
        throw errorUnexpected(t.loc, "{", t.source);
    }
    
    public boolean isLive() {
        return index < tokens.size();
    }
    
    public Token lookahead() {
        return (isLive()) ? tokens.get(index) : null;
    }
    
    public Token nextTokenExpecting(String expected) throws CompilerError {
        if (!isLive()) {
            throw errorUnexpected(loc, expected, "ran out of input");
        }
        index++;
        return tokens.get(index - 1);
    }
    
    public void requireToken(String expected) throws CompilerError {
        Token t = nextTokenExpecting(expected);
        if (!t.source.equals(expected)) { throw errorUnexpected(t.loc, expected, "found " + t.source); }
    }
}
