package rubble.test;

import rubble.data.CompilerError;
import rubble.parser.Lexer;
import rubble.test.TestHarness.*;

public final class TestLexer {
    
    public static final TestHarness.TestCase[] cases = {
        new Crashes() {
            public String name() { return "Bracket1"; }
            public String expected() { return "@1,3,1,3 Unclosed [."; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer("([").lex());
            }
        },
        new Matches() {
            public String name() { return "Bracket2"; }
            public String expected() { return "(Token @1,1,1,4 {(} Block {(Token @1,2,1,3 {a} Identifier {})})(Token @1,4,1,5 {x} Identifier {})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer("(a)x").lex());
            }
        },
        new Matches() {
            public String name() { return "Bracket3"; }
            public String expected() { return "(Token @1,1,1,9 {[} Block {(Token @1,2,1,3 {x} Identifier {})(Token @1,4,1,6 {do} Block {})(Token @1,7,1,8 {y} Identifier {})})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer("[x do y]").lex());
            }
        },
        new Matches() {
            public String name() { return "Bracket4"; }
            public String expected() { return "(Token @1,1,1,8 {[} Block {(Token @1,2,1,3 {x} Identifier {})(Token @1,4,1,5 {asType} Reserved {})(Token @1,6,1,7 {y} Identifier {})})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer("[x : y]").lex());
            }
        },
        new Matches() {
            public String name() { return "Bracket5"; }
            public String expected() { return "(Token @1,1,1,9 {(} Block {(Token @1,2,1,4 {(} Block {})(Token @1,4,1,8 {[} Block {(Token @1,5,1,7 {{} Block {})})})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer("(()[{}])").lex());
            }
        },
        new Matches() {
            public String name() { return "Lexer1"; }
            public String expected() { return "(Token @1,1,1,4 {abc} Identifier {})(Token @1,5,1,8 {def} Reserved {})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer("abc def").lex());
            };
        },
        new Matches() {
            public String name() { return "Lexer2"; }
            public String expected() { return "(Token @1,1,1,2 {1} Number {})(Token @1,2,1,3 {,} Comma {})(Token @1,3,1,4 {;} Semicolon {})(Token @1,4,1,8 {-123} Number {})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer("1,;-123").lex());
            };
        },
        new Matches() {
            public String name() { return "Lexer3"; }
            public String expected() { return "(Token @1,1,1,2 {negate} Reserved {})(Token @1,2,1,3 {a} Identifier {})(Token @1,3,1,4 {,} Comma {})(Token @1,4,1,5 {-} Operator {})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer("-a,-").lex());
            };
        },
        new Matches() {
            public String name() { return "Lexer4"; }
            public String expected() { return "(Token @1,2,1,3 {a} Identifier {})(Token @2,2,2,5 {abc} Identifier {})(Token @2,5,2,6 {-} Operator {})(Token @2,6,2,7 {1} Number {})(Token @2,8,2,11 {<<:} Operator {})"; }

            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer(" a\n abc-1 <<:").lex());
            };
        },
        new Matches() {
            public String name() { return "Lexer5"; }
            public String expected() { return "(Token @1,1,1,2 {&} Operator {})(Token @1,3,1,4 {addressOf} Reserved {})(Token @1,4,1,5 {x} Identifier {})(Token @1,6,1,7 {a} Identifier {})(Token @1,7,1,8 {&} Operator {})(Token @1,8,1,9 {x} Identifier {})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer("& &x a&x").lex());
            };
        },
        new Matches() {
            public String name() { return "Lexer6"; }
            public String expected() { return "(Token @1,1,1,2 {*} Operator {})(Token @1,3,1,4 {valueAt} Reserved {})(Token @1,4,1,5 {x} Identifier {})(Token @1,6,1,7 {a} Identifier {})(Token @1,7,1,8 {*} Operator {})(Token @1,8,1,9 {x} Identifier {})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer("* *x a*x").lex());
            };
        },
        new Crashes() {
            public String name() { return "Lexer7"; }
            public String expected() { return "@1,1,1,1 The tab character may not appear in source code."; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer("\t").lex());
            };
        },
        new Crashes() {
            public String name() { return "Lexer8"; }
            public String expected() { return "@1,1,1,2 Unmatched closing bracket."; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer(")").lex());
            };
        },
        new Matches() {
            public String name() { return "Lexer9"; }
            public String expected() { return "(Token @1,1,1,6 {break} Reserved {})(Token @1,7,1,10 {def} Reserved {})(Token @1,11,1,15 {else} Reserved {})(Token @1,16,1,23 {forever} Reserved {})(Token @1,24,1,26 {if} Reserved {})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer("break def else forever if").lex());
            };
        },
        new Matches() {
            public String name() { return "Lexer10"; }
            public String expected() { return "(Token @1,1,1,4 {let} Reserved {})(Token @1,5,1,11 {return} Reserved {})(Token @1,12,1,16 {then} Reserved {})(Token @1,17,1,20 {var} Reserved {})(Token @1,21,1,23 {->} Reserved {})(Token @1,24,1,25 {=} Reserved {})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer("let return then var -> =").lex());
            };
        },
        new Matches() {
            public String name() { return "Lexer11"; }
            public String expected() { return "(Token @1,1,1,3 {10} Number {})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer("10").lex());
            };
        },
        new Matches() {
            public String name() { return "Lexer12"; }
            public String expected() { return "(Token @1,1,1,4 {abc} Identifier {})(Token @1,5,1,8 {def} Reserved {})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly(new Lexer("abc def # ghi").lex());
            };
        }
    };
}
