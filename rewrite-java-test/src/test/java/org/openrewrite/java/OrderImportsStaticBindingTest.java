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
package org.openrewrite.java;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.Cursor;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RewriteTest;

import java.util.*;

import static java.util.Collections.*;
import static org.openrewrite.Tree.randomId;

import static org.openrewrite.java.Assertions.java;

class OrderImportsStaticBindingTest implements RewriteTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preserveExplicitMethodBinding(boolean removeUnused) {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(removeUnused, null)).parser(JavaParser.fromJavaVersion().styles(singletonList(legacyStyle()))).cycles(3).expectedCyclesThatMakeChanges(1),
          java("""
            package p;
            public class Utils {
                public static <T> T assertNotNull(T value) { return value; }
                public static void checkEmpty() {}
                public static void checkPositive() {}
            }
            """),
          java("""
            package p;
            public class Assertions {
                public static void assertNotNull(Object value) {}
                public static void assertTrue(boolean value) {}
                public static void assertFalse(boolean value) {}
                public static void assertEquals(Object left, Object right) {}
            }
            """),
          java("""
            import static p.Assertions.*;
            import static p.Utils.assertNotNull;
            import static p.Utils.checkEmpty;
            import static p.Utils.checkPositive;

            class Example {
                void test(Object value) {
                    assertNotNull(value);
                    assertTrue(true);
                    assertFalse(false);
                    assertEquals(value, value);
                    checkEmpty();
                    checkPositive();
                }
            }
            """, """
            import static p.Assertions.*;
            import static p.Utils.*;
            import static p.Utils.assertNotNull;

            class Example {
                void test(Object value) {
                    assertNotNull(value);
                    assertTrue(true);
                    assertFalse(false);
                    assertEquals(value, value);
                    checkEmpty();
                    checkPositive();
                }
            }
            """)
        );
    }
    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void doNotIntroduceCompetingMethodFromStar(boolean removeUnused, boolean missingTargetMetadata) {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(removeUnused, null)).parser(JavaParser.fromJavaVersion().styles(singletonList(legacyStyle(missingTargetMetadata)))).cycles(3).expectedCyclesThatMakeChanges(removeUnused ? 1 : 0),
          java("""
            package p;
            public class AUtils {
                public static <T> T assertNotNull(T value) { return value; }
            }
            """),
          java("""
            package p;
            public class Assertions {
                public static void assertNotNull(Object value) {}
                public static void assertTrue(boolean value) {}
                public static void assertFalse(boolean value) {}
                public static void assertEquals(Object left, Object right) {}
            }
            """),
          removeUnused ? java("""
            import static p.AUtils.*;
            import static p.Assertions.assertEquals;
            import static p.Assertions.assertFalse;
            import static p.Assertions.assertTrue;

            class Example {
                void test(Object value) {
                    Object result = assertNotNull(value);
                    assertTrue(true);
                    assertFalse(false);
                    assertEquals(result, value);
                }
            }
            """, """
            import static p.AUtils.assertNotNull;
            import static p.Assertions.assertEquals;
            import static p.Assertions.assertFalse;
            import static p.Assertions.assertTrue;

            class Example {
                void test(Object value) {
                    Object result = assertNotNull(value);
                    assertTrue(true);
                    assertFalse(false);
                    assertEquals(result, value);
                }
            }
            """) : java("""
            import static p.AUtils.*;
            import static p.Assertions.assertEquals;
            import static p.Assertions.assertFalse;
            import static p.Assertions.assertTrue;

            class Example {
                void test(Object value) {
                    Object result = assertNotNull(value);
                    assertTrue(true);
                    assertFalse(false);
                    assertEquals(result, value);
                }
            }
            """)
        );
    }

    @Test
    void emptySource() {
        rewriteRun(spec -> spec.recipe(new OrderImports(true, null)).parser(JavaParser.fromJavaVersion().styles(singletonList(legacyStyle()))), java("class Empty {}"));
        rewriteRun(spec -> spec.recipe(new OrderImports(false, null)).parser(JavaParser.fromJavaVersion().styles(singletonList(legacyStyle()))), java("class Empty {}"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void foldNoncompetingMethods(boolean removeUnused) {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(removeUnused, null)).cycles(3).expectedCyclesThatMakeChanges(1),
          java("""
            import static java.util.Collections.emptyList;
            import static java.util.Collections.emptyMap;
            import static java.util.Collections.emptySet;
            import static java.util.Objects.requireNonNull;

            class Example {
                Thread.State state;
                int priority = Thread.MAX_PRIORITY;
                void test() {
                    requireNonNull(emptyList());
                    requireNonNull(emptyMap());
                    requireNonNull(emptySet());
                }
            }
            """, """
            import static java.util.Collections.*;
            import static java.util.Objects.requireNonNull;

            class Example {
                Thread.State state;
                int priority = Thread.MAX_PRIORITY;
                void test() {
                    requireNonNull(emptyList());
                    requireNonNull(emptyMap());
                    requireNonNull(emptySet());
                }
            }
            """)
        );
    }


    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preserveFieldBindingAgainstInheritedMember(boolean removeUnused) {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(removeUnused, null)).parser(JavaParser.fromJavaVersion().styles(singletonList(legacyStyle())))
            .cycles(3).expectedCyclesThatMakeChanges(1),
          java("""
            package p;
            public class Left {
                public static int VALUE = 1, FIRST = 2, SECOND = 3;
            }
            """),
          java("""
            package p;
            public class Base {
                public static int VALUE = 4;
            }
            """),
          java("""
            package p;
            public class Right extends Base {
                public static int THIRD = 5, FOURTH = 6, FIFTH = 7;
            }
            """),
          java("""
            import static p.Left.FIRST;
            import static p.Left.SECOND;
            import static p.Left.VALUE;
            import static p.Right.*;

            class Example {
                int value = VALUE + FIRST + SECOND + THIRD + FOURTH + FIFTH;
            }
            """, """
            import static p.Left.*;
            import static p.Left.VALUE;
            import static p.Right.*;

            class Example {
                int value = VALUE + FIRST + SECOND + THIRD + FOURTH + FIFTH;
            }
            """)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preserveMemberTypeBinding(boolean removeUnused) {
        rewriteRun(
          spec -> spec.recipe(new OrderImports(removeUnused, null)).parser(JavaParser.fromJavaVersion().styles(singletonList(legacyStyle())))
            .cycles(3).expectedCyclesThatMakeChanges(1),
          java("""
            package p;
            public class Left {
                public static class Member {}
                public static int FIRST = 2, SECOND = 3;
            }
            """),
          java("""
            package p;
            public class Right {
                public static class Member {}
                public static int THIRD = 5, FOURTH = 6, FIFTH = 7;
            }
            """),
          java("""
            import static p.Left.FIRST;
            import static p.Left.Member;
            import static p.Left.SECOND;
            import static p.Right.*;

            class Example {
                Member member;
                int value = FIRST + SECOND + THIRD + FOURTH + FIFTH;
            }
            """, """
            import static p.Left.*;
            import static p.Left.Member;
            import static p.Right.*;

            class Example {
                Member member;
                int value = FIRST + SECOND + THIRD + FOURTH + FIFTH;
            }
            """)
        );
    }

    private static NamedStyles legacyStyle() {
        return legacyStyle(false);
    }

    private static NamedStyles legacyStyle(boolean missingTargetMetadata) {
        ImportLayoutStyle style = new ImportLayoutStyle(5, 3, IntelliJ.importLayout().getLayout(), emptyList()) {
            @Override
            public List<JRightPadded<J.Import>> orderImports(List<JRightPadded<J.Import>> imports,
                    Collection<JavaType.FullyQualified> classpath, boolean dirty, @Nullable Cursor cursor) {
                // Legacy shared styles inspect only the external classpath. Project member
                // conflicts are missed, so each group of three imports folds unconditionally.
                Map<String, Integer> counts = new HashMap<>();
                for (JRightPadded<J.Import> padded : imports) {
                    if (padded.getElement().isStatic()) {
                        counts.merge(padded.getElement().getQualid().getTarget().printTrimmed(), 1, Integer::sum);
                    }
                }
                List<JRightPadded<J.Import>> result = new ArrayList<>();
                Set<String> folded = new HashSet<>();
                for (JRightPadded<J.Import> padded : imports) {
                    J.Import anImport = padded.getElement();
                    String owner = anImport.getQualid().getTarget().printTrimmed();
                    if (anImport.isStatic() && counts.getOrDefault(owner, 0) >= 3) {
                        if (folded.add(owner)) {
                            J.FieldAccess qualid = anImport.getQualid().withName(anImport.getQualid().getName().withSimpleName("*"));
                            if (missingTargetMetadata) {
                                qualid = qualid.withTarget(qualid.getTarget().withType(null));
                            }
                            result.add(padded.withElement(anImport.withQualid(qualid)));
                        }
                    } else {
                        result.add(padded);
                    }
                }
                if (result.size() == imports.size()) {
                    boolean same = true;
                    for (int i = 0; i < result.size(); i++) {
                        same &= result.get(i) == imports.get(i);
                    }
                    if (same) {
                        return imports;
                    }
                }
                return result;
            }
        };
        return new NamedStyles(randomId(), "legacy-static", "Legacy static folding", null, emptySet(), singletonList(style));
    }

}
