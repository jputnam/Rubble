package rubble.test;

import java.util.ArrayList;

import rubble.checks.FunctionsReturn;
import rubble.checks.MainExists;
import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.parser.Layout;
import rubble.parser.Lexer;
import rubble.parser.Parser;
import rubble.test.TestHarness.*;

/**
 * Tests the sanity checkers.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class TestChecker {

    private static String checkReturns(String decl) throws CompilerError {
        ArrayList<Token> tokens = new Layout(new Lexer(decl).lex()).layout();
        
        FunctionsReturn.ensureFunctionsReturn(Parser.parse(tokens));
        return "ok";
    }
    
    private static String mainExists(String decl) throws CompilerError {
        ArrayList<Token> tokens = new Layout(new Lexer(decl).lex()).layout();
        Location loc = (tokens.size() == 0) ? new Location(1,1) : new Location(tokens.get(0).loc, tokens.get(tokens.size() - 1).loc);
        
        MainExists.ensureMainExists(loc, Parser.parse(tokens));
        return "ok";
    }
    
    public static final TestHarness.TestCase[] cases = {
        new Matches() {
            public String name() { return "Functions return 1"; }
            public String expected() { return "ok"; }
            public String userCode() throws CompilerError {
                return checkReturns("def main() () { return () }"); // Return
            }
        },
        new Crashes() {
            public String name() { return "Functions return 2"; }
            public String expected() { return "@1,1,1,23 The function may reach the end of control flow without returning a value."; }
            public String userCode() throws CompilerError {
                return checkReturns("def foo() () { a = 5 }"); // Assign
            }
        },
        new Crashes() {
            public String name() { return "Functions return 3"; }
            public String expected() { return "@1,1,1,21 The function may reach the end of control flow without returning a value."; }
            public String userCode() throws CompilerError {
                return checkReturns("def foo() () { a b }"); // Call
            }
        },
        new Matches() {
            public String name() { return "Functions return 4"; }
            public String expected() { return "ok"; }
            public String userCode() throws CompilerError {
                return checkReturns("def foo() () do forever do"); // Forever
            }
        },
        new Crashes() {
            public String name() { return "Functions return 5"; }
            public String expected() { return "@1,1,1,27 The function may reach the end of control flow without returning a value."; }
            public String userCode() throws CompilerError {
                return checkReturns("def foo() () { let a = 5 }"); // Let
            }
        },
        new Crashes() {
            public String name() { return "Functions return 6"; }
            public String expected() { return "@1,1,1,62 The function may reach the end of control flow without returning a value."; }
            public String userCode() throws CompilerError {
                return checkReturns("def foo() () do a forever { forever { forever { break a } } }");
            }
        },
        new Matches() {
            public String name() { return "Functions return 7"; }
            public String expected() { return "ok"; }
            public String userCode() throws CompilerError {
                return checkReturns("def foo() () do a forever { forever { forever { break } ; return () } }");
            }
        },
        new Matches() {
            public String name() { return "Functions return 8"; }
            public String expected() { return "ok"; }
            public String userCode() throws CompilerError {
                return checkReturns("def foo() () do return () ; a = b; a b; let a = b; { a b }");
            }
        },
        new Crashes() {
            public String name() { return "Functions return 9"; }
            public String expected() { return "@1,1,1,47 The function may reach the end of control flow without returning a value."; }
            public String userCode() throws CompilerError {
                return checkReturns("def foo() () do a = b; a b; let a = b; { a b }");
            }
        },
        new Crashes() {
            public String name() { return "Functions return 10"; }
            public String expected() { return "@1,1,1,38 The function may reach the end of control flow without returning a value."; }
            public String userCode() throws CompilerError {
                return checkReturns("def foo() () do forever { { break } }");
            }
        },
        new Crashes() {
            public String name() { return "Functions return 11"; }
            public String expected() { return "@1,1,1,58 The function may reach the end of control flow without returning a value."; }
            public String userCode() throws CompilerError {
                return checkReturns("def foo() () do a forever { forever { { { break a } } } }");
            }
        },
        new Crashes() {
            public String name() { return "Functions return 12"; }
            public String expected() { return "@1,1,1,40 The function may reach the end of control flow without returning a value."; }
            public String userCode() throws CompilerError {
                return checkReturns("def foo() () do if a then { return () }");
            }
        },
        new Matches() {
            public String name() { return "Functions return 13"; }
            public String expected() { return "ok"; }
            public String userCode() throws CompilerError {
                return checkReturns("def foo() () do a forever { if a then { break } } ; return ()");
            }
        },
        new Crashes() {
            public String name() { return "Functions return 14"; }
            public String expected() { return "@1,1,1,50 The function may reach the end of control flow without returning a value."; }
            public String userCode() throws CompilerError {
                return checkReturns("def foo() () do a forever { if a then { break } }");
            }
        },
        new Matches() {
            public String name() { return "Functions return 15"; }
            public String expected() { return "ok"; }
            public String userCode() throws CompilerError {
                return checkReturns("def foo() () do a forever { if a then { break } } ; return ()");
            }
        },
        new Crashes() {
            public String name() { return "Functions return 16"; }
            public String expected() { return "@1,55,1,75 The function may reach the end of control flow without returning a value."; }
            public String userCode() throws CompilerError {
                return checkReturns("def foo() () { return 0 }; def bar() () { return 0 }; def baz() () { a b }");
            }
        },
        new Crashes() {
            public String name() { return "Main exists 1"; }
            public String expected() { return "@1,1,1,50 A function named main must exist."; }
            public String userCode() throws CompilerError {
                return mainExists("def foo() () {}; def bar() () {}; def baz() () {}");
            }
        },
        new Crashes() {
            public String name() { return "Main exists 2"; }
            public String expected() { return "@1,1,1,17 main() must return an immutable Int32."; }
            public String userCode() throws CompilerError {
                return mainExists("def main() () {}; def bar() () {}; def baz() () {}");
            }
        },
        new Crashes() {
            public String name() { return "Main exists 3"; }
            public String expected() { return "@1,18,1,34 main() must return an immutable Int32."; }
            public String userCode() throws CompilerError {
                return mainExists("def foo() () {}; def main() () {}; def baz() () {}");
            }
        },
        new Crashes() {
            public String name() { return "Main exists 4"; }
            public String expected() { return "@1,35,1,51 main() must return an immutable Int32."; }
            public String userCode() throws CompilerError {
                return mainExists("def foo() () {}; def bar() () {}; def main() () {}");
            }
        },
        new Crashes() {
            public String name() { return "Main exists 5"; }
            public String expected() { return "@1,1,1,21 main() must take one immutable () argument."; }
            public String userCode() throws CompilerError {
                return mainExists("def main(a) Int32 {}");
            }
        },
        new Crashes() {
            public String name() { return "Main exists 6"; }
            public String expected() { return "@1,1,1,29 main() must take one immutable () argument."; }
            public String userCode() throws CompilerError {
                return mainExists("def main(a: var ()) Int32 {}");
            }
        },
        new Matches() {
            public String name() { return "Main exists 7"; }
            public String expected() { return "ok"; }
            public String userCode() throws CompilerError {
                return mainExists("def main(a: ()) Int32 {}");
            }
        },
        new Crashes() {
            public String name() { return "Main exists 8"; }
            public String expected() { return "@1,27,1,55 main() must take one immutable () argument."; }
            public String userCode() throws CompilerError {
                return mainExists("def main(a: ()) Int32 {}; def main(a: var ()) Int32 {}");
            }
        }
    };
}
