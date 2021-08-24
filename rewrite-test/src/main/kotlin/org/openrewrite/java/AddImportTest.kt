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

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.openrewrite.*
import org.openrewrite.Tree.randomId
import org.openrewrite.java.marker.JavaSourceSet
import org.openrewrite.java.tree.Flag
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType

interface AddImportTest : JavaRecipeTest {
    fun addImports(vararg adds: () -> TreeVisitor<*, ExecutionContext>): Recipe = adds
        .map { add -> toRecipe(add) }
        .reduce { r1, r2 -> return r1.doNext(r2) }

    @Test
    fun dontDuplicateImports(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            { AddImport("org.springframework.http.HttpStatus", null, false) },
            { AddImport("org.springframework.http.HttpStatus.Series", null, false) }
        ),
        before = "class A {}",
        after = """
            import org.springframework.http.HttpStatus;
            import org.springframework.http.HttpStatus.Series;
            
            class A {}
        """
    )

    @Test
    fun dontDuplicateImports2(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            { AddImport("org.junit.jupiter.api.Test", null, false) }
        ),
        before = """
            import org.junit.jupiter.api.AfterEach;
            import org.junit.jupiter.api.Assertions;
            import org.junit.jupiter.api.BeforeAll;
            import org.junit.jupiter.api.BeforeEach;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            class A {}
        """,
        after = """
            import org.junit.jupiter.api.*;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            
            class A {}
        """,
        cycles = 1,
        expectedCyclesThatMakeChanges = 1
    )

    @Test
    fun dontDuplicateImports3(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            { AddImport("org.junit.jupiter.api.Assertions", "assertNull", false) }
        ),
        before = """
            import static org.junit.jupiter.api.Assertions.assertFalse;
            import static org.junit.jupiter.api.Assertions.assertTrue;
            
            import java.util.List;

            class A {}
        """,
        after = """
            import static org.junit.jupiter.api.Assertions.*;
            
            import java.util.List;
            
            class A {}
        """,
        cycles = 1,
        expectedCyclesThatMakeChanges = 1
    )

    @Test
    fun dontImportYourself(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports({ AddImport("com.myorg.A", null, false) }),
        before = """
            package com.myorg;
            
            class A {
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/777")
    @Test
    fun dontImportFromSamePackage(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports({ AddImport("com.myorg.B", null, false) }),
        dependsOn = arrayOf(
            """
            package com.myorg;
            
            class B {
            }
        """
        ),
        before = """
            package com.myorg;
            
            class A {
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/772")
    @Test
    fun importOrderingIssue(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            { AddImport("org.springframework.http.HttpHeaders", null, false) },
        ),
        before = """
            import javax.ws.rs.core.Response.ResponseBuilder;
            import java.util.Locale;

            class A {}
        """,
        after = """
            import org.springframework.http.HttpHeaders;

            import javax.ws.rs.core.Response.ResponseBuilder;
            import java.util.Locale;

            class A {}
        """
    )

    @Test
    fun addMultipleImports(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            { AddImport("java.util.List", null, false) },
            { AddImport("java.util.Set", null, false) }
        ),
        before = """
            class A {}
        """,
        after = """
            import java.util.List;
            import java.util.Set;

            class A {}
        """
    )

    @Test
    fun addNamedImport(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            { AddImport("java.util.List", null, false) }
        ),
        before = "class A {}",
        after = """
            import java.util.List;
            
            class A {}
        """
    )

    @Test
    fun doNotAddImportIfNotReferenced(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            { AddImport("java.util.List", null, true) }
        ),
        before = """
            package a;
            
            class A {}
        """
    )

    @Test
    fun addImportInsertsNewMiddleBlock(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            { AddImport("java.util.List", null, false) }
        ),
        before = """
            package a;
            
            import com.sun.naming.*;
            
            import static java.util.Collections.*;
            
            class A {}
        """,
        after = """
            package a;
            
            import com.sun.naming.*;
            
            import java.util.List;
            
            import static java.util.Collections.*;
            
            class A {}
        """
    )

    @Test
    fun addFirstImport(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            { AddImport("java.util.List", null, false) }
        ),
        before = """
            package a;
            
            class A {}
        """,
        after = """
            package a;
            
            import java.util.List;
            
            class A {}
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/484")
    @Test
    fun addImportIfReferenced(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    ctx: ExecutionContext
                ): J.ClassDeclaration {
                    val c = super.visitClassDeclaration(classDecl, ctx)
                    var b = c.body
                    if (ctx.getMessage("cyclesThatResultedInChanges", 0) == 0) {
                        val t = JavaTemplate.builder(
                            { cursor },
                            "BigDecimal d = BigDecimal.valueOf(1).setScale(1, RoundingMode.HALF_EVEN);"
                        )
                            .imports("java.math.BigDecimal", "java.math.RoundingMode")
                            .build()

                        b = b.withTemplate(t, b.coordinates.lastStatement())
                        maybeAddImport("java.math.BigDecimal")
                        maybeAddImport("java.math.RoundingMode")
                    }
                    return c.withBody(b)
                }
            }
        },
        before = """
            package a;

            class A {
            }
        """,
        after = """
            package a;
            
            import java.math.BigDecimal;
            import java.math.RoundingMode;
            
            class A {
                BigDecimal d = BigDecimal.valueOf(1).setScale(1, RoundingMode.HALF_EVEN);
            }
        """
    )

    @Test
    fun doNotAddWildcardImportIfNotReferenced(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            { AddImport("java.util.*", null, true) }
        ),
        before = """
            package a;
            
            class A {}
        """
    )

    @Test
    fun lastImportWhenFirstClassDeclarationHasJavadoc(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            { AddImport("java.util.Collections", "*", false) }
        ),
        before = """
            import java.util.List;
            
            /**
             * My type
             */
            class A {}
        """,
        after = """
            import java.util.List;
            
            import static java.util.Collections.*;
            
            /**
             * My type
             */
            class A {}
        """
    )

    @Test
    fun namedImportAddedAfterPackageDeclaration(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            { AddImport("java.util.List", null, false) }
        ),
        before = """
            package a;
            class A {}
        """,
        after = """
            package a;
            
            import java.util.List;
            
            class A {}
        """
    )

    @Test
    fun importsAddedInAlphabeticalOrder(jp: JavaParser) {
        val otherPackages = listOf("c", "c.c", "c.c.c")
        val otherImports = otherPackages.mapIndexed { i, pkg ->
            "package $pkg;\npublic class C$i {}"
        }

        listOf("b" to 0, "c.b" to 1, "c.c.b" to 2).forEach {
            val (pkg, order) = it

            val expectedImports = otherPackages.mapIndexed { i, otherPkg -> "$otherPkg.C$i" }.toMutableList()
            expectedImports.add(order, "$pkg.B")

            assertChanged(
                jp,
                dependsOn = arrayOf(
                    *otherImports.toTypedArray(),
                    """
                            package $pkg;
                            public class B {}
                        """
                ),
                recipe = addImports(
                    { AddImport("$pkg.B", null, false) }
                ),
                before = """
                    package a;
        
                    import c.C0;
                    import c.c.C1;
                    import c.c.c.C2;
        
                    class A {}
                """,
                after = "package a;\n\n${expectedImports.joinToString("\n") { fqn -> "import $fqn;" }}\n\nclass A {}"
            )

            jp.reset()
        }
    }

    @Test
    fun doNotAddImportIfAlreadyExists(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            { AddImport("java.util.List", null, false) }
        ),
        before = """
            package a;
            
            import java.util.List;
            class A {}
        """
    )

    @Test
    fun doNotAddImportIfCoveredByStarImport(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            { AddImport("java.util.List", null, false) }
        ),
        before = """
            package a;
            
            import java.util.*;
            class A {}
        """
    )

    @Test
    fun dontAddImportWhenClassHasNoPackage(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            { AddImport("C", null, false) }
        ),
        before = "class A {}"
    )

    @Test
    fun dontAddImportForPrimitive(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            { AddImport("int", null, false) }
        ),
        before = "class A {}"
    )

    @Test
    fun addNamedImportIfStarStaticImportExists(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            { AddImport("java.util.List", null, false) }
        ),
        before = """
            package a;
            
            import static java.util.List.*;
            class A {}
        """,
        after = """
            package a;
            
            import java.util.List;
            
            import static java.util.List.*;
            
            class A {}
        """
    )

    @Test
    fun addNamedStaticImport(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            { AddImport("java.util.Collections", "emptyList", false) }
        ),
        before = """
            import java.util.*;
            class A {}
        """,
        after = """
            import java.util.*;
            
            import static java.util.Collections.emptyList;
            
            class A {}
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/108")
    @Test
    fun addStaticImportField(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            { AddImport("mycompany.Type", "FIELD", false) }
        ),
        dependsOn = arrayOf(
            """
                package mycompany;
                public class Type {
                    public static String FIELD;
                }
            """
        ),
        before = "class A {}",
        after = """
            import static mycompany.Type.FIELD;
            
            class A {}
        """
    )

    @Test
    fun dontAddStaticWildcardImportIfNotReferenced(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            { AddImport("java.util.Collections", "*", true) }
        ),
        before = """
            package a;
            
            class A {}
        """
    )

    @Test
    fun addNamedStaticImportWhenReferenced(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : Recipe() {
            override fun getDisplayName(): String {
                return "Test"
            }

            override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
                return object : JavaIsoVisitor<ExecutionContext>() {
                    override fun visitMethodInvocation(m: J.MethodInvocation, ctx: ExecutionContext) =
                        m.withSelect(null)
                }
            }

        }.doNext(
            addImports({ AddImport("java.util.Collections", "emptyList", true) })
        ),
        before = """
            package a;
            
            import java.util.List;
            
            class A {
                public A() {
                    List<String> list = java.util.Collections.emptyList();
                }
            }
        """,
        after = """
            package a;
            
            import java.util.List;
            
            import static java.util.Collections.emptyList;
            
            class A {
                public A() {
                    List<String> list = emptyList();
                }
            }
        """
    )

    @Test
    fun doNotAddNamedStaticImportIfNotReferenced(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            { AddImport("java.util.Collections", "emptyList", true) }
        ),
        before = """
            package a;
            
            class A {}
        """
    )

    @Test
    fun addStaticWildcardImportWhenReferenced(jp: JavaParser) = assertChanged(
        jp,
        recipe = FixEmptyListMethodType().doNext(
            addImports(
                { AddImport("java.util.Collections", "*", true) }
            )
        ),
        before = """
            package a;
            
            import java.util.List;
            
            class A {
                public A() {
                    List<String> list = java.util.Collections.emptyList();
                }
            }
        """,
        after = """
            package a;
            
            import java.util.List;
            
            import static java.util.Collections.*;
            
            class A {
                public A() {
                    List<String> list = emptyList();
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/477")
    @Test
    fun dontAddImportForStaticImportsIndirectlyReferenced(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.classpath("jackson-databind").build(),
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitCompilationUnit(cu: J.CompilationUnit, p: ExecutionContext): J.CompilationUnit {
                    maybeAddImport("com.fasterxml.jackson.databind.ObjectMapper")
                    return super.visitCompilationUnit(cu, p)
                }
            }
        },
        dependsOn = arrayOf(
            """
                import com.fasterxml.jackson.databind.ObjectMapper;
                class Helper {
                    static ObjectMapper OBJECT_MAPPER;
                }
            """
        ),
        before = """
            class Test {
                void test() {
                    Helper.OBJECT_MAPPER.writer();
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/776")
    @Test
    fun addImportAndFoldIntoWildcard(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package foo;
                public class B {
                }
                public class C {
                }
            """
        ),
        recipe = addImports(
            { AddImport("java.util.ArrayList", null, false) }
        ),
        before = """
            import foo.B;
            import foo.C;
            import java.util.Collections;
            import java.util.List;
            import java.util.HashSet;
            import java.util.HashMap;
            
            class A {
                B b = new B();
                C c = new C();
                Map<String, String> map = new HashMap<>();
                Set<String> set = new HashSet<>();
                List<String> test = Collections.singletonList("test");
                List<String> test2 = new ArrayList<>();
            }
        """,
        after = """
            import foo.B;
            import foo.C;
            
            import java.util.*;

            class A {
                B b = new B();
                C c = new C();
                Map<String, String> map = new HashMap<>();
                Set<String> set = new HashSet<>();
                List<String> test = Collections.singletonList("test");
                List<String> test2 = new ArrayList<>();
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/780")
    @Test
    fun addImportWhenDuplicatesExist(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports({ AddImport("org.springframework.http.MediaType", null, false) }),
        before = """
            import javax.ws.rs.Path;
            import javax.ws.rs.Path;
            
            class A {}
        """,
        after = """
            import org.springframework.http.MediaType;
            
            import javax.ws.rs.Path;
            import javax.ws.rs.Path;
            
            class A {}
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/867")
    @Test
    fun addImportWithCommentOnClassAndNoImportsOrPackageName(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, """
                        /**
                         * Do suppress those warnings
                         */
                        @SuppressWarnings("other")
                    """.trimIndent()
                ).build()

                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    p: ExecutionContext
                ): J.ClassDeclaration {
                    val cd = super.visitClassDeclaration(classDecl, p)
                    if (cd.leadingAnnotations.size == 0) {
                        maybeAddImport("java.lang.SuppressWarnings")
                        return cd.withTemplate(t, cd.coordinates.addAnnotation { _, _ -> 0 })
                    }
                    return cd
                }
            }
        },
        before = """
            class Test {
            
                class Inner1 {
                }
            }
        """,
        after = """
            import java.lang.SuppressWarnings;
            
            /**
             * Do suppress those warnings
             */
            @SuppressWarnings("other")
            class Test {
            
                /**
                 * Do suppress those warnings
                 */
                @SuppressWarnings("other")
                class Inner1 {
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/880")
    @Test
    fun doNotFoldNormalImportWithNamespaceConflict(jp: JavaParser) {
        val inputs = arrayOf(
            """
            package org.test;
            
            import org.bar.*;
            import org.foo.FooA;
            import org.foo.FooB;
            import org.foo.FooC;
            import org.foo.FooD;
            
            public class Test {
                FooA fooA = new FooA();
                FooB fooB = new FooB();
                FooC fooC = new FooC();
                FooD fooD = new FooD();
                Shared shared = new Shared();

                BarA barA = new BarA();
                BarB barB = new BarB();
                BarC barC = new BarC();
                BarD barD = new BarD();
                BarE barE = new BarE();
            }
            """.trimIndent(),
            """package org.foo; public class Shared {}""".trimIndent(),
            """package org.foo; public class FooA {}""".trimIndent(),
            """package org.foo; public class FooB {}""".trimIndent(),
            """package org.foo; public class FooC {}""".trimIndent(),
            """package org.foo; public class FooD {}""".trimIndent(),
            """package org.foo; public class FooE {}""".trimIndent(),
            """package org.bar; public class Shared {}""".trimIndent(),
            """package org.bar; public class BarA {}""".trimIndent(),
            """package org.bar; public class BarB {}""".trimIndent(),
            """package org.bar; public class BarC {}""".trimIndent(),
            """package org.bar; public class BarD {}""".trimIndent(),
            """package org.bar; public class BarE {}""".trimIndent()
        )

        val sourceFiles = parser.parse(executionContext, *inputs)

        val classNames = arrayOf(
            "org.foo.Shared", "org.foo.FooA", "org.foo.FooB", "org.foo.FooC", "org.foo.FooD", "org.foo.FooE",
            "org.bar.Shared", "org.bar.BarA", "org.bar.BarB", "org.bar.BarC", "org.bar.BarD", "org.bar.BarE")

        val fqns: MutableSet<JavaType.FullyQualified> = mutableSetOf()
        classNames.forEach { fqns.add(JavaType.Class.build(it)) }
        val sourceSet = JavaSourceSet(randomId(),"main", fqns)
        val markedFiles: MutableList<J.CompilationUnit> = mutableListOf()
        sourceFiles.forEach { markedFiles.add(it.withMarkers(it.markers.addIfAbsent(sourceSet))) }

        val recipe: AddImport<ExecutionContext> = AddImport("org.foo.Shared", null, false)
        val result = recipe.visit(markedFiles[0], InMemoryExecutionContext())
        Assertions.assertThat((result as J.CompilationUnit).imports.size == 6).isTrue
        Assertions.assertThat((result).imports[5].qualid.printTrimmed()).isEqualTo("org.foo.Shared")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/880")
    @Test
    fun doNotFoldStaticsWithNamespaceConflict(jp: JavaParser) {
        val classNames = arrayOf("org.fuz.Fuz", "org.buz.Buz")

        val fqns: MutableSet<JavaType.FullyQualified> = mutableSetOf()
        val flags = setOf(Flag.Public, Flag.Static)
        val methodSignature: JavaType.Method.Signature = JavaType.Method.Signature(JavaType.buildType("boolean"), listOf())
        val variables: MutableList<JavaType.Variable> = mutableListOf()

        val methodsFoo: MutableList<JavaType.Method> = mutableListOf()
        val methodNamesFoo = arrayOf("assertShared", "assertA", "assertB", "assertC")
        methodNamesFoo.forEach { methodsFoo.add(
            JavaType.Method.build(flags, JavaType.Class.build("org.fuz.Fuz"), it, null, methodSignature, listOf(), listOf(), listOf())) }
        fqns.add(JavaType.Class.build(
            Flag.flagsToBitMap(flags), classNames[0], JavaType.Class.Kind.Class, variables,
            listOf(), methodsFoo, null, null, listOf(), false))

        val methodsBar: MutableList<JavaType.Method> = mutableListOf()
        val methodNamesBar = arrayOf("assertShared", "assertThatA", "assertThatB", "assertThatC")
        methodNamesBar.forEach { methodsBar.add(
            JavaType.Method.build(flags, JavaType.Class.build("org.buz.Buz"), it, null, methodSignature, listOf(), listOf(), listOf())) }
        fqns.add(JavaType.Class.build(
            Flag.flagsToBitMap(flags), classNames[1], JavaType.Class.Kind.Class, variables,
            listOf(), methodsBar, null, null, listOf(), false))

        val sourceSet = JavaSourceSet(randomId(),"main", fqns)
        val markedFiles: MutableList<J.CompilationUnit> = mutableListOf()

        val inputs = arrayOf(
            """
            package org.fuz;
            public class Fuz {
                public static boolean assertShared() { return true; }
                public static boolean assertA() { return true; }
                public static boolean assertB() { return true; }
                public static boolean assertC() { return true; }
            }
            """.trimIndent()
            ,
            """
            package org.buz;
            public class Buz {
                public static boolean assertShared() { return true; }
                public static boolean assertThatA() { return true; }
                public static boolean assertThatB() { return true; }
                public static boolean assertThatC() { return true; }
            }
            """.trimIndent(),
            """
            package org.test;
            
            import static org.fuz.Fuz.assertA;
            import static org.fuz.Fuz.assertB;
            import static org.fuz.Fuz.assertC;
            import static org.buz.Buz.assertThatA;
            import static org.buz.Buz.assertThatB;
            
            public class Test {
                boolean fooA = assertA();
                boolean fooB = assertB();
                boolean fooC = assertC();
                boolean barA = assertThatA();
                boolean barB = assertThatB();
                boolean barC = org.buz.Buz.assertThatC();
            }
            """.trimIndent()
        )

        // Inputs are processed last so that fqns are setup properly in flyweights.
        val sourceFiles = parser.parse(executionContext, *inputs)
        sourceFiles.forEach { markedFiles.add(it.withMarkers(it.markers.addIfAbsent(sourceSet))) }

        val recipe: AddImport<ExecutionContext> = AddImport("org.buz.Buz", "assertThatC", false)
        val result = recipe.visit(markedFiles[2], InMemoryExecutionContext())
        Assertions.assertThat((result as J.CompilationUnit).imports.size == 6).isTrue
        Assertions.assertThat((result).imports[5].qualid.printTrimmed()).isEqualTo("org.buz.Buz.assertThatC")
    }

    /**
     * This visitor removes the "java.util.Collections" receiver from method invocations of "java.util.Collections.emptyList()".
     * This allows us to test that AddImport with setOnlyIfReferenced = true will add a static import when an applicable static method call is present
     */
    private class FixEmptyListMethodType : Recipe() {
        override fun getDisplayName(): String {
            return "Fix Empty List"
        }

        override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
            return object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitMethodInvocation(
                    method: J.MethodInvocation,
                    ctx: ExecutionContext
                ): J.MethodInvocation {
                    val original: J.MethodInvocation = super.visitMethodInvocation(method, ctx)
                    if (original.name.simpleName == "emptyList") {
                        return original.withSelect(null)
                    }
                    return original
                }
            }
        }
    }
}
