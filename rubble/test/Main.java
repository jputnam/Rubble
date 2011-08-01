package rubble.test;


public class Main {
    
    public static void main(String[] args) {
        System.out.print("TestLexer: ");
        TestHarness.testAll(TestLexer.cases);
        System.out.print("\nTestLayout: ");
        TestHarness.testAll(TestLayout.cases);
        System.out.print("\nTestParser: ");
        TestHarness.testAll(TestParser.cases);
    }
    
}
