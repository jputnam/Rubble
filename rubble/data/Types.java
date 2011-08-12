package rubble.data;

import java.util.ArrayList;

import rubble.data.Names.*;

/**
 * A container class for types, type-level numbers, and witnesses indicating
 * the phase of compilation the types are being used at.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class Types {
    
    public static final class Mono { }
    
    public static abstract class Parsed { }
    
    public static final class Poly extends Parsed { }
    
    
    public static enum NatTag { NatExternal, NatKnown, NatLiteral, NatVar, NatUnknown }
    
    public static abstract class Nat<Name, Phase> {
        
        public final NatTag tag;
        
        public Nat(NatTag tag) {
            this.tag = tag;
        }
        
        public void resolveNames(NamingContext context) throws CompilerError { }
    }
    
    public static final class NatExternal<Name, Phase> extends Nat<Name, Phase> {
        
        public final Location loc;
        public final Name name;
        
        public NatExternal(Location loc, Name name) {
            super(NatTag.NatVar);
            this.loc = loc;
            this.name = name;
        }
        /*
        public void resolveNames(NamingContext context) throws CompilerError {
            name = context.resolve(loc, source);
        }
        */
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
    
    public static final class NatVar extends Nat<ResolvedName, Poly> {
        
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
    
    
    public static enum Tag {
        Arrow("a function"),
        Buffer("a buffer"),
        Ground("a ground type"),
        Known("a known type"),
        Ptr("a pointer"),
        Tuple("a tuple"),
        TypeVar("a type variable"),
        Unknown("an unknown type");
        
        public final String pretty;
        
        Tag(String pretty) {
            this.pretty = pretty;
        }
    }

    public static enum GroundTag {
        Boolean, Int8, Int16, Int32, Int64,
        Unit, UInt8, UInt16, UInt32, UInt64
    }
    
    
    public static abstract class Type<Phase> {
        
        public final boolean isMutable;
        public final Tag tag;
        
        public Type(Tag tag, boolean isMutable) {
            this.tag = tag;
            this.isMutable = isMutable;
        }
        
        public abstract Type<Phase> mutable();
        
        public abstract void resolveNames(NamingContext context) throws CompilerError;
    }
    
    public static final class Arrow<Phase> extends Type<Phase> {
        
        public final Type<Phase> domain;
        public final Type<Phase> codomain;
        
        public Arrow(Type<Phase> domain, Type<Phase> codomain, boolean isMutable) {
            super(Tag.Arrow, isMutable);
            this.domain = domain;
            this.codomain = codomain;
        }
        
        public Type<Phase> mutable() {
            return new Arrow<Phase>(domain, codomain, true);
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            domain.resolveNames(context);
            codomain.resolveNames(context);
        }
        
        public String toString() {
            return "(Arrow " + domain.toString() + "->" + codomain.toString() + " " + isMutable + ")";
        }
    }
    
    public static final class Buffer<Phase> extends Type<Phase> {
        
        public final Type<Phase> contained;
        public final Nat<Phase> size;
        
        public Buffer(Nat<Phase> size, Type<Phase> contained, boolean isMutable) {
            super(Tag.Buffer, isMutable);
            this.contained = contained;
            this.size = size;
        }
        
        public Type<Phase> mutable() {
            return new Buffer<Phase>(size, contained, true);
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            size.resolveNames(context);
            contained.resolveNames(context);
        }
        
        public String toString() {
            return "(Buffer " + size.toString() + " " + contained.toString() + ")";
        }
    }
    
    public static final class Ground extends Type<Mono> {
        
        public final GroundTag groundTag;
        
        public Ground(GroundTag groundTag, boolean isMutable) {
            super(Tag.Ground, isMutable);
            this.groundTag = groundTag;
        }
        
        public Type<Mono> mutable() {
            return new Ground(groundTag, true);
        }
        
        public void resolveNames(NamingContext context) { }
        
        public String toString() {
            return "(Ground " + groundTag.toString() + " " + isMutable + ")";
        }
    }
    
    public static final class Known<Phase extends Parsed> extends Type<Phase> {
        
        public final Type<Mono> type;
        
        public Known(Type<Mono> type) {
            super(Tag.Known, type.isMutable);
            this.type = type;
        }
        
        public Type<Phase> mutable() {
            return new Known<Phase>(type.mutable());
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            type.resolveNames(context);
        }
        
        public String toString() {
            return "<" + type.toString() + ">";
        }
    }
    
    public static final class Ptr<Phase> extends Type<Phase> {
        
        public final Type<Phase> pointee;
        
        public Ptr(Type<Phase> pointee, boolean isMutable) {
            super(Tag.Ptr, isMutable);
            this.pointee = pointee;
        }
        
        public Type<Phase> mutable() {
            return new Ptr<Phase>(pointee, true);
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            pointee.resolveNames(context);
        }
        
        public String toString() {
            return "(Ptr " + pointee.toString() + " " + isMutable + ")";
        }
    }
    
    public static final class Tuple<Phase> extends Type<Phase> {
        
        public final ArrayList<Type<Phase>> members;
        
        public Tuple(ArrayList<Type<Phase>> members, boolean isMutable) {
            super(Tag.Tuple, isMutable);
            this.members = members;
        }
        
        public Type<Phase> mutable() {
            return new Tuple<Phase>(members, true);
        }
        
        public void resolveNames(NamingContext context) throws CompilerError {
            for (Type<Phase> m: members) {
                m.resolveNames(context);
            }
        }
        
        public String toString() {
            String ms = "";
            for (Type<Phase> t: members) {
                ms += t.toString();
            }
            return "(Tuple " + ms + " " + isMutable + ")";
        }
    }
    
    public static final class TypeVar extends Type<Poly> {
        
        public int id;
        public final boolean isNeutral;
        
        public TypeVar(int id, boolean isNeutral, boolean isMutable) {
            super(Tag.TypeVar, isMutable);
            this.id = id;
            this.isNeutral = isNeutral;
        }
        
        public Type<Poly> mutable() {
            return new TypeVar(id, isNeutral, true);
        }

        public void resolveNames(NamingContext context) { }
        
        public String toString() {
            return "(TypeVar " + id + " " + isMutable + ")";
        }
    }
    
    public static Unknown UNKNOWN_IMMUTABLE = new Unknown(false, false);
    public static Unknown UNKNOWN_NEUTRAL = new Unknown(true, false);
    
    public static final class Unknown extends Type<Parsed> {
        
        // If the type is neutral, this overrides mutability; the type
        // variable can be unified with either a mutable type or an immutable one.
        public final boolean isNeutral;
        
        public Unknown(boolean isNeutral, boolean isMutable) {
            super(Tag.Unknown, isMutable);
            this.isNeutral = isNeutral;
        }
        
        public Type<Parsed> mutable() {
            return new Unknown(isNeutral, true);
        }
        
        public void resolveNames(NamingContext context) { }
        
        public String toString() {
            return "(? " + isNeutral + " " + isMutable + ")";
        }
    }
}
