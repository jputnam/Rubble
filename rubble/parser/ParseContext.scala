package rubble.parser

import rubble.parser.Parser._


object ParseContext {
    
    def BaseContext(): ParseContext = {
        return null
    }
    
}

sealed case class ParseContext(
        expr: Parser.Expression,
        tau: Parser.Type
        ) {
    
}