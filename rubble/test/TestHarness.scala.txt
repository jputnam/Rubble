package rubble.test

import rubble.data.ParseError
import scala.collection.mutable.ArrayBuffer


object TestHarness {
    
    
    sealed abstract class TestResult { }
    sealed case class Success() extends TestResult { }
    sealed case class Failure(name: String, message: String) extends TestResult { }
    sealed case class Crash(name: String, message: String) extends TestResult { }
    
    
    private def runTest[A](name: String, test: => A, expected: A): TestResult = {
        try {
            val result: A = test
            if (result == expected) {
                return Success()
            } else {
                return Failure(name, result.toString)
            }
        } catch {
            case e: ParseError => {
                e match {
                    case ParseError(_, m) => return Crash(name, m)
                }
            }
        }
    }
    
    
    def matches[A](name: String, test: => A, expected: A): Unit => TestResult = (_) => (runTest(name, test, expected))
    
    
    def fails[A](name: String, test: => A, expected: String): Unit => TestResult = (_) => runTest(name, test, null) match {
        case Crash(name, report) => if (report == expected) Success() else Failure(name, "Crashed but reported: " + report)
        case a => a
    }
    
    
    def testAll(tests: (Unit => TestResult)*): Boolean = {
        val failureReport = ArrayBuffer.empty[(String, String)]
        val crashReport = ArrayBuffer.empty[(String, String)]
        for (t <- tests) {
            t(Unit) match {
                case Success() => {
                    print(".")
                }
                case Failure(name, message) => {
                    print ("!")
                    failureReport += ((name, message))
                }
                case Crash(name, message) => {
                    print ("X")
                    crashReport += ((name, message))
                }
            }
        }
        println("")
        if (failureReport.size > 0) {
            println("The following tests failed:")
            for ((name, message) <- failureReport) {
                println(name + "\n" + message + "\n")
            }
        }
        if (crashReport.size > 0) {
            println("The following tests crashed:")
            for ((name, message) <- crashReport) {
                println(name + "\n" + message + "\n")
            }
        }
        return failureReport.size == 0 && crashReport.size == 0
    }
}