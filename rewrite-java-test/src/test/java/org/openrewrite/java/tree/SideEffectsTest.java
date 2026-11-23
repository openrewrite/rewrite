/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;

import static org.assertj.core.api.Assertions.assertThat;

class SideEffectsTest {

    @Test
    void ternaryIsKeptWholeOnlyWhenABranchHasSideEffects() {
        J.Ternary ternary = (J.Ternary) initializer("c ? b() : false");
        assertThat(ternary.getSideEffects()).containsExactly(ternary);

        J.Ternary pureBranches = (J.Ternary) initializer("b() ? 1 : 0");
        assertThat(pureBranches.getSideEffects()).containsExactly(pureBranches.getCondition());
    }

    @Test
    void arrayAccessHasSideEffectsOfArrayAndIndex() {
        J.ArrayAccess arrayAccess = (J.ArrayAccess) initializer("arr()[i++]");
        assertThat(arrayAccess.getSideEffects()).containsExactly(arrayAccess.getIndexed(), arrayAccess.getDimension().getIndex());
    }

    @Test
    void shortCircuitIsKeptWholeOnlyWhenTheRightHasSideEffects() {
        J.Binary and = (J.Binary) initializer("b() && b()");
        assertThat(and.getSideEffects()).containsExactly(and);

        J.Binary or = (J.Binary) initializer("c || b()");
        assertThat(or.getSideEffects()).containsExactly(or);

        J.Binary pureRight = (J.Binary) initializer("b() && c");
        assertThat(pureRight.getSideEffects()).containsExactly(pureRight.getLeft());
    }

    private static Expression initializer(String expression) {
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build()
          .parse(
            """
              class T {
                  boolean c;
                  int i;
                  boolean b() { return c; }
                  int[] arr() { return null; }
                  Object o = %s;
              }
              """.formatted(expression))
          .map(J.CompilationUnit.class::cast)
          .findFirst()
          .orElseThrow();
        J.VariableDeclarations o = (J.VariableDeclarations) cu.getClasses().getFirst().getBody().getStatements().getLast();
        return o.getVariables().getFirst().getInitializer();
    }
}
