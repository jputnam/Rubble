package rubble.data

import scala.collection.mutable.ArrayBuffer


object AST {
    
    sealed abstract class Expression[Type](val loc: Location, val tau: Type) { }
    
    sealed case class AddressOf[Type](
            override val loc : Location,
            override val tau : Type,
            val value        : Expression[Type])
            extends Expression[Type](loc, tau) { }
    
    sealed case class Apply[Type](
            override val loc : Location,
            override val tau : Type,
            val function     : Expression[Type],
            val argument     : Expression[Type])
            extends Expression[Type](loc, tau) { }
    
    sealed case class ArrayLiteral[Type](
            override val loc : Location,
            override val tau : Type,
            val es           : ArrayBuffer[Expression[Type]])
            extends Expression[Type](loc, tau) { }
    
    sealed case class IfE[Type](
            override val loc : Location,
            override val tau : Type,
            val cond         : Expression[Type],
            val t            : Expression[Type],
            val f            : Expression[Type])
            extends Expression[Type](loc, tau) { }
    
    sealed case class Index[Type](
            override val loc : Location,
            override val tau : Type,
            val base         : Expression[Type],
            val offset       : ArrayBuffer[Expression[Type]])
            extends Expression[Type](loc, tau) { }
    
    sealed case class Integer[Type](
            override val loc : Location,
            override val tau : Type,
            val actual       : String,
            val value        : BigInt)
            extends Expression[Type](loc, tau) { }
    
    sealed case class Tuple[Type](
            override val loc : Location,
            override val tau : Type,
            val value        : ArrayBuffer[Expression[Type]])
            extends Expression[Type](loc, tau) { }
    
    sealed case class ValueAt[Type](
            override val loc : Location,
            override val tau : Type,
            val address      : Expression[Type])
            extends Expression[Type](loc, tau) { }
    
    sealed case class Variable[Type](
            override val loc : Location,
            override val tau : Type,
            val name         : String)
            extends Expression[Type](loc, tau) { }
    
    
    sealed abstract class LValue[Type] { }
    
    sealed case class Direct[Type](
            name: String)
            extends LValue[Type] { }
    
    sealed case class Indirect[Type](
            name: String,
            offset: ArrayBuffer[Expression[Type]])
            extends LValue[Type] { }
    
    
    sealed abstract class Statement[Type](val loc: Location) { }
    
    sealed case class Assign[Type](
            override val loc : Location,
            val assignee     : ArrayBuffer[LValue[Type]],
            val value        : Expression[Type])
            extends Statement[Type](loc) { }
    
    sealed case class Break[Type](
            override val loc: Location,
            val depth: Int)
            extends Statement[Type](loc) { }
    
     sealed case class Call[Type](
            override val loc : Location,
            val function     : Expression[Type],
            val argument     : Expression[Type])
            extends Statement[Type](loc) { }
    
   sealed case class Forever[Type](
            override val loc: Location,
            val label: String,
            val block: ArrayBuffer[Statement[Type]])
            extends Statement[Type](loc) { }
    
    sealed case class IfS[Type](
            override val loc : Location,
            val cond         : Expression[Type],
            val t            : ArrayBuffer[Statement[Type]],
            val f            : ArrayBuffer[Statement[Type]])
            extends Statement[Type](loc) { }
    
    sealed case class Nested[Type](
            override val loc : Location,
            val statements   : ArrayBuffer[Statement[Type]])
            extends Statement[Type](loc) { }
    
    sealed case class Return[Type](
            override val loc : Location,
            val value        : Expression[Type])
            extends Statement[Type](loc) { }
}