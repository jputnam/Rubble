package rubble.checks;

import java.util.ArrayList;

import rubble.data.AST;
import rubble.data.CompilerError;
import rubble.data.Types.*;


/**
 * Contains a function that conservatively checks whether all functions must
 * return a value.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class FunctionsReturn {
    
    /**
     * Checks that each function must return a value.  If a loop contains
     * a break, it assumes the break will be taken.  If a conditional exists,
     * it assumes that both legs can be taken.
     * 
     * @param loc
     * @param declarations
     * @throws CompilerError
     */
    public static void ensureFunctionsReturn(ArrayList<AST.Declaration<String, Parsed>> declarations) throws CompilerError {
        for (AST.Declaration<String, Parsed> d: declarations) {
            if (d.tag == AST.DeclarationTag.Def) {
                if (statementsReturn(((AST.Def<String, Parsed>)d).body) >= 0) {
                    throw CompilerError.check(d.loc, "The function may reach the end of control flow without returning a value.");
                }
            }
        }
    }
    
    /**
     * Checks whether a list of statements returns or is broken out of.  If it
     * returns or has a nonterminating loop, the value returned is negative.
     * If it is broken out of, the value returned is the number of loops
     * beside the current one to escape.  If it does not return, the value
     * is 0.  I have guaranteed elsewhere that break statements can't escape,
     * so I don't need to worry about that here.
     * 
     * @param body
     * @return 0 if the statements return, a small number if the
     * statements are broken out of, and a negative number otherwise.
     * @throws CompilerError
     */
    private static int statementsReturn(ArrayList<AST.Statement<String, Parsed>> body) throws CompilerError {
        for (int i = body.size() - 1; i >= 0; i--) {
            
            AST.Statement<String, Parsed> stmt = body.get(i);
            int depth;
            
            switch (stmt.tag) {
            case Break:
                return ((AST.Break<String, Parsed>)stmt).depth + 1;
            
            case Forever:
                depth = statementsReturn(((AST.Forever<String, Parsed>)stmt).body);
                switch (depth) {
                case 0:
                    return -1;
                case 1:
                    break;
                default:
                    return depth - 1;
                }
                break;
            
            case IfS:
                AST.IfS<String, Parsed> ifs = (AST.IfS<String, Parsed>) stmt;
                depth = Math.max(statementsReturn(ifs.trueBranch), statementsReturn(ifs.falseBranch));
                switch(depth) {
                case 0:
                    break;
                default:
                    return depth;
                }
                break;
            
            case Nested:
                depth = statementsReturn(((AST.Nested<String, Parsed>)stmt).body);
                switch (depth) {
                case 0:
                    break;
                default:
                    return depth;
                }
                break;
            
            case Return:
                return -1;
            default:
                // Assign, Call, and Let have no nested statements.
            }
        }
        return 0;
    }
}
