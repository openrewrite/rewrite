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
