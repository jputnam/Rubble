package rubble.data;

import java.util.ArrayList;

import rubble.data.Names.*;


public final class Types {
    
    public static final class Mono { }
    
    public static abstract class Parsed { }
    
    public static final class Poly extends Parsed { }
    
    
    public static abstract class Nat<Name, Phase> {
        
        public final NatTag tag;
        
        public Nat(NatTag tag) {
            this.tag = tag;
        }
    }
    
    public static enum NatTag { NatExternal, NatKnown, NatLiteral, NatVar, NatUnknown }
    
    public static final class NatExternal<Name, Phase> extends Nat<Name, Phase> {
        
        public final Name name;
        
        public NatExternal(Name name) {
            super(NatTag.NatVar);
            this.name = name;
        }
        
        public String toString() {
            return "{NE " + name.toString() + "}";
        }
    }
    
    public static final class NatKnown<Name, Phase extends Parsed> extends Nat<Name, Phase> {
        
        public final Nat<ResolvedName, Mono> nat;
        
        public NatKnown(Nat<ResolvedName, Mono> nat) {
            super(NatTag.NatKnown);
            this.nat = nat;
        }
        
        public String toString() {
            return "<" + nat.toString() + ">";
        }
    }
    
    public static final class NatLiteral extends Nat<ResolvedName, Mono> {
        
        public final long value;
        
        public NatLiteral(long value) {
            super(NatTag.NatLiteral);
            this.value = value;
        }
        
        public String toString() {
            return "{" + value + "}";
        }
    }
    
    public static final class NatVar<Name> extends Nat<Name, Poly> {
        
        public final int index;
        
        public NatVar(int index) {
            super(NatTag.NatVar);
            this.index = index;
        }
        
        public String toString() {
            return "{NV " + index + "}";
        }
    }
    
    public static final class NatUnknown extends Nat<String, Parsed> {
        
        public NatUnknown() {
            super(NatTag.NatUnknown);
        }
        
        public String toString() {
            return "?";
        }
    }
    
    
    public static enum Tag { Arrow, Buffer, Ground, Known, Ptr, Tuple, TypeVar, Unknown }

    public static enum GroundTag {
        Boolean, Int8, Int16, Int32, Int64,
        Unit, UInt8, UInt16, UInt32, UInt64
    }
    
    
    public static abstract class Type<Name, Phase> {
        
        public final boolean isMutable;
        public final Tag tag;
        
        public Type(Tag tag, boolean isMutable) {
            this.tag = tag;
            this.isMutable = isMutable;
        }
        
        public abstract Type<Name, Phase> mutable();
    }
    
    public static final class Arrow<Name, Phase> extends Type<Name, Phase> {
        
        public final Type<Name, Phase> domain;
        public final Type<Name, Phase> codomain;
        
        public Arrow(Type<Name, Phase> domain, Type<Name, Phase> codomain, boolean isMutable) {
            super(Tag.Arrow, isMutable);
            this.domain = domain;
            this.codomain = codomain;
        }
        
        public Type<Name, Phase> mutable() {
            return new Arrow<Name, Phase>(domain, codomain, true);
        }
        
        public String toString() {
            return "(Arrow " + domain.toString() + "->" + codomain.toString() + " " + isMutable + ")";
        }
    }
    
    public static final class Buffer<Name, Phase> extends Type<Name, Phase> {
        
        public final Type<Name, Phase> contained;
        public final Nat<Name, Phase> size;
        
        public Buffer(Nat<Name, Phase> size, Type<Name, Phase> contained, boolean isMutable) {
            super(Tag.Buffer, isMutable);
            this.contained = contained;
            this.size = size;
        }
        
        public Type<Name, Phase> mutable() {
            return new Buffer<Name, Phase>(size, contained, true);
        }
        
        public String toString() {
            return "(Buffer " + size.toString() + " " + contained.toString() + ")";
        }
    }
    
    public static final class Ground<Name> extends Type<Name, Mono> {
        
        public final GroundTag groundTag;
        
        public Ground(GroundTag groundTag, boolean isMutable) {
            super(Tag.Ground, isMutable);
            this.groundTag = groundTag;
        }
        
        public Type<Name, Mono> mutable() {
            return new Ground<Name>(groundTag, true);
        }
        
        public String toString() {
            return "(Ground " + groundTag.toString() + " " + isMutable + ")";
        }
    }
    
    public static final class Known<Name, Phase extends Parsed> extends Type<Name, Phase> {
        
        public final Type<Name, Mono> type;
        
        public Known(Type<Name, Mono> type) {
            super(Tag.Known, type.isMutable);
            this.type = type;
        }
        
        public Type<Name, Phase> mutable() {
            return new Known<Name, Phase>(type.mutable());
        }
        
        public String toString() {
            return "<" + type.toString() + ">";
        }
    }
    
    public static final class Ptr<Name, Phase> extends Type<Name, Phase> {
        
        public final Type<Name, Phase> pointee;
        
        public Ptr(Type<Name, Phase> pointee, boolean isMutable) {
            super(Tag.Ptr, isMutable);
            this.pointee = pointee;
        }
        
        public Type<Name, Phase> mutable() {
            return new Ptr<Name, Phase>(pointee, true);
        }
        
        public String toString() {
            return "(Ptr " + pointee.toString() + " " + isMutable + ")";
        }
    }
    
    public static final class Tuple<Name, Phase> extends Type<Name, Phase> {
        
        public final ArrayList<Type<Name, Phase>> members;
        
        public Tuple(ArrayList<Type<Name, Phase>> members, boolean isMutable) {
            super(Tag.Tuple, isMutable);
            this.members = members;
        }
        
        public Type<Name, Phase> mutable() {
            return new Tuple<Name, Phase>(members, true);
        }
        
        public String toString() {
            String ms = "";
            for (Type<Name, Phase> t: members) {
                ms += t.toString();
            }
            return "(Tuple " + ms + " " + isMutable + ")";
        }
    }
    
    public static final class TypeVar<Name> extends Type<Name, Poly> {
        
        public int id;
        
        public TypeVar(int id, boolean isMutable) {
            super(Tag.TypeVar, isMutable);
            this.id = id;
        }
        
        public Type<Name, Poly> mutable() {
            return new TypeVar<Name>(id, true);
        }
        
        public String toString() {
            return "(TypeVar " + id + " " + isMutable + ")";
        }
    }
    
    public static Unknown<String> UNKNOWN_IMMUTABLE = new Unknown<String>(false, false);
    public static Unknown<String> UNKNOWN_NEUTRAL = new Unknown<String>(true, false);
    
    public static final class Unknown<Name> extends Type<Name, Parsed> {
        
        // If the type is neutral, this overrides mutability; the type
        // variable can be unified with either a mutable type or an immutable one.
        public final boolean isNeutral;
        
        public Unknown(boolean isNeutral, boolean isMutable) {
            super(Tag.Unknown, isMutable);
            this.isNeutral = isNeutral;
        }
        
        public Type<Name, Parsed> mutable() {
            return new Unknown<Name>(isNeutral, true);
        }
        
        public String toString() {
            return "(? " + isNeutral + " " + isMutable + ")";
        }
    }
}
