package org.openrewrite.java.internal.cache

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.JavaExecutionContextView
import org.openrewrite.java.JavaParser
import org.openrewrite.java.marker.JavaSourceSet
import org.openrewrite.java.tree.J

interface JavaTypeCacheTest {

    @Test
    fun referentiallyEqualClass(jp: JavaParser) {
        val ctx = JavaExecutionContextView(InMemoryExecutionContext())
        ctx.typeCache = ClasspathJavaTypeCache()

        val cu = jp.parse(
            ctx,
            """
            import java.util.List;
            @SuppressWarnings("ALL")
            class Test {
                List l1;
                List l2;
            }
        """
        )[0]

        val (t1, t2) = cu.classes[0].body.statements
            .map { it as J.VariableDeclarations }
            .map { it.type }

        JavaSourceSet.build("main", emptyList(), ctx)

        assertThat(t1).isSameAs(t2)
    }
}
