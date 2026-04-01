import dotty.tools.dotc.ast.untpd
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.parsing.Parsers.Parser
import dotty.tools.dotc.util.SourceFile
import dotty.tools.dotc.CompilationUnit
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.Driver
import dotty.tools.dotc.config.Settings

object TestSpanDebug {
  def main(args: Array[String]): Unit = {
    val code = """object Test {
  var (a, b) = (1, 2)
  (a, b) = (3, 4)
}"""
    
    // Setup compiler context
    val driver = new Driver
    given Context = driver.initCtx.fresh
    
    val source = SourceFile.virtual("test.scala", code)
    val unit = CompilationUnit(source)
    val parser = Parser(source)
    val tree = parser.parse()
    
    def showTree(t: untpd.Tree, indent: Int = 0): Unit = {
      val prefix = "  " * indent
      val span = t.span
      val spanStr = if (span.exists) {
        val start = span.start
        val end = span.end
        val content = if (start >= 0 && end <= code.length) {
          code.substring(start, end).replace("\n", "\\n")
        } else "???"
        s"[${start}-${end}] '$content'"
      } else "[no span]"
      
      println(s"${prefix}${t.getClass.getSimpleName}: $spanStr")
      
      t match {
        case pkg: untpd.PackageDef =>
          pkg.stats.foreach(showTree(_, indent + 1))
        case mod: untpd.ModuleDef =>
          mod.impl.body.foreach(showTree(_, indent + 1))
        case vd: untpd.ValDef =>
          println(s"${prefix}  name: ${vd.name}")
          if (vd.tpt != null) showTree(vd.tpt, indent + 1)
          if (vd.rhs != null) showTree(vd.rhs, indent + 1)
        case asg: untpd.Assign =>
          println(s"${prefix}  LHS:")
          showTree(asg.lhs, indent + 2)
          println(s"${prefix}  RHS:")
          showTree(asg.rhs, indent + 2)
        case tuple: untpd.Tuple =>
          println(s"${prefix}  Elements:")
          tuple.trees.foreach(showTree(_, indent + 2))
        case _ =>
          // Show children
          t.productIterator.foreach {
            case child: untpd.Tree => showTree(child, indent + 1)
            case list: List[_] => list.foreach {
              case tree: untpd.Tree => showTree(tree, indent + 1)
              case _ =>
            }
            case _ =>
          }
      }
    }
    
    showTree(tree)
  }
}
