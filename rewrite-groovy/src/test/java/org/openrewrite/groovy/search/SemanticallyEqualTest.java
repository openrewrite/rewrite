/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.groovy.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.J;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Groovy parser does not attribute {@link J.Identifier#getFieldType()} on references, so these cases exercise
 * what {@code SemanticallyEqual} may conclude when type information is systemically absent rather than merely missing.
 */
class SemanticallyEqualTest {

    @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/953")
    @Test
    void fieldNotEqualToShadowedNameVariable() {
        J.Assignment assignment = firstAssignment("""
          class A {
              private final boolean readOnly
              A(boolean readOnly) {
                  this.readOnly = readOnly
              }
          }
          """);
        assertThat(SemanticallyEqual.areEqual(assignment.getVariable(), assignment.getAssignment())).isFalse();
        assertThat(SemanticallyEqual.areEqual(assignment.getAssignment(), assignment.getVariable())).isFalse();
    }

    @Test
    void identifiersWithSameNameRemainEqual() {
        J.Assignment assignment = firstAssignment("""
          class A {
              void m(boolean readOnly) {
                  readOnly = readOnly
              }
          }
          """);
        assertThat(SemanticallyEqual.areEqual(assignment.getVariable(), assignment.getAssignment())).isTrue();
    }

    private J.Assignment firstAssignment(String source) {
        G.CompilationUnit cu = GroovyParser.builder().build()
          .parse(new InMemoryExecutionContext(), source)
          .findFirst()
          .map(G.CompilationUnit.class::cast)
          .orElseThrow();
        J.ClassDeclaration clazz = (J.ClassDeclaration) cu.getStatements().getFirst();
        J.MethodDeclaration method = (J.MethodDeclaration) clazz.getBody().getStatements().stream()
          .filter(J.MethodDeclaration.class::isInstance)
          .findFirst()
          .orElseThrow();
        return (J.Assignment) method.getBody().getStatements().getFirst();
    }
}
