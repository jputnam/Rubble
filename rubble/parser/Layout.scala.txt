package rubble.parser

import rubble.data.ParseError
import rubble.data.Location
import rubble.data.Tokens._
import rubble.data.Tokens.Bracket._
import scala.collection.mutable.ArrayBuffer


object Layout {

    def layout(tokens: ArrayBuffer[Token]): ArrayBuffer[Token] = {
        val l = new Layout(tokens, 0, 1)
        val result = l.layoutBlockBody(ArrayBuffer.empty[Token], false)
        
        // This means that we stopped parsing before we ran out of input.
        // I don't think that should be possible. so this should be an ICE.
        if (l.index < tokens.length) {
            throw new ParseError(tokens(l.index).loc, "ICE: The parser stopped before it ran out of input.")
        }
        return result
    }
}


class Layout private (private val tokens: ArrayBuffer[Token], private var index: Int, private var semicolonColumn: Int) {
    
    private var permitSemicolon = false
    
    
    private def layoutAny
            ( result: ArrayBuffer[Token]
            , onImplicitSemicolon: => Unit
            , onImplicitEndOfBlock: => Unit
            ): ArrayBuffer[Token] = {
        
        def deletePrecedingSemicolon(): Unit = {
        }
        
        while (index < tokens.length) {
            if (tokens(index).loc.start.column == semicolonColumn) {
                onImplicitSemicolon
            }
            else if (tokens(index).loc.start.column < semicolonColumn) {
                onImplicitEndOfBlock
            }
            
            /* 
             * Why am I incrementing index now instead of later?  If the current
             * token is a colon, I need to recurse, but that recursion needs to
             * start with the token after the current one.  I could try to do
             * something tricky, like putting an extra increment before that call
             * and a decrement afterward, and that would work, I think, but is far
             * too clever for comfort.
             */
            val current = tokens(index)
            index += 1
            
            current match {
                case Block(loc, actual, Brace, subtokens) =>
                    if (loc.end.column < semicolonColumn) {
                        throw new ParseError(loc.end, "The closing } must be at or to the right of the semicolon column of its enclosing block.")
                    }
                    result += Block(loc, actual, Brace, new Layout(subtokens, 0, semicolonColumn).layoutBlock(true))
                    permitSemicolon = true
                    
                case Block(loc, actual, ImplicitBrace, subtokens) =>
                    val block = layoutBlock(false)
                    val newLoc = if (block.length == 0) loc else new Location(loc.start, block(block.length-1).loc.end)
                    result += Block(newLoc, actual, ImplicitBrace, block)
                    permitSemicolon = true
                    
                case Block(loc, actual, bracket, subtokens) =>
                    if (loc.end.column <= semicolonColumn) {
                        throw new ParseError(loc, "The statement ended before you closed the brackets.")
                    }
                    result += Block(loc, actual, bracket, new Layout(subtokens, 0, semicolonColumn).layoutBrackets)
                    permitSemicolon = true
                    
                case Semicolon(_) =>
                    if (permitSemicolon) {
                        result += current
                        permitSemicolon = false
                    }
                    
                case _ =>
                    result += current
                    permitSemicolon = true
            }
        }
        
        // Delete trailing semicolons
        if (result.length > 0) {
            result(result.length - 1) match {
                case Semicolon(_) =>
                    result remove (result.length - 1)
                case _ =>
            }
        }
        return result
    }
    
    
    private def layoutBrackets(): ArrayBuffer[Token] = {
        layoutAny(ArrayBuffer.empty[Token]
            , { throw new ParseError(tokens(index).loc, "The statement ended before you closed the brackets.") }
            , { throw new ParseError(tokens(index).loc, "The statement ended before you closed the brackets.") }
            )
    }
    
    private def layoutBlock
            ( isExplicit: Boolean
            ): ArrayBuffer[Token] = {
        val result = ArrayBuffer.empty[Token]
        
        // This will happen with an empty explicit block or an implicit block
        // which is at the end of whatever contains it.
        if (index >= tokens.length) {
            return result
        }
        if (tokens(index).loc.start.column <= semicolonColumn) {
            if (isExplicit) {
                throw new ParseError(tokens(index).loc, "The parser can't implicitly close an explicit brace.")
            } else {
                return result
            }
        }
        
        semicolonColumn = tokens(index).loc.start.column
        return layoutBlockBody(result, isExplicit)
    }
    
    
    private def layoutBlockBody
            ( result: ArrayBuffer[Token]
            , isExplicit: Boolean
            ): ArrayBuffer[Token] =
        
        layoutAny(result
            , { if (permitSemicolon) {
                    result += Semicolon(tokens(index).loc.before)
                    permitSemicolon = false
                }
            },{ if (isExplicit) {
                    throw new ParseError(tokens(index).loc, "The parser can't implicitly close an explicit brace.")
                } else {
                    return result
                }
            })
}