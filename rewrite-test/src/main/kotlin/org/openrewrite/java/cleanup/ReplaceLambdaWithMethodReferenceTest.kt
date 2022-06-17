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
package org.openrewrite.java.cleanup

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.tree.J

interface ReplaceLambdaWithMethodReferenceTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = ReplaceLambdaWithMethodReference()

    @Issue("https://github.com/openrewrite/rewrite/issues/1926")
    @Test
    fun multipleMethodInvocations() = assertUnchanged(
        before = """
            import java.nio.file.Path;
            import java.nio.file.Paths;
            import java.util.List;import java.util.stream.Collectors;
            
            class Test {
                Path path = Paths.get("");
                List<String> method(List<String> l) {
                    return l.stream()
                        .filter(s -> path.getFileName().toString().equals(s))
                        .collect(Collectors.toList());
                }
            }
        """.trimIndent()
    )

    @Test
    fun containsMultipleStatements() = assertUnchanged(
        before = """
            import java.util.List;
            import java.util.stream.Collectors;

            class Test {
                List<Integer> even(List<Integer> l) {
                    return l.stream().map(n -> {
                        if (n % 2 == 0) return n;
                        return n * 2;
                    }).collect(Collectors.toList());
                }
            }
        """
    )

    @Suppress("RedundantCast")
    @Issue("https://github.com/openrewrite/rewrite/issues/1772")
    @Test
    fun typeCastOnMethodInvocationReturnType() = assertUnchanged(
        before = """
            import java.util.List;
            import java.util.stream.Collectors;
            import java.util.stream.Stream;

            class Test {
                public void foo() {
                    List<String> bar = Stream.of("A", "b")
                            .map(s -> (String) s.toLowerCase())
                            .collect(Collectors.toList());
                }
            }
        """
    )

    @Test
    fun instanceOf() = assertChanged(
        dependsOn = arrayOf(
            """
            package org.test;
            public class CheckType {
            }
        """
        ),
        before = """
            import java.util.List;
            import java.util.stream.Collectors;

            import org.test.CheckType;

            class Test {
                List<Object> method(List<Object> input) {
                    return input.stream().filter(n -> n instanceof CheckType).collect(Collectors.toList());
                }
            }
        """,
        after = """
            import java.util.List;
            import java.util.stream.Collectors;

            import org.test.CheckType;

            class Test {
                List<Object> method(List<Object> input) {
                    return input.stream().filter(CheckType.class::isInstance).collect(Collectors.toList());
                }
            }
        """,
        afterConditions = { cu ->
            val value = (((((((cu.classes[0].body.statements[0] as J.MethodDeclaration)
                .body!!.statements[0] as J.Return)
                .expression as J.MethodInvocation)
                .select as J.MethodInvocation)
                .arguments[0] as J.MemberReference)
                .containing as J.FieldAccess)
                .target as J.Identifier).type!!.toString()
            assertThat(value).isEqualTo("org.test.CheckType")
        }
    )

    @Suppress("Convert2MethodRef")
    @Test
    fun functionMultiParamReference() = assertChanged(
        dependsOn = arrayOf(
            """
                public interface ObservableValue<T> {
                }
            """,
            """
                @FunctionalInterface
                public interface ChangeListener<T> {
                    void changed(ObservableValue<? extends T> observable, T oldValue, T newValue);
                }
            """.trimIndent()
        ),
        before = """
            import java.util.function.Function;
            class Test {
            
                ChangeListener listener = (o, oldVal, newVal) -> {
                    onChange(o, oldVal, newVal);
                };
                
                protected void onChange(ObservableValue<?> o, Object oldVal, Object newVal) {
                    String strVal = newVal.toString();
                    System.out.println(strVal);
                }
            }
        """,
        after = """
            import java.util.function.Function;
            class Test {
            
                ChangeListener listener = this::onChange;
                
                protected void onChange(ObservableValue<?> o, Object oldVal, Object newVal) {
                    String strVal = newVal.toString();
                    System.out.println(strVal);
                }
            }
        """
    )

    @Suppress("Convert2MethodRef")
    @Test
    fun nonStaticMethods() = assertChanged(
        before = """
            import java.util.Collections;
            class Test {
                Runnable r = () -> run();
                public void run() {
                    Collections.singletonList(1).forEach(n -> run());
                }
            }
            
            class Test2 {
                Test t = new Test();
                Runnable r = () -> t.run();
            }
        """,
        after = """
            import java.util.Collections;
            class Test {
                Runnable r = this::run;
                public void run() {
                    Collections.singletonList(1).forEach(n -> run());
                }
            }
            
            class Test2 {
                Test t = new Test();
                Runnable r = t::run;
            }
        """
    )

    @Suppress("Convert2MethodRef")
    @Test
    fun staticMethods() = assertChanged(
        before = """
            import java.util.Collections;
            class Test {
                Runnable r = () -> run();
                public static void run() {
                    Collections.singletonList(1).forEach(n -> run());
                }
            }
            
            class Test2 {
                Runnable r = () -> Test.run();
            }
        """,
        after = """
            import java.util.Collections;
            class Test {
                Runnable r = Test::run;
                public static void run() {
                    Collections.singletonList(1).forEach(n -> run());
                }
            }
            
            class Test2 {
                Runnable r = Test::run;
            }
        """
    )

    @Suppress("Convert2MethodRef")
    @Test
    fun systemOutPrint() = assertChanged(
        before = """
            import java.util.List;

            class Test {
                void method(List<Integer> input) {
                    return input.forEach(x -> System.out.println(x));
                }
            }
        """,
        after = """
            import java.util.List;

            class Test {
                void method(List<Integer> input) {
                    return input.forEach(System.out::println);
                }
            }
        """
    )

    @Suppress("Convert2MethodRef", "CodeBlock2Expr")
    @Test
    fun systemOutPrintInBlock() = assertChanged(
        before = """
            import java.util.List;

            class Test {
                void method(List<Integer> input) {
                    return input.forEach(x -> { System.out.println(x); });
                }
            }
        """,
        after = """
            import java.util.List;

            class Test {
                void method(List<Integer> input) {
                    return input.forEach(System.out::println);
                }
            }
        """
    )

    @Suppress("RedundantCast")
    @Test
    fun castType() = assertChanged(
        dependsOn = arrayOf(
            """
            package org.test;
            public class CheckType {
            }
        """
        ),
        before = """
            import java.util.List;
            import java.util.stream.Collectors;

            import org.test.CheckType;

            class Test {
                List<Object> filter(List<Object> l) {
                    return l.stream()
                        .filter(CheckType.class::isInstance)
                        .map(o -> (CheckType) o)
                        .collect(Collectors.toList());
                }
            }
        """,
        after = """
            import java.util.List;
            import java.util.stream.Collectors;

            import org.test.CheckType;

            class Test {
                List<Object> filter(List<Object> l) {
                    return l.stream()
                        .filter(CheckType.class::isInstance)
                        .map(CheckType.class::cast)
                        .collect(Collectors.toList());
                }
            }
        """,
        afterConditions = { cu ->
            val value = (((((((cu.classes[0].body.statements[0] as J.MethodDeclaration)
                .body!!.statements[0] as J.Return)
                .expression as J.MethodInvocation)
                .select as J.MethodInvocation)
                .arguments[0] as J.MemberReference)
                .containing as J.FieldAccess)
                .target as J.Identifier).type!!.toString()
            assertThat(value).isEqualTo("org.test.CheckType")
        }
    )

    @Suppress("Convert2MethodRef")
    @Test
    fun notEqualToNull() = assertChanged(
        before = """
            import java.util.List;
            import java.util.stream.Collectors;

            class Test {
                List<Object> filter(List<Object> l) {
                    return l.stream()
                        .filter(o -> o != null)
                        .collect(Collectors.toList());
                }
            }
        """,
        after = """
            import java.util.List;
            import java.util.Objects;
            import java.util.stream.Collectors;

            class Test {
                List<Object> filter(List<Object> l) {
                    return l.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                }
            }
        """
    )

    @Suppress("Convert2MethodRef")
    @Test
    fun isEqualToNull() = assertChanged(
        before = """
            import java.util.List;
            import java.util.stream.Collectors;

            class Test {
                boolean containsNull(List<Object> l) {
                    return l.stream()
                        .anyMatch(o -> o == null);
                }
            }
        """,
        after = """
            import java.util.List;
            import java.util.Objects;
            import java.util.stream.Collectors;

            class Test {
                boolean containsNull(List<Object> l) {
                    return l.stream()
                        .anyMatch(Objects::isNull);
                }
            }
        """
    )

    @Suppress("Convert2MethodRef", "CodeBlock2Expr")
    @Test
    fun voidMethodReference() = assertChanged(
        before =
            """
                class Test {
                    Runnable r = () -> {
                        this.execute();
                    };

                    void execute() {}
                }
            """,
        after =
            """
                class Test {
                    Runnable r = this::execute;

                    void execute() {}
                }
            """
    )

    @Suppress("Convert2MethodRef", "CodeBlock2Expr")
    @Test
    fun functionReference() = assertChanged(
        before =
            """
                import java.util.function.Function;

                class Test {
                    Function<Integer, String> f = (i) -> {
                        return this.execute(i);
                    };
                    
                    String execute(Integer i) {
                        return i.toString();
                    }
                }
            """,
        after =
            """
                import java.util.function.Function;

                class Test {
                    Function<Integer, String> f = this::execute;
                    
                    String execute(Integer i) {
                        return i.toString();
                    }
                }
            """
    )
}
