package rubble.data;

import java.util.ArrayList;

import rubble.data.Names.*;

/**
 * A container class for types, type-level numbers, and witnesses indicating
 * the phase of compilation the types are being used at.  <Name> is
 * instantiated with String when the AST is parsed and ResolvedName after
 * name resolution.  <Phase> is instantiated with Parsed after parsing,
 * Poly after initialization, and Mono after the types are resolved to
 * monomorphic types.
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
        
        public abstract Nat<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError;
    }
    
    public static final class NatExternal<Name, Phase> extends Nat<Name, Phase> {
        
        public final Location loc;
        public final Name name;
        
        public NatExternal(Location loc, Name name) {
            super(NatTag.NatVar);
            this.loc = loc;
            this.name = name;
        }
        
        public Nat<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new NatExternal<ResolvedName, Poly>(this.loc, context.resolve(loc, name.toString()));
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
        
        public Nat<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new NatKnown<ResolvedName, Poly>(nat);
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
        
        public Nat<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            throw CompilerError.ice(new Location(1,1,1,1), "resolveNames() was called on a NatLiteral.");
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
        
        public Nat<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return this;
        }
        
        public String toString() {
            return "{NV " + index + "}";
        }
    }
    
    public static final class NatUnknown extends Nat<String, Parsed> {
        
        public NatUnknown() {
            super(NatTag.NatUnknown);
        }
        
        public Nat<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new NatVar(context.natLevel++);
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
    
    
    public static abstract class Type<Name, Phase> {
        
        public final Tag tag;
        
        public Type(Tag tag) {
            this.tag = tag;
        }
        
        public abstract Type<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError;
    }
    
    public static final class Arrow<Name, Phase> extends Type<Name, Phase> {
        
        public final ArrayList<Variable<Name, Phase>> domain;
        public final Type<Name, Phase> codomain;
        
        public Arrow(ArrayList<Variable<Name, Phase>> domain, Type<Name, Phase> codomain) {
            super(Tag.Arrow);
            this.domain = domain;
            this.codomain = codomain;
        }
        
        public Type<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
        	ArrayList<Variable<ResolvedName, Poly>> newDomain = new ArrayList<Variable<ResolvedName, Poly>>();
        	
        	for (Variable<Name, Phase> var: domain) {
        		newDomain.add(var.resolveNames(context));
        	}
        	
            return new Arrow<ResolvedName, Poly>(newDomain, codomain.resolveNames(context));
        }
        
        public String toString() {
            return "(Arrow " + domain.toString() + "->" + codomain.toString() + ")";
        }
    }
    
    public static final class Buffer<Name, Phase> extends Type<Name, Phase> {
        
        public final Mode containedMode;
        public final Type<Name, Phase> contained;
        public final Nat<Name, Phase> size;
        
        public Buffer(Nat<Name, Phase> size, Mode containedMode, Type<Name, Phase> contained) {
            super(Tag.Buffer);
            this.size = size;
            this.containedMode = containedMode;
            this.contained = contained;
        }
        
        public Type<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new Buffer<ResolvedName, Poly>(size.resolveNames(context), containedMode, contained.resolveNames(context));
        }
        
        public String toString() {
            return "(Buffer " + size.toString() + " " + contained.toString() + ")";
        }
    }
    
    public static final class Ground extends Type<ResolvedName, Mono> {
        
        public final GroundTag groundTag;
        
        public Ground(GroundTag groundTag) {
            super(Tag.Ground);
            this.groundTag = groundTag;
        }
        
        public Type<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            throw CompilerError.ice(new Location(1,1), "resolveNames() was called on a ground type.");
        }
        
        public String toString() {
            return "(Ground " + groundTag.toString() + ")";
        }
    }
    
    public static final class Known<Name, Phase extends Parsed> extends Type<Name, Phase> {
        
        public final Type<ResolvedName, Mono> type;
        
        public Known(Type<ResolvedName, Mono> type) {
            super(Tag.Known);
            this.type = type;
        }
        
        public Type<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new Known<ResolvedName, Poly>(type);
        }
        
        public String toString() {
            return "<" + type.toString() + ">";
        }
    }
    
    public static final class Ptr<Name, Phase> extends Type<Name, Phase> {
        
        public final Mode pointeeMode;
        public final Type<Name, Phase> pointee;
        
        public Ptr(Mode pointeeMode, Type<Name, Phase> pointee) {
            super(Tag.Ptr);
            this.pointeeMode = pointeeMode;
            this.pointee = pointee;
        }
        
        public Type<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new Ptr<ResolvedName, Poly>(pointeeMode, pointee.resolveNames(context));
        }
        
        public String toString() {
            return "(Ptr " + pointee.toString() + ")";
        }
    }
    
    public static final class Tuple<Name, Phase> extends Type<Name, Phase> {
        
        public final ArrayList<Variable<Name, Phase>> members;
        
        public Tuple(ArrayList<Variable<Name, Phase>> members) {
            super(Tag.Tuple);
            this.members = members;
        }
        
        public Type<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            ArrayList<Variable<ResolvedName, Poly>> newMembers = new ArrayList<Variable<ResolvedName, Poly>>();
            for (Variable<Name, Phase> m: members) {
                newMembers.add(m.resolveNames(context));
            }
            return new Tuple<ResolvedName, Poly>(newMembers);
        }
        
        public String toString() {
            String ms = "";
            for (Variable<Name, Phase> t: members) {
                ms += t.toString();
            }
            return "(Tuple " + ms + ")";
        }
    }
    
    public static final class TypeVar extends Type<ResolvedName, Poly> {
        
        public int id;
        
        public TypeVar(int id) {
            super(Tag.TypeVar);
            this.id = id;
        }
        
        public Type<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return this;
        }
        
        public String toString() {
            return "(TypeVar " + id + ")";
        }
    }
    
    public static Unknown UNKNOWN = new Unknown();
    
    public static final class Unknown extends Type<String, Parsed> {
        
        public Unknown() {
            super(Tag.Unknown);
        }
        
        public Type<ResolvedName, Poly> resolveNames(NamingContext context) throws CompilerError {
            return new TypeVar(context.typeLevel++);
        }
        
        public String toString() {
            return "(?)";
        }
    }
}
