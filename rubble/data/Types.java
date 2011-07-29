package rubble.data;

import java.util.ArrayList;

public final class Types {

    public final class Reference<Tau> {
        
        public final boolean isMutable;
        public final Tau tau;
        
        public Reference(boolean isMutable, Tau tau) {
            this.isMutable = isMutable;
            this.tau = tau;
        }
    }
    
    
    public enum Tag { Arrow, Buffer, Ground, Ptr, Tuple, TypeVar }

    public enum GroundTag {
        Boolean, Int8, Int16, Int32, Int64,
        Unit, UInt8, UInt16, UInt32, Unit64
    }
    
    
    public abstract class Type {
        
        public final Tag tag;
        
        public Type(Tag tag) {
            this.tag = tag;
        }
    }
    
    public final class Arrow extends Type {
        
        public final Type domain;
        public final Type codomain;
        
        public Arrow(Type domain, Type codomain) {
            super(Tag.Arrow);
            this.domain = domain;
            this.codomain = codomain;
        }
    }
    
    public final class Buffer extends Type {
        
        public final Reference<Type> contained;
        public final int size;
        
        public Buffer(int size, Reference<Type> contained) {
            super(Tag.Buffer);
            this.contained = contained;
            this.size = size;
        }
    }
    
    public final class Ground extends Type {
        
        public final GroundTag groundTag;
        
        public Ground(GroundTag g) {
            super(Tag.Ground);
            groundTag = g;
        }
    }
    
    public final class Ptr extends Type {
        
        public final Reference<Type> pointee;
        
        public Ptr(Reference<Type> pointee) {
            super(Tag.Ptr);
            this.pointee = pointee;
        }
    }
    
    public final class Tuple extends Type {
        
        public final ArrayList<Type> members;
        
        public Tuple(ArrayList<Type> members) {
            super(Tag.Tuple);
            this.members = members;
        }
    }
    
    public final class TypeVar extends Type {
        
        public int id;
        
        public TypeVar(int id) {
            super(Tag.TypeVar);
            this.id = id;
        }
    }
}
