package rubble.data;

import java.util.ArrayList;

public final class Types {

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
        
        public final boolean isMutable;
        public final Tag tag;
        
        public Type(Tag tag, boolean isMutable) {
            this.tag = tag;
            this.isMutable = isMutable;
        }
        
        public abstract Type mutable();
    }
    
    public static final class Arrow extends Type {
        
        public final Type domain;
        public final Type codomain;
        
        public Arrow(Type domain, Type codomain, boolean isMutable) {
            super(Tag.Arrow, isMutable);
            this.domain = domain;
            this.codomain = codomain;
        }
        
        public Type mutable() {
            return new Arrow(domain, codomain, true);
        }
    }
    
    public static final class Buffer extends Type {
        
        public final Type contained;
        public final Nat size;
        
        public Buffer(Nat size, Type contained, boolean isMutable) {
            super(Tag.Buffer, isMutable);
            this.contained = contained;
            this.size = size;
        }
        
        public Type mutable() {
            return new Buffer(size, contained, true);
        }
    }
    
    public static final class Ground extends Type {
        
        public final GroundTag tag;
        
        public Ground(GroundTag tag, boolean isMutable) {
            super(Tag.Ground, isMutable);
            this.tag = tag;
        }
        
        public Type mutable() {
            return new Ground(tag, true);
        }
    }
    
    public static final class Ptr extends Type {
        
        public final Type pointee;
        
        public Ptr(Type pointee, boolean isMutable) {
            super(Tag.Ptr, isMutable);
            this.pointee = pointee;
        }
        
        public Type mutable() {
            return new Ptr(pointee, true);
        }
    }
    
    public static final class Tuple extends Type {
        
        public final ArrayList<Type> members;
        
        public Tuple(ArrayList<Type> members, boolean isMutable) {
            super(Tag.Tuple, isMutable);
            this.members = members;
        }
        
        public Type mutable() {
            return new Tuple(members, true);
        }
    }
    
    public static final class TypeVar extends Type {
        
        public int id;
        
        public TypeVar(int id, boolean isMutable) {
            super(Tag.TypeVar, isMutable);
            this.id = id;
        }
        
        public Type mutable() {
            return new TypeVar(id, true);
        }
    }
}
