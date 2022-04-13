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

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

interface UseGenericTypeMethodParametersTest: JavaRecipeTest {
    override val recipe: Recipe?
        get() = UseGenericTypeMethodParameters(null)

    @Test
    fun doNotChangeAnnotatedParam() = assertUnchanged(
        before = """
            import java.util.HashSet;
            import java.util.Set;
            
            @SuppressWarnings("all")
            class Test {
                private Set<Integer> field = new HashSet<>();
                private boolean containsNumber(@Deprecated Set field, int number) {
                    return field.contains(number);
                }
                boolean useMethod() {
                    return containsNumber(field, 1);
                }
            }
        """
    )

    @Test
    fun changesAllowedAnnotation() = assertChanged(
        recipe = UseGenericTypeMethodParameters(listOf("java.lang.Deprecated")),
        before = """
            import java.util.HashSet;
            import java.util.Set;
            
            @SuppressWarnings("all")
            class Test {
                private Set<Integer> field = new HashSet<>();
                private boolean containsNumber(@Deprecated Set field, int number) {
                    return field.contains(number);
                }
                boolean useMethod() {
                    return containsNumber(field, 1);
                }
            }
        """,
        after = """
            import java.util.HashSet;
            import java.util.Set;
            
            @SuppressWarnings("all")
            class Test {
                private Set<Integer> field = new HashSet<>();
                private boolean containsNumber(@Deprecated Set<Integer> field, int number) {
                    return field.contains(number);
                }
                boolean useMethod() {
                    return containsNumber(field, 1);
                }
            }
        """
    )

    @Test
    fun doNotChangeOverloadedMethod() = assertUnchanged(
        before = """
            import java.util.HashSet;
            import java.util.Set;
            
            @SuppressWarnings("all")
            class Test {
                private boolean containsNumber(Set field, String number) {
                    return this.containsNumber(field, number);
                }

                private boolean containsNumber(Set field, Object number) {
                    return field.contains(number);
                }
                void multipleTypes() {
                    Set ints = new HashSet<Integer>(Arrays.asList(1, 2, 3));
                    containsNumber(ints, 1);
                }
            }
        """
    )

    @Test
    fun methodInvocationWithRawType() = assertUnchanged(
        before = """
            import java.util.HashSet;
            import java.util.Set;
            
            @SuppressWarnings("all")
            class Test {
                private boolean containsNumber(Set field, Object number) {
                    return field.contains(number);
                }
                void multipleTypes() {
                    Set ints = new HashSet<Integer>(Arrays.asList(1, 2, 3));
                    containsNumber(ints, 1);
                }
            }
        """
    )

    @SuppressWarnings("all")
    @Test
    fun moreThanOnePossibleType() = assertUnchanged(
        before = """
            import java.util.HashSet;
            import java.util.Set;
            
            @SuppressWarnings("all")
            class Test {
                private boolean containsNumber(Set field, Object number) {
                    return field.contains(number);
                }
                void multipleTypes() {
                    Set<Integer> ints = new HashSet<Integer>(Arrays.asList(1, 2, 3));
                    containsNumber(ints, 1);
                    Set<String> strings = new HashSet<String>(Arrays.asList("1", "2", "3"));
                    containsNumber(strings, "1");
                }
            }
        """
    )

    @SuppressWarnings("all")
    @Test
    fun changeRawParameter() = assertChanged(
        before = """
            import java.util.HashSet;
            import java.util.Set;
            
            @SuppressWarnings("all")
            class Test {
                private Set<Integer> field = new HashSet<>();
                private boolean containsNumber(Set field, int number) {
                    return field.contains(number);
                }
                boolean useMethod() {
                    return containsNumber(field, 1);
                }
            }
        """,
        after = """
            import java.util.HashSet;
            import java.util.Set;
            
            @SuppressWarnings("all")
            class Test {
                private Set<Integer> field = new HashSet<>();
                private boolean containsNumber(Set<Integer> field, int number) {
                    return field.contains(number);
                }
                boolean useMethod() {
                    return containsNumber(field, 1);
                }
            }
        """
    )

    @SuppressWarnings("all")
    @Test
    fun changeMultipleRawParameters() = assertChanged(
        before = """
            import java.util.*;
            
            @SuppressWarnings("all")
            class Test {
                private Set<Integer> field = new HashSet<>();
                private List<Integer> number = new ArrayList<>();
                private boolean containsNumbers(Set fields, List numbers) {
                    return fields.containsAll(numbers);
                }
                boolean useMethod() {
                    return containsNumbers(field, number);
                }
            }
        """,
        after = """
            import java.util.*;
            
            @SuppressWarnings("all")
            class Test {
                private Set<Integer> field = new HashSet<>();
                private List<Integer> number = new ArrayList<>();
                private boolean containsNumbers(Set<Integer> fields, List<Integer> numbers) {
                    return fields.containsAll(numbers);
                }
                boolean useMethod() {
                    return containsNumbers(field, number);
                }
            }
        """
    )
}
