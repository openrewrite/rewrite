/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
                        StringBuilder marker = new StringBuilder();
                        for (JavaType.Variable variable : vsList) {
                            if (r.reads(a, variable)) {
                                if (!marker.toString().isEmpty()) marker.append(", ");
                                marker.append("reads ").append(variable.getName());
                            }
                            if (w.writes(a, variable)) {
                                if (!marker.toString().equals("")) marker.append(", ");
                                marker.append("writes ").append(variable.getName());
                            }
                        }
                        if (!marker.toString().isEmpty()) {
                            a = a.withMarkers(a.getMarkers().searchResult(marker.toString()));
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
