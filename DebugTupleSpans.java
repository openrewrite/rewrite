import org.openrewrite.scala.ScalaParser;
import org.openrewrite.scala.tree.S;
import org.openrewrite.java.tree.J;

public class DebugTupleSpans {
    public static void main(String[] args) {
        String code = "object Test {\n  var (a, b) = (1, 2)\n  (a, b) = (3, 4)\n}";
        System.out.println("Source code:");
        System.out.println(code);
        System.out.println("\nCharacter positions:");
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            System.out.printf("%2d: '%s'\n", i, c == '\n' ? "\\n" : String.valueOf(c));
        }
    }
}
