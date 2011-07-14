package rubble.parser

import rubble.data.ParseFailure
import rubble.data.SourceLocation
import rubble.data.Tokens._
import rubble.data.Tokens.Bracket._
import scala.collection.mutable.ArrayBuffer


class Layout(private val tokens: ArrayBuffer[Token]) {
    
    
    private def layout(input: ArrayBuffer[Token], offset: Int, semicolonColumn: Int): (ArrayBuffer[Token], Int) = {
        val output = ArrayBuffer.empty[Token]
        var current = offset
        var permitSemicolon = true
        
        while (current < input.length) {
            if (input(current).loc.startColumn == semicolonColumn && permitSemicolon) {
                output += Semicolon(new SourceLocation(input(current).loc.startRow, input(current).loc.startColumn, 0))
                permitSemicolon = false
            }
            else if (input(current).loc.startColumn < semicolonColumn) {
                return (output, current)
            }
            
            input(current) match {
                case Block(_, _, _) =>
                    // TODO
                case Semicolon(_) =>
                    if (permitSemicolon) {
                        output += input(current)
                        permitSemicolon = false
                    }
                case _ =>
                    output += input(current)
                    permitSemicolon = true
            }
            current += 1
        }
        return (output, current)
    }
    
    
    def layout(): ArrayBuffer[Token] = {
        val (result, index) = layout(tokens, 0, 1)
        
        // This means that we stopped parsing before we ran out of input.
        // I don't think that should be possible. so this should be an ICE.
        if (index < tokens.length) {
            throw new ParseFailure(tokens(index).loc, "FIXME: ")
        }
        return result
    }
}
