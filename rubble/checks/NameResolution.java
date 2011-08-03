package rubble.test;

import java.util.ArrayList;
import java.util.Hashtable;

import rubble.data.AST.*;
import rubble.data.Names.*;
import rubble.data.Types.*;
import rubble.data.Unit;


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
    
    int nameSupply;
    
    public NameResolution() {
        nameSupply = 0;
    }
    
    public Type<Poly> initializeType(Type<Parsed> t) {
        return null;
    }
    
    public TypeVar newVar(boolean isMutable) {
        nameSupply++;
        return new TypeVar(nameSupply - 1, isMutable);
    }
    
    public ArrayList<Declaration<Unit, Parsed, ResolvedName>> resolve(ArrayList<Declaration<Unit, Parsed, String>> decls) {
        int nameSupply = 0;
        
        Hashtable<String, Type<Poly>> globalLets = new Hashtable<String, Type<Poly>>();
        Hashtable<String, Type<Poly>> globalDefs = new Hashtable<String, Type<Poly>>();
        
        for (Declaration<Unit, Parsed, String> d: decls) {
            // get the declared type.
            if (d.tag == DeclarationTag.GlobalLet) {
                for (Binding<Unit, Parsed, String> b: ((GlobalLet<Unit, Parsed, String>)d).bindings) {
                    for (Reference<Parsed, String> r: b.references) {
                        globalLets.put(r.name, initializeType(r.type));
                    }
                }
            }
            if (d.tag == DeclarationTag.Def){
                Def<Unit, Parsed, String> def = (Def<Unit, Parsed, String>)d;
                globalDefs.put(def.name, initializeType(new Arrow<Parsed>(null, null, false)));
            }
        }
        
        // Finalize the lets.  At this point, they are required to be monomorphic.
        
        return null;
    }
    
}
