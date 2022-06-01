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
package org.openrewrite.java.search

import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.Tree.randomId
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.Space
import org.openrewrite.marker.Markers

interface SemanticallyEqualTest {

    companion object {
        val jp: JavaParser = JavaParser
            .fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build()

        private const val annotInterface = """
            @interface MyAnnotation { 
                boolean value();
                String srcValue(); 
            }
            @interface NoArgAnnotation1{}
            @interface NoArgAnnotation2{}
        """
    }

    private fun parseSources(@Language("java") sources: Array<String>): List<J.CompilationUnit> {
        jp.reset()
        return jp.parse(InMemoryExecutionContext { t -> fail(t) }, *sources)
    }


    @Issue("https://github.com/openrewrite/rewrite/issues/345")
    @Test
    fun fullyQualifiedReference() {
        val cu = parseSources(arrayOf(
            """
                package org.mco.anno;
                public @interface McAnno {}
            """,
            """
                import org.mco.anno.McAnno;
                @McAnno
                class M {}
                @org.mco.anno.McAnno
                class N {}
            """
        ))[1]
        val c1Anno = cu.classes[0].leadingAnnotations[0]
        val c2Anno = cu.classes[1].leadingAnnotations[0]

        assertThat(SemanticallyEqual.areEqual(c2Anno, c2Anno)).isTrue
        assertThat(SemanticallyEqual.areEqual(c1Anno, c2Anno)).isTrue
    }

    @Test
    fun noArgumentsTest() {
        val cus = parseSources(
            arrayOf(
                """
                @NoArgAnnotation1
                class A {}
            """,
                """
                @NoArgAnnotation1
                class B {}
            """,
                """
                @NoArgAnnotation2
                class C {}
            """,
                annotInterface
            ))

        val firstAnnot = cus[0].classes[0].leadingAnnotations[0]
        val secondAnnot = cus[1].classes[0].leadingAnnotations[0]
        assertThat(SemanticallyEqual.areEqual(firstAnnot, secondAnnot)).isTrue

        val thirdAnnot = cus[2].classes[0].leadingAnnotations[0]
        assertThat(SemanticallyEqual.areEqual(secondAnnot,thirdAnnot)).isFalse
    }

    @Suppress("rawtypes")
    @Test
    fun tagAnnotationEquality() {
        val clazz = parseSources(arrayOf(
            """
                @Tag(FastTests.class)
                @Tag(FastTests.class)
                @Tag(SlowTests.class)
                class A {}
            """,
            """
                @interface Tags {
                    Tag[] value();
                }
            """,
            """
                @java.lang.annotation.Repeatable(Tags.class)
                @interface Tag {
                    Class value();
                }
            """,
            "public interface FastTests {}",
            "public interface SlowTests {}"
        ))[0].classes[0]

        val fastTest = clazz.leadingAnnotations[0]
        val fastTest2 = clazz.leadingAnnotations[1]
        assertThat(SemanticallyEqual.areEqual(fastTest, fastTest2)).isTrue

        val slowTest = clazz.leadingAnnotations[2]
        assertThat(SemanticallyEqual.areEqual(fastTest, slowTest)).isFalse
    }

    @Test
    fun annotationEquality() {
        val cus = parseSources(arrayOf(
            """
                @MyAnnotation(value = true, srcValue = "true")
                class A {}
            """,
            """
                @MyAnnotation(value = true, srcValue = "true")
                class B {}
            """,
            annotInterface
        ))

        val firstAnnot = cus[0].classes[0].leadingAnnotations[0]
        val secondAnnot = cus[1].classes[0].leadingAnnotations[0]
        assertThat(SemanticallyEqual.areEqual(firstAnnot, secondAnnot)).isTrue
    }

    @Test
    fun identEquality() {
        val cus = parseSources(arrayOf(
            """
                @MyAnnotation(value = true)
                class A {}
            """,
            """
                @MyAnnotation(value = true)
                class B {}
            """,
            "@interface MyAnnotation { boolean value(); }"
        ))

        val firstIdent = cus[0].classes[0].leadingAnnotations[0].annotationType
        val secondIdent = cus[1].classes[0].leadingAnnotations[0].annotationType
        assertThat(SemanticallyEqual.areEqual(firstIdent, secondIdent)).isTrue

        val thirdIdent = J.Identifier(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            "YourAnnotation",
            JavaType.buildType("YourAnnotation"), null
        )
        assertThat(SemanticallyEqual.areEqual(firstIdent, thirdIdent)).isFalse
    }

    @Suppress("rawtypes")
    @Test
    fun fieldAccessEquality() {
        val cus = parseSources(arrayOf(
            """
                @Category(FastTest.class)
                @Category(SlowTest.class)
                class A {
                }
            """,
            """
                @Category(FastTest.class)
                @Category(SlowTest.class)
                class B {
                }
            """,
            """
                @interface Categories {
                    Category[] value();
                }
            """,
            """
                @java.lang.annotation.Repeatable(Categories.class)
                @interface Category {
                    Class value();
                }
            """,
            "class FastTest {}",
            "class SlowTest {}"
        ))

        val firstFieldAccess = cus[0].classes[0].leadingAnnotations[0].arguments!!.first()
        val secondFieldAccess = cus[1].classes[0].leadingAnnotations[0].arguments!!.first()
        assertThat(SemanticallyEqual.areEqual(firstFieldAccess, secondFieldAccess)).isTrue

        val thirdFieldAccess = cus[0].classes[0].leadingAnnotations[1].arguments!!.first()
        assertThat(SemanticallyEqual.areEqual(firstFieldAccess, thirdFieldAccess)).isFalse
    }

    @Test
    fun assignEquality() {
        val cus = parseSources(arrayOf(
            """
                @MyAnnotation(value = true)
                class A {}
            """,
            """
                @MyAnnotation(value = true)
                class B {}
            """,
            "@interface MyAnnotation { boolean value(); }"
        ))

        val firstAssign = cus[0].classes[0].leadingAnnotations[0].arguments!!.first() as J.Assignment
        val secondAssign = cus[1].classes[0].leadingAnnotations[0].arguments!!.first() as J.Assignment
        assertThat(SemanticallyEqual.areEqual(firstAssign, secondAssign)).isTrue

        val thirdAssign = secondAssign.withVariable(
            (secondAssign.variable as J.Identifier).withSimpleName("otherValue")
        )
        assertThat(SemanticallyEqual.areEqual(firstAssign, thirdAssign)).isFalse

        val fourthAssign = secondAssign.withAssignment(
            (secondAssign.assignment as J.Literal).withValue(false)
        )
        assertThat(SemanticallyEqual.areEqual(firstAssign, fourthAssign)).isFalse
    }

    @Suppress("CStyleArrayDeclaration")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun arrayAccess() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original(String s) {
                        int n[] = new int[] {0};
                        int m = n[0];
                    }
                    void isEqual(String s) {
                        int n[] = new int[] {0};
                        int m = n[0];
                    }
                    void notEqual(String s) {
                        int o[] = new int[] {0};
                        int m = o[0];
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration).body!!.statements[1] as J.VariableDeclarations).variables[0].initializer!!
        val isEqual = ((body.statements[1] as J.MethodDeclaration).body!!.statements[1] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration).body!!.statements[1] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun arrayType() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    String[] original() {
                        String[] s;
                    }
                    String[] isEqual() {
                        return null;
                    }
                    Integer[] notEqual() {
                        return null;
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration).returnTypeExpression)!!
        val isEqual = ((body.statements[1] as J.MethodDeclaration).returnTypeExpression)!!
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration).returnTypeExpression)!!
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun assert() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original(String s) {
                        assert s != null;
                    }
                    void isEqual(String s) {
                        assert s != null;
                    }
                    void notEqual(String s) {
                        assert s == null;
                    }
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.MethodDeclaration).body!!.statements[0] as J.Assert
        val isEqual = (body.statements[1] as J.MethodDeclaration).body!!.statements[0] as J.Assert
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[2] as J.MethodDeclaration).body!!.statements[0] as J.Assert
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("UnusedAssignment")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun assignment() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original() {
                        String s;
                        s = "foo";
                    }
                    void isEqual() {
                        String s;
                        s = "foo";
                    }
                    void notEqual() {
                        String s;
                        s = "bar";
                    }
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.MethodDeclaration).body!!.statements[1] as J.Assignment
        val isEqual = (body.statements[1] as J.MethodDeclaration).body!!.statements[1] as J.Assignment
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[1] as J.MethodDeclaration).body!!.statements[1] as J.Assignment
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isTrue
    }

    @Suppress("PointlessArithmeticExpression")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun binary() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original() {
                        int n = 0 + 1;
                    }
                    void isEqual() {
                        int n = 0 + 1;
                    }
                    void notEqual() {
                        int n = 1 + 1;
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration).body!!.statements[0] as J.VariableDeclarations).variables[0].initializer as J.Binary
        val isEqual = ((body.statements[1] as J.MethodDeclaration).body!!.statements[0] as J.VariableDeclarations).variables[0].initializer as J.Binary
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration).body!!.statements[0] as J.VariableDeclarations).variables[0].initializer as J.Binary
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("PointlessArithmeticExpression")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun block() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original() {
                        int n = 0 + 1;
                    }
                    void isEqual() {
                        int n = 0 + 1;
                    }
                    void notEqual() {
                        int n = 1 + 1;
                    }
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.MethodDeclaration).body!!
        val isEqual = (body.statements[1] as J.MethodDeclaration).body!!
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[2] as J.MethodDeclaration).body!!
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("LoopStatementThatDoesntLoop", "LoopStatementThatDoesntLoop", "UnnecessaryLabelOnBreakStatement")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun breakNoLabel() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original() {
                        while (true) {
                            break;
                        }
                    }
                    void isEqual() {
                        while (true) {
                            break;
                        }
                    }
                    void notEqual() {
                        labeled:
                        while(true) {
                            break labeled;
                        }
                    }
                }
            """
        ))[0].classes[0].body

        val original = (((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.WhileLoop)
            .body as J.Block)
            .statements[0]
        val isEqual = (((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.WhileLoop)
            .body as J.Block)
            .statements[0]
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.Label)
            .statement as J.WhileLoop)
            .body as J.Block)
            .statements[0]
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("LoopStatementThatDoesntLoop", "UnnecessaryLabelOnBreakStatement")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun breakWithLabel() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original() {
                        labeled:
                        while(true) {
                            break labeled;
                        }
                    }
                    void isEqual() {
                        labeled:
                        while(true) {
                            break labeled;
                        }
                    }
                    void notEqual() {
                        while (true) {
                            break;
                        }
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.Label)
            .statement as J.WhileLoop)
            .body as J.Block)
            .statements[0]
        val isEqual = ((((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.Label)
            .statement as J.WhileLoop)
            .body as J.Block)
            .statements[0]
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.WhileLoop)
            .body as J.Block)
            .statements[0]
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("SwitchStatementWithTooFewBranches")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun caseStatement() {
        val body = parseSources(arrayOf(
            """
                class A {
                    void original(String s) {
                        switch (s) {
                            case "a": {
                                String statement = "isEqual";
                                break;
                            }
                            default:
                                break;
                        }
                    }
                    void isEqual(String s) {
                        switch (s) {
                            case "a": {
                                String
                                    statement =
                                    "isEqual";
                                break;
                            }
                            default:
                                break;
                        }
                    }
                    void notEqual(String s) {
                        switch (s) {
                            case "a": {
                                String
                                    statement =
                                    "notEqual";
                                break;
                            }
                            default:
                                break;
                        }
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration).body!!.statements[0] as J.Switch).cases.statements[0]
        val isEqual = ((body.statements[1] as J.MethodDeclaration).body!!.statements[0] as J.Switch).cases.statements[0]
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[1] as J.MethodDeclaration).body!!.statements[0] as J.Switch).cases.statements[0]
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isTrue
    }

    @Suppress("EmptyTryBlock", "CatchMayIgnoreException")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun catch() {
        val body = parseSources(arrayOf(
            """
                class A {
                    void original(String s) {
                        try {
                        } catch (Exception e) {
                            if (e instanceof IllegalStateException) {
                                e.printStackTrace();
                            }
                        }
                    }
                    void isEqual(String s) {
                        try {
                        } catch (Exception e) {
                            if (e instanceof IllegalStateException) {
                                e.printStackTrace();
                            }
                        }
                    }
                    void notEqual(String s) {
                        try {
                        } catch (Exception e) {
                            if (e instanceof IllegalArgumentException) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration).body!!.statements[0] as J.Try).catches[0]
        val isEqual = ((body.statements[1] as J.MethodDeclaration).body!!.statements[0] as J.Try).catches[0]
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration).body!!.statements[0] as J.Try).catches[0]
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun classDeclaration() {
        val cus0 = parseSources(arrayOf(
            "class A {}",
            "class B {}"
        ))
        val cus1 = parseSources(arrayOf(
            "class A {}"
        ))
        val original = cus0[0].classes[0]
        val isEqual = cus1[0].classes[0]
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = cus0[1].classes[0]
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun compilationUnit() {
        val originalCu = parseSources(arrayOf(
            """
                package foo;
                import java.util.ArrayList;
                import java.util.List;
                
                class Test {
                    List<String> s = new ArrayList<>();
                }
            """
        ))[0]
        val isEqualCu = parseSources(arrayOf(
            """
                package foo;
                import java.util.ArrayList;
                import java.util.List;
                
                class Test {
                    List<String> s = new ArrayList<>();
                }
            """
        ))[0]
        val notEqualCu = parseSources(arrayOf(
            """
                package foo;
                import java.util.ArrayList;
                import java.util.List;
                
                class NotEqual {
                    List<String> s = new ArrayList<>();
                }
            """
        ))[0]
        assertThat(SemanticallyEqual.areEqual(originalCu, isEqualCu)).isTrue
        assertThat(SemanticallyEqual.areEqual(originalCu, notEqualCu)).isFalse
    }

    @Suppress("StatementWithEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun controlParentheses() {
        val body = parseSources(arrayOf(
            """
                class A {
                    void original(String s) {
                        if (s.isEmpty()) {
                        }
                    }
                    void isEqual(String s) {
                        if (s.isEmpty()) {
                        }
                    }
                    void notEqual(String s) {
                        if (s == null) {
                        }
                    }
                }
            """
        ))[0].classes[0].body
        val original = ((body.statements[0] as J.MethodDeclaration).body!!.statements[0] as J.If).ifCondition
        val isEqual = ((body.statements[1] as J.MethodDeclaration).body!!.statements[0] as J.If).ifCondition
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration).body!!.statements[0] as J.If).ifCondition
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("UnnecessaryContinue", "UnnecessaryLabelOnContinueStatement")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun continueStatementNoLabel() {
        val body = parseSources(arrayOf(
            """
                import java.util.ArrayList;
                import java.util.List;
                
                class A {
                    void original(List<String> strings) {
                        for (String s : strings) {
                            continue;
                        }
                    }
                    void isEqual(List<String> strings) {
                        for (String s : strings) {
                            continue;
                        }
                    }
                    void notEqual(List<String> strings) {
                        outer:
                        for (String s : strings) {
                            continue outer;
                        }
                    }
                }
            """
        ))[0].classes[0].body
        val original = (((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.ForEachLoop).body as J.Block)
            .statements[0]
        val isEqual = (((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.ForEachLoop).body as J.Block)
            .statements[0]
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.Label)
            .statement as J.ForEachLoop).body as J.Block)
            .statements[0]
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun continueStatementWithLabel() {
        val body = parseSources(arrayOf(
            """
                import java.util.List;
                class A {
                    void original(List<String> strings, List<String> contains) {
                        equal:
                        for (String s : strings) {
                            for (String s2 : contains) {
                                if (s.equals("")) {
                                    continue equal;
                                }
                            }
                        }
                    }
                    void isEqual(List<String> strings, List<String> contains) {
                        equal:
                        for (String s : strings) {
                            for (String s2 : contains) {
                                if (s.equals("")) {
                                    continue equal;
                                }
                            }
                        }
                    }
                    void notEqual(List<String> strings, List<String> contains) {
                        other:
                        for (String s : strings) {
                            for (String s2 : contains) {
                                if (s.equals("")) {
                                    continue other;
                                }
                            }
                        }
                    }
                }
            """
        ))[0].classes[0].body
        val original = ((((((((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.Label)
            .statement as J.ForEachLoop).body as J.Block)
            .statements[0] as J.ForEachLoop)
            .body as J.Block).statements[0] as J.If)
            .thenPart as J.Block)
            .statements[0]
        val isEqual = ((((((((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.Label)
            .statement as J.ForEachLoop).body as J.Block)
            .statements[0] as J.ForEachLoop)
            .body as J.Block).statements[0] as J.If)
            .thenPart as J.Block)
            .statements[0]
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((((((((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.Label)
            .statement as J.ForEachLoop).body as J.Block)
            .statements[0] as J.ForEachLoop)
            .body as J.Block).statements[0] as J.If)
            .thenPart as J.Block)
            .statements[0]
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun doWhile() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original(int i) {
                        do {
                            i++;
                        } while (i < 10);
                    }
                    void isEqual(int i) {
                        do {
                            i++;
                        } while (i < 10);
                    }
                    void notEqual(int i) {
                        do {
                            i++;
                        } while (i < 5);
                    }
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.MethodDeclaration).body!!.statements[0] as J.DoWhileLoop
        val isEqual = (body.statements[1] as J.MethodDeclaration).body!!.statements[0] as J.DoWhileLoop
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[2] as J.MethodDeclaration).body!!.statements[0] as J.DoWhileLoop
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("UnusedAssignment", "StatementWithEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun elseStatement() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original(int i) {
                        if (i == 0) {
                        } else {
                            i++;
                        }
                    }
                    void isEqual(int i) {
                        if (i == 0) {
                        } else {
                            i++;
                        }
                    }
                    void notEqual(int i) {
                        if (i == 0) {
                        } else {
                            i += 1;
                        }
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration).body!!.statements[0] as J.If).elsePart!!
        val isEqual = ((body.statements[1] as J.MethodDeclaration).body!!.statements[0] as J.If).elsePart!!
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration).body!!.statements[0] as J.If).elsePart!!
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("UnusedAssignment", "UnnecessarySemicolon")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun empty() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original() {
                        ;
                    }
                    void isEqual() {
                        ;
                    }
                    void notEqual(int i) {
                        i++;
                    }
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.MethodDeclaration).body!!.statements[0] as J.Empty
        val isEqual = (body.statements[1] as J.MethodDeclaration).body!!.statements[0] as J.Empty
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[2] as J.MethodDeclaration).body!!.statements[0] as J.Unary
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun enumValue() {
        val cus = parseSources(arrayOf(
            "public enum A { ONE }",
            "public enum B { ONE }"
        ))
        val cus1 = parseSources(arrayOf(
            "public enum A { ONE }"
        ))

        val original = (cus[0].classes[0].body.statements[0] as J.EnumValueSet).enums[0] as J.EnumValue
        val isEqual = (cus1[0].classes[0].body.statements[0] as J.EnumValueSet).enums[0] as J.EnumValue
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (cus[1].classes[0].body.statements[0] as J.EnumValueSet).enums[0] as J.EnumValue
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun enumValueSet() {
        val cus = parseSources(arrayOf(
            "public enum A { ONE, TWO }",
            "public enum B { ONE, TWO }"
        ))
        val cus1 = parseSources(arrayOf(
            "public enum A { ONE, TWO }"
        ))

        val original = cus[0].classes[0].body.statements[0]
        val isEqual = cus1[0].classes[0].body.statements[0]
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = cus[1].classes[0]
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun fieldAccess() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original() {
                        int val = Inner.field0;
                    }
                    void isEqual() {
                        int val = Inner.field0;
                    }
                    void notEqual() {
                        int val = Inner.field1;
                    }
                    class Inner {
                        public static final int field0 = 0;
                        public static final int field1 = 0;
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("StatementWithEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun forEachLoop() {
        val body = parseSources(arrayOf(
            """
                import java.util.List;
                class Test {
                    void original(List<String> strings) {
                        for (String s : strings) {
                        }
                    }
                    void isEqual(List<String> strings) {
                        for (String s : strings) {
                        }
                    }
                    void notEqual(List<String> strings) {
                        for (String a : strings) {
                        }
                    }
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.ForEachLoop
        val isEqual = (body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.ForEachLoop
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.ForEachLoop
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("StatementWithEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun forEachControl() {
        val body = parseSources(arrayOf(
            """
                import java.util.List;
                class Test {
                    void original(List<String> strings) {
                        for (String s : strings) {
                        }
                    }
                    void isEqual(List<String> strings) {
                        for (String s : strings) {
                        }
                    }
                    void notEqual(List<String> strings) {
                        for (String a : strings) {
                        }
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.ForEachLoop).control
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.ForEachLoop).control
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.ForEachLoop).control
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("ForLoopReplaceableByWhile", "StatementWithEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun forLoop() {
        val body = parseSources(arrayOf(
            """
                import java.util.List;
                class Test {
                    void original(List<String> strings) {
                        for (int i = 0; i < strings.size(); i++) {
                        }
                    }
                    void isEqual(List<String> strings) {
                        for (int i = 0; i < strings.size(); i++) {
                        }
                    }
                    void notEqual(List<String> strings) {
                        for (int j = 0; j < strings.size(); j++) {
                        }
                    }
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.ForLoop
        val isEqual = (body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.ForLoop
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.ForLoop
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("StatementWithEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun forLoopControl() {
        val body = parseSources(arrayOf(
            """
                import java.util.List;
                class Test {
                    void original(List<String> strings) {
                        for (int i = 0; i < strings.size(); i++) {
                        }
                    }
                    void isEqual(List<String> strings) {
                        for (int i = 0; i < strings.size(); i++) {
                        }
                    }
                    void notEqual(List<String> strings) {
                        for (int j = 0; j < strings.size(); j++) {
                        }
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.ForLoop).control
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.ForLoop).control
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.ForLoop).control
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun identifier() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original() {
                        int val;
                    }
                    void isEqual() {
                        int val;
                    }
                    void notEqual() {
                        int value;
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].name
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].name
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].name
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("ConstantConditions", "StatementWithEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun ifStatement() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original() {
                        if (true) {}
                    }
                    void isEqual() {
                        if (true) {}
                    }
                    void notEqual() {
                        if (false) {}
                    }
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.If
        val isEqual = (body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.If
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.If
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("StatementWithEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun instanceOf() {
        val body = parseSources(arrayOf(
            """
                import java.util.Collection;
                import java.util.List;
                import java.util.Set;
                
                class Test {
                    void original(Collection<String> strings) {
                        if (strings instanceof List) {}
                    }
                    void isEqual(Collection<String> strings) {
                        if (strings instanceof List) {}
                    }
                    void notEqual(Collection<String> strings) {
                        if (strings instanceof Set) {}
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.If).ifCondition.tree
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.If).ifCondition.tree
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.If).ifCondition.tree
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun import() {
        val cus = parseSources(arrayOf(
            """
                import java.util.List;
                import java.util.ArrayList;
            """,
            "import java.util.List;"
        ))

        val original = cus[0].imports[0]
        val isEqual = cus[1].imports[0]
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = cus[0].imports[1]
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("UnnecessaryLabelOnContinueStatement", "UnnecessaryContinue")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun label() {
        val body = parseSources(arrayOf(
            """
                import java.util.Collection;
                
                class Test {
                    void original(Collection<String> strings) {
                        label:
                        for (String s : strings) {
                            continue label;
                        }
                    }
                    void isEqual(Collection<String> strings) {
                        label:
                        for (String s : strings) {
                            continue label;
                        }
                    }
                    void notEqual(Collection<String> strings) {
                        diff:
                        for (String s : strings) {
                            continue diff;
                        }
                    }
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.Label
        val isEqual = (body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.Label
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.Label
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun lambda() {
        val body = parseSources(arrayOf(
            """
                import java.util.function.Function;
                
                class Test {
                    void original() {
                        Function<String, String> func = (String s) -> "";
                    }
                    void isEqual() {
                        Function<String, String> func = (String s) -> "";
                    }
                    void notEqual() {
                        Function<String, String> func = (String o) -> "";
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Test
    fun literalEquality() {
        val body = parseSources(arrayOf(
            """
                class A {
                    void original() {
                        int i = 0;
                        String str = "thisString";
                        String str2 = null;
                    }
                    void isEqual() {
                        int i = 0;
                        String str = "thisString";
                        String str2 = null;
                    }
                }
            """
        ))[0].classes[0].body

        val intLiteral0 = (body.statements[0] as J.MethodDeclaration).body!!.statements[0]
        val intLiteral1 = (body.statements[1] as J.MethodDeclaration).body!!.statements[0]
        assertThat(SemanticallyEqual.areEqual(intLiteral0, intLiteral1))

        val strLiteral0 = (body.statements[0] as J.MethodDeclaration).body!!.statements[1]
        val strLiteral1 = (body.statements[1] as J.MethodDeclaration).body!!.statements[1]
        assertThat(SemanticallyEqual.areEqual(strLiteral0, strLiteral1))

        val nullLiteral0 = (body.statements[0] as J.MethodDeclaration).body!!.statements[2]
        val nullLiteral1 = (body.statements[1] as J.MethodDeclaration).body!!.statements[2]
        assertThat(SemanticallyEqual.areEqual(nullLiteral0, nullLiteral1))
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun memberReference() {
        val body = parseSources(arrayOf(
            """
                class A {
                    public static void func0(String s) {}
                    public static void func1(String s) {}
                }
            """,
            """
                import java.util.stream.Stream;

                class Test {
                    void original() {
                        Stream.of("s").forEach(A::func0);
                    }
                    void isEqual() {
                        Stream.of("s").forEach(A::func0);
                    }
                    void notEqual() {
                        Stream.of("s").forEach(A::func1);
                    }
                }
            """
        ))[1].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.MethodInvocation).arguments[0]
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.MethodInvocation).arguments[0]
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.MethodInvocation).arguments[0]
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun methodDeclaration() {
        val body0 = parseSources(arrayOf(
            """
                class Test {
                    void original() {
                        String s = "";
                    }
                    void notEqual() {
                        String s = "notEqual";
                    }
                }
            """
        ))[0].classes[0].body

        val body1 = parseSources(arrayOf(
            """
                class Test {
                    void original() {
                        String s = "";
                    }
                }
            """
        ))[0].classes[0].body

        val original = body0.statements[0] as J.MethodDeclaration
        val isEqual = body1.statements[0] as J.MethodDeclaration
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = body0.statements[1] as J.MethodDeclaration
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun methodInvocation() {
        val body = parseSources(arrayOf(
            """
                class A {
                    public static void func0(String s) {}
                    public static void func1(String s) {}
                }
            """,
            """
                import java.util.stream.Stream;

                class Test {
                    void original() {
                        Stream.of("s").forEach(A::func0);
                    }
                    void isEqual() {
                        Stream.of("s").forEach(A::func0);
                    }
                    void notEqual() {
                        Stream.of("s").forEach(A::func1);
                    }
                }
            """
        ))[1].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.MethodInvocation)
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.MethodInvocation)
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.MethodInvocation)
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("EmptyTryBlock", "CatchMayIgnoreException")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun multiCatch() {
        val body = parseSources(arrayOf(
            """
                import java.io.File;
                import java.io.FileInputStream;
                import java.io.FileNotFoundException;
                
                class A {
                    void original() {
                        File f = new File("file.text");
                        try (FileInputStream fis = new FileInputStream(f)) {}
                        catch (FileNotFoundException | RuntimeException e) {}
                    }
                    void isEqual() {
                        File f = new File("file.text");
                        try (FileInputStream fis = new FileInputStream(f)) {}
                        catch (FileNotFoundException | RuntimeException e) {}
                    }
                    void notEqual() {
                        File f = new File("file.text");
                        try (FileInputStream fis = new FileInputStream(f)) {}
                        catch (FileNotFoundException | NullPointerException e) {}
                    }
                }
            """
        ))[0].classes[0].body

        val original = (((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[1] as J.Try)
            .catches[0].parameter.tree as J.VariableDeclarations)
            .typeExpression!!
        val isEqual = (((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[1] as J.Try)
            .catches[0].parameter.tree as J.VariableDeclarations)
            .typeExpression!!
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[1] as J.Try)
            .catches[0].parameter.tree as J.VariableDeclarations)
            .typeExpression!!
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun newArray() {
        val body = parseSources(arrayOf(
            """
                class A {
                    void original() {
                        int[] n = new int[0];
                    }
                    void isEqual() {
                        int[] n = new int[0];
                    }
                    void notEqual() {
                        long[] n = new long[0];
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun newClass() {
        val body = parseSources(arrayOf(
            """
                import java.util.*;
                
                class A {
                    void original(List<String> s) {
                        List<String> l = new ArrayList<>(s);
                    }
                    void isEqual(Collection<String> s) {
                        List<String> l = new ArrayList<>(s);
                    }
                    void notEqual(List<String> s) {
                        Set<String> l = new HashSet<>(s);
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun packageStatement() {
        val cus = parseSources(arrayOf(
            "package foo;",
            "package foo;",
            "package bar;"
        ))

        val original = cus[0].packageDeclaration as J.Package
        val isEqual = cus[1].packageDeclaration as J.Package
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = cus[2].packageDeclaration as J.Package
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun parameterizedType() {
        val body = parseSources(arrayOf(
            """
                import java.util.ArrayList;
                import java.util.HashSet;
                import java.util.List;
                import java.util.Set;
                
                class A {
                    void original() {
                        List<String> l = new ArrayList<>();
                    }
                    void isEqual() {
                        List<String> l = new ArrayList<>();
                    }
                    void notEqual() {
                        Set<String> l = new HashSet<>();
                    }
                }
            """
        ))[0].classes[0].body

        val original = (((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations)
            .variables[0].initializer!! as J.NewClass).clazz!!
        val isEqual = (((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations)
            .variables[0].initializer!! as J.NewClass).clazz!!
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations)
            .variables[0].initializer!! as J.NewClass).clazz!!
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun parentheses() {
        val body = parseSources(arrayOf(
            """
                class A {
                    void original() {
                        int n = (0);
                    }
                    void isEqual() {
                        int n = (0);
                    }
                    void notEqual() {
                        int n = (1);
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations)
            .variables[0].initializer!!
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations)
            .variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations)
            .variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun primitive() {
        val body = parseSources(arrayOf(
            """
                class A {
                    void original() {
                        int n = 0;
                    }
                    void isEqual() {
                        int n = 0;
                    }
                    void notEqual() {
                        char c = 'c';
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).typeExpression!!
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).typeExpression!!
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).typeExpression!!
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun returnStatement() {
        val body = parseSources(arrayOf(
            """
                class A {
                    int original() {
                        return 0;
                    }
                    int isEqual() {
                        return 0;
                    }
                    int notEqual() {
                        return 1;
                    }
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.Return
        val isEqual = (body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.Return
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.Return
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("DuplicateBranchesInSwitch")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun switchStatement() {
        val body = parseSources(arrayOf(
            """
                class A {
                    void original(String s) {
                        switch (s) {
                            case "a": {
                                String statement = "isEqual";
                                break;
                            }
                            case "b": {
                                String statement = "isEqual";
                                break;
                            }
                            default:
                                break;
                        }
                    }
                    void isEqual(String s) {
                        switch (s) {
                            case "a": {
                                String
                                    statement =
                                    "isEqual";
                                break;
                            }
                            case "b": {
                                String
                                    statement =
                                    "isEqual";
                                break;
                            }
                            default:
                                break;
                        }
                    }
                }
            """
        ))[0].classes[0].body

        val switch0 = (body.statements[0] as J.MethodDeclaration).body!!.statements[0]
        val switch1 = (body.statements[1] as J.MethodDeclaration).body!!.statements[0]
        assertThat(SemanticallyEqual.areEqual(switch0, switch1)).isTrue
    }

    @Suppress("EmptySynchronizedStatement", "SynchronizationOnLocalVariableOrMethodParameter")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun synchronized() {
        val body = parseSources(arrayOf(
            """
                class A {
                    void original() {
                        Integer n = 0;
                        synchronized(n) {
                        }
                    }
                    void isEqual() {
                        Integer n = 0;
                        synchronized(n) {
                        }
                    }
                    void notEqual() {
                        Integer m = 0;
                        synchronized(m) {
                        }
                    }
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.MethodDeclaration)
            .body!!.statements[1] as J.Synchronized
        val isEqual = (body.statements[1] as J.MethodDeclaration)
            .body!!.statements[1] as J.Synchronized
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[2] as J.MethodDeclaration)
            .body!!.statements[1] as J.Synchronized
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun ternary() {
        val body = parseSources(arrayOf(
            """
                class A {
                    void original(int n) {
                        String evenOrOdd = n % 2 == 0 ? "even" : "odd";
                    }
                    void isEqual(int n) {
                        String evenOrOdd = n % 2 == 0 ? "even" : "odd";
                    }
                    void notEqual(int n) {
                        String evenOrOdd = n % 2 == 1 ? "odd" : "even";
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun throws() {
        val body = parseSources(arrayOf(
            """
                class A {
                    void original(IllegalStateException ex) {
                        throw new RuntimeException(ex);
                    }
                    void isEqual(IllegalArgumentException ex) {
                        throw new RuntimeException(ex);
                    }
                    void notEqual(IllegalStateException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.MethodDeclaration).body!!.statements[0] as J.Throw
        val isEqual = (body.statements[1] as J.MethodDeclaration).body!!.statements[0] as J.Throw
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[2] as J.MethodDeclaration).body!!.statements[0] as J.Throw
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("EmptyTryBlock", "CatchMayIgnoreException")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun tryStatement() {
        val body = parseSources(arrayOf(
            """
                import java.io.File;
                import java.io.FileInputStream;
                import java.io.FileNotFoundException;
                
                class A {
                    void original() {
                        File f = new File("file.text");
                        try (FileInputStream fis0 = new FileInputStream(f)) {
                        }
                        catch (Exception e) {}
                    }
                    void isEqual() {
                        File f = new File("file.text");
                        try (FileInputStream fis0 = new FileInputStream(f)) {
                        }
                        catch (Exception e) {}
                    }
                    void notEqual() {
                        File f = new File("file.text");
                        try (FileInputStream fis1 = new FileInputStream(f)) {
                        }
                        catch (Exception e) {}
                    }
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.MethodDeclaration)
            .body!!.statements[1] as J.Try
        val isEqual = (body.statements[1] as J.MethodDeclaration)
            .body!!.statements[1] as J.Try
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[2] as J.MethodDeclaration)
            .body!!.statements[1] as J.Try
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("EmptyTryBlock", "CatchMayIgnoreException")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun tryResource() {
        val body = parseSources(arrayOf(
            """
                import java.io.File;
                import java.io.FileInputStream;
                import java.io.FileNotFoundException;
                
                class A {
                    void original() {
                        File f = new File("file.text");
                        try (FileInputStream fis0 = new FileInputStream(f)) {
                        }
                        catch (Exception e) {}
                    }
                    void isEqual() {
                        File f = new File("file.text");
                        try (FileInputStream fis0 = new FileInputStream(f)) {
                        }
                        catch (Exception e) {}
                    }
                    void notEqual() {
                        File f = new File("file.text");
                        try (FileInputStream fis1 = new FileInputStream(f)) {
                        }
                        catch (Exception e) {}
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[1] as J.Try).resources!![0]
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[1] as J.Try).resources!![0]
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[1] as J.Try).resources!![0]
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("RedundantCast", "unchecked")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun typeCast() {
        val body = parseSources(arrayOf(
            """
                class A {
                    void original() {
                        Object o = (Class<String>) Class.forName("java.lang.String");
                    }
                    void isEqual() {
                        Object o = (Class<String>) Class.forName("java.lang.String");
                    }
                    void notEqual() {
                        Object o = (Class<Integer>) Class.forName("java.lang.Integer");
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun typeParameter() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    class A<E extends Number> {}
                    class B<E extends Number> {}
                    class C<T> {}
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.ClassDeclaration).typeParameters!![0]
        val isEqual = (body.statements[1] as J.ClassDeclaration).typeParameters!![0]
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[2] as J.ClassDeclaration).typeParameters!![0]
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Suppress("PointlessBooleanExpression", "ConstantConditions")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun unary() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original() {
                        boolean b = !(1 == 2);
                    }
                    void isEqual() {
                        boolean b = !(1 == 2);
                    }
                    void notEqual() {
                        boolean b = !(2 == 2);
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        val isEqual = ((body.statements[1] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations).variables[0].initializer!!
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }


    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun variableDeclaration() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original() {
                        Integer i = 0;
                    }
                    void isEqual() {
                        Integer i = 0;
                    }
                    void notEqual() {
                        Integer k = 0;
                    }
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.MethodDeclaration).body!!.statements[0]
        val isEqual = (body.statements[1] as J.MethodDeclaration).body!!.statements[0]
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[2] as J.MethodDeclaration).body!!.statements[0]
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun namedVariable() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original() {
                        Integer i = 0;
                    }
                    void isEqual() {
                        Integer i = 0;
                    }
                    void notEqual() {
                        Integer k = 0;
                    }
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.MethodDeclaration).body!!.statements[0] as J.VariableDeclarations).variables[0]
        val isEqual = ((body.statements[1] as J.MethodDeclaration).body!!.statements[0] as J.VariableDeclarations).variables[0]
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.MethodDeclaration).body!!.statements[0] as J.VariableDeclarations).variables[0]
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }
    @Suppress("InfiniteLoopStatement", "StatementWithEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun whileLoop() {
        val body = parseSources(arrayOf(
            """
                class Test {
                    void original() {
                        while (true) {}
                    }
                    void isEqual() {
                        while (true) {}
                    }
                    void notEqual() {
                        while (false) {}
                    }
                }
            """
        ))[0].classes[0].body

        val original = (body.statements[0] as J.MethodDeclaration).body!!.statements[0] as J.WhileLoop
        val isEqual = (body.statements[1] as J.MethodDeclaration).body!!.statements[0] as J.WhileLoop
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = (body.statements[2] as J.MethodDeclaration).body!!.statements[0] as J.WhileLoop
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isFalse
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1856")
    @Test
    fun wildCard() {
        val body = parseSources(arrayOf(
            """
                import java.util.List;
                
                class Test {
                    List<? extends B> original;
                    List<? extends B> isEqual;
                    List<? extends C> notEqual;
                    interface B {}
                    interface C {}
                }
            """
        ))[0].classes[0].body

        val original = ((body.statements[0] as J.VariableDeclarations).typeExpression!! as J.ParameterizedType).typeParameters!![0]
        val isEqual = ((body.statements[1] as J.VariableDeclarations).typeExpression!! as J.ParameterizedType).typeParameters!![0]
        assertThat(SemanticallyEqual.areEqual(original, isEqual)).isTrue

        val notEqual = ((body.statements[2] as J.VariableDeclarations).typeExpression!! as J.ParameterizedType).typeParameters!![0]
        assertThat(SemanticallyEqual.areEqual(original, notEqual)).isTrue
    }
}
