package rubble.test;

/**
 * The test driver.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public class Main {
    
    public static void main(String[] args) {
        System.out.print("Test Lexer: ");
        TestHarness.testAll(TestLexer.cases);
        System.out.print("\nTest Layout: ");
        TestHarness.testAll(TestLayout.cases);
        System.out.print("\nTest Parser: ");
        TestHarness.testAll(TestParser.cases);
        System.out.print("\nTest Checker: ");
        TestHarness.testAll(TestChecker.cases);
    }
}
