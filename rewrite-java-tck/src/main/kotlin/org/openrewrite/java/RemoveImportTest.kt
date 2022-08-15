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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue
import org.openrewrite.Tree
import org.openrewrite.java.style.ImportLayoutStyle
import org.openrewrite.style.NamedStyles

interface RemoveImportTest : JavaRecipeTest {
    fun removeImport(type: String, force: Boolean = false) = toRecipe {
        RemoveImport<ExecutionContext>(type, force)
    }

    @Test
    fun removeNamedImport(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeImport("java.util.List"),
        before = """
            import java.util.List;
            class A {}
        """,
        after = "class A {}"
    )

    @Test
    fun leaveImportIfRemovedTypeIsStillReferredTo(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = removeImport("java.util.List"),
        before = """
            import java.util.List;
            class A {
               List<Integer> list;
            }
        """
    )

    @Test
    fun leaveWildcardImportIfRemovedTypeIsStillReferredTo(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = removeImport("java.util.*"),
        before = """
            import java.util.*;
            class A {
               List<Integer> list;
            }
        """
    )

    @Test
    fun removeStarImportIfNoTypesReferredTo(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeImport("java.util.List"),
        before = """
            import java.util.*;
            class A {}
        """,
        after = "class A {}"
    )

    @Test
    fun replaceStarImportWithNamedImportIfOnlyOneReferencedTypeRemains(jp: JavaParser) = assertChanged(
            jp,
            recipe = removeImport("java.util.List"),
            before = """
            import java.util.*;
            
            class A {
               Collection<Integer> c;
            }
        """,
            after = """
            import java.util.Collection;
            
            class A {
               Collection<Integer> c;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/919")
    @Test
    fun leaveStarImportInPlaceIfFiveOrMoreTypesStillReferredTo(jp: JavaParser) = assertUnchanged(
            jp,
            recipe = removeImport("java.util.List"),
            before = """
            import java.util.*;
            class A {
               Collection<Integer> c;
               Set<Integer> s = new HashSet<>();
               Map<Integer, Integer> m = new HashMap<>();
            }
        """
    )

    @Test
    fun removeStarStaticImport(jp: JavaParser) = assertChanged(
            jp,
            recipe = removeImport("java.util.Collections"),
            before = """
            import static java.util.Collections.*;
            class A {}
        """,
            after = "class A {}"
    )

    @Test
    fun removeStarStaticImportWhenRemovingSpecificMethod(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeImport("java.util.Collections.emptyList"),
        before = """
            import static java.util.Collections.*;
            class A {
                Object o = emptySet();
            }
        """,
        after = """
            import static java.util.Collections.emptySet;
            class A {
                Object o = emptySet();
            }
        """
    )

    @Test
    fun removeStarImportEvenIfReferredTo(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeImport("java.util.List", true),
        before = """
            import java.util.List;
            class A {
                List<String> l;
            }
        """,
        after = """
            class A {
                List<String> l;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/918")
    @Test
    fun leaveStarStaticImportIfReferenceStillExists(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeImport("java.util.Collections"),
        before = """
            import static java.util.Collections.*;
            class A {
               Object o = emptyList();
            }
        """,
        after = """
            import static java.util.Collections.emptyList;
            class A {
               Object o = emptyList();
            }
        """.trimIndent()
    )

    @Test
    fun removeStaticImport(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeImport("java.time.DayOfWeek.MONDAY"),
        before = """
            import java.time.DayOfWeek;
            
            import static java.time.DayOfWeek.MONDAY;
            import static java.time.DayOfWeek.TUESDAY;
            
            class WorkWeek {
                DayOfWeek shortWeekStarts(){
                    return TUESDAY;
                }
            }
        """,
        after = """
            import java.time.DayOfWeek;
            
            import static java.time.DayOfWeek.TUESDAY;
            
            class WorkWeek {
                DayOfWeek shortWeekStarts(){
                    return TUESDAY;
                }
            }
        """
    )

    @Test
    fun leaveNamedStaticImportIfReferenceStillExists(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeImport("java.util.Collections"),
        before = """
            import static java.util.Collections.emptyList;
            import static java.util.Collections.emptySet;
            
            class A {
               Object o = emptyList();
            }
        """,
        after = """
            import static java.util.Collections.emptyList;
            
            class A {
               Object o = emptyList();
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/701")
    @Test
    fun preservesWhitespaceAfterPackageDeclaration(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeImport("java.util.List"),
        before = """
            package com.example.foo;
            
            import java.util.List;
            import java.util.ArrayList;
            
            public class A {
                ArrayList<String> foo = new ArrayList<>();
            }
        """,
        after = """
            package com.example.foo;
            
            import java.util.ArrayList;
            
            public class A {
                ArrayList<String> foo = new ArrayList<>();
            }
        """
    )

    @Test
    fun preservesWhitespaceAfterRemovedImport(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeImport("java.util.List"),
        before = """
            package com.example.foo;
            
            import java.util.Collection;
            import java.util.List;
            
            import java.util.ArrayList;
            
            public class A {
            }
        """,
        after = """
            package com.example.foo;
            
            import java.util.Collection;
            
            import java.util.ArrayList;
            
            public class A {
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/701")
    @Test
    fun preservesWhitespaceAfterPackageDeclarationNoImportsRemain(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeImport("java.util.List"),
        before = """
            package com.example.foo;
            
            import java.util.List;
            
            public class A {
            }
        """,
        after = """
            package com.example.foo;
            
            public class A {
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/701")
    @Test
    fun preservesWhitespaceBetweenGroupsOfImports(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeImport("java.util.List"),
        dependsOn = arrayOf("""
            package com.yourorg.b;
            public class B {}
        """),
        before = """
            package com.example.foo;
            
            import com.yourorg.b.B;
            
            import java.util.List;
            import java.util.ArrayList;
            
            public class A {
                ArrayList<B> foo = new ArrayList<>();
            }
        """,
        after = """
            package com.example.foo;
            
            import com.yourorg.b.B;
            
            import java.util.ArrayList;
            
            public class A {
                ArrayList<B> foo = new ArrayList<>();
            }
        """
    )

    @Test
    fun doesNotAffectClassBodyFormatting(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeImport("java.util.List"),
        before = """
            package com.example.foo;
            
            import java.util.List;
            import java.util.ArrayList;
            
            public class A {
            // Intentionally misaligned to ensure AutoFormat has not been applied to the class body
            ArrayList<String> foo = new ArrayList<>();
            }
        """,
        after = """
            package com.example.foo;
            
            import java.util.ArrayList;
            
            public class A {
            // Intentionally misaligned to ensure AutoFormat has not been applied to the class body
            ArrayList<String> foo = new ArrayList<>();
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/784")
    @Test
    fun removeFromWildcardAndDuplicateImport(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeImport("java.util.Collection"),
        before = """
            package a;

            import java.util.*;
            import java.util.List;

            public class A {
                Set<Integer> s;
                List<Integer> l;
            }
        """,
        after = """
            package a;

            import java.util.Set;
            import java.util.List;

            public class A {
                Set<Integer> s;
                List<Integer> l;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/781")
    @Test
    fun generateNewUUIDPerUnfoldedImport(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeImport("java.util.Collection")
            .doNext(ChangeType("java.util.List", "java.util.Collection", null)),
        before = """
            package a;

            import java.util.*;
            import java.util.List;

            public class A {
                Set<Integer> s;
                List<Integer> l;
            }
        """,
        after = """
            package a;

            import java.util.Collection;
            import java.util.Set;

            public class A {
                Set<Integer> s;
                Collection<Integer> l;
            }
        """,
        afterConditions = { cu ->
            cu.imports[0].id != cu.imports[1].id
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/918")
    @Test
    fun leaveAloneIfThreeOrMoreStaticsAreInUse(jp: JavaParser) = assertUnchanged(
        jp,
        dependsOn = arrayOf("""
            package org.test;
            public class Example {
                public static final int VALUE_1 = 1;
                public static final int VALUE_2 = 2;
                public static final int VALUE_3 = 3;
                public static int method1() { return 1; }
            }
        """),
        recipe = removeImport("org.test.Example.VALUE_1"),
        before = """
            package org.test.a;
            
            import static org.test.Example.*;
            
            public class Test {
                public void method() {
                    int value2 = VALUE_2;
                    int value3 = VALUE_3;
                    int methodValue = method1();
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/918")
    @Test
    fun unfoldStaticImportIfTwoOrLessAreUsed(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf("""
            package org.test;
            public class Example {
                public static final int VALUE_1 = 1;
                public static final int VALUE_2 = 2;
                public static final int VALUE_3 = 3;
                public static int method1() { return 1; }
            }
        """),
        recipe = removeImport("org.test.Example.VALUE_1"),
        before = """
            package org.test.a;
            
            import static org.test.Example.*;
            
            public class Test {
                public void method() {
                    int value2 = VALUE_2;
                    int methodValue = method1();
                }
            }
        """,
        after = """
            package org.test.a;
            
            import static org.test.Example.method1;
            import static org.test.Example.VALUE_2;
            
            public class Test {
                public void method() {
                    int value2 = VALUE_2;
                    int methodValue = method1();
                }
            }
        """
    )

    @Test
    fun doNotUnfoldPackage() = assertUnchanged(
        JavaParser.fromJavaVersion().styles(
            listOf(
                NamedStyles(
                    Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                        ImportLayoutStyle.builder()
                            .packageToFold("java.util.*")
                            .importAllOthers()
                            .importStaticAllOthers()
                            .build()
                    )
                )
            )
        ).build(),
        recipe = removeImport("java.util.Map"),
        before = """
            import java.util.*;
            
            class Test {
                List<String> l;
            }
        """
    )

    @Test
    fun doNotUnfoldSubPackage() = assertUnchanged(
        JavaParser.fromJavaVersion().styles(
            listOf(
                NamedStyles(
                    Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                        ImportLayoutStyle.builder()
                            .packageToFold("java.util.*")
                            .importAllOthers()
                            .importStaticAllOthers()
                            .build()
                    )
                )
            )
        ).build(),
        recipe = removeImport("java.util.concurrent.ConcurrentLinkedQueue"),
        before = """
            import java.util.*;
            import java.util.concurrent.*;
            
            class Test {
                Map<Integer, Integer> m = new ConcurrentHashMap<>();
            }
        """
    )

    @Test
    fun unfoldSubpackage() = assertChanged(
        JavaParser.fromJavaVersion().styles(
            listOf(
                NamedStyles(
                    Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                        ImportLayoutStyle.builder()
                            .packageToFold("java.util.*", false)
                            .importAllOthers()
                            .importStaticAllOthers()
                            .build()
                    )
                )
            )
        ).build(),
        recipe = removeImport("java.util.concurrent.ConcurrentLinkedQueue"),
        before = """
            import java.util.*;
            import java.util.concurrent.*;
            
            class Test {
                Map<Integer, Integer> m = new ConcurrentHashMap<>();
            }
        """,
        after = """
            import java.util.*;
            import java.util.concurrent.ConcurrentHashMap;
            
            class Test {
                Map<Integer, Integer> m = new ConcurrentHashMap<>();
            }
        """
    )

    @Test
    fun doNotUnfoldStaticPackage() = assertUnchanged(
        JavaParser.fromJavaVersion().styles(
            listOf(
                NamedStyles(
                    Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                        ImportLayoutStyle.builder()
                            .staticPackageToFold("java.util.Collections.*")
                            .importAllOthers()
                            .importStaticAllOthers()
                            .build()
                    )
                )
            )
        ).build(),
        recipe = removeImport("java.util.Collections.emptyMap"),
        before = """
            import java.util.*;
            import static java.util.Collections.*;
            
            class Test {
                List<String> l = emptyList();
            }
        """
    )

    @Test
    fun doNotUnfoldStaticSubPackage() = assertUnchanged(
        JavaParser.fromJavaVersion().styles(
            listOf(
                NamedStyles(
                    Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                        ImportLayoutStyle.builder()
                            .staticPackageToFold("java.util.*")
                            .importAllOthers()
                            .importStaticAllOthers()
                            .build()
                    )
                )
            )
        ).build(),
        recipe = removeImport("java.util.Collections.emptyMap"),
        before = """
            import java.util.List;
            import static java.util.Collections.*;
            
            class Test {
                List<Integer> l = emptyList();
            }
        """
    )

    @Test
    fun unfoldStaticSubpackage() = assertChanged(
        JavaParser.fromJavaVersion().styles(
            listOf(
                NamedStyles(
                    Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                        ImportLayoutStyle.builder()
                            .packageToFold("java.util.*", false)
                            .importAllOthers()
                            .importStaticAllOthers()
                            .build()
                    )
                )
            )
        ).build(),
        recipe = removeImport("java.util.Collections.emptyMap"),
        before = """
            import java.util.List;
            import static java.util.Collections.*;
            
            class Test {
                List<Integer> l = emptyList();
            }
        """,
        after = """
            import java.util.List;
            import static java.util.Collections.emptyList;
            
            class Test {
                List<Integer> l = emptyList();
            }
        """
    )
}
