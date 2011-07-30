package rubble.data;

import java.util.ArrayList;

public final class Types {

    public static final class ModalType {
        
        public final boolean isMutable;
        public final Type tau;
        
        public ModalType(boolean isMutable, Type tau) {
            this.isMutable = isMutable;
            this.tau = tau;
        }
    }
    
    
    public static abstract class Nat {
        
        public final NatTag tag;
        
        public Nat(NatTag tag) {
            this.tag = tag;
        }
    }
    
    public static enum NatTag { NatLiteral, NatVar }
    
    public static final class NatLiteral extends Nat {
        
        public final long value;
        
        public NatLiteral(long value) {
            super(NatTag.NatLiteral);
            this.value = value;
        }
    }
    
    public static final class NatVar extends Nat {
        
        public final String name;
        
        public NatVar(String name) {
            super(NatTag.NatVar);
            this.name = name;
        }
    }
    
    
    public static enum Tag { Arrow, Buffer, Ground, Ptr, Tuple, TypeVar }

    public static enum GroundTag {
        Boolean, Int8, Int16, Int32, Int64,
        Unit, UInt8, UInt16, UInt32, UInt64
    }
    
    
    public static abstract class Type {
        
        public final Tag tag;
        
        public Type(Tag tag) {
            this.tag = tag;
        }
    }
    
    public static final class Arrow extends Type {
        
        public final ArrayList<ModalType> domain;
        public final Type codomain;
        
        public Arrow(ArrayList<ModalType> domain, Type codomain) {
            super(Tag.Arrow);
            this.domain = domain;
            this.codomain = codomain;
        }
    }
    
    public static final class Buffer extends Type {
        
        public final ModalType contained;
        public final Nat size;
        
        public Buffer(Nat size, ModalType contained) {
            super(Tag.Buffer);
            this.contained = contained;
            this.size = size;
        }
    }
    
    public static final class Ground extends Type {
        
        public final GroundTag groundTag;
        
        public Ground(GroundTag g) {
            super(Tag.Ground);
            groundTag = g;
        }
    }
    
    public static final class Ptr extends Type {
        
        public final ModalType pointee;
        
        public Ptr(ModalType pointee) {
            super(Tag.Ptr);
            this.pointee = pointee;
        }
    }
    
    public static final class Tuple extends Type {
        
        public final ArrayList<Type> members;
        
        public Tuple(ArrayList<Type> members) {
            super(Tag.Tuple);
            this.members = members;
        }
    }
    
    public static final class TypeVar extends Type {
        
        public int id;
        
        public TypeVar(int id) {
            super(Tag.TypeVar);
            this.id = id;
        }
    }
}
