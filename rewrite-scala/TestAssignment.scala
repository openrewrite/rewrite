import dotty.tools.dotc.ast.Trees.*
import dotty.tools.dotc.ast.untpd
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.Driver
import dotty.tools.dotc.util.SourceFile
import dotty.tools.dotc.parsing.Parsers

@main def testAssignment(): Unit = {
  val driver = new Driver
  given Context = driver.getInitialContext

  // Test simple assignment
  val source1 = SourceFile.virtual("test.scala", "obj.field = 42")
  val parser1 = new Parsers.Parser(source1)
  val tree1 = parser1.parse()
  
  println(s"Assignment AST: ${tree1.getClass.getSimpleName}")
  tree1 match {
    case pkg: untpd.PackageDef =>
      pkg.stats.foreach { stat =>
        println(s"  Statement: ${stat.getClass.getSimpleName}")
        stat match {
          case app: untpd.Apply =>
            println(s"    Fun: ${app.fun}")
            println(s"    Args: ${app.args}")
          case _ =>
            println(s"    Details: $stat")
        }
      }
    case _ =>
      println(s"Unexpected: $tree1")
  }
}
