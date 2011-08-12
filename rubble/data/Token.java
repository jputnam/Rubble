package rubble.data;

import java.util.Arrays;
import java.util.ArrayList;

/**
 * Tokens that the source gets transformed into.  Block tokens are used for
 * bracket pairs; the subtoken list is the tokens enclosed by that block.
 * All other tokens should have an empty subtoken list.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class Token {
    
	public static enum Tag {
		Block, Comma, Identifier, Number, Operator, Reserved, Semicolon
	}
	
	public final static String IMPLICIT_BRACE = "do";
	private final static ArrayList<Token> NIL = new ArrayList<Token>();
	
	public final Location loc;
	public final String source;
	public final Tag tag;
	public final ArrayList<Token> subtokens;
	
	public Token(Location loc, String source, Tag tag) {
		this.loc = loc;
		this.source = source;
		this.tag = tag;
		this.subtokens = NIL;
	}
	
	public Token(Location loc, String source, Tag tag, ArrayList<Token> subtokens) {
		this.loc = loc;
		this.source = source;
		this.tag = tag;
		this.subtokens = subtokens;
	}
	
	public Token(Location loc, String source, Tag tag, Token... subtokens) {
		this.loc = loc;
		this.source = source;
		this.tag = tag;
		this.subtokens = new ArrayList<Token>(Arrays.asList(subtokens));
	}
	
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("(Token " + loc.toString() + " {" + source + "} " + tag.toString() + " {");
		for (Token t: subtokens) {
			result.append(t.toString());
		}
		result.append("})");
		return result.toString();
	}
}
