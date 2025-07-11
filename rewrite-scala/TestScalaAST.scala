import dotty.tools.dotc.ast.Trees.*
import dotty.tools.dotc.ast.untpd
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Names.*
import dotty.tools.dotc.Driver
import dotty.tools.dotc.Compiler
import dotty.tools.dotc.util.SourceFile
import dotty.tools.dotc.parsing.Parsers

@main def testAST(): Unit = {
  val driver = new Driver
  given Context = driver.getInitialContext

  val testCases = List(
    "val x = 5",
    "var y = 10",
    "val x: Int = 5",
    "lazy val z = compute()",
    "private val secret = 42",
    "val (a, b) = (1, 2)"
  )

  testCases.foreach { code =>
    println(s"\nParsing: $code")
    val source = SourceFile.virtual("test.scala", code)
    val parser = new Parsers.Parser(source)
    val tree = parser.parse()
    
    println(s"Tree type: ${tree.getClass.getSimpleName}")
    tree match {
      case pkg: untpd.PackageDef =>
        pkg.stats.foreach { stat =>
          println(s"  Statement type: ${stat.getClass.getSimpleName}")
          stat match {
            case vd: untpd.ValDef =>
              println(s"    Name: ${vd.name}")
              println(s"    Mods: ${vd.mods.flags}")
              println(s"    Mods class: ${vd.mods.getClass.getSimpleName}")
              println(s"    Type: ${if (vd.tpt.isEmpty) "inferred" else vd.tpt}")
              println(s"    RHS: ${vd.rhs.getClass.getSimpleName}")
            case _ =>
              println(s"    Details: $stat")
          }
        }
      case _ =>
        println(s"Unexpected tree: $tree")
    }
  }
}
