package rubble.parser

import rubble.data.ParseError
import rubble.data.Location
import rubble.data.Tokens._
import rubble.data.Tokens.Bracket._
import scala.collection.mutable.ArrayBuffer


class Lexer(private var s: String) {
    
    private var row: Int = 1
    private var column: Int = 1
    
    private var separated: Boolean = true
    
    
    private def dropWhitespace(): Unit = {
        var workDone = true
        while (workDone) {
            workDone = false
            
            
            // Remove spaces.
            val str = s takeWhile (c => c == ' ')
            s = s drop str.length
            column += str.length
            if (str.length > 0) {
                workDone = true
                separated = true
            }
            // After this, s might be "", so charAt(0) can fail.
            
            // Remove comments.
            if ((s take 1) == "#") {
                s = s dropWhile (c => c != '\n')
                workDone = true
                separated = true
            }
            
            // Remove newlines.
            if ((s take 1) == "\n") {
                s = s drop 1
                row += 1
                column = 1
                workDone = true
                separated = true
            }
        }
        
        if ((s take 1) == "\t") {
            throw new ParseError(row, column, 1, "The tab character is illegal.")
        }
    }
    
    
    private def lexBlock(inBackTick: Boolean): Token = {
        def lexBlockHelper(open: String, close: String, bracket: Bracket): Token = {
            val startRow = row
            val startColumn = column
            s = s drop 1
            column += 1
            separated = true
            val tokens = lex(ArrayBuffer.empty[Token], bracket == BackTick)
            
            if ((s take 1) != close) {
                throw new ParseError(row, column, 1, "Unclosed " + open + ".  " + (s take 1) + " was found instead.")
            }
            s = s drop 1
            column += 1
            
            separated = false
            return Block(new Location(startRow, startColumn, row, column), open, bracket, tokens)
        }
        
        val c = s charAt 0
        if (c == '(') {
            return lexBlockHelper("(", ")", Paren)
        }
        else if (c == '[') {
            return lexBlockHelper("[", "]", Square)
        }    
        else if (c == '{') {
            val startRow = row
            val startColumn = column
            val block = lexBlockHelper("{", "}", Brace)
            return block
        }    
        else if (c == ':') {
            column += 1
            s = s drop 1
            return Block(new Location(row, column, 1), ":", ImplicitBrace, ArrayBuffer.empty[Token])
        }    
        else if (c == '`') {
            return lexBlockHelper("`", "`", BackTick)
        }
        return null
    }
    
    
    private val reservedWords = Set("def", "break", "else", "if", "forever", "return", "val", "var")
    
    private def lexIdentifier(): Token = {
        if ((s take 1) matches "[_a-zA-Z]") {
            val str = s takeWhile (c => c.toString matches "[_a-zA-Z0-9]")
            column += str.length
            s = s drop str.length
            
            separated = false
            val loc = new Location(row, column, str.length)
            return if (reservedWords contains str) Reserved(loc, str) else Identifier(loc, str)
        }
        return null
    }
    
    
    private def lexInteger(): Token = {
        val hasHyphen = separated && ((s charAt 0) == '-')
        val s1 = if (hasHyphen) s drop 1 else s
        
        val digits = s1 takeWhile (c => c isDigit)
        if (digits.length > 0) {
            val str = if (hasHyphen) ('-' + digits) else digits
            s = s1 drop str.length
            column += str.length
            
            separated = false
            return Integer(new Location(row, column, str.length), str, BigInt apply str)
        }
        
        return null
    }
    
    
    private val reservedSymbols = Set("=", "addressOf", "negate", "valueAt")
    
    private def lexOperator(): Token = {
        var str = s takeWhile (c => c.toString matches """[!@$%\^\&*\-=+\\|<>/\?]""")
        if (str.length > 0) {
            s = s drop str.length
            column += str.length
            
            val op = if (separated && (s take 1 matches """[_a-zA-Z\(\[\{]"""))
                (str match { case "-" => "negate"
                           ; case "*" => "valueAt"
                           ; case "&" => "addressOf"
                           ; case _   => str })
                else str
            
            separated = false
            val loc = new Location(row, column, str.length)
            return if (reservedSymbols contains str) Reserved(loc, op) else Operator(loc, op)
        }
        return null
    }
    
    
    private def lexSeparator(): Token = {
        if ((s charAt 0) == ',') {
            s = s drop 1
            column += 1
            
            separated = true
            return Comma(new Location(row, column, 1))
        }
        if ((s charAt 0) == ';') {
            s = s drop 1
            column += 1
            
            separated = true
            return Semicolon(new Location(row, column, 1))
        }
        return null
    }
    
    
    private def lex(result: ArrayBuffer[Token], inBackTick: Boolean): ArrayBuffer[Token] = {
        dropWhitespace
        if (s == "") return result
        
        val token = lexBlock(inBackTick) +++ lexIdentifier +++ lexInteger +++ lexOperator +++ lexSeparator
        return if (token != null) { lex(result += token, inBackTick) } else { result }
    }
    
    
    def lex(): ArrayBuffer[Token] = {
        val result = lex(ArrayBuffer.empty[Token], false)
        if (s != "") {
            val message = if (s take 1 matches ")\\]}") "Unmatched closing bracket." else "Unrecognized character."
            throw new ParseError(new Location(row, column, 1), message)
        }
        return result
    }
}
