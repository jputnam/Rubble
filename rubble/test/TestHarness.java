package rubble.test;

import java.util.ArrayList;

import rubble.data.CompilerError;
import rubble.data.Token;

/**
 * The test harness.
 * 
 * Copyright (c) 2011 Jared Putnam
 * Released under the terms of the 2-clause BSD license, which should be
 * included with this source.
 */
public final class TestHarness {
    
    public static enum TestResult { Succeeded, Failed, Crashed }
    
    public static final class TestReport {
        
        public String name;
        public String message;
        public TestResult result;
        
        public TestReport(String name, String message, TestResult result) {
            this.name = name;
            this.message = message;
            this.result = result;
        }
    }
    
    
    public abstract static class TestCase {
        
        public abstract TestReport runTest();
        
        public abstract String name();
        public abstract String expected();
        public abstract String userCode() throws CompilerError;
    }
    
    public abstract static class Matches extends TestCase {
        
        public final TestReport runTest() {
            try {
                String userResult = userCode();
                if (expected().equals(userResult)) {
                    return new TestReport(name(), "", TestResult.Succeeded);
                } else {
                    return new TestReport(name(), userResult, TestResult.Failed);
                }
            } catch (CompilerError ce) {
                return new TestReport(name(), ce.loc.toString() + " " + ce.message, TestResult.Crashed);
            } catch (RuntimeException e) {
                return new TestReport(name(), "A runtime error was thrown.  " + e.getMessage(), TestResult.Crashed);
            }
        }
    }
    
    public abstract static class Crashes extends TestCase {
        
        public final TestReport runTest() {
            try {
                return new TestReport(name(), "The test returned " + userCode(), TestResult.Failed);
            } catch (CompilerError ce) {
                if (expected().equals(ce.loc.toString() + " " + ce.message)) {
                    return new TestReport(name(), "", TestResult.Succeeded);
                } else {
                    return new TestReport(name(), "The test crashed with: " + ce.loc.toString() + " " + ce.message, TestResult.Crashed);
                }
            } catch (Error e) {
                return new TestReport(name(), "A runtime error was thrown.", TestResult.Crashed);
            }
        }
    }
    
    public static void testAll(TestCase[] cases) {
        ArrayList<TestReport> failures = new ArrayList<TestReport>();
        ArrayList<TestReport> crashes = new ArrayList<TestReport>();
        for (TestCase test: cases) {
            TestReport report = test.runTest();
            switch (report.result) {
            case Succeeded:
                System.out.print('.');
                break;
            case Failed:
                System.out.print('!');
                failures.add(report);
                break;
            default:
                System.out.print('X');
                crashes.add(report);
            }
        }
        System.out.print("\n");
        if (failures.size() > 0) {
            System.out.print("\n" + failures.size() + " failures:\n");
            for (TestReport tr: failures) {
                System.out.println(tr.name);
                System.out.println(tr.message);
            }
        }
        if (crashes.size() > 0) {
            System.out.print("\n" + crashes.size() + " crashes:\n");
            for (TestReport tr: crashes) {
                System.out.println(tr.name);
                System.out.println(tr.message);
            }
        }
    }
    
    public static String ugly(ArrayList<Token> tokens) {
        StringBuilder result = new StringBuilder();
        for (Token token: tokens) {
            result.append(token.toString());
        }
        return result.toString();
    }
}
