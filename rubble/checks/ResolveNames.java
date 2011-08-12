package rubble.checks;

import java.util.ArrayList;

import rubble.data.AST.*;
import rubble.data.CompilerError;
import rubble.data.NamingContext;
import rubble.data.Types.*;


public final class ResolveNames {
    
    public static void resolveNames(ArrayList<Declaration<Parsed>> ds) throws CompilerError {
        
        // Name resolution is in definition order.  Why?  Because I don't want
        // to perform a topological sort to get a valid elaboration.
        NamingContext context = new NamingContext();
        for (Declaration<Parsed> d: ds) {
            d.resolveNames(context);
            context.discardNonGlobals();
        }
    }
}
