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
import org.openrewrite.*
import org.openrewrite.java.style.ImportLayoutStyle
import org.openrewrite.java.tree.J
import org.openrewrite.style.NamedStyles

interface AddImportTest : JavaRecipeTest {

    fun addImports(vararg adds: () -> TreeVisitor<*, ExecutionContext>): Recipe = adds
        .map { add -> toRecipe(add) }
        .reduce { r1, r2 -> return r1.doNext(r2) }

    @Issue("https://github.com/openrewrite/rewrite/issues/2155")
    @Test
    fun addImportBeforeImportWithSameInsertIndex(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            { AddImport("org.junit.jupiter.api.Assertions", "assertFalse", false) }
        ),
        before = """
            import static org.junit.jupiter.api.Assertions.assertTrue;

            import org.junit.Test;

            public class MyTest {
            }
        """,
        after = """
            import static org.junit.jupiter.api.Assertions.assertFalse;
            import static org.junit.jupiter.api.Assertions.assertTrue;

            import org.junit.Test;

            public class MyTest {
            }
        """
    )
    @Test
    fun importIsAddedToCorrectBlock(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            { AddImport("org.mockito.junit.jupiter.MockitoExtension", null, false) }
        ),
        before = """
            import java.util.List;

            import org.junit.jupiter.api.extension.ExtendWith;
            import org.mockito.Mock;

            public class MyTest {
            }
        """,
        after = """
            import java.util.List;

            import org.junit.jupiter.api.extension.ExtendWith;
            import org.mockito.Mock;
            import org.mockito.junit.jupiter.MockitoExtension;

            public class MyTest {
            }
        """
    )

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
    fun dontDuplicateImports3(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.classpath("junit-jupiter-api").build(),
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

    @Test
    fun dontImportJavaLang(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports({ AddImport("java.lang.String", null, false) }),
        before = """
            package com.myorg;

            class A {
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1156")
    fun dontImportJavaLangWhenUsingDefaultPackage(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports({ AddImport("java.lang.String", null, false) }),
        before = """
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
    fun addStaticImportForUnreferencedField(jp: JavaParser) = assertChanged(
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1030")
    @Test
    fun addStaticImportForReferencedField(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : Recipe() {
            override fun getDisplayName() = "test"
            override fun getDescription() = "Test recipe."
            override fun getVisitor() = object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    ctx: ExecutionContext
                ): J.ClassDeclaration {
                    var cd = classDecl
                    if (cd.body.statements.isNotEmpty()) {
                        return cd
                    }
                    cd = cd.withTemplate(
                        JavaTemplate.builder(this::getCursor, "ChronoUnit unit = MILLIS;")
                            .imports("java.time.temporal.ChronoUnit")
                            .staticImports("java.time.temporal.ChronoUnit.MILLIS")
                            .build(),
                        cd.body.coordinates.lastStatement()
                    )
                    maybeAddImport("java.time.temporal.ChronoUnit")
                    maybeAddImport("java.time.temporal.ChronoUnit", "MILLIS")

                    return cd
                }
            }
        },
        before = """
            public class A {
            
            }
        """,
        after = """
            import java.time.temporal.ChronoUnit;
            
            import static java.time.temporal.ChronoUnit.MILLIS;
            
            public class A {
                ChronoUnit unit = MILLIS;
            
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1030")
    @Test
    fun dontAddImportToStaticFieldWithNamespaceConflict(jp: JavaParser) = assertUnchanged(
        recipe = addImports({ AddImport("java.time.temporal.ChronoUnit", "MILLIS", true) }),
        before = """
            package a;
            
            import java.time.temporal.ChronoUnit;
            
            class A {
                static final int MILLIS = 1;
                ChronoUnit unit = ChronoUnit.MILLIS;
            }
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
            override fun getDisplayName() = "test"
            override fun getDescription() = "Test recipe."

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
            import java.util.Map;
            import java.util.Set;
            
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

    @Issue("https://github.com/openrewrite/rewrite/issues/933")
    @Test
    fun unorderedImportsWithNewBlock(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports({ AddImport("java.time.Duration", null, false) }),
        before = """
            import org.foo.B;
            import org.foo.A;
            
            class A {}
        """,
        after = """
            import org.foo.B;
            import org.foo.A;
            
            import java.time.Duration;
            
            class A {}
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/880")
    @Test
    fun doNotFoldNormalImportWithNamespaceConflict(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports({ AddImport("java.util.List", null, false) }),
        executionContext = executionContext.apply {putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, false)},
        before = """
            import java.awt.*; // contains a List class
            import java.util.Collection;
            import java.util.Collections;
            import java.util.Map;
            import java.util.Set;
            
            @SuppressWarnings("ALL")
            class Test {
                List list;
            }
        """,
        after = """
            import java.awt.*; // contains a List class
            import java.util.Collection;
            import java.util.Collections;
            import java.util.List;
            import java.util.Map;
            import java.util.Set;
            
            @SuppressWarnings("ALL")
            class Test {
                List list;
            }
        """
    )

    @Test
    fun foldPackageWithEmptyImports() = assertChanged(
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
        recipe = addImports({ AddImport("java.util.List", null, false) }),
        before = """
        """,
        after = """
            import java.util.*;
        """
    )

    @Test
    fun foldPackageWithExistingImports() = assertChanged(
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
        recipe = addImports({ AddImport("java.util.Map", null, false) }),
        before = """
            import java.util.List;
        """,
        after = """
            import java.util.*;
        """
    )

    @Test
    fun foldSubPackageWithExistingImports() = assertChanged(
        JavaParser.fromJavaVersion().styles(
            listOf(
                NamedStyles(
                    Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                        ImportLayoutStyle.builder()
                            .packageToFold("java.util.*", true)
                            .importAllOthers()
                            .importStaticAllOthers()
                            .build()
                    )
                )
            )
        ).build(),
        recipe = addImports({ AddImport("java.util.concurrent.ConcurrentHashMap", null, false) }),
        before = """
            import java.util.List;
        """,
        after = """
            import java.util.List;
            import java.util.concurrent.*;
        """
    )

    @Test
    fun foldStaticSubPackageWithEmptyImports() = assertChanged(
        JavaParser.fromJavaVersion().styles(
            listOf(
                NamedStyles(
                    Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                        ImportLayoutStyle.builder()
                            .staticPackageToFold("java.util.*", true)
                            .importAllOthers()
                            .importStaticAllOthers()
                            .build()
                    )
                )
            )
        ).build(),
        recipe = addImports({ AddImport("java.util.Collections", "emptyMap", false) }),
        before = """
        """,
        after = """
            import static java.util.Collections.*;
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1687")
    @Test
    fun noImportLayout() = assertChanged(
        JavaParser.fromJavaVersion().styles(
            listOf(
                NamedStyles(
                    Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                        ImportLayoutStyle(999, 999, emptyList(), emptyList())
                    )
                )
            )
        ).build(),
        recipe = addImports({ AddImport("java.util.List", null, false) }),
        before = """
        """,
        after = """
            import java.util.List;
        """
    )

    @Test
    fun foldStaticSubPackageWithExistingImports() = assertChanged(
        JavaParser.fromJavaVersion().styles(
            listOf(
                NamedStyles(
                    Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                        ImportLayoutStyle.builder()
                            .staticPackageToFold("java.util.*", true)
                            .importAllOthers()
                            .importStaticAllOthers()
                            .build()
                    )
                )
            )
        ).build(),
        recipe = addImports({ AddImport("java.util.Collections", "emptyMap", false) }),
        before = """
            import java.util.List;
        """,
        after = """
            import java.util.List;
            
            import static java.util.Collections.*;
        """
    )

    /**
     * This visitor removes the "java.util.Collections" receiver from method invocations of "java.util.Collections.emptyList()".
     * This allows us to test that AddImport with setOnlyIfReferenced = true will add a static import when an applicable static method call is present
     */
    private class FixEmptyListMethodType : Recipe() {
        override fun getDisplayName(): String {
            return "Fix Empty List"
        }

        override fun getDescription(): String {
            return "AddImportTest testing recipe."
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
