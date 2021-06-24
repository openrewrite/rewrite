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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue

interface RemoveImportTest : JavaRecipeTest {
    fun removeImport(type: String) =
            RemoveImport<ExecutionContext>(type).toRecipe()

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

    @Test
    fun leaveStarImportInPlaceIfThreeOrMoreTypesStillReferredTo(jp: JavaParser) = assertUnchanged(
            jp,
            recipe = removeImport("java.util.List"),
            before = """
            import java.util.*;
            class A {
               Collection<Integer> c;
               Set<Integer> s = new HashSet<>();
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

    @Disabled
    @Issue("https://github.com/openrewrite/rewrite/issues/687")
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
}
