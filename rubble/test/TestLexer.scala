package rubble.test

import rubble.data.Location
import rubble.data.Tokens._
import rubble.data.Tokens.Bracket._
import rubble.parser.Layout
import rubble.parser.Lexer
import rubble.test.TestHarness._

import scala.collection.mutable.ArrayBuffer


object TestLexer {
    
    def main(args: Array[String]): Unit = {
        testAll(fails  ("bracket1", new Lexer("([").lex, "Unclosed [."),
                matches("bracket2", new Lexer("(a)x").lex,
                        ArrayBuffer(Block((new Location(1,1,1,4)),"(",Paren,ArrayBuffer(Identifier((new Location(1,2,1,3)),"a"))), Identifier((new Location(1,4,1,5)),"x"))),
                matches("bracket3", new Lexer("[x do y]").lex, ArrayBuffer(
                        Block((new Location(1,1,1,9)),"[",Square, ArrayBuffer(
                                Identifier((new Location(1,2,1,3)),"x"),
                                Block((new Location(1,4,1,6)),"do",ImplicitBrace,ArrayBuffer()),
                                Identifier((new Location(1,7,1,8)),"y"))))),
                matches("bracket4", new Lexer("(()[{}])").lex, ArrayBuffer(
                        Block((new Location(1,1,1,9)),"(",Paren,ArrayBuffer(
                                Block((new Location(1,2,1,4)),"(",Paren,ArrayBuffer()),
                                Block((new Location(1,4,1,8)),"[",Square,ArrayBuffer(
                                        Block((new Location(1,5,1,7)),"{",Brace,ArrayBuffer()))))))),
                matches("lexer1", new Lexer("abc def 1 ,;-a,- a -1 a-1 <<: & &x * *x=").lex, ArrayBuffer(Identifier((new Location(1,1,1,4)),"abc"), Reserved((new Location(1,5,1,8)),"def")))
                )
    }
}