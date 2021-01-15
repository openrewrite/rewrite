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
import org.openrewrite.Tree.randomId
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JLeftPadded
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.Space
import org.openrewrite.marker.Markers

interface SemanticallyEqualTest {

    companion object {
        private const val annotInterface = """
            @interface MyAnnotation { 
                boolean value();
                String srcValue(); 
            }
        """
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

        val fastTest = cu[0].classes[0].annotations[0]
        val fastTest2 = cu[0].classes[0].annotations[1]
        val slowTest = cu[0].classes[0].annotations[2]

        assertThat(SemanticallyEqual.areEqual(fastTest, fastTest2)).isTrue()
        assertThat(SemanticallyEqual.areEqual(fastTest, slowTest)).isFalse()
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

        val firstAnnot = cu[0].classes[0].annotations[0]

        jp.reset()

        val secondAnnot = jp.parse(
            """
                @MyAnnotation(value = true, srcValue = "true")
                class B {}
            """,
            annotInterface
        )[0].classes[0].annotations[0]

        assertThat(SemanticallyEqual.areEqual(firstAnnot, secondAnnot)).isTrue()
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

        val firstIdent = cu[0].classes[0].annotations[0].annotationType
        val secondIdent = J.Ident.build(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            "MyAnnotation",
            JavaType.buildType("MyAnnotation")
        )
        val thirdIdent = J.Ident.build(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            "YourAnnotation",
            JavaType.buildType("YourAnnotation")
        )

        assertThat(SemanticallyEqual.areEqual(firstIdent, secondIdent)).isTrue()

        assertThat(SemanticallyEqual.areEqual(firstIdent, thirdIdent)).isFalse()
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

        val firstFieldAccess = cu[0].classes[0].annotations[0].args?.elem?.first()?.elem
        val secondFieldAccess = J.FieldAccess(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            J.Ident.build(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                "FastTest",
                JavaType.Class.build("FastTest")
            ),
            JLeftPadded(
                Space.EMPTY,
                J.Ident.build(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    "class",
                    null
                )
            ),
            JavaType.Class.build("java.lang.Class")
        )
        val thirdFieldAccess = cu[0].classes[0].annotations[1].args?.elem?.first()?.elem

        assertThat(
            SemanticallyEqual
                .areEqual(
                    firstFieldAccess,
                    secondFieldAccess
                )
        ).isTrue()

        assertThat(
            SemanticallyEqual
                .areEqual(
                    firstFieldAccess,
                    thirdFieldAccess
                )
        ).isFalse()
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

        val firstAssign = cu[0].classes[0].annotations[0].args?.elem?.first()?.elem
        val secondAssign = J.Assign(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            J.Ident.build(
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
                    JavaType.Primitive.Boolean
                )
            ),
            JavaType.Primitive.Boolean
        )
        val thirdAssign = secondAssign.withVariable(
            (secondAssign.variable as J.Ident).withName("otherValue")
        )
        val fourthAssign = secondAssign.withAssignment(
            secondAssign.assignment.withElem(
                (secondAssign.assignment.elem as J.Literal).withValue(false)
            )
        )

        assertThat(
            SemanticallyEqual
                .areEqual(
                    firstAssign,
                    secondAssign
                )
        ).isTrue()

        assertThat(
            SemanticallyEqual
                .areEqual(
                    firstAssign,
                    thirdAssign
                )
        ).isFalse()

        assertThat(
            SemanticallyEqual
                .areEqual(
                    firstAssign,
                    fourthAssign
                )
        ).isFalse()
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

        val intLiteral = (cu[0].classes[0].body.statements[0].elem as J.VariableDecls).vars[0].elem.initializer.elem
        val strLiteral = (cu[0].classes[0].body.statements[1].elem as J.VariableDecls).vars[0].elem.initializer.elem
        val nullLiteral = (cu[0].classes[0].body.statements[2].elem as J.VariableDecls).vars[0].elem.initializer.elem

        assertThat(
            SemanticallyEqual
                .areEqual(
                    intLiteral,
                    J.Literal(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        0,
                        "0",
                        JavaType.Primitive.Int
                    )
                )
        ).isTrue()

        assertThat(
            SemanticallyEqual
                .areEqual(
                    strLiteral,
                    J.Literal(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        "thisString",
                        "thisString",
                        JavaType.Primitive.String
                    )
                )
        ).isTrue()

        assertThat(
            SemanticallyEqual
                .areEqual(
                    nullLiteral,
                    J.Literal(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        null,
                        "null",
                        JavaType.Primitive.String
                    )
                )
        ).isTrue()

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
                        JavaType.Primitive.Int
                    )
                )
        ).isFalse()
    }

    @Test
    fun typeEqualityDependsOnlyOnFqn(jp: JavaParser) {
        val nameA = J.Ident.build(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            "name",
            JavaType.Class.build("org.foo.Bar")
        )
        val nameB = J.Ident.build(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            "name",
            JavaType.Class.build(
                "org.foo.Bar",
                listOf(),
                listOf(),
                listOf(JavaType.Class.build("org.foo.Baz")),
                listOf(),
                null
            )
        )

        assertThat(SemanticallyEqual.areEqual(nameA, nameB)).isTrue()
    }
}
