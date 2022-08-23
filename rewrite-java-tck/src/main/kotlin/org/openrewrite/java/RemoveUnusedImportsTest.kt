/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.Tree
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.style.ImportLayoutStyle
import org.openrewrite.style.NamedStyles
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

interface RemoveUnusedImportsTest : JavaRecipeTest, RewriteTest {
    override val recipe: Recipe
        get() = RemoveUnusedImports()

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(recipe)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/969")
    @Test
    fun doNotRemoveInnerClassImport(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.Map.Entry;

            public abstract class MyMapEntry<K, V> implements Entry<K, V> {
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1798")
    @Test
    fun doNotRemoveInnerClassInSamePackage(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            package java.util;

            import java.util.Map.Entry;

            public abstract class MyMapEntry<K, V> implements Entry<K, V> {
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1052")
    @Test
    fun usedInJavadocWithThrows(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.time.DateTimeException;
            
            class A {
                /** 
                 * @throws DateTimeException when ...
                 */
                void foo() {}
            }
        """
    )

    @Test
    fun usedInJavadoc(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.List;
            import java.util.Collection;
            
            /** {@link List} */
            class A {
                /** {@link Collection} */
                void foo() {}
            }
        """
    )

    @Test
    fun removeNamedImport(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.List;
            class A {}
        """,
        after = "class A {}"
    )

    @Test
    fun leaveImportIfRemovedTypeIsStillReferredTo(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.List;
            class A {
               List<Integer> list;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1251")
    @Test
    fun leaveImportIfAnnotationOnEnum(jp: JavaParser) = assertUnchanged(
        jp,
        dependsOn = arrayOf(
            """
            package com.google.gson.annotations;
            
            import java.lang.annotation.Documented;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Documented
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.FIELD, ElementType.METHOD})
            public @interface SerializedName {
                String value();
            }
        """
        ),
        before = """
            import com.google.gson.annotations.SerializedName;
            
            public enum PKIState {
                @SerializedName("active") ACTIVE,
                @SerializedName("dismissed") DISMISSED
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/617")
    @Test
    fun leaveImportForStaticImportEnumInAnnotation(jp: JavaParser) = assertUnchanged(
        jp,
        dependsOn = arrayOf(
            """
            package org.openrewrite.test;
            
            public @interface YesOrNo {
                Status status();
                enum Status {
                    YES, NO
                }
            }
        """
        ),
        before = """
            package org.openrewrite.test;
            
            import static org.openrewrite.test.YesOrNo.Status.YES;
            
            @YesOrNo(status = YES)
            public class Foo {}
        """
    )

    @Test
    fun removeStarImportIfNoTypesReferredTo(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.*;
            class A {}
        """,
        after = "class A {}"
    )

    @Test
    fun replaceStarImportWithNamedImportIfOnlyOneReferencedTypeRemains(jp: JavaParser) = assertChanged(
        jp,
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
    fun unfoldIfLessThanStarCount(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.*;
            class A {
               Collection<Integer> c;
               Set<Integer> s = new HashSet<>();
            }
        """,
        after = """
            import java.util.Collection;
            import java.util.HashSet;
            import java.util.Set;
            class A {
               Collection<Integer> c;
               Set<Integer> s = new HashSet<>();
            }
        """
    )

    @Test
    fun leaveStarImportInPlaceIfMoreThanStarCount(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.*;
            class A {
               Collection<Integer> c;
               Set<Integer> s = new HashSet<>();
               List<String> l = Arrays.asList("a","b","c");
            }
        """
    )

    @Test
    fun removeStarStaticImport(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import static java.util.Collections.*;
            class A {}
        """,
        after = "class A {}"
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/687")
    @Test
    fun leaveStarStaticImportIfReferenceStillExists(jp: JavaParser) = assertChanged(
        jp,
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
    fun removeStaticImportIfNotReferenced(jp: JavaParser) = assertChanged(
        jp,
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

    @Test
    fun leaveNamedStaticImportOnFieldIfReferenceStillExists(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package foo;
                public class B {
                    public static final String STRING = "string";
                    public static final String STRING2 = "string2";
                }
            """,
            """
                package foo;
                public class C {
                    public static final String ANOTHER = "string";
                }
            """
        ),
        before = """
            import static foo.B.STRING;
            import static foo.B.STRING2;
            import static foo.C.*;
            
            public class A {
                String a = STRING;
            }
        """,
        after = """
            import static foo.B.STRING;
            
            public class A {
                String a = STRING;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/429")
    @Test
    fun removePackageInfoImports(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.logCompilationWarningsAndErrors(true).build(),
        dependsOn = arrayOf(
            """
                package foo;
                public @interface FooAnnotation {}
                public @interface Foo {}
                public @interface Bar {}
            """
        ),
        before = """
            @Foo
            @Bar
            package foo.bar.baz;
            
            import foo.Bar;
            import foo.Foo;
            import foo.FooAnnotation;
        """,
        after = """
            @Foo
            @Bar
            package foo.bar.baz;
            
            import foo.Bar;
            import foo.Foo;
        """
    )

    @Test
    fun removePackageInfoStarImports(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package foo;
                public @interface FooAnnotation {}
                public @interface Foo {}
                public @interface Bar {}
            """
        ),
        before = """
            @Foo
            @Bar
            package foo.bar.baz;
            
            import foo.*;
        """,
        after = """
            @Foo
            @Bar
            package foo.bar.baz;
            
            import foo.Bar;
            import foo.Foo;
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/594")
    @Test
    fun dontRemoveStaticReferenceToPrimitiveField(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
            public class A {
                int t = TYPE_FORWARD_ONLY;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/580")
    @Test
    fun resultSetType(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.sql.ResultSet;
            public class A {
                int t = ResultSet.TYPE_FORWARD_ONLY;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/701")
    @Test
    fun ensuresWhitespaceAfterPackageDeclarationNoImportsRemain(jp: JavaParser) = assertChanged(
        jp,
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

    @Test
    fun doesNotAffectClassBodyFormatting(jp: JavaParser) = assertChanged(
        jp,
        before = """
            package com.example.foo;
            
            import java.util.List;
            import java.util.ArrayList;
            
            public class A {
            // Intentionally misaligned to ensure formatting is not overzealous
            ArrayList<String> foo = new ArrayList<>();
            }
        """,
        after = """
            package com.example.foo;
            
            import java.util.ArrayList;
            
            public class A {
            // Intentionally misaligned to ensure formatting is not overzealous
            ArrayList<String> foo = new ArrayList<>();
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/845")
    @Test
    fun doesNotRemoveStaticReferenceToNewClass() = assertUnchanged(
        dependsOn = arrayOf(
            """
            package org.openrewrite;
            public class Bar {
                public static final class Buz {
                    public Buz() {}
                }
            }
        """
        ),
        before = """
            package foo.test;

            import static org.openrewrite.Bar.Buz;

            public class Test {
                private void method() {
                    new Buz();
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/877")
    @Test
    fun doNotUnfoldStaticValidWildCard() = assertUnchanged(
        dependsOn = arrayOf(
            """
            package org.openrewrite;
            public class Foo {
                public static final int FOO_CONSTANT = 10;
                public static final class Bar {
                    private Bar() {}
                    public static void helper() {}
                }
                public static void fooMethod() {}
            }
        """
        ),
        before = """
            package foo.test;
            
            import static org.openrewrite.Foo.*;
            
            public class Test {
                int val = FOO_CONSTANT;
                private void method() {
                    fooMethod();
                    Bar.helper();
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/877")
    @Test
    fun unfoldStaticUses() = assertChanged(
        dependsOn = arrayOf(
            """
            package org.openrewrite;
            public class Foo {
                public static final int FOO_CONSTANT = 10;
                public static final class Bar {
                    private Bar(){}
                    public static void helper() {}
                }
                public static void fooMethod() {}
            }
        """
        ),
        before = """
            package foo.test;
            
            import static org.openrewrite.Foo.*;
            
            public class Test {
                int val = FOO_CONSTANT;
                private void method() {
                    Bar.helper();
                }
            }
        """,
        after = """
            package foo.test;
            
            import static org.openrewrite.Foo.FOO_CONSTANT;
            import static org.openrewrite.Foo.Bar;
            
            public class Test {
                int val = FOO_CONSTANT;
                private void method() {
                    Bar.helper();
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
                            .staticPackageToFold("java.util.Collections.*")
                            .importAllOthers()
                            .importStaticAllOthers()
                            .build()
                    )
                )
            )
        ).build(),
        before = """
            import java.util.*;
            
            import static java.util.Collections.*;

            class Test {
                List<String> l = emptyList();
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
                            .staticPackageToFold("java.util.*", false)
                            .importAllOthers()
                            .importStaticAllOthers()
                            .build()
                    )
                )
            )
        ).build(),
        before = """
            import java.util.concurrent.*;
            
            import static java.util.Collections.*;

            class Test {
                Object o = emptyMap();
                ConcurrentHashMap<String, String> m;
            }
        """,
        after = """
            import java.util.concurrent.ConcurrentHashMap;
            
            import static java.util.Collections.emptyMap;
            
            class Test {
                Object o = emptyMap();
                ConcurrentHashMap<String, String> m;
            }
        """
    )

    @Test
    fun doNotUnfoldSubpackage() = assertUnchanged(
        JavaParser.fromJavaVersion().styles(
            listOf(
                NamedStyles(
                    Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                        ImportLayoutStyle.builder()
                            .packageToFold("java.util.*", true)
                            .staticPackageToFold("java.util.*", true)
                            .importAllOthers()
                            .importStaticAllOthers()
                            .build()
                    )
                )
            )
        ).build(),
        before = """
            import java.util.concurrent.*;
            
            import static java.util.Collections.*;
            
            class Test {
                ConcurrentHashMap<String, String> m = emptyMap();
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1509")
    @Test
    fun removeImportsForSamePackage(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
            package com.google.gson.annotations;
            
            import java.lang.annotation.Documented;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Documented
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.FIELD, ElementType.METHOD})
            public @interface SerializedName {
                String value();
            }
        """
        ),
        before = """
            package com.google.gson.annotations;

            import com.google.gson.annotations.SerializedName;
            
            public enum PKIState {
                @SerializedName("active") ACTIVE,
                @SerializedName("dismissed") DISMISSED
            }
        """,
        after = """
            package com.google.gson.annotations;
            
            public enum PKIState {
                @SerializedName("active") ACTIVE,
                @SerializedName("dismissed") DISMISSED
            }
        """

    )

    @Test
    fun removeImportUsedAsLambdaParameter(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.HashMap;
            import java.util.function.Function;

            public class Test {
                public static void foo(){
                    final HashMap<Integer,String> map = new HashMap<>();
                    map.computeIfAbsent(3, integer -> String.valueOf(integer + 1));
                }
            }
        """,
        after = """
            import java.util.HashMap;

            public class Test {
                public static void foo(){
                    final HashMap<Integer,String> map = new HashMap<>();
                    map.computeIfAbsent(3, integer -> String.valueOf(integer + 1));
                }
            }
        """
    )

    @Test
    fun removeImportUsedAsMethodParameter(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.HashMap;
            import java.util.ArrayList;
            import java.util.Set;

            public class Test {
                public static void foo(){
                    new ArrayList<>(new HashMap<Integer, String>().keySet());
                }
            }
        """,
        after = """
            import java.util.HashMap;
            import java.util.ArrayList;

            public class Test {
                public static void foo(){
                    new ArrayList<>(new HashMap<Integer, String>().keySet());
                }
            }
        """
    )
}
