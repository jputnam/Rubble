package rubble.parser;

import java.util.ArrayList;

import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.data.Token.Tag;

public final class Layout {

    private final ArrayList<Token> tokens;
    private int index;
    private boolean permitSemicolon;

    public Layout(ArrayList<Token> tokens) {
        this.tokens = tokens;
        index = 0;
        permitSemicolon = false;
    }

    private Layout(ArrayList<Token> tokens, int index) {
        this.tokens = tokens;
        this.index = index;
        permitSemicolon = false;
    }

    private abstract class LayoutActions {

        public abstract void onImplicitSemicolon(Location loc, ArrayList<Token> result, boolean permitSemicolon) throws CompilerError;

        public abstract ArrayList<Token> onImplicitEndOfBlock(Location loc, ArrayList<Token> result) throws CompilerError;
    }
    
    private class BlockActions extends LayoutActions {
        
        private final boolean isExplicit;
        
        public BlockActions(boolean isExplicit) {
            this.isExplicit = isExplicit;
        }
        
        public void onImplicitSemicolon(Location loc, ArrayList<Token> result, boolean permitSemicolon) throws CompilerError {
            if (permitSemicolon) {
                result.add(new Token(loc, ";", Tag.Semicolon));
            }
        }
        
        public ArrayList<Token> onImplicitEndOfBlock(Location loc, ArrayList<Token> result) throws CompilerError {
            if (isExplicit) {
                throw CompilerError.lexical(loc, "The parser can't implicitly close an explicit brace.");
            }
            return result;
        }
    }

    private class BracketActions extends LayoutActions {
        
        public void onImplicitSemicolon(Location loc, ArrayList<Token> result, boolean permitSemicolon) throws CompilerError {
            throw CompilerError.lexical(loc, "The statement ended before all brackets were closed.");
        }
        
        public ArrayList<Token> onImplicitEndOfBlock(Location loc, ArrayList<Token> result) throws CompilerError {
            throw CompilerError.lexical(loc, "The statement ended before all brackets were closed.");
        }
    }
    
    public ArrayList<Token> layout() throws CompilerError {
        return layoutBlockBody(new ArrayList<Token>(), false, 1);
    }

    private ArrayList<Token> layoutAny(ArrayList<Token> result, LayoutActions actions, int semicolonColumn) throws CompilerError {
        while (index < tokens.size()) {
            Token current = tokens.get(index);

            if (current.loc.startColumn == semicolonColumn) {
                actions.onImplicitSemicolon(current.loc, result, permitSemicolon);
                permitSemicolon = false;
            } else if (tokens.get(index).loc.startColumn < semicolonColumn) {
                return actions.onImplicitEndOfBlock(current.loc, result);
            }

            index += 1;
            switch (current.tag) {
            case Block:
                if (current.source.equals("{")) {
                    // Remember that endColumn is one greater than the final
                    // column of the block.
                    if (current.loc.endColumn - 1 < semicolonColumn) {
                        throw CompilerError.lexical(current.loc, "The closing } must be at or to the right of the semicolon column of its enclosing block.");
                    }
                    result.add(new Token(current.loc, "{", Tag.Block, new Layout(current.subtokens, 0).layoutBlock(true, semicolonColumn)));
                    permitSemicolon = true;
                    
                } else if (current.source.equals(Token.IMPLICIT_BRACE)) {
                    ArrayList<Token> block = layoutBlock(false, semicolonColumn);
                    Location newLoc = (block.size() == 0) ? current.loc : new Location(current.loc.startRow, current.loc.startColumn, block.get(block.size() - 1).loc.endRow, block.get(block.size() - 1).loc.endColumn);
                    result.add(new Token(newLoc, Token.IMPLICIT_BRACE, Tag.Block, block));
                    permitSemicolon = true;
                    
                } else {
                    if (current.loc.endColumn - 1 <= semicolonColumn) {
                        throw CompilerError.lexical(current.loc, "The statement ended before you closed the brackets.");
                    }
                    result.add(new Token(current.loc, current.source, current.tag, new Layout(current.subtokens, 0).layoutBrackets(semicolonColumn)));
                    permitSemicolon = true;

                }
                break;
            case Semicolon:
                if (permitSemicolon) {
                    result.add(current);
                    permitSemicolon = false;
                }
                break;
            default:
                result.add(current);
                permitSemicolon = true;
            }
        }
        
        // Remove trailing semicolons.
        if (result.size() > 0) {
            if (result.get(result.size() - 1).tag == Tag.Semicolon) {
                result.remove(result.size() - 1);
            }
        }
        return result;
    }
    
    private ArrayList<Token> layoutBlock(boolean isExplicit, int semicolonColumn) throws CompilerError {
        ArrayList<Token> result = new ArrayList<Token>();
        
        // This will happen with an empty explicit block or an implicit block
        // at the end of whatever contains it.
        if (index >= tokens.size()) {
            return result;
        }
        if (tokens.get(index).loc.startColumn <= semicolonColumn) {
            if (isExplicit) {
                throw CompilerError.lexical(tokens.get(index).loc, "The parser can't implicitly close an explicit brace.");
            } else {
                return result;
            }
        }
        return layoutBlockBody(result, isExplicit, tokens.get(index).loc.startColumn);
    }
    
    private ArrayList<Token> layoutBlockBody(ArrayList<Token> result, boolean isExplicit, int semicolonColumn) throws CompilerError {
        return layoutAny(result, new BlockActions(isExplicit), semicolonColumn);
    }
    
    private ArrayList<Token> layoutBrackets(int semicolonColumn) throws CompilerError {
        return layoutAny(new ArrayList<Token>(), new BracketActions(), semicolonColumn);
    }
}
