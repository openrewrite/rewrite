/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.internal.template;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.internal.template.PatternVariables.neverCompletesNormally;
import static org.openrewrite.java.internal.template.PatternVariables.simplifiedPatternVariableCondition;

@SuppressWarnings({"SizeReplaceableByIsEmpty", "PointlessBooleanExpression", "ConstantValue", "DuplicateCondition"})
class PatternVariablesTest {

    private final JavaParser parser = JavaParser.fromJavaVersion().build();

    @Nested
    class Simplify {
        @Test
        void none() {
            String condition = simplify("o == null");
            assertThat(condition).isNull();
        }

        @Test
        void simple() {
            String condition = simplify("o instanceof String s");
            assertThat(condition).isEqualTo("((Object)null) instanceof String s");
        }

        @Test
        void multiple() {
            String condition = simplify("(o instanceof String s && s.length() > 0) || (o instanceof Integer i && i > 0)");
            assertThat(condition).isEqualTo("(((Object)null) instanceof String s&&true)||(((Object)null) instanceof Integer i&&true)");
        }

        @Test
        void binaryOr() {
            String condition = simplify("o instanceof String s || 1 > 0 || o instanceof Integer i || 1 > 0");
            assertThat(condition).isEqualTo("((Object)null) instanceof String s||true||((Object)null) instanceof Integer i||true");
        }

        @Test
        void collapseNested() {
            String condition = simplify("1 > 2 && (o.hasCode() == 0 || 1 == 2) || o instanceof String s");
            assertThat(condition).isEqualTo("true||((Object)null) instanceof String s");
        }

        @Test
        void unaryNot() {
            String condition = simplify("!(o instanceof String s || true)");
            assertThat(condition).isEqualTo("!(((Object)null) instanceof String s||true)");
        }

        private @Nullable String simplify(@Language(value = "java", prefix = "class $ { boolean b = ", suffix = "; }") String condition) {
            @Language("java")
            String source = """
                    class Test {
                        void test(Object o) {
                            if ($condition) {
                                System.out.println(s);
                            }
                        }
                    }
                    """.replace("$condition", condition);
            J.CompilationUnit cu = parser.parse(source)
                    .findFirst()
                    .map(J.CompilationUnit.class::cast)
                    .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));
            J.MethodDeclaration method = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
            @SuppressWarnings("DataFlowIssue") J.If ifStatement = (J.If) method.getBody().getStatements().get(0);
            return simplifiedPatternVariableCondition(ifStatement.getIfCondition().getTree(), null);
        }
    }

    @SuppressWarnings({"UnnecessarySemicolon", "UnnecessaryReturnStatement"})
    @Nested
    class CompletesNormally {
        @Test
        void emptyBlock() {
            Statement stmt = statement("{}");
            assertThat(neverCompletesNormally(stmt)).isFalse();
        }

        @Test
        void nullStatement() {
            Statement stmt = statement(";");
            assertThat(neverCompletesNormally(stmt)).isFalse();
        }

        @Test
        void return_() {
            Statement stmt = statement("return;");
            assertThat(neverCompletesNormally(stmt)).isTrue();
        }

        @Test
        void continue_() {
            Statement stmt = statement("continue;");
            assertThat(neverCompletesNormally(stmt)).isTrue();
        }

        @Test
        void labelledContinue() {
            Statement stmt = statement("continue x;");
            assertThat(neverCompletesNormally(stmt)).isTrue();
        }

        @Test
        void break_() {
            Statement stmt = statement("break;");
            assertThat(neverCompletesNormally(stmt)).isTrue();
        }

        @Test
        void labelledBreak() {
            Statement stmt = statement("break x;");
            assertThat(neverCompletesNormally(stmt)).isTrue();
        }

        private Statement statement(@Language(value = "java", prefix = "class $ { void test() { ", suffix = "} }") String statement) {
            @Language("java")
            String source = """
                    class Test {
                        void test(Object o) {
                            $statement
                        }
                    }
                    """.replace("$statement", statement);
            J.CompilationUnit cu = parser.parse(source)
                    .findFirst()
                    .map(J.CompilationUnit.class::cast)
                    .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));
            J.MethodDeclaration method = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
            //noinspection DataFlowIssue
            return method.getBody().getStatements().get(0);
        }
    }
}
