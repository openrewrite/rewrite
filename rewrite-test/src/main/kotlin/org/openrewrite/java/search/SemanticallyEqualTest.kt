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

        assertThat(SemanticallyEqual(annotation)
            .visit(
                J.Annotation(
                    randomId(),
                    J.Ident.build(
                        randomId(),
                        "MyAnnotation",
                        JavaType.buildType("MyAnnotation"),
                        Formatting.EMPTY
                    ),
                    J.Annotation.Arguments(
                        randomId(),
                        listOf(
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
                            ),
                            J.Assign(
                                randomId(),
                                J.Ident.build(
                                    randomId(),
                                    "srcValue",
                                    null,
                                    Formatting.EMPTY
                                ),
                                J.Literal(
                                    randomId(),
                                    "true",
                                    "\"true\"",
                                    JavaType.Primitive.String,
                                    Formatting.EMPTY
                                ),
                                JavaType.buildType("java.lang.String"),
                                Formatting.EMPTY
                            )
                        ),
                        Formatting.EMPTY
                    ),
                    Formatting.EMPTY
                )
            )
        ).isTrue()
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
