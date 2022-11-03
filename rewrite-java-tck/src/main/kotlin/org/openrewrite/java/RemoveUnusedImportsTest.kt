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
import org.openrewrite.Tree
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.style.ImportLayoutStyle
import org.openrewrite.style.NamedStyles
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

interface RemoveUnusedImportsTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(RemoveUnusedImports())
    }

    @Test
    fun enums() = rewriteRun(
        java("""
            package org.openrewrite;
            public enum E {
                A, B, C
            }
        """),
        java("""
            import org.openrewrite.E;
            import static org.openrewrite.E.A;
            import static org.openrewrite.E.B;
            public class Test {
                E a = A;
                E b = B;
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/969")
    @Test
    fun doNotRemoveInnerClassImport() = rewriteRun(
        java("""
            import java.util.Map.Entry;

            public abstract class MyMapEntry<K, V> implements Entry<K, V> {
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1798")
    @Test
    fun doNotRemoveInnerClassInSamePackage() = rewriteRun(
        java(
            """
            package java.util;

            import java.util.Map.Entry;

            public abstract class MyMapEntry<K, V> implements Entry<K, V> {
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1052")
    @Test
    fun usedInJavadocWithThrows() = rewriteRun(
        java("""
            import java.time.DateTimeException;
            
            class A {
                /** 
                 * @throws DateTimeException when ...
                 */
                void foo() {}
            }
        """)
    )

    @Test
    fun usedInJavadoc() = rewriteRun(
        java("""
            import java.util.List;
            import java.util.Collection;
            
            /** {@link List} */
            class A {
                /** {@link Collection} */
                void foo() {}
            }
        """)
    )

    @Test
    fun removeNamedImport() = rewriteRun(
        java("""
            import java.util.List;
            class A {}
        """,
        "class A {}")
    )

    @Test
    fun leaveImportIfRemovedTypeIsStillReferredTo() = rewriteRun(
        java("""
            import java.util.List;
            class A {
               List<Integer> list;
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1251")
    @Test
    fun leaveImportIfAnnotationOnEnum() = rewriteRun(
        java("""
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
        """),
        java("""
            import com.google.gson.annotations.SerializedName;
            
            public enum PKIState {
                @SerializedName("active") ACTIVE,
                @SerializedName("dismissed") DISMISSED
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/617")
    @Test
    fun leaveImportForStaticImportEnumInAnnotation() = rewriteRun(
        java("""
            package org.openrewrite.test;
            
            public @interface YesOrNo {
                Status status();
                enum Status {
                    YES, NO
                }
            }
        """
        ),
        java("""
            package org.openrewrite.test;
            
            import static org.openrewrite.test.YesOrNo.Status.YES;
            
            @YesOrNo(status = YES)
            public class Foo {}
        """)
    )

    @Test
    fun removeStarImportIfNoTypesReferredTo() = rewriteRun(
        java("""
            import java.util.*;
            class A {}
        """,
        "class A {}")
    )

    @Test
    fun replaceStarImportWithNamedImportIfOnlyOneReferencedTypeRemains() = rewriteRun(

        java("""
            import java.util.*;
            
            class A {
                Collection<Integer> c;
            }
        """,
        """
            import java.util.Collection;
            
            class A {
                Collection<Integer> c;
            }
        """)
    )

    @Test
    fun unfoldIfLessThanStarCount() = rewriteRun(
        java("""
            import java.util.*;
            class A {
               Collection<Integer> c;
               Set<Integer> s = new HashSet<>();
            }
        """,
        """
            import java.util.Collection;
            import java.util.HashSet;
            import java.util.Set;
            class A {
               Collection<Integer> c;
               Set<Integer> s = new HashSet<>();
            }
        """)
    )

    @Test
    fun leaveStarImportInPlaceIfMoreThanStarCount() = rewriteRun(
        java("""
            import java.util.*;
            class A {
               Collection<Integer> c;
               Set<Integer> s = new HashSet<>();
               List<String> l = Arrays.asList("a","b","c");
            }
        """)
    )

    @Test
    fun removeStarStaticImport() = rewriteRun(
        java("""
            import static java.util.Collections.*;
            class A {}
        """,
        "class A {}")
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/687")
    @Test
    fun leaveStarStaticImportIfReferenceStillExists() = rewriteRun(
        java("""
            import static java.util.Collections.*;
            class A {
               Object o = emptyList();
            }
        """,
        """
            import static java.util.Collections.emptyList;
            class A {
               Object o = emptyList();
            }
        """)
    )

    @Test
    fun removeStaticImportIfNotReferenced() = rewriteRun(
        java("""
            import java.time.DayOfWeek;
            
            import static java.time.DayOfWeek.MONDAY;
            import static java.time.DayOfWeek.TUESDAY;
            
            class WorkWeek {
                DayOfWeek shortWeekStarts(){
                    return TUESDAY;
                }
            }
        """,
        """
            import java.time.DayOfWeek;
            
            import static java.time.DayOfWeek.TUESDAY;
            
            class WorkWeek {
                DayOfWeek shortWeekStarts(){
                    return TUESDAY;
                }
            }
        """)
    )

    @Test
    fun leaveNamedStaticImportIfReferenceStillExists() = rewriteRun(

        java("""
            import static java.util.Collections.emptyList;
            import static java.util.Collections.emptySet;
            
            class A {
               Object o = emptyList();
            }
        """,
        """
            import static java.util.Collections.emptyList;
            
            class A {
               Object o = emptyList();
            }
        """)
    )

    @Test
    fun leaveNamedStaticImportOnFieldIfReferenceStillExists() = rewriteRun(
        java("""
                package foo;
                public class B {
                    public static final String STRING = "string";
                    public static final String STRING2 = "string2";
                }
        """),
        java("""
                package foo;
                public class C {
                    public static final String ANOTHER = "string";
                }
        """),
        java("""
            import static foo.B.STRING;
            import static foo.B.STRING2;
            import static foo.C.*;
            
            public class A {
                String a = STRING;
            }
        """,
        """
            import static foo.B.STRING;
            
            public class A {
                String a = STRING;
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/429")
    @Test
    fun removePackageInfoImports() = rewriteRun(
        java(
            """
                package foo;
                public @interface FooAnnotation {}
                public @interface Foo {}
                public @interface Bar {}
            """
        ),
        java("""
            @Foo
            @Bar
            package foo.bar.baz;
            
            import foo.Bar;
            import foo.Foo;
            import foo.FooAnnotation;
        """,
        """
            @Foo
            @Bar
            package foo.bar.baz;
            
            import foo.Bar;
            import foo.Foo;
        """)
    )

    @Test
    fun removePackageInfoStarImports() = rewriteRun(
        java(
            """
                package foo;
                public @interface FooAnnotation {}
                public @interface Foo {}
                public @interface Bar {}
            """
        ),
        java("""
            @Foo
            @Bar
            package foo.bar.baz;
            
            import foo.*;
        """,
        """
            @Foo
            @Bar
            package foo.bar.baz;
            
            import foo.Bar;
            import foo.Foo;
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/594")
    @Test
    fun dontRemoveStaticReferenceToPrimitiveField() = rewriteRun(
        java("""
            import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
            public class A {
                int t = TYPE_FORWARD_ONLY;
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/580")
    @Test
    fun resultSetType() = rewriteRun(
        java("""
            import java.sql.ResultSet;
            public class A {
                int t = ResultSet.TYPE_FORWARD_ONLY;
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/701")
    @Test
    fun ensuresWhitespaceAfterPackageDeclarationNoImportsRemain() = rewriteRun(

        java("""
            package com.example.foo;
            import java.util.List;
            public class A {
            }
        """,
        """
            package com.example.foo;
            
            public class A {
            }
        """)
    )

    @Test
    fun doesNotAffectClassBodyFormatting() = rewriteRun(

        java("""
            package com.example.foo;
            
            import java.util.List;
            import java.util.ArrayList;
            
            public class A {
            // Intentionally misaligned to ensure formatting is not overzealous
            ArrayList<String> foo = new ArrayList<>();
            }
        """,
        """
            package com.example.foo;
            
            import java.util.ArrayList;
            
            public class A {
            // Intentionally misaligned to ensure formatting is not overzealous
            ArrayList<String> foo = new ArrayList<>();
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/845")
    @Test
    fun doesNotRemoveStaticReferenceToNewClass() = rewriteRun(
        java(
            """
            package org.openrewrite;
            public class Bar {
                public static final class Buz {
                    public Buz() {}
                }
            }
        """
        ),
        java("""
            package foo.test;

            import static org.openrewrite.Bar.Buz;

            public class Test {
                private void method() {
                    new Buz();
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/877")
    @Test
    fun doNotUnfoldStaticValidWildCard() = rewriteRun(
        java(
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
        java("""
            package foo.test;
            
            import static org.openrewrite.Foo.*;
            
            public class Test {
                int val = FOO_CONSTANT;
                private void method() {
                    fooMethod();
                    Bar.helper();
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/877")
    @Test
    fun unfoldStaticUses() = rewriteRun(
        java(
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
        """),
        java("""
            package foo.test;
            
            import static org.openrewrite.Foo.*;
            
            public class Test {
                int val = FOO_CONSTANT;
                private void method() {
                    Bar.helper();
                }
            }
        """,
        """
            package foo.test;
            
            import static org.openrewrite.Foo.FOO_CONSTANT;
            import static org.openrewrite.Foo.Bar;
            
            public class Test {
                int val = FOO_CONSTANT;
                private void method() {
                    Bar.helper();
                }
            }
        """)
    )

    @Test
    fun doNotUnfoldPackage() = rewriteRun(
        { spec -> spec.parser(JavaParser.fromJavaVersion().styles(
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
        ))},
        java("""
            import java.util.*;
            
            import static java.util.Collections.*;

            class Test {
                List<String> l = emptyList();
            }
        """)
    )

    @Test
    fun unfoldSubpackage() = rewriteRun(
        {spec -> spec.parser(JavaParser.fromJavaVersion().styles(
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
        ))},
        java("""
            import java.util.concurrent.*;
            
            import static java.util.Collections.*;

            class Test {
                Object o = emptyMap();
                ConcurrentHashMap<String, String> m;
            }
        """,
        """
            import java.util.concurrent.ConcurrentHashMap;
            
            import static java.util.Collections.emptyMap;
            
            class Test {
                Object o = emptyMap();
                ConcurrentHashMap<String, String> m;
            }
        """)
    )

    @Test
    fun doNotUnfoldSubpackage() = rewriteRun(
        { spec -> spec.parser(JavaParser.fromJavaVersion().styles(
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
        ))},
        java("""
            import java.util.concurrent.*;
            
            import static java.util.Collections.*;
            
            class Test {
                ConcurrentHashMap<String, String> m = emptyMap();
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1509")
    @Test
    fun removeImportsForSamePackage() = rewriteRun(
        java(
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
        java("""
            package com.google.gson.annotations;

            import com.google.gson.annotations.SerializedName;
            
            public enum PKIState {
                @SerializedName("active") ACTIVE,
                @SerializedName("dismissed") DISMISSED
            }
        """,
        """
            package com.google.gson.annotations;
            
            public enum PKIState {
                @SerializedName("active") ACTIVE,
                @SerializedName("dismissed") DISMISSED
            }
        """)
    )

    @Test
    fun removeImportUsedAsLambdaParameter() = rewriteRun(
        java("""
            import java.util.HashMap;
            import java.util.function.Function;

            public class Test {
                public static void foo(){
                    final HashMap<Integer,String> map = new HashMap<>();
                    map.computeIfAbsent(3, integer -> String.valueOf(integer + 1));
                }
            }
        """,
        """
            import java.util.HashMap;

            public class Test {
                public static void foo(){
                    final HashMap<Integer,String> map = new HashMap<>();
                    map.computeIfAbsent(3, integer -> String.valueOf(integer + 1));
                }
            }
        """)
    )

    @Test
    fun removeImportUsedAsMethodParameter() = rewriteRun(
        java("""
            import java.util.HashMap;
            import java.util.ArrayList;
            import java.util.Set;

            public class Test {
                public static void foo(){
                    new ArrayList<>(new HashMap<Integer, String>().keySet());
                }
            }
        """,
        """
            import java.util.HashMap;
            import java.util.ArrayList;

            public class Test {
                public static void foo(){
                    new ArrayList<>(new HashMap<Integer, String>().keySet());
                }
            }
        """)
    )

    @Test
    fun removeMultipleDirectImports() = rewriteRun(
        java("""
            import java.util.Collection;
            import java.util.Collection;

            import static java.util.Collections.emptyList;
            import static java.util.Collections.emptyList;

            class A {
               Collection<Integer> c = emptyList();
            }
        """,
        """
            import java.util.Collection;

            import static java.util.Collections.emptyList;

            class A {
               Collection<Integer> c = emptyList();
            }
        """)
    )

    @Test
    fun removeMultipleWildcardImports() = rewriteRun(
        java("""
            import java.util.*;
            import java.util.*;

            import static java.util.Collections.emptyList;

            class A {
               Collection<Integer> c = emptyList();
               Set<Integer> s = new HashSet<>();
               List<String> l = Arrays.asList("a","b","c");
            }
        """,
        """
            import java.util.*;

            import static java.util.Collections.emptyList;

            class A {
               Collection<Integer> c = emptyList();
               Set<Integer> s = new HashSet<>();
               List<String> l = Arrays.asList("a","b","c");
            }
        """)
    )

    @Test
    fun removeWildcardImportWithDirectImport() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(2) },
        java("""
            import java.util.*;
            import java.util.Collection;

            import static java.util.Collections.*;
            import static java.util.Collections.emptyList;

            class A {
               Collection<Integer> c = emptyList();
            }
        """,
        """
            import java.util.Collection;

            import static java.util.Collections.emptyList;

            class A {
               Collection<Integer> c = emptyList();
            }
        """),
    )

    @Test
    fun removeDirectImportWithWildcardImport() = rewriteRun(
        java("""
            import java.util.Set;
            import java.util.*;

            import static java.util.Collections.emptyList;
            import static java.util.Collections.*;

            class A {
               Collection<Integer> c = emptyList();
               Set<Integer> s = emptySet();
               List<String> l = Arrays.asList("c","b","a");
               Iterator<Short> i = emptyIterator();
            }
        """,
        """
            import java.util.*;

            import static java.util.Collections.*;

            class A {
               Collection<Integer> c = emptyList();
               Set<Integer> s = emptySet();
               List<String> l = Arrays.asList("c","b","a");
               Iterator<Short> i = emptyIterator();
            }
        """)
    )

    @Test
    fun removeMultipleImportsWhileUnfoldingWildcard() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(2) },
        java("""
            import java.util.Set;
            import java.util.*;

            import static java.util.Collections.emptyList;
            import static java.util.Collections.*;

            class A {
               Collection<Integer> c = emptyList();
               Set<Integer> s = emptySet();
            }
        """,
        """
            import java.util.Set;
            import java.util.Collection;

            import static java.util.Collections.emptyList;
            import static java.util.Collections.emptySet;

            class A {
               Collection<Integer> c = emptyList();
               Set<Integer> s = emptySet();
            }
        """)
    )

    @Test
    fun keepInnerClassImports() = rewriteRun(
        java("""
            import java.util.*;
            import java.util.Map.Entry;

            import static java.util.Collections.*;

            class A {
               Collection<Integer> c = emptyList();
               Set<Integer> s = emptySet();
               List<String> l = Arrays.asList("c","b","a");
               Iterator<Short> i = emptyIterator();
               Entry<String, Integer> entry;
            }
        """)
    )
}
