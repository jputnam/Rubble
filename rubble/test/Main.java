package rubble.test;


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
