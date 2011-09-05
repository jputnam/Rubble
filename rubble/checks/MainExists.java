package rubble.checks;

import java.util.ArrayList;
import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Variable;
import rubble.data.Names.*;
import rubble.data.Types;
import rubble.data.Types.*;


/**
 * Contains a function that checks whether main() exists and has the right
 * type.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
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
    public static void ensureMainExists(Location loc, ArrayList<AST.Declaration<String, Parsed>> declarations) throws CompilerError {
        boolean found = false;
        for (AST.Declaration<String, Parsed> d: declarations) {
            
            if (d.tag == AST.DeclarationTag.Def) {
                AST.Def<String, Parsed> def = (AST.Def<String, Parsed>)d;
                
                if (def.name.equals("main")) {
                    Type<String, Parsed> returnType = def.returnType;
                    if (!isSpecificGround(returnType, GroundTag.Int32)) {
                        throw CompilerError.parse(def.loc, "main() must return an immutable Int32.");
                    }
                    
                    ArrayList<Variable<String, Parsed>> args = def.arguments;
                    if (args.size() != 1) {
                        throw CompilerError.parse(def.loc, "main() must take one immutable () argument.");
                    }
                    
                    Type<String, Parsed> type = args.get(0).type;
                    if (!isSpecificGround(type, GroundTag.Unit)) {
                        throw CompilerError.parse(def.loc, "main() must take one immutable () argument.");
                    }
                    found = true;
                }
            }
        }
        if (!found) { throw CompilerError.parse(loc, "A function named main must exist."); }
    }
    
    private static boolean isSpecificGround(Type<String, Parsed> type, GroundTag tag) {
        if (type.tag != Types.Tag.Known) { return false; }
        
        Type<ResolvedName, Mono> inner = ((Known<String, Parsed>)type).type;
        if (inner.tag != Types.Tag.Ground) { return false; }
        
        return ((Ground)inner).groundTag == tag;
    }
    
}
