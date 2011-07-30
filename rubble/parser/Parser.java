package rubble.parser;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Unit;
import rubble.data.Location;
import rubble.data.Token;


public abstract class Parser<T> {
    
    public static abstract class LeftDenotation<A> {
        
        public int lbp = 0;
        public abstract A apply(A ast) throws CompilerError;
    }
    
    public final ParseContext context;
    public final String name;
    public final String separator;
    
    public Parser(ParseContext context, String name, String separator) {
        this.context = context;
        this.name = name;
        this.separator = separator;
    }
    
    public Parser(Location loc, ArrayList<Token> tokens, String name, String separator) {
        this.context = new ParseContext(loc, tokens);
        this.name = name;
        this.separator = separator;
    }
    
    
    public final CompilerError errorUnexpectedToken(Location loc, String source) {
        return ParseContext.errorUnexpected(loc, name, "found " + source);
    }
    
    protected final ParseContext inBraces() throws CompilerError {
        Token t = nextToken();
        switch(t.tag) {
        case Block:
            if (t.source.equals("{") || t.source.equals(Token.IMPLICIT_BRACE)) {
                return new ParseContext(t.loc, t.subtokens);
            }
            // Intentional fallthrough.
        default:
            throw ParseContext.errorUnexpected(t.loc, "{", "found " + t.source);
        }
    }
    
    protected final ParseContext inParens() throws CompilerError {
        Token t = nextToken();
        switch(t.tag) {
        case Block:
            if (t.source.equals("(")) {
                return new ParseContext(t.loc, t.subtokens);
            }
            // Intentional fallthrough.
        default:
            throw ParseContext.errorUnexpected(t.loc, "(", "found " + t.source);
        }
    }
    
    protected final Token nextToken() throws CompilerError {
        return context.nextTokenExpecting(name);
    }
    
    /// This is allowed to return null.
    protected abstract LeftDenotation<T> leftDenotation(Token token) throws CompilerError;
    
    protected abstract T nullDenotation(Token token) throws CompilerError;
    
    public static final ArrayList<AST.Declaration<Unit>> parse(Location loc, ArrayList<Token> tokens) throws CompilerError {
        return (new Declaration(loc, tokens)).parseListFull("EOF");
    }
    
    protected final T parse(int rbp) throws CompilerError {
        T ast = nullDenotation(nextToken());
        
        while (context.isLive()) {
            LeftDenotation<T> ld = leftDenotation(context.tokens.get(context.index));
            if (ld != null && rbp < ld.lbp) {
                context.index += 1;
                ast = ld.apply(ast);
            } else {
                return ast;
            }
        }
        return ast;
    }
    
    protected final T parseFull(String terminal) throws CompilerError {
        T result = parse(0);
        if (context.isLive()) {
            Token t = context.lookahead();
            throw ParseContext.errorUnexpected(t.loc, terminal, t.source);
        }
        return result;
    }
    
    
    protected final ArrayList<T> parseList() throws CompilerError {
        ArrayList<T> result = new ArrayList<T>();
        if (context.tokens.size() == 0) { return result; }
        
        while (true) {
            result.add(parse(0));
            Token t = context.lookahead();
            if (t == null || !t.source.equals(separator)) { return result; }
            context.nextTokenExpecting(separator);
        }
    }
    
    protected final ArrayList<T> parseListFull(String terminal) throws CompilerError {
        ArrayList<T> result = parseList();
        if (context.isLive()) {
            Token t = context.lookahead();
            ParseContext.errorUnexpected(t.loc, separator + " or " + terminal, "found " + t.source);
        }
        return result;
    }
}
