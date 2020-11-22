package org.openrewrite.java

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.openrewrite.RefactorVisitorTest
import org.openrewrite.java.tree.J

interface ChangeFieldVisibilityTest : RefactorVisitorTest {

    @Test
    fun changedFieldVisibility(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { cu: J.CompilationUnit ->
                ChangeFieldVisibility().apply { setClassType(cu.classes[0].type.asClass()); setFieldName("foo"); setVisibility("private"); }
            },
            before =
                """
                    public class A {
                        public String foo;
                    }
                """,
            after =
                """
                    public class A {
                        private String foo;
                    }
                """
    )
}