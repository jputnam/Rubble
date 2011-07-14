package rubble.data


object Tokens {
    
    object Bracket extends Enumeration {
        type Bracket = Value;
        val BackTick, Paren, Square, Brace, ImplicitBrace, BackTicks = Value;
    }
    import Bracket._;
    
    
    sealed class Token(val loc : SourceLocation) { }
    
    
    implicit def mplus(l: Token) = new {
        def +++(r: => Token): Token = if (null eq l) { r } else { l }
    }
    
    
    sealed case class Block
        ( override val loc : SourceLocation
        , val bracket      : Bracket
        , val subTokens    : scala.collection.mutable.ArrayBuffer[Token]
        ) extends Token(loc) { }
    
    sealed case class Comma(override val loc : SourceLocation) extends Token(loc) { }
    
    sealed case class EOS(override val loc : SourceLocation) extends Token(loc) { }
    
    sealed case class Identifier
        ( override val loc : SourceLocation
        , val actual       : String
        ) extends Token(loc) { }
    
    sealed case class Integer
        ( override val loc : SourceLocation
        , val actual       : String
        , val value        : BigInt
        ) extends Token(loc) { }
    
    sealed case class Operator
        ( override val loc : SourceLocation
        , val actual       : String
        ) extends Token(loc) { }
    
    sealed case class Reserved
        ( override val loc : SourceLocation
        , val actual       : String
        ) extends Token(loc) { }
    
    sealed case class Semicolon(override val loc: SourceLocation) extends Token(loc) { }
    
}
