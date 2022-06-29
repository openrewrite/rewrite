package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.effects.Reads;
import org.openrewrite.java.effects.Writes;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class Java11EffectsAnalysisTest implements RewriteTest {

    private final Reads r = new Reads();
    private final Writes w = new Writes();

    @Test
    void test() {
        rewriteRun(
                spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {
                    @Override
                    public J.Assignment visitAssignment(J.Assignment assign, ExecutionContext ctx) {
                        J.Assignment a = super.visitAssignment(assign, ctx);
                        Set<JavaType.Variable> vs = getCursor().firstEnclosingOrThrow(J.CompilationUnit.class)
                                .getTypesInUse().getVariables();
                        List<JavaType.Variable> vsList = new ArrayList<>(vs);
                        vsList.sort(Comparator.comparing(JavaType.Variable::getName));
                        String marker = "";
                        for (JavaType.Variable variable : vsList) {
                            if (r.reads(a, variable)) {
                                if (marker != "") marker += ", ";
                                marker += "reads " + variable.getName();
                            }
                            if (w.writes(a, variable)) {
                                if (marker != "") marker += ", ";
                                marker += "writes " + variable.getName();
                            }
                        }
                        if (marker != "") {
                            a = a.withMarkers(a.getMarkers().searchResult(marker));
                        }
                        return a;
                    }
                })),
                java(
                        "" +
                                "import java.util.List; " +
                                "import java.util.ArrayList; " +
                                "class C { int m(int p) { " +
                                "  int x = 0, y = x;" +
                                "  x = 1;" +
                                "  x = m(y);" +
                                "  x = y;" +
                                "  List<String> list = new ArrayList<>(p);\n" +
                                "  for(int i = 0; i < 10; i++) {\n" +
                                "     System.out.println(list.get(i));\n" +
                                "  }" +
                                "  return p;" +
                                "}}",
                        "" +
                                "import java.util.List; " +
                                "import java.util.ArrayList; " +
                                "class C { int m(int p) { " +
                                "  int x = 0, y = x;" +
                                "  /*~~(writes x)~~>*/x = 1;" +
                                "  /*~~(writes x, reads y)~~>*/x = m(y);" +
                                "  /*~~(writes x, reads y)~~>*/x = y;" +
                                "  List<String> list = new ArrayList<>(p);\n" +
                                "  for(int i = 0; i < 10; i++) {\n" +
                                "     System.out.println(list.get(i));\n" +
                                "  }" +
                                "  return p;" +
                                "}}"
                )
        );
    }
}
