import scala.tools.nsc._
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.Settings

object TestTupleAST {
  def main(args: Array[String]): Unit = {
    val settings = new Settings()
    settings.usejavacp.value = true
    val global = new Global(settings, new ConsoleReporter(settings))
    import global._
    
    val code = """
      object Test {
        var (a, b) = (1, 2)
        (a, b) = (3, 4)
      }
    """
    
    val unit = newCompilationUnit(code)
    val tree = new syntaxAnalyzer.SourceFileParser(unit.source).parse()
    
    println("AST Structure:")
    println(showRaw(tree))
  }
}
