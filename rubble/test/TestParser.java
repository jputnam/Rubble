package rubble.test;

import java.util.ArrayList;

import rubble.data.CompilerError;
import rubble.data.Location;
import rubble.data.Token;
import rubble.parser.Declaration;
import rubble.parser.Expression;
import rubble.parser.Lexer;
import rubble.parser.Layout;
import rubble.parser.Statement;
import rubble.parser.Type;
import rubble.test.TestHarness.*;

/**
 * Tests the parsers.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class TestParser {
    
    private static String parseDecl(String decl) throws CompilerError {
        ArrayList<Token> tokens = new Layout(new Lexer(decl).lex()).layout();
        Location loc = (tokens.size() == 0) ? new Location(1,1) : new Location(tokens.get(0).loc, tokens.get(tokens.size() - 1).loc);
        return new Declaration(loc, tokens).parse(0).toString();
    }
    
    private static String parseExpr(String decl) throws CompilerError {
        ArrayList<Token> tokens = new Layout(new Lexer(decl).lex()).layout();
        Location loc = (tokens.size() == 0) ? new Location(1,1) : new Location(tokens.get(0).loc, tokens.get(tokens.size() - 1).loc);
        return new Expression(loc, tokens).parse(0).toString();
    }
    
    private static String parseStmt(String decl) throws CompilerError {
        ArrayList<Token> tokens = new Layout(new Lexer(decl).lex()).layout();
        Location loc = (tokens.size() == 0) ? new Location(1,1) : new Location(tokens.get(0).loc, tokens.get(tokens.size() - 1).loc);
        return new Statement(loc, tokens).parse(0).toString();
    }
    
    private static String parseType(String decl) throws CompilerError {
        ArrayList<Token> tokens = new Layout(new Lexer(decl).lex()).layout();
        Location loc = (tokens.size() == 0) ? new Location(1,1) : new Location(tokens.get(0).loc, tokens.get(tokens.size() - 1).loc);
        return new Type(loc, tokens).parse(0).toString();
    }
    
    public static final TestHarness.TestCase[] cases = {
        new Matches() {
            public String name() { return "Expression 1"; }
            public String expected() { return "(A @1,1,1,2 (Var @1,1,1,2 {a}) $ (Var @1,3,1,4 {b}))"; }
            public String userCode() throws CompilerError {
                return parseExpr("a b");
            }
        },
        new Matches() {
            public String name() { return "Expression 2"; }
            public String expected() { return "(A @1,4,1,5 (Var @1,4,1,5 {b}) $ (Tuple @1,1,1,2 (Var @1,1,1,2 {a})(@1,7,1,8 {1})))"; }
            public String userCode() throws CompilerError {
                return parseExpr("a `b` 1");
            }
        },
        new Matches() {
            public String name() { return "Expression 3"; }
            public String expected() { return "(A @1,4,1,5 (Var @1,4,1,5 {b}) $ (Tuple @1,6,1,7 (Var @1,6,1,7 {c})(Var @1,1,1,2 {a})(@1,9,1,10 {1})))"; }
            public String userCode() throws CompilerError {
                return parseExpr("a `b c` 1");
            }
        },
        new Matches() {
            public String name() { return "Expression 4"; }
            public String expected() { return "(A @1,3,1,4 (Var @1,3,1,4 {+}) $ (Tuple @1,1,1,2 (Var @1,1,1,2 {a})(@1,5,1,6 {1})))"; }
            public String userCode() throws CompilerError {
                return parseExpr("a + 1");
            }
        },
        new Matches() {
            public String name() { return "Expression 5"; }
            public String expected() { return "(A @1,1,1,2 (Var @1,1,1,2 {a}) $ (A @1,3,1,4 (Var @1,3,1,4 {b}) $ (A @1,5,1,6 (Var @1,5,1,6 {c}) $ (A @1,7,1,8 (Var @1,7,1,8 {d}) $ (Var @1,9,1,10 {e})))))"; }
            public String userCode() throws CompilerError {
                return parseExpr("a b c d e");
            }
        },
        new Matches() {
            public String name() { return "Expression 6"; }
            public String expected() { return "(A @1,3,1,4 (Var @1,3,1,4 {+}) $ (Tuple @1,1,1,2 (@1,1,1,2 {1})(A @1,11,1,12 (Var @1,11,1,12 {/}) $ (Tuple @1,5,1,6 (A @1,5,1,6 (Var @1,5,1,6 {a}) $ (A @1,7,1,8 (Var @1,7,1,8 {b}) $ (Var @1,9,1,10 {c})))(Var @1,13,1,14 {d})))))"; }
            public String userCode() throws CompilerError {
                return parseExpr("1 + a b c / d");
            }
        },
        new Matches() {
            public String name() { return "Expression 7"; }
            public String expected() { return "(A @1,1,1,2 (Var @1,1,1,2 {a}) $ (A @1,3,1,4 (Index @1,3,1,4 (Var @1,3,1,4 {b}) (Var @1,6,1,7 {c})) $ (Var @1,9,1,10 {d})))"; }

            public String userCode() throws CompilerError {
                return parseExpr("a b [c] d");
            }
        },
        new Matches() {
            public String name() { return "Expression 8"; }
            public String expected() { return "(A @1,2,1,3 (A @1,2,1,3 (Var @1,2,1,3 {a}) $ (Var @1,4,1,5 {b})) $ (Var @1,7,1,8 {c}))"; }

            public String userCode() throws CompilerError {
                return parseExpr("(a b) c");
            }
        },
        new Matches() {
            public String name() { return "Expression 9"; }
            public String expected() { return "(A @1,7,1,8 (Var @1,7,1,8 {+}) $ (Tuple @1,1,1,2 (A @1,1,1,2 (Var @1,1,1,2 {a}) $ (A @1,3,1,4 (Var @1,3,1,4 {b}) $ (Var @1,5,1,6 {c})))(A @1,9,1,10 (Var @1,9,1,10 {d}) $ (A @1,11,1,12 (Var @1,11,1,12 {e}) $ (Var @1,13,1,14 {f})))))"; }
            public String userCode() throws CompilerError {
                return parseExpr("a b c + d e f");
            }
        },
        new Matches() {
            public String name() { return "Expression 10"; }
            public String expected() { return "[@1,1,1,3 ]"; }
            public String userCode() throws CompilerError {
                return parseExpr("[]");
            }
        },
        new Matches() {
            public String name() { return "Expression 11"; }
            public String expected() { return "[@1,1,1,4 (@1,2,1,3 {1})]"; }
            public String userCode() throws CompilerError {
                return parseExpr("[1]");
            }
        },
        new Matches() {
            public String name() { return "Expression 12"; }
            public String expected() { return "[@1,1,1,10 (@1,2,1,3 {1})(@1,4,1,5 {2})(@1,6,1,7 {3})(@1,8,1,9 {4})]"; }
            public String userCode() throws CompilerError {
                return parseExpr("[1,2,3,4]");
            }
        },
        new Crashes() {
            public String name() { return "Expression 13"; }
            public String expected() { return "@1,1,1,5 The parser expected an expression but ran out of input."; }
            public String userCode() throws CompilerError {
                return parseExpr("[1,]");
            }
        },
        new Crashes() {
            public String name() { return "Expression 14"; }
            public String expected() { return "@1,2,1,4 The parser expected an expression but ran out of input."; }
            public String userCode() throws CompilerError {
                return parseExpr("a[]");
            }
        },
        new Crashes() {
            public String name() { return "Expression 15"; }
            public String expected() { return "@1,4,1,5 The parser expected ] but found ,."; }
            public String userCode() throws CompilerError {
                return parseExpr("a[1,]");
            }
        },
        new Matches() {
            public String name() { return "Expression 16"; }
            public String expected() { return "(A @1,2,1,3 (Var @1,2,1,3 {*}) $ (Tuple @1,1,1,2 (@1,1,1,2 {1})(A @1,5,1,6 (Var @1,5,1,6 {+}) $ (Tuple @1,4,1,5 (@1,4,1,5 {2})(@1,6,1,7 {3})))))"; }
            public String userCode() throws CompilerError {
                return parseExpr("1*(2+3)");
            }
        },
        new Matches() {
            public String name() { return "Expression 17"; }
            public String expected() { return "(A @1,7,1,8 (Var @1,7,1,8 {+}) $ (Tuple @1,3,1,4 (A @1,3,1,4 (Var @1,3,1,4 {+}) $ (Tuple @1,1,1,2 (Var @1,1,1,2 {a})(Var @1,5,1,6 {b})))(Var @1,9,1,10 {c})))"; }
            public String userCode() throws CompilerError {
                return parseExpr("a + b + c");
            }
        },
        new Matches() {
            public String name() { return "Expression 18"; }
            public String expected() { return "(A @1,21,1,23 (Var @1,21,1,23 {&&}) $ (Tuple @1,7,1,9 (A @1,7,1,9 (Var @1,7,1,9 {&&}) $ (Tuple @1,1,1,6 (Var @1,1,1,6 {false})(A @1,12,1,14 (Var @1,12,1,14 {==}) $ (Tuple @1,10,1,11 (@1,10,1,11 {1})(A @1,17,1,18 (Var @1,17,1,18 {+}) $ (Tuple @1,15,1,16 (@1,15,1,16 {4})(Var @1,19,1,20 {a})))))))(A @1,29,1,31 (Var @1,29,1,31 {==}) $ (Tuple @1,24,1,28 (Var @1,24,1,28 {true})(A @1,34,1,35 (Var @1,34,1,35 {<}) $ (Tuple @1,32,1,33 (Var @1,32,1,33 {b})(Var @1,36,1,37 {c})))))))"; }
            public String userCode() throws CompilerError {
                return parseExpr("false && 1 == 4 + a && true == b < c");
            }
        },
/* 
 * (A @1,21,1,23 (Var @1,21,1,23 {&&}) $ (Tuple @1,7,1,9
 *     (A @1,7,1,9 (Var @1,7,1,9 {&&}) $ (Tuple @1,1,1,6
 *         (Var @1,1,1,6 {false})
 *         (A @1,12,1,14 (Var @1,12,1,14 {==}) $ (Tuple @1,10,1,11
 *             (@1,10,1,11 {1})
 *             (A @1,17,1,18 (Var @1,17,1,18 {+}) $ (Tuple @1,15,1,16
 *                 (@1,15,1,16 {4})
 *                 (Var @1,19,1,20 {a})
 *             ))
 *         ))
 *     ))
 *     (A @1,29,1,31 (Var @1,29,1,31 {==}) $ (Tuple @1,24,1,28
 *         (Var @1,24,1,28 {true})
 *         (A @1,34,1,35 (Var @1,34,1,35 {<}) $ (Tuple @1,32,1,33
 *             (Var @1,32,1,33 {b})
 *             (Var @1,36,1,37 {c})
 *         ))
 *     ))
 * ))
 */
        new Crashes() {
            public String name() { return "Type 1"; }
            public String expected() { return "@1,1,1,5 The parser expected a type but found Int7."; }
            public String userCode() throws CompilerError {
                return parseType("Int7");
            }
        },
        new Matches() {
            public String name() { return "Type 2"; }
            public String expected() { return "<(Ground Int8 false)>"; }
            public String userCode() throws CompilerError {
                return parseType("Int8 2");
            }
        },
        new Matches() {
            public String name() { return "Type 3"; }
            public String expected() { return "<(Ground Int8 true)>"; }
            public String userCode() throws CompilerError {
                return parseType("var Int8");
            }
        },
        new Matches() {
            public String name() { return "Type 4"; }
            public String expected() { return "(Arrow <(Ground Boolean false)>-><(Ground Boolean false)> false)"; }
            public String userCode() throws CompilerError {
                return parseType("(Boolean) -> Boolean");
            }
        },
        new Matches() {
            public String name() { return "Type 5"; }
            public String expected() { return "(Arrow <(Ground Boolean true)>-><(Ground Boolean false)> false)"; }
            public String userCode() throws CompilerError {
                return parseType("(var Boolean) -> Boolean");
            }
        },
        new Matches() {
            public String name() { return "Type 6"; }
            public String expected() { return "(Buffer <{4}> (Arrow <(Ground Boolean false)>-><(Ground Boolean false)> false))"; }
            public String userCode() throws CompilerError {
                return parseType("Buffer[4, (Boolean) -> Boolean]");
            }
        },
        new Crashes() {
            public String name() { return "Type 7"; }
            public String expected() { return "@1,8,1,9 The parser expected a positive integer but found 0."; }
            public String userCode() throws CompilerError {
                return parseType("Buffer[0, Unit]");
            }
        },
        new Crashes() {
            public String name() { return "Type 8"; }
            public String expected() { return "@1,8,1,10 The parser expected a positive integer but found -1."; }
            public String userCode() throws CompilerError {
                return parseType("Buffer[-1, Unit]");
            }
        },
        new Matches() {
            public String name() { return "Type 9"; }
            public String expected() { return "(Buffer ? <(Ground Unit false)>)"; }
            public String userCode() throws CompilerError {
                return parseType("Buffer[_, ()]");
            }
        },
        new Matches() {
            public String name() { return "Type 10"; }
            public String expected() { return "(Buffer {NE a} <(Ground Unit false)>)"; }
            public String userCode() throws CompilerError {
                return parseType("Buffer[a, ()]");
            }
        },
        new Matches() {
            public String name() { return "Typed expression 1"; }
            public String expected() { return "(AsType @1,1,1,2 (Var @1,1,1,2 {a}) : <(Ground Int8 false)>)"; }
            public String userCode() throws CompilerError {
                return parseExpr("a: Int8");
            }
        },
        new Crashes() {
            public String name() { return "Statement 1"; }
            public String expected() { return "@1,1,1,1 The parser expected a statement but ran out of input."; }
            public String userCode() throws CompilerError {
                return parseStmt("");
            }
        },
        new Crashes() {
            public String name() { return "Statement 2"; }
            public String expected() { return "@1,1,1,7 The parser expected an expression but ran out of input."; }
            public String userCode() throws CompilerError {
                return parseStmt("return");
            }
        },
        new Matches() {
            public String name() { return "Statement 3"; }
            public String expected() { return "(Return @1,1,1,7 (Var @1,8,1,9 {a}))"; }
            public String userCode() throws CompilerError {
                return parseStmt("return a");
            }
        },
        new Matches() {
            public String name() { return "Statement 4"; }
            public String expected() { return "(Return @1,1,1,7 (Tuple @1,8,1,11 (Var @1,8,1,9 {a})(Var @1,10,1,11 {b})))"; }
            public String userCode() throws CompilerError {
                return parseStmt("return a,b");
            }
        },
        new Matches() {
            public String name() { return "Statement 5"; }
            public String expected() { return "(Return @1,1,1,7 (Tuple @1,8,1,13 (Var @1,8,1,9 {a})(Var @1,10,1,11 {b})(Var @1,12,1,13 {c})))"; }
            public String userCode() throws CompilerError {
                return parseStmt("return a,b,c");
            }
        },
        new Crashes() {
            public String name() { return "Statement 6"; }
            public String expected() { return "@1,1,1,2 The parser expected a statement but found an incomplete statement."; }
            public String userCode() throws CompilerError {
                return parseStmt("a");
            }
        },
        new Matches() {
            public String name() { return "Statement 7"; }
            public String expected() { return "(Call @1,1,1,2 (Var @1,1,1,2 {a}) (Var @1,3,1,4 {b}))"; }
            public String userCode() throws CompilerError {
                return parseStmt("a b");
            }
        },
        new Crashes() {
            public String name() { return "Statement 8"; }
            public String expected() { return "@1,1,1,2 The parser expected a statement but found a."; }
            public String userCode() throws CompilerError {
                return parseStmt("a b, c");
            }
        },
        new Matches() {
            public String name() { return "Statement 9"; }
            public String expected() { return "(Assign @1,1,1,2 (Direct @1,1,1,2 {a}) (@1,5,1,6 {5}))"; }
            public String userCode() throws CompilerError {
                return parseStmt("a = 5");
            }
        },
        new Matches() {
            public String name() { return "Statement 10"; }
            public String expected() { return "(Assign @1,1,1,2 (TupleL @1,1,1,5 (Direct @1,1,1,2 {a})(Direct @1,4,1,5 {b})) (@1,8,1,9 {5}))"; }
            public String userCode() throws CompilerError {
                return parseStmt("a, b = 5");
            }
        },
        new Matches() {
            public String name() { return "Statement 11"; }
            public String expected() { return "(Assign @1,1,1,2 (TupleL @1,1,1,8 (Direct @1,1,1,2 {a})(Direct @1,4,1,5 {b})(Direct @1,7,1,8 {c})) (Tuple @1,11,1,18 (@1,11,1,12 {1})(@1,14,1,15 {2})(@1,17,1,18 {3})))"; }
            public String userCode() throws CompilerError {
                return parseStmt("a, b, c = 1, 2, 3");
            }
        },
        new Matches() {
            public String name() { return "Statement 12"; }
            public String expected() { return "(Assign @1,1,1,2 (Indirect @1,1,1,2 (Var @1,2,1,3 {a})) (@1,6,1,7 {1}))"; }
            public String userCode() throws CompilerError {
                return parseStmt("*a = 1");
            }
        },
        new Crashes() {
            public String name() { return "Statement 13"; }
            public String expected() { return "@1,1,1,2 The parser expected a statement but found addressOf."; }
            public String userCode() throws CompilerError {
                return parseStmt("&a = 1");
            }
        },
        new Matches() {
            public String name() { return "Statement 14"; }
            public String expected() { return "(Assign @1,1,1,2 (IndexL @1,1,1,2 (Direct @1,1,1,2 {a})[(Var @1,3,1,4 {b})]) (Var @1,8,1,9 {c}))"; }
            public String userCode() throws CompilerError {
                return parseStmt("a[b] = c");
            }
        },
        new Matches() {
            public String name() { return "Statement 15"; }
            public String expected() { return "(Assign @1,1,1,2 (IndexL @1,1,1,2 (Direct @1,1,1,2 {a})[(A @1,3,1,4 (Var @1,3,1,4 {b}) $ (Var @1,5,1,6 {c}))]) (Var @1,10,1,11 {d}))"; }
            public String userCode() throws CompilerError {
                return parseStmt("a[b c] = d");
            }
        },
        new Matches() {
            public String name() { return "Statement 16"; }
            public String expected() { return "(Assign @1,1,1,5 (IndexL @1,2,1,3 (Indirect @1,2,1,3 (Var @1,3,1,4 {a}))[(A @1,6,1,7 (* @1,6,1,7 (Var @1,7,1,8 {b})) $ (Var @1,9,1,10 {c}))]) (Var @1,14,1,15 {c}))"; }
            public String userCode() throws CompilerError {
                return parseStmt("(*a)[*b c] = c");
            }
        },
        new Crashes() {
            public String name() { return "Statement 17"; }
            public String expected() { return "@1,1,1,8 The parser expected { but ran out of input."; }
            public String userCode() throws CompilerError {
                return parseStmt("forever");
            }
        },
        new Matches() {
            public String name() { return "Statement 18"; }
            public String expected() { return "(Forever @1,1,1,8 {} )"; }
            public String userCode() throws CompilerError {
                return parseStmt("forever do");
            }
        },
        new Matches() {
            public String name() { return "Statement 19"; }
            public String expected() { return "(Forever @1,1,1,4 {abc} )"; }
            public String userCode() throws CompilerError {
                return parseStmt("abc forever do");
            }
        },
        new Matches() {
            public String name() { return "Statement 20"; }
            public String expected() { return "(Forever @1,1,1,4 {abc} (Call @1,16,1,24 (Var @1,16,1,24 {freebird}) (Var @1,24,1,26 {()})))"; }
            public String userCode() throws CompilerError {
                return parseStmt("abc forever do freebird()");
            }
        },
        new Crashes() {
            public String name() { return "Statement 21"; }
            public String expected() { return "@1,1,1,6 There is no enclosing loop to break out of."; }
            public String userCode() throws CompilerError {
                return parseStmt("break");
            }
        },
        new Matches() {
            public String name() { return "Statement 22"; }
            public String expected() { return "(Forever @1,1,1,8 {} (Break @1,12,1,17 0))"; }
            public String userCode() throws CompilerError {
                return parseStmt("forever do break");
            }
        },
        new Matches() {
            public String name() { return "Statement 23"; }
            public String expected() { return "(Forever @1,1,1,4 {abc} (Forever @1,16,1,23 {} (Break @1,27,1,32 1)))"; }
            public String userCode() throws CompilerError {
                return parseStmt("abc forever do forever do break abc");
            }
        },
        new Crashes() {
            public String name() { return "Statement 24"; }
            public String expected() { return "@1,12,1,17 The break target was not found."; }
            public String userCode() throws CompilerError {
                return parseStmt("forever do break abc");
            }
        },
        new Crashes() {
            public String name() { return "Statement 25"; }
            public String expected() { return "@1,1,1,6 The parser expected = but ran out of input."; }
            public String userCode() throws CompilerError {
                return parseStmt("let x");
            }
        },
        new Matches() {
            public String name() { return "Statement 26"; }
            public String expected() { return "(Let @1,1,1,10 (Binding @1,5,1,10 {x (? false false)}(@1,9,1,10 {1})))"; }
            public String userCode() throws CompilerError {
                return parseStmt("let x = 1");
            }
        },
        new Crashes() {
            public String name() { return "Statement 27"; }
            public String expected() { return "@1,1,1,4 The parser expected an identifier or binding block but found 1."; }
            public String userCode() throws CompilerError {
                return parseStmt("let 1 = x");
            }
        },
        new Matches() {
            public String name() { return "Statement 28"; }
            public String expected() { return "(Let @1,1,1,16 (Binding @1,5,1,16 {x (? false false)}(Tuple @1,9,1,16 (@1,9,1,10 {1})(@1,12,1,13 {2})(@1,15,1,16 {3}))))"; }
            public String userCode() throws CompilerError {
                return parseStmt("let x = 1, 2, 3");
            }
        },
        new Matches() {
            public String name() { return "Statement 29"; }
            public String expected() { return "(Let @1,1,1,19 (Binding @1,5,1,19 {a (? false false)}{b (? false false)}{c (? false false)}{d (? false false)}(@1,18,1,19 {1})))"; }
            public String userCode() throws CompilerError {
                return parseStmt("let a, b, c, d = 1");
            }
        },
        new Matches() {
            public String name() { return "Statement 30"; }
            public String expected() { return "(Let @1,1,1,27 (Binding @1,5,1,27 {a (? false false)}{b (? false false)}{c <(Ground UInt64 false)>}(@1,26,1,27 {1})))"; }
            public String userCode() throws CompilerError {
                return parseStmt("let a, b: _, c: UInt64 = 1");
            }
        },
        new Matches() {
            public String name() { return "Statement 31"; }
            public String expected() { return "(Let @1,1,1,23 (Binding @1,5,1,23 {a <(Ground Int32 false)>}{b <(Ground Int32 false)>}{c (? false false)}(@1,22,1,23 {1})))"; }
            public String userCode() throws CompilerError {
                return parseStmt("let a, b: Int32, c = 1");
            }
        },
        new Matches() {
            public String name() { return "Statement 32"; }
            public String expected() { return "(Let @1,1,1,17 (Binding @1,5,1,17 {a (? false true)}(@1,16,1,17 {1})))"; }
            public String userCode() throws CompilerError {
                return parseStmt("let a: var _ = 1");
            }
        },
        new Matches() {
            public String name() { return "Statement 33"; }
            public String expected() { return "(Let @1,1,1,21 (Binding @1,5,1,21 {a <(Ground Int16 true)>}(@1,20,1,21 {1})))"; }
            public String userCode() throws CompilerError {
                return parseStmt("let a: var Int16 = 1");
            }
        },
        new Matches() {
            public String name() { return "Statement 34"; }
            public String expected() { return "(Let @1,1,1,14 (Binding @1,5,1,14 {a (? false true)}(@1,13,1,14 {1})))"; }
            public String userCode() throws CompilerError {
                return parseStmt("let var a = 1");
            }
        },
        new Matches() {
            public String name() { return "Statement 35"; }
            public String expected() { return "(Let @1,1,1,17 (Binding @1,5,1,17 {a (? false true)}{b (? false false)}(@1,16,1,17 {1})))"; }
            public String userCode() throws CompilerError {
                return parseStmt("let var a, b = 1");
            }
        },
        new Matches() {
            public String name() { return "Statement 36"; }
            public String expected() { return "(Let @1,1,1,17 (Binding @1,5,1,17 {a (? false false)}{b (? false true)}(@1,16,1,17 {1})))"; }
            public String userCode() throws CompilerError {
                return parseStmt("let a, var b = 1");
            }
        },
        new Matches() {
            public String name() { return "Statement 37"; }
            public String expected() { return "(Let @1,1,1,21 (Binding @1,5,1,21 {a <(Ground Int16 true)>}(@1,20,1,21 {1})))"; }
            public String userCode() throws CompilerError {
                return parseStmt("let var a: Int16 = 1");
            }
        },
        new Crashes() {
            public String name() { return "Statement 38"; }
            public String expected() { return "@1,9,1,10 A variable may not be both marked as mutable and declared as having a mutable type."; }
            public String userCode() throws CompilerError {
                return parseStmt("let var a: var Int16 = 1");
            }
        },
        new Matches() {
            public String name() { return "Statement 39"; }
            public String expected() { return "(IfS @1,1,1,3 (Var @1,4,1,5 {a}) (Call @1,13,1,14 (Var @1,13,1,14 {b}) (Var @1,15,1,16 {c})) )"; }
            public String userCode() throws CompilerError {
                return parseStmt("if a then { b c }");
            }
        },
        new Matches() {
            public String name() { return "Statement 40"; }
            public String expected() { return "(IfS @1,1,1,3 (Var @1,4,1,5 {a}) (Call @1,13,1,14 (Var @1,13,1,14 {b}) (Var @1,15,1,16 {c})) (Call @1,26,1,27 (Var @1,26,1,27 {d}) (Var @1,28,1,29 {e})))"; }
            public String userCode() throws CompilerError {
                return parseStmt("if a then { b c } else { d e }");
            }
        },
        new Matches() {
            public String name() { return "Declaration 1"; }
            public String expected() { return "(GlobalLet @1,1,1,20 (Binding @1,5,1,20 {x <(Ground Int8 true)>}(Var @1,19,1,20 {q})))"; }
            public String userCode() throws CompilerError {
                return parseDecl("let var x: Int8 = q");
            }
        },
        new Matches() {
            public String name() { return "Declaration 2"; }
            public String expected() { return "(Def @1,1,1,17 a {# Implicit argument <(Ground Unit false)>} : <(Ground UInt8 false)>{})"; }
            public String userCode() throws CompilerError {
                return parseDecl("def a() UInt8 do");
            }
        },
        new Matches() {
            public String name() { return "Declaration 3"; }
            public String expected() { return "(Def @1,1,1,23 a {b <(Ground Int8 false)>} : <(Ground Int8 false)>{})"; }
            public String userCode() throws CompilerError {
                return parseDecl("def a(b: Int8) Int8 do");
            }
        },
        new Matches() {
            public String name() { return "Declaration 4"; }
            public String expected() { return "(Def @1,1,1,33 a {b <(Ground Int8 false)>}{c (? false true)}{d (? false false)} : <(Ground Int8 false)>{})"; }
            public String userCode() throws CompilerError {
                return parseDecl("def a(b: Int8, var c, d) Int8 do");
            }
        },
        new Matches() {
            public String name() { return "Declaration 5"; }
            public String expected() { return "(Def @1,1,1,39 a {# Implicit argument <(Ground Unit false)>} : <(Ground Int8 true)>{(Return @1,21,1,27 (@1,28,1,29 {1}))(Return @1,31,1,37 (@1,38,1,39 {2}))})"; }
            public String userCode() throws CompilerError {
                return parseDecl("def a() var Int8 do return 1; return 2");
            }
        },
        new Crashes() {
            public String name() { return "Declaration 6"; }
            public String expected() { return "@1,1,1,4 You cannot have an empty let block."; }
            public String userCode() throws CompilerError {
                return parseDecl("let do");
            }
        },
        new Matches() {
            public String name() { return "Declaration 7"; }
            public String expected() { return "(GlobalLet @1,1,1,20 (Binding @1,8,1,13 {a (? false false)}(@1,12,1,13 {1}))(Binding @1,15,1,20 {b (? false false)}(@1,19,1,20 {2})))"; }
            public String userCode() throws CompilerError {
                return parseDecl("let do a = 1; b = 2");
            }
        }
    };
}
/*


testDecl3 = AssertEqual (decl "def x(a, b (Boolean -> Boolean), c Int8) UInt64: break d ; break e")
(Def (Location 1 1) (Location 1 67) "x"
    [("a",Type (Arrow [Type (Ground Boolean)] (Type (Ground Boolean))))
    ,("b",Type (Arrow [Type (Ground Boolean)] (Type (Ground Boolean))))
    ,("c",Type (Ground Int8))]
    (Type (Ground UInt64))
[Stmt (Location 1 50) (Break "d")
,Stmt (Location 1 60) (Break "e")]
,Nil (Location 1 67))

*/