package rubble.data


object Tokens {
    
    object Bracket extends Enumeration {
        type Bracket = Value;
        val BackTick, Paren, Square, Brace, ImplicitBrace = Value;
    }
    import Bracket._;
    
    
    sealed class Token(val loc : Location) { }
    
    
    implicit def mplus(l: Token) = new {
        def +++(r: => Token): Token = if (null eq l) { r } else { l }
    }
    
    
    sealed case class Block
        ( override val loc : Location
        , val bracket      : Bracket
        , val subTokens    : scala.collection.mutable.ArrayBuffer[Token]
        ) extends Token(loc) { }
    
    sealed case class Comma(override val loc : Location) extends Token(loc) { }
    
    sealed case class EOS(override val loc : Location) extends Token(loc) { }
    
    sealed case class Identifier
        ( override val loc : Location
        , val actual       : String
        ) extends Token(loc) { }
    
    sealed case class Integer
        ( override val loc : Location
        , val actual       : String
        , val value        : BigInt
        ) extends Token(loc) { }
    
    sealed case class Operator
        ( override val loc : Location
        , val actual       : String
        ) extends Token(loc) { }
    
    sealed case class Reserved
        ( override val loc : Location
        , val actual       : String
        ) extends Token(loc) { }
    
    sealed case class Semicolon(override val loc: Location) extends Token(loc) { }
    
}
