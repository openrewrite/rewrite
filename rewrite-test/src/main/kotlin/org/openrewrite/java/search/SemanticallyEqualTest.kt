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
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Parser
import org.openrewrite.Tree.randomId
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.*
import org.openrewrite.marker.Markers
import java.util.*

interface SemanticallyEqualTest {

    companion object {
        private const val annotInterface = """
            @interface MyAnnotation { 
                boolean value();
                String srcValue(); 
            }
            @interface NoArgAnnotation1{}
            @interface NoArgAnnotation2{}
        """
    }

    @Issue("#345")
    @Test
    fun fullyQualifiedReference(jp: JavaParser) {
        val mcAnnoClass =
            """
                package org.mco.anno;
                public @interface McAnno {}
            """
        val j = JavaParser.fromJavaVersion().dependsOn(Collections.singletonList(Parser.Input.fromString(mcAnnoClass))).build()
        val cu1 = j.parse(
            """
                import org.mco.anno.McAnno;
                @McAnno
                class M {}
                @org.mco.anno.McAnno
                class N {}
            """)
        val c1Anno = cu1[0].classes[0].leadingAnnotations[0]
        val c2Anno = cu1[0].classes[1].leadingAnnotations[0]

        assertThat(SemanticallyEqual.areEqual(c2Anno, c2Anno)).isTrue
        assertThat(SemanticallyEqual.areEqual(c1Anno, c2Anno)).isTrue
    }

    @Test
    fun noArgumentsTest(jp: JavaParser) {
        val cu = jp.parse(
            """
                @NoArgAnnotation1
                class A {}
            """,
            annotInterface
        )

        val firstAnnot = cu[0].classes[0].leadingAnnotations[0]

        jp.reset()

        val secondAnnot = jp.parse(
            """
                @NoArgAnnotation2
                class B {}
            """,
            annotInterface
        )[0].classes[0].leadingAnnotations[0]

        jp.reset()

        val thirdAnnot = jp.parse(
            """
                @NoArgAnnotation2
                class B {}
            """,
            annotInterface
        )[0].classes[0].leadingAnnotations[0]

        assertThat(SemanticallyEqual.areEqual(firstAnnot, secondAnnot)).isFalse
        assertThat(SemanticallyEqual.areEqual(secondAnnot,thirdAnnot)).isTrue
    }

    @Test
    fun tagAnnotationEquality(jp: JavaParser) {
        val cu = jp.parse(
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
        )

        val fastTest = cu[0].classes[0].leadingAnnotations[0]
        val fastTest2 = cu[0].classes[0].leadingAnnotations[1]
        val slowTest = cu[0].classes[0].leadingAnnotations[2]

        assertThat(SemanticallyEqual.areEqual(fastTest, fastTest2)).isTrue
        assertThat(SemanticallyEqual.areEqual(fastTest, slowTest)).isFalse
    }

    @Test
    fun annotationEquality(jp: JavaParser) {
        val cu = jp.parse(
            """
                @MyAnnotation(value = true, srcValue = "true")
                class A {}
            """,
            annotInterface
        )

        val firstAnnot = cu[0].classes[0].leadingAnnotations[0]

        jp.reset()

        val secondAnnot = jp.parse(
            """
                @MyAnnotation(value = true, srcValue = "true")
                class B {}
            """,
            annotInterface
        )[0].classes[0].leadingAnnotations[0]

        assertThat(SemanticallyEqual.areEqual(firstAnnot, secondAnnot)).isTrue
    }

    @Test
    fun identEquality(jp: JavaParser) {
        val cu = jp.parse(
            """
                @MyAnnotation(value = true)
                class A {}
            """,
            "@interface MyAnnotation { boolean value(); }"
        )

        val firstIdent = cu[0].classes[0].leadingAnnotations[0].annotationType
        val secondIdent = J.Identifier.build(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            "MyAnnotation",
            JavaType.buildType("MyAnnotation")
        )
        val thirdIdent = J.Identifier.build(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            "YourAnnotation",
            JavaType.buildType("YourAnnotation")
        )

        assertThat(SemanticallyEqual.areEqual(firstIdent, secondIdent)).isTrue

        assertThat(SemanticallyEqual.areEqual(firstIdent, thirdIdent)).isFalse
    }

    @Test
    fun fieldAccessEquality(jp: JavaParser) {
        val cu = jp.parse(
            """
                @Category(FastTest.class)
                @Category(SlowTest.class)
                class A {
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
        )

        val firstFieldAccess = cu[0].classes[0].leadingAnnotations[0].arguments!!.first()
        val secondFieldAccess = J.FieldAccess(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            J.Identifier.build(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                "FastTest",
                JavaType.Class.build("FastTest")
            ),
            JLeftPadded(
                Space.EMPTY,
                J.Identifier.build(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    "class",
                    null
                ),
                Markers.EMPTY
            ),
            JavaType.Class.build("java.lang.Class")
        )
        val thirdFieldAccess = cu[0].classes[0].leadingAnnotations[1].arguments!!.first()

        assertThat(
            SemanticallyEqual
                .areEqual(
                    firstFieldAccess,
                    secondFieldAccess
                )
        ).isTrue

        assertThat(
            SemanticallyEqual
                .areEqual(
                    firstFieldAccess,
                    thirdFieldAccess
                )
        ).isFalse
    }

    @Test
    fun assignEquality(jp: JavaParser) {
        val cu = jp.parse(
            """
                @MyAnnotation(value = true)
                class A {}
            """,
            "@interface MyAnnotation { boolean value(); }"
        )

        val firstAssign = cu[0].classes[0].leadingAnnotations[0].arguments!!.first()
        val secondAssign = J.Assignment(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            J.Identifier.build(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                "value",
                null
            ),
            JLeftPadded(
                Space.format(" "),
                J.Literal(
                    randomId(),
                    Space.format(" "),
                    Markers.EMPTY,
                    true,
                    "true",
                    null,
                    JavaType.Primitive.Boolean
                ),
                Markers.EMPTY
            ),
            JavaType.Primitive.Boolean
        )
        val thirdAssign = secondAssign.withVariable(
            (secondAssign.variable as J.Identifier).withName("otherValue")
        )
        val fourthAssign = secondAssign.withAssignment(
            (secondAssign.assignment as J.Literal).withValue(false)
        )

        assertThat(
            SemanticallyEqual
                .areEqual(
                    firstAssign,
                    secondAssign
                )
        ).isTrue

        assertThat(
            SemanticallyEqual
                .areEqual(
                    firstAssign,
                    thirdAssign
                )
        ).isFalse

        assertThat(
            SemanticallyEqual
                .areEqual(
                    firstAssign,
                    fourthAssign
                )
        ).isFalse
    }

    @Test
    fun literalEquality(jp: JavaParser) {
        val cu = jp.parse(
            """
                class A {
                    int i = 0;
                    String str = "thisString";
                    String str2 = null;
                }
            """
        )

        val intLiteral = (cu[0].classes[0].body.statements[0] as J.VariableDeclarations).variables[0].initializer
        val strLiteral = (cu[0].classes[0].body.statements[1] as J.VariableDeclarations).variables[0].initializer
        val nullLiteral = (cu[0].classes[0].body.statements[2] as J.VariableDeclarations).variables[0].initializer

        assertThat(
            SemanticallyEqual
                .areEqual(
                    intLiteral!!,
                    J.Literal(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        0,
                        "0",
                        null,
                        JavaType.Primitive.Int
                    )
                )
        ).isTrue

        assertThat(
            SemanticallyEqual
                .areEqual(
                    strLiteral!!,
                    J.Literal(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        "thisString",
                        "thisString",
                        null,
                        JavaType.Primitive.String
                    )
                )
        ).isTrue

        assertThat(
            SemanticallyEqual
                .areEqual(
                    nullLiteral!!,
                    J.Literal(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        null,
                        "null",
                        null,
                        JavaType.Primitive.String
                    )
                )
        ).isTrue

        assertThat(
            SemanticallyEqual
                .areEqual(
                    strLiteral,
                    J.Literal(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        0,
                        "0",
                        null,
                        JavaType.Primitive.Int
                    )
                )
        ).isFalse
    }

    @Test
    fun typeEqualityDependsOnlyOnFqn(jp: JavaParser) {
        val nameA = J.Identifier.build(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            "name",
            object : JavaType.ShallowClass("org.foo.Bar") {
                override fun deepEquals(type: JavaType?): Boolean {
                    return false
                }

            }
        )
        val nameB = J.Identifier.build(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            "name",
            JavaType.Class.build(
                Collections.singleton(Flag.Public),
                "org.foo.Bar",
                JavaType.Class.Kind.Class,
                listOf(),
                listOf(JavaType.Class.build("org.foo.Baz")),
                listOf(),
                null,
                null
            )
        )

        assertThat(SemanticallyEqual.areEqual(nameA, nameB)).isTrue
    }
}
