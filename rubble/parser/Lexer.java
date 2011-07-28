package rubble.parser;

import java.util.ArrayList;
import java.util.Arrays;

import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Token.TokenType;

public final class Lexer {
    
    /*
     * I really wanted to use regular expressions, but the Java regex API
     * doesn't let me start at arbitrary offsets.  Since the Java substring
     * time complexity is not specified, I won't take a chance on quadratic
     * behavior.
     * 
     * ASCII table:
     *   20   30   40   50   60   70   Bit
     * f /    ?         _         Del  80
     * e .    >         ^         ~    40
     * d -    =         ]         }    20
     * c ,    <         \         |    10
     * b +    ;         [         {     8
     * a *    :         Z         z     4
     * 9 )    9                         2
     * 8 (    8                         1
     * 
     * 7 '    7                        80
     * 6 &    6                        40
     * 5 %    5                        20
     * 4 $    4                        10
     * 3 #    3                         8
     * 2 "    2                         4
     * 1 !    1    A         a          2
     * 0 ' '  0    @         `          1
     */
    private final static int[] identifierOrBlock   = { 0x0100, 0x0000, 0xfffe, 0x8fff, 0xfffe, 0x0fff };
    private final static int[] identifierBeginning = { 0x0000, 0x0000, 0xfffe, 0x87ff, 0xfffe, 0x07ff };
    private final static int[] identifierCharacter = { 0x0000, 0x03ff, 0xfffe, 0x87ff, 0xfffe, 0x07ff };
    private final static int[] operatorCharacter   = { 0xec72, 0xf400, 0x0000, 0x5000, 0x0000, 0x5000 };
    
    private boolean matchChar(int[] charSet, int c) {
        if (c < 32 || c > 128) { return false; }
        int ix = (c - 32) / 16;
        int bit = (c - 32) % 16;
        return (charSet[ix] & (1 << bit)) == 1;
    }
    
    
    private final static String[] rwArray = { "break", "def", "else", "forever", "if", "let", "return", "var" };
    private final static ArrayList<String> reservedWords = new ArrayList<String>(Arrays.asList(rwArray));
    
    private int row;
    private int column;
    private String source;
    private int index;
    private boolean separated;
    
    
    public Lexer(String source) {
        row = 1;
        column = 1;
        this.source = source;
        index = 0;
        separated = true;
    }
    
    private void dropWhitespace() throws CompilerError {
        boolean workDone = true;
        while (workDone) {
            workDone = false;
            
            // Remove spaces.
            while (source.startsWith(" ", index)) {
                workDone = true;
                index += 1;
                column += 1;
                separated = true;
            }
            
            // Remove comments.
            if (source.startsWith("#", index)) {
                int newIndex = source.indexOf('\n', index);
                index = newIndex == -1 ? source.length() : newIndex;
            }
            
            // Remove newlines.
            if (source.startsWith("\n", index)) {
                workDone = true;
                index += 1;
                row += 1;
                column = 1;
                separated = true;
            }
        }
        
        if (source.startsWith("\t", index)) {
            throw CompilerError.lexical(new Location(row, column), "The tab character may not appear in source code.");
        }
    }
    
    private Token lexBlock(boolean inBackticks) throws CompilerError {
        int c = source.charAt(index);
        switch (c) {
        case '(':
            return lexBlockHelper("(", ")");
        case '[':
            return lexBlockHelper("[", "]");
        case '{':
            return lexBlockHelper("{", "}");
        case '`':
            return inBackticks ? null : lexBlockHelper("`", "`");
        default:
            return null;
        }
    }

    private Token lexBlockHelper(String open, String close) throws CompilerError {
        int startRow = row;
        int startColumn = column;
        index += 1;
        column += 1;
        separated = true;
        ArrayList<Token> subtokens = lex(open == "`");

        if (!source.startsWith(close, index)) {
            String message = index >= source.length() ? "" : ("  " + source.charAt(index) + " was found instead.");
            throw CompilerError.lexical(new Location(row, column), "Unclosed " + open + "." + message);
        }
        index += 1;
        column += 1;
        separated = false;
        return new Token(new Location(startRow, startColumn, row, column), open, TokenType.Block, subtokens);
    }
    
    private Token lexIdentifier() {
        if (matchChar(identifierBeginning, source.charAt(index))) {
            int startIndex = index;
            int startColumn = column;
            index += 1;
            column += 1;
            while (matchChar(identifierCharacter, source.charAt(index))) {
                index += 1;
                column += 1;
            }
            separated = false;
            String identifier = source.substring(startIndex, index);
            Token.TokenType tag = (identifier == "do") ? TokenType.Block
                    : (reservedWords.contains(identifier)) ? TokenType.Reserved : TokenType.Identifier;
            return new Token(new Location(row, startColumn, column), identifier, tag);
        }
        return null;
    }
    
    private Token lexNumber() {
        int startIndex = index;
        int startColumn = column;
        
        // Handle negative numbers.
        if (source.charAt(index) == '-' && source.length() > index + 1) {
            if (!(source.charAt(index) > '0' && source.charAt(index) < '9')) { return null; }
            index += 2;
            column += 2;
        }
        while(source.charAt(index) > '0' && source.charAt(index) < '9') {
            index += 1;
            column += 1;
        }
        if (index == startIndex) { return null; }
        separated = false;
        return new Token(new Location(row, startColumn, column), source.substring(startIndex, index), TokenType.Number);
    }
    
    private Token lexOperator() {
        int startIndex = index;
        int startColumn = column;
        
        while (matchChar(operatorCharacter, source.charAt(index))) {
            index += 1;
            column += 1;
        }
        if (index == startIndex) { return null; }
        String op = source.substring(startIndex, index);
        Token.TokenType tag = TokenType.Operator;
        if (source.length() > index + 1 && matchChar(identifierOrBlock, source.charAt(index))) {
            if (op == "-") {
                op = "negate";
                tag = TokenType.Reserved;
            } else if (op == "*") {
                op = "valueAt";
                tag = TokenType.Reserved;
            } else if (op == "&") {
                op = "addressOf";
                tag = TokenType.Reserved;
            }
        }
        if (op == ":") {
            op = "asType";
        }
        
        separated = false;
        return new Token(new Location(row, startColumn, column), op, tag);
    }
    
    private Token lexSeparator() {
        if (source.startsWith(",", index)) {
            index += 1;
            column += 1;
            separated = true;
            return new Token(new Location(row, column - 1, column), ",", TokenType.Comma);
        } else if (source.startsWith(";", index)) {
            index += 1;
            column += 1;
            separated = true;
            return new Token(new Location(row, column - 1, column), ";", TokenType.Semicolon);
        }
        return null;
    }
    
    private ArrayList<Token> lex(boolean inBackTicks) throws CompilerError {
        ArrayList<Token> result = new ArrayList<Token>();
        
        dropWhitespace();
        while (index < source.length()) {
            Token token = lexBlock(inBackTicks);
            if (token == null) { token = lexIdentifier(); }
            if (token == null) { token = lexNumber(); }
            if (token == null) { token = lexOperator(); }
            if (token == null) { token = lexSeparator(); }
            if (token == null) { return result; }
            result.add(token);
            
            dropWhitespace();
        }
        return result;
    }
    
    public ArrayList<Token> lex() throws CompilerError {
        ArrayList<Token> result = lex(false);
        if (index < source.length()) {
            char c = source.charAt(index);
            String message = (c == ')' || c == ']' || c == '}') ? "Unmatched closing bracket." : "Unrecognized token.";
            throw CompilerError.lexical(new Location(row, column), message);
        }
        return result;
    }
}
