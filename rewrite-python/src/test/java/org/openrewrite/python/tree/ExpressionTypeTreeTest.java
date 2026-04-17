/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressionTypeTreeTest {

    @Test
    void withTypeOnWrappedMethodInvocation() {
        J.MethodInvocation mi = new J.MethodInvocation(
                Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                null, null,
                new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, Collections.emptyList(), "foo", null, null),
                JContainer.empty(),
                null
        );
        Py.ExpressionTypeTree ett = new Py.ExpressionTypeTree(
                Tree.randomId(), Space.EMPTY, Markers.EMPTY, mi
        );

        // Should not throw UnsupportedOperationException
        Py.ExpressionTypeTree result = ett.withType(JavaType.Primitive.Int);
        assertThat(result).isSameAs(ett);
    }

    @Test
    void withTypeOnWrappedMethodDeclaration() {
        J.MethodDeclaration md = new J.MethodDeclaration(
                Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                Collections.emptyList(), Collections.emptyList(), null,
                null,
                new J.MethodDeclaration.IdentifierWithAnnotations(
                        new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, Collections.emptyList(), "bar", null, null),
                        Collections.emptyList()
                ),
                JContainer.empty(), null, null, null, null
        );
        Py.ExpressionTypeTree ett = new Py.ExpressionTypeTree(
                Tree.randomId(), Space.EMPTY, Markers.EMPTY, md
        );

        // Should not throw UnsupportedOperationException
        Py.ExpressionTypeTree result = ett.withType(JavaType.Primitive.Int);
        assertThat(result).isSameAs(ett);
    }

    @Test
    void withTypeOnWrappedIdentifier() {
        J.Identifier ident = new J.Identifier(
                Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                Collections.emptyList(), "x", JavaType.Primitive.String, null
        );
        Py.ExpressionTypeTree ett = new Py.ExpressionTypeTree(
                Tree.randomId(), Space.EMPTY, Markers.EMPTY, ident
        );

        // Should update the type normally
        Py.ExpressionTypeTree result = ett.withType(JavaType.Primitive.Int);
        assertThat(result).isNotSameAs(ett);
        assertThat(result.getType()).isEqualTo(JavaType.Primitive.Int);
    }
}
