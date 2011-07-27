package rubble.data

import scala.collection.mutable.ArrayBuffer


object Types {
    
    // Modality
    object Mutability extends Enumeration {
        type Mutability = Value
        val Mutable, Immutable = Value
    }
    import Mutability._
    
    
    sealed case class Mode(val mutability: Mutability) { }
    
    
    // Enumerations for ground types
    object Primitive extends Enumeration {
        type Primitive = Value
        val Boolean, Unit, Int8, Int16, Int32, Int64,
            UInt8, UInt16, UInt32, UInt64 = Value
    }
    import Primitive._
    
    
    // Sizes for buffers
    sealed abstract class BufferSize { }
    
    sealed case class KnownSize(val size: BigInt) extends BufferSize { }
    
    sealed case class SizeVar(val i: Int) extends BufferSize { }
    
    
    // Types
    sealed abstract class Type { }
    
    sealed case class Arrow(val domainMode: Mode, val domain: Type, val codomain: Type) extends Type { }
    
    sealed case class Buffer(val size: BufferSize, val mode: Mode, val t: Type) extends Type { }
    
    sealed case class Ground(val t: Primitive) extends Type { }
    
    sealed case class Ptr(val mode: Mode, val t: Type) extends Type { }
    
    sealed case class Tuple(val ts: ArrayBuffer[(Mode, Type)]) extends Type { }
    
    sealed case class TypeVar(val i: Int) extends Type { }
}