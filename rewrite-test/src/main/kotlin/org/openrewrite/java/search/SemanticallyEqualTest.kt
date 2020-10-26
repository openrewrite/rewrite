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
import org.openrewrite.Formatting
import org.openrewrite.Tree.randomId
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType

interface SemanticallyEqualTest {

    @Test
    fun tagAnnotationEquality(jp: JavaParser) {
        val cu = jp.parse(
            """
                @Tagg(FastTests.class)
                @Tagg(FastTests.class)
                @Tagg(SlowTests.class)
                class A {}
            """,
            """
                @interface Tagg {
                    Class[] value();
                }
            """,
            "public interface FastTests {}",
            "public interface SlowTests {}"
        )

        val fastTest = cu[0].classes[0].annotations[0]
        val fastTest2 = cu[0].classes[0].annotations[1]
        val slowTest = cu[0].classes[0].annotations[2]

        assertThat(SemanticallyEqual(fastTest).visit(fastTest2)).isTrue()
        assertThat(SemanticallyEqual(fastTest).visit(slowTest)).isFalse()
    }

    @Test
    fun annotationEquality(jp: JavaParser) {
        val cu = jp.parse(
            """
                @MyAnnotation(value = true, srcValue = "true")
                class A {}
            """,
            """
                @interface MyAnnotation { 
                    boolean value();
                    String srcValue(); 
                }
            """
        )

        val annotation = cu[0].classes[0].annotations[0]

        jp.reset();

        val otherAnnot = jp.parse(
            """
                @MyAnnotation(value = true, srcValue = "true")
                class B {}
            """,
            """
                @interface MyAnnotation { 
                    boolean value();
                    String srcValue(); 
                }
            """
        )[0].classes[0].annotations[0]

        assertThat(SemanticallyEqual(annotation).visit(otherAnnot)).isTrue()
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

        val ident = cu[0].classes[0].annotations[0].annotationType

        assertThat(SemanticallyEqual(ident)
            .visit(
                J.Ident.build(
                    randomId(),
                    "MyAnnotation",
                    JavaType.buildType("MyAnnotation"),
                    Formatting.EMPTY
                )
            )
        ).isTrue()

        assertThat(SemanticallyEqual(ident)
            .visit(
                J.Ident.build(
                    randomId(),
                    "YourAnnotation",
                    JavaType.buildType("YourAnnotation"),
                    Formatting.EMPTY
                )
            )
        ).isFalse()
    }

    @Test
    fun fieldAccessEquality(jp: JavaParser) {
        val cu = jp.parse(
            """
                @Category(FastTest.class)
                class A {
                }
            """,
            "@interface Category { Class<?>[] value(); }",
            "class FastTest {}"
        )

        val annotFieldAccess = cu[0].classes[0].annotations[0].args.args[0];

        assertThat(SemanticallyEqual(annotFieldAccess)
            .visit(
                J.FieldAccess(
                    randomId(),
                    J.Ident.build(randomId(), "FastTest", JavaType.buildType("FastTest"), Formatting.EMPTY),
                    J.Ident.build(randomId(), "class", null, Formatting.EMPTY),
                    JavaType.buildType("java.lang.Class"),
                    Formatting.EMPTY
                )
            )
        ).isTrue()

        assertThat(SemanticallyEqual(annotFieldAccess)
            .visit(
                J.FieldAccess(
                    randomId(),
                    J.Ident.build(randomId(), "SlowTest", JavaType.buildType("SlowTest"), Formatting.EMPTY),
                    J.Ident.build(randomId(), "class", null, Formatting.EMPTY),
                    JavaType.buildType("java.lang.Class"),
                    Formatting.EMPTY
                )
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

        val assign = cu[0].classes[0].annotations[0].args.args[0]

        assertThat(SemanticallyEqual(assign).
            visit(
                J.Assign(
                    randomId(),
                    J.Ident.build(
                        randomId(),
                        "value",
                        null,
                        Formatting.EMPTY
                    ),
                    J.Literal(
                        randomId(),
                        true,
                        "true",
                        JavaType.Primitive.Boolean,
                        Formatting.EMPTY
                    ),
                    JavaType.Primitive.Boolean,
                    Formatting.EMPTY
                )
            )
        ).isTrue()

        assertThat(SemanticallyEqual(assign).
        visit(
            J.Assign(
                randomId(),
                J.Ident.build(
                    randomId(),
                    "otherValue",
                    null,
                    Formatting.EMPTY
                ),
                J.Literal(
                    randomId(),
                    true,
                    "true",
                    JavaType.Primitive.Boolean,
                    Formatting.EMPTY
                ),
                JavaType.Primitive.Boolean,
                Formatting.EMPTY
            )
        )
        ).isFalse()

        assertThat(SemanticallyEqual(assign).
            visit(
                J.Assign(
                    randomId(),
                    J.Ident.build(
                        randomId(),
                        "value",
                        null,
                        Formatting.EMPTY
                    ),
                    J.Literal(
                        randomId(),
                        false,
                        "true",
                        JavaType.Primitive.Boolean,
                        Formatting.EMPTY
                    ),
                    JavaType.Primitive.Boolean,
                    Formatting.EMPTY
                )
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

        val intLiteral = (cu[0].classes[0].body.statements[0] as J.VariableDecls).vars[0].initializer as J.Literal
        val strLiteral = (cu[0].classes[0].body.statements[1] as J.VariableDecls).vars[0].initializer as J.Literal
        val nullLiteral = (cu[0].classes[0].body.statements[2] as J.VariableDecls).vars[0].initializer as J.Literal

        assertThat(SemanticallyEqual(intLiteral)
            .visit(
                J.Literal(
                    randomId(),
                    0,
                    "0",
                    JavaType.Primitive.Int,
                    Formatting.EMPTY
                )
            )
        ).isTrue()

        assertThat(SemanticallyEqual(strLiteral)
            .visit(
                J.Literal(
                    randomId(),
                    "thisString",
                    "thisString",
                    JavaType.Primitive.String,
                    Formatting.EMPTY
                )
            )
        ).isTrue()

        assertThat(SemanticallyEqual(nullLiteral)
            .visit(
                J.Literal(
                    randomId(),
                    null,
                    "null",
                    JavaType.Primitive.String,
                    Formatting.EMPTY
                )
            )
        ).isTrue()

        assertThat(SemanticallyEqual(strLiteral)
            .visit(
                J.Literal(
                    randomId(),
                    0,
                    "0",
                    JavaType.Primitive.Int,
                    Formatting.EMPTY
                )
            )
        ).isFalse()
    }
}
