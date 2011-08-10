package rubble.checks;

import java.util.ArrayList;
import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Types;


/**
 * Contains a function that checks whether main() exists and has the right
 * type.
 * 
 * @author Jared Putnam
 */
public final class MainExists {
    
    /**
     * The function answers two questions.  Does main exist, and, if so, does
     * it have the right type?  If it does, the function simply terminates.
     * If it doesn't, it throws an exception with the appropriate message.  It
     * does not validate that only main function exists.  If more than one main
     * exists, it checks all of them.
     * 
     * The correct type is (()) -> Int32.
     * 
     * @param loc
     * @param declarations
     * @throws CompilerError
     */
    public static void ensureMainExists(Location loc, ArrayList<AST.Declaration<Types.Parsed>> declarations) throws CompilerError {
        boolean found = false;
        for (AST.Declaration<Types.Parsed> d: declarations) {
            
            if (d.tag == AST.DeclarationTag.Def) {
                AST.Def<Types.Parsed> def = (AST.Def<Types.Parsed>)d;
                
                if (def.name.equals("main")) {
                    Types.Type<Types.Parsed> returnType = def.returnType;
                    if (returnType.isMutable == true || !isSpecificGround(returnType, Types.GroundTag.Int32)) {
                        throw CompilerError.parse(def.loc, "main() must return an immutable Int32.");
                    }
                    
                    ArrayList<AST.Reference<Types.Parsed>> args = def.arguments;
                    if (args.size() != 1) {
                        throw CompilerError.parse(def.loc, "main() must take one immutable () argument.");
                    }
                    
                    Types.Type<Types.Parsed> type = args.get(0).type;
                    if (type.isMutable == true || !isSpecificGround(type, Types.GroundTag.Unit)) {
                        throw CompilerError.parse(def.loc, "main() must take one immutable () argument.");
                    }
                    found = true;
                }
            }
        }
        if (!found) { throw CompilerError.parse(loc, "A function named main must exist."); }
    }
    
    private static boolean isSpecificGround(Types.Type<Types.Parsed> type, Types.GroundTag tag) {
        if (type.tag != Types.Tag.Known) { return false; }
        
        Types.Type<Types.Mono> inner = ((Types.Known<Types.Parsed>)type).type;
        if (inner.tag != Types.Tag.Ground) { return false; }
        
        return ((Types.Ground)inner).groundTag == tag;
    }
    
}
