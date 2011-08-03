package rubble.checks;

import java.util.ArrayList;
import java.util.Hashtable;

import rubble.data.AST.*;
import rubble.data.Names.*;
import rubble.data.Types;
import rubble.data.Types.*;


/**
 * Resolves variable names.
 * 
 * @author Jared
 */
public final class NameResolution {
    
    // collect the global names (and their types?)
    // for each declaration
    //    for functions
    //        for each statement
    //           decorate the expressions
    //    for defs, decorate the expression
    // for each type
    
    int natNames;
    int varNames;
    
    public NameResolution() {
        natNames = 0;
        varNames = 0;
    }
    
    public Nat<ResolvedName, Poly> initializeNat(Nat<String, Parsed> t) {
        return null;
    }
    
    public Type<ResolvedName, Poly> initializeType(Type<String, Parsed> t) {
        switch(t.tag) {
        // Buffer
        case Arrow:
            return new Arrow<ResolvedName, Poly>(initializeType(((Arrow<ResolvedName, Parsed>)t).domain), initializeType(((Arrow<ResolvedName, Parsed>)t).codomain), t.isMutable);
        case Buffer:
            // ?
            return new Buffer<ResolvedName, Poly>(initializeNat(((Buffer<ResolvedName, Parsed>)t).size), initializeType(((Buffer<ResolvedName, Parsed>)t).contained), t.isMutable);
        case Known:
            return new Known<ResolvedName, Poly>(((Known<ResolvedName, Parsed>)t).type);
        case Ptr:
            return new Ptr<ResolvedName, Poly>(initializeType(((Ptr<ResolvedName, Parsed>)t).pointee), t.isMutable);
        case Tuple:
            ArrayList<Type<ResolvedName, Poly>> newMembers = new ArrayList<Type<ResolvedName, Poly>>();
            for (Type<ResolvedName, Parsed> member: ((Types.Tuple<ResolvedName, Parsed>)t).members) {
                newMembers.add(initializeType(member));
            }
            return new Types.Tuple<ResolvedName, Poly>(newMembers, t.isMutable);
        case Unknown:
            return newVar(t.isMutable);
        default: return null;
        }
    }
    
    public NatVar<ResolvedName> newNat() {
        return null;
    }
    
    public TypeVar<ResolvedName> newVar(boolean isMutable) {
        varNames++;
        return new TypeVar<ResolvedName>(varNames - 1, isMutable);
    }
    
    public ArrayList<Declaration<ResolvedName, Parsed>> resolve(ArrayList<Declaration<String, Parsed>> decls) {
        int nameSupply = 0;
        
        Hashtable<String, Type<ResolvedName, Poly>> globalLets = new Hashtable<String, Type<ResolvedName, Poly>>();
        Hashtable<String, Type<ResolvedName, Poly>> globalDefs = new Hashtable<String, Type<ResolvedName, Poly>>();
        
        for (Declaration<String, Parsed> d: decls) {
            // get the declared type.
            if (d.tag == DeclarationTag.GlobalLet) {
                for (Binding<String, Parsed> b: ((GlobalLet<String, Parsed>)d).bindings) {
                    for (Reference<String, Parsed> r: b.references) {
                        globalLets.put(r.name, initializeType(r.type));
                    }
                }
            }
            if (d.tag == DeclarationTag.Def){
                Def<String, Parsed> def = (Def<String, Parsed>)d;
                // globalDefs.put(def.name, initializeType(new Arrow<ResolvedName, Parsed>(null, null, false)));
            }
        }
        
        // Finalize the lets.  At this point, they are required to be monomorphic.
        
        return null;
    }
    
}
