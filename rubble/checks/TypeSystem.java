package rubble.checks;

import java.util.ArrayList;

import rubble.data.CompilerError;
import rubble.data.AST;
import rubble.data.AST.*;
import rubble.data.Location;
import rubble.data.Types;
import rubble.data.Types.*;


/**
 * Typechecks expressions and decorates the terms with the correct types.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class TypeSystem {
    /*
    private int nv;
    private int tv;
    
    public Expression<Poly> initializeSubexpression(Expression<Parsed> e, Type<Poly> expectedType) throws CompilerError {
        switch (e.tag) {
        case AddressOf: {
            Type<Poly> pointeeType;
            
            switch (expectedType.tag) {
            case Ptr:
                pointeeType = ((Ptr<Poly>)expectedType).pointee;
                break;
            case TypeVar:
                pointeeType = newTypeVar();
                break;
            default:
                throw CompilerError.check(e.loc, "The type checker expected " + expectedType.tag.pretty + " but found a pointer.");
            }
            return new AddressOf<Poly>(e.loc, expectedType, initializeSubexpression(((AddressOf<Parsed>)e).value, pointeeType));
        }
        
        case Apply: {
            Type<Poly> domain;
            Type<Poly> codomain;
            
            switch (expectedType.tag) {
            case Arrow:
                domain = ((Arrow<Poly>)expectedType).domain;
                codomain = ((Arrow<Poly>)expectedType).codomain;
                break;
            case TypeVar:
                domain = newTypeVar();
                codomain = newTypeVar();
                break;
            default:
                throw CompilerError.check(e.loc, "The type checker expected " + expectedType.tag.pretty + " but found a function.");
            }
            return new Apply<Poly>(e.loc, expectedType, initializeSubexpression(((Apply<Parsed>)e).function, domain), initializeSubexpression(((Apply<Parsed>)e).argument, codomain));
        }
        
        case BufferLiteral: {
            Type<Poly> contained;
            
            switch (expectedType.tag) {
            case Buffer:
                contained = ((Types.Buffer<Poly>)expectedType).contained;
                break;
            case TypeVar:
                contained = newTypeVar();
                break;
            default:
                throw CompilerError.check(e.loc, "The type checker expected " + expectedType.tag.pretty + " but found a buffer literal.");
            }
            ArrayList<Expression<Poly>> es = new ArrayList<Expression<Poly>>();
            for (Expression<Parsed> member: ((AST.BufferLiteral<Parsed>)e).es) {
                es.add(initializeSubexpression(member, contained));
            }
            return new AST.BufferLiteral<Poly>(e.loc, expectedType, es);
        }
        
        case IfE: {
            // Is this right?  I would expect it not to matter whether it was mutable...
            // Should it be neutral?
            
            // Should I separate modes out again?
            Type<Poly> bool = new Known<Poly>(new Ground(GroundTag.Boolean, false));
        }
        case Index:
        case Number:
        case Tuple:
        case ValueAt:
        case Variable:
        default: throw CompilerError.ice(e.loc, "Unhandled expression in initializeSubexpression.  The problematic term was " + e.toString());
        }
    }
    
    private Nat<Poly> initializeNat(Location loc, Nat<Parsed> nat) throws CompilerError {
        switch (nat.tag) {
        case NatExternal:
            NatExternal<Poly> ne = new NatExternal<Poly>(((NatExternal<Parsed>)nat).loc, ((NatExternal<Parsed>)nat).source);
            ne.name = ((NatExternal<Parsed>)nat).name;
            return ne;
            
        case NatKnown:
            return new NatKnown<Poly>(((NatKnown<Parsed>)nat).nat);
            
        case NatUnknown:
            return new NatVar(nv++);

        default: throw CompilerError.ice(loc, "Unhandled nat in initializeNat.  The problematic term was " + nat.toString());
        }
    }
    
    private Type<Poly> initializeType(Location loc, Type<Parsed> tau) throws CompilerError {
        switch (tau.tag) {
        case Arrow:
            return new Arrow<Poly>(initializeType(loc, ((Arrow<Parsed>)tau).domain), initializeType(loc, ((Arrow<Parsed>)tau).codomain), tau.isMutable);
            
        case Buffer:
            return new Buffer<Poly>(initializeNat(loc, ((Buffer<Parsed>)tau).size), initializeType(loc, ((Buffer<Parsed>)tau).contained), tau.isMutable);
            
        case Known:
            return new Known<Poly>(((Known<Parsed>)tau).type);
            
        case Ptr:
            return new Ptr<Poly>(initializeType(loc, ((Ptr<Parsed>)tau).pointee), tau.isMutable);
            
        case Tuple:
            ArrayList<Type<Poly>> ts = new ArrayList<Type<Poly>>(); // ((Types.Tuple<Parsed>)tau).members;
            for (Type<Parsed> t: ((Types.Tuple<Parsed>)tau).members) {
                ts.add(initializeType(loc, t));
            }
            return new Types.Tuple<Poly>(ts, tau.isMutable);
            
        case Unknown:
            return new TypeVar(tv++, ((Unknown)tau).isNeutral, tau.isMutable);
            
        default:
            throw CompilerError.ice(loc, "Unhandled type in initializeType.  The problematic term was " + tau.toString());
        }
    }
    
    private TypeVar newTypeVar() {
        return new TypeVar(tv++, true, false);
    } */
}
