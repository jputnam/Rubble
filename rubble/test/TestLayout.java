package rubble.test;

import rubble.data.CompilerError;
import rubble.parser.Lexer;
import rubble.parser.Layout;
import rubble.test.TestHarness.*;


public final class TestLayout {

    public static final TestHarness.TestCase[] cases = {
        new Crashes() {
            public String name() { return "Layout1"; }
            public String expected() { return "@2,1,2,2 The statement ended before you closed the brackets."; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly((new Layout(new Lexer("(\n)").lex())).layout());
            }
        },
        new Matches() {
            public String name() { return "Layout2"; }
            public String expected() { return "(Token @1,1,2,2 {{} Block {})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly((new Layout(new Lexer("{\n}").lex())).layout());
            }
        },
        new Matches() {
            public String name() { return "Layout3"; }
            public String expected() { return "(Token @1,2,2,2 {{} Block {})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly((new Layout(new Lexer(" {\n}").lex())).layout());
            }
        },
        new Matches() {
            public String name() { return "Layout4"; }
            public String expected() { return "(Token @1,1,1,2 {a} Identifier {})(Token @1,3,1,5 {do} Block {})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly((new Layout(new Lexer("a do").lex())).layout());
            }
        },
        new Matches() {
            public String name() { return "Layout5"; }
            public String expected() { return "(Token @1,1,1,2 {a} Identifier {})(Token @1,3,1,7 {do} Block {(Token @1,6,1,7 {b} Identifier {})})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly((new Layout(new Lexer("a do b").lex())).layout());
            }
        },
        new Matches() {
            public String name() { return "Layout6"; }
            public String expected() { return "(Token @1,1,1,2 {a} Identifier {})(Token @1,2,1,5 {{} Block {(Token @1,3,1,4 {b} Identifier {})})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly((new Layout(new Lexer("a{b}").lex())).layout());
            }
        },
        new Matches() {
            public String name() { return "Layout7"; }
            public String expected() { return "(Token @1,1,3,2 {{} Block {(Token @1,3,2,4 {{} Block {})})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly((new Layout(new Lexer("{ {\n  }\n}").lex())).layout());
            }
        },
        new Crashes() {
            public String name() { return "Layout8"; }
            public String expected() { return "@2,2,2,3 The closing } must be at or to the right of the semicolon column of its enclosing block."; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly((new Layout(new Lexer("{ {\n }\n}").lex())).layout());
            }
        },
        new Crashes() {
            public String name() { return "Layout9"; }
            public String expected() { return "@2,2,2,3 The parser can't implicitly close an explicit brace."; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly((new Layout(new Lexer("{ {\n a}\n}").lex())).layout());
            }
        },
        new Crashes() {
            public String name() { return "Layout10"; }
            public String expected() { return "@2,3,2,3 The parser can't implicitly close an explicit brace."; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly((new Layout(new Lexer("{ {a\n  b}\n}").lex())).layout());
            }
        },
        new Matches() {
            public String name() { return "Layout11"; }
            public String expected() { return "(Token @1,1,3,2 {{} Block {(Token @1,2,2,6 {{} Block {(Token @1,3,2,5 {(} Block {})})})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly((new Layout(new Lexer("{{(\n   )}\n}").lex())).layout());
            }
        },
        new Crashes() {
            public String name() { return "Layout12"; }
            public String expected() { return "@2,3,2,4 The statement ended before you closed the brackets."; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly((new Layout(new Lexer("{{(\n  )}\n}").lex())).layout());
            }
        },
        new Matches() {
            public String name() { return "Layout13"; }
            public String expected() { return "(Token @1,1,2,6 {{} Block {(Token @1,2,2,5 {(} Block {(Token @2,3,2,4 {a} Identifier {})})})"; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly((new Layout(new Lexer("{(\n  a)}").lex())).layout());
            }
        },
        new Crashes() {
            public String name() { return "Layout14"; }
            public String expected() { return "@2,2,2,2 The statement ended before all brackets were closed."; }
            public String userCode() throws CompilerError {
                return TestHarness.ugly((new Layout(new Lexer("{(\n a)}").lex())).layout());
            }
        }
    };
}
