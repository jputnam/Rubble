package rubble.data;

import java.util.Arrays;
import java.util.ArrayList;


public final class Token {
    
	public enum TokenType {
		Block, Comma, Identifier, Number, Operator, Reserved, Semicolon
	}
	
	public final static String IMPLICIT_BRACE = "do";
	private final static ArrayList<Token> NIL = new ArrayList<Token>();
	
	public final Location loc;
	public final String source;
	public final TokenType tag;
	public final ArrayList<Token> subtokens;
	
	public Token(Location loc, String source, TokenType tag) {
		this.loc = loc;
		this.source = source;
		this.tag = tag;
		this.subtokens = NIL;
	}
	
	public Token(Location loc, String source, TokenType tag, ArrayList<Token> subtokens) {
		this.loc = loc;
		this.source = source;
		this.tag = tag;
		this.subtokens = subtokens;
	}
	
	public Token(Location loc, String source, TokenType tag, Token... subtokens) {
		this.loc = loc;
		this.source = source;
		this.tag = tag;
		this.subtokens = new ArrayList<Token>(Arrays.asList(subtokens));
	}
	
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("new Token(" + loc.toString() + ",\"" + source + "\", " + tag.toString());
		for (Token t: subtokens) {
			result.append(", " + t.toString());
		}
		result.append(")");
		return result.toString();
	}
}
