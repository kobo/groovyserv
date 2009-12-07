import org.jggug.kobo.groovyserv.*;
import junit.textui.TestRunner;
import junit.framework.*

def loader = new GroovyClassLoader()
loader.addClasspath("./src/test/groovy")
loader.addClasspath("./src/main/groovy")

suite = new TestSuite()


["org.jggug.kobo.groovyserv.DumpTest", "org.jggug.kobo.groovyserv.ExecTest"].each {
  suite.addTest(new TestSuite(loader.loadClass(it)))
}

TestResult result = TestRunner.run(suite)

