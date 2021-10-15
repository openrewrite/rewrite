/*
 * Copyright 2021 the original author or authors.
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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

@Suppress(
    "SizeReplaceableByIsEmpty",
    "DuplicateCondition",
    "ConstantConditions",
    "ExcessiveRangeCheck",
    "ConstantOnWrongSideOfComparison",
    "StatementWithEmptyBody",
    "BooleanMethodNameMustStartWithQuestion",
    "PointlessBooleanExpression",
    "ResultOfMethodCallIgnored",
    "Convert2MethodRef"
)
interface IsEmptyCallOnCollectionsTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = IsEmptyCallOnCollections()

    @Test
    fun isEmptyCallOnCollections(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.List;

            class Test {
                static void method(List<String> l) {
                    if (l.size() == 0 || 0 == l.size()) {
                        // empty body
                    } else if (l.size() != 0 || 0 != l.size()) {
                        // empty body
                    }
                }
            }
        """,
        after = """
            import java.util.List;

            class Test {
                static void method(List<String> l) {
                    if (l.isEmpty() || l.isEmpty()) {
                        // empty body
                    } else if (!l.isEmpty() || !l.isEmpty()) {
                        // empty body
                    }
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1112")
    @Disabled // note, can merge these into the test above after fixing // todo
    fun formatting(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.List;

            class Test {
                static boolean method(List<String> l, List<List<String>> ll) {
                    if (true || l.size() == 0) {
                        //ll.stream().filter(p -> p.size() == 0).findAny();
                    }
                    return l.size() == 0;
                }
            }
        """,
        after = """
            import java.util.List;

            class Test {
                static boolean method(List<String> l, List<List<String>> ll) {
                    if (true || l.isEmpty()) {
                        ll.stream().filter(p -> p.isEmpty()).findAny();
                    }
                    return l.isEmpty();
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1112")
    @Disabled // note, can merge these into the test above after fixing // todo
    fun handleLambda(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.List;

            class Test {
                static void method(List<List<String>> ll) {
                    ll.stream().filter(p -> p.size() == 0).findAny();
                }
            }
        """,
        after = """
            import java.util.List;

            class Test {
                static void method(List<List<String>> ll) {
                    ll.stream().filter(p -> p.isEmpty()).findAny();
                }
            }
        """
    )

}
