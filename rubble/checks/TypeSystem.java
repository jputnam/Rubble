package rubble.test;

import rubble.data.AST;
import rubble.data.Types;
import rubble.data.Unit;


/**
 * Typechecks expressions and decorates the terms with the correct types.
 * 
 * @author Jared Putnam
 */
public final class TypeSystem {
    
    public static AST.Expression<Types.Type<Types.Poly>, Types.Poly, String> check(AST.Expression<Unit, Types.Parsed, String> ast) {
        return null;
    }
    
}
