package rubble.data

import scala.collection.mutable.ArrayBuffer


object AST {
    
    sealed class Type { }
    
    
    sealed abstract class Expression(val loc: Location, var tau: Type) { }
    
    sealed case class AddressOf
        ( override val loc : Location
        , _tau             : Type
        , val e            : Expression
        ) extends Expression(loc, _tau) { }
    
    sealed case class Apply
        ( override val loc : Location
        , _tau             : Type
        , val function     : Expression
        , val arguments    : ArrayBuffer[Expression]
        ) extends Expression(loc, _tau) { }
    
    sealed case class ArrayLiteral
        ( override val loc : Location
        , _tau             : Type
        , val es           : ArrayBuffer[Expression]
        ) extends Expression(loc, _tau) { }
    
    sealed case class Conditional
        ( override val loc : Location
        , _tau             : Type
        , val cond         : Expression
        , val t            : Expression
        , val f            : Expression
        ) extends Expression(loc, _tau) { }
    
    sealed case class Index
        ( override val loc : Location
        , _tau             : Type
        , val base         : Expression
        , val offset       : ArrayBuffer[Expression]
        ) extends Expression(loc, _tau) { }
    
    sealed case class Integer
        ( override val loc : Location
        , _tau             : Type
        , val actual       : String
        , val value        : BigInt
        ) extends Expression(loc, _tau) { }
    
    sealed case class Parenthesized
        ( override val loc : Location
        , _tau             : Type
        , e                : ArrayBuffer[Expression]
        ) extends Expression(loc, _tau) { }
    
    sealed case class ValueAt
        ( override val loc : Location
        , _tau             : Type
        , val e            : Expression
        ) extends Expression(loc, _tau) { }
    
    sealed case class Variable
        ( override val loc : Location
        , _tau             : Type
        , val name         : String
        ) extends Expression(loc, _tau) { }
    
}