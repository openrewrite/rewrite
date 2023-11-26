/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java;

import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.format.BlankLines;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;

@Incubating(since = "7.35.0")
public class ExtractField<P> extends JavaVisitor<P> {
    private final J.VariableDeclarations scope;

    public ExtractField(J.VariableDeclarations scope) {
        if (scope.getTypeAsFullyQualified() == null) {
            throw new IllegalArgumentException("Scope must have a fully qualified type");
        }
        this.scope = scope;
    }

    @Override
    public J visitBlock(J.Block block, P p) {
        J.Block b = (J.Block) super.visitBlock(block, p);
        if (getCursor().getMessage("extractTo", false)) {
            J.VariableDeclarations field = autoFormat(
                    scope.withId(randomId())
                            .withVariables(ListUtils.map(scope.getVariables(), v -> v
                                    .withInitializer(null)
                                    .withVariableType(new JavaType.Variable(null, Flag.Private.getBitMask(),
                                            v.getSimpleName(), getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class).getType(),
                                            v.getType(), emptyList())))
                            )
                            .withModifiers(singletonList(new J.Modifier(randomId(), Space.EMPTY, Markers.EMPTY, null, J.Modifier.Type.Private, emptyList()))),
                    p, getCursor()
            );
            b = b.withStatements(ListUtils.concat(field, b.getStatements()));

            // allow for a blank line(s) between the new field and the subsequent statement
            // (if that is the prevailing style of the project)
            Cursor updatedCursor = new Cursor(getCursor().getParentOrThrow(), b);
            b = b.withStatements(ListUtils.map(b.getStatements(), (n, stat) -> n == 1 ?
                    BlankLines.formatBlankLines(stat, updatedCursor) : stat));
        }
        return b;
    }

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        if (multiVariable == scope) {
            getCursor()
                    .getPathAsCursors(c -> c.getValue() instanceof J.Block && c.getParentTreeCursor().getValue() instanceof J.ClassDeclaration)
                    .next()
                    .putMessage("extractTo", true);

            String fieldType = requireNonNull(multiVariable.getTypeAsFullyQualified()).getFullyQualifiedName();
            J.Assignment assignment = JavaTemplate
                    .builder("this.#{} = #{any(" + fieldType + ")}")
                    .contextSensitive()
                    .build()
                    .apply(
                            getCursor(),
                            multiVariable.getCoordinates().replace(),
                            multiVariable.getVariables().get(0).getSimpleName(),
                            multiVariable.getVariables().get(0).getInitializer()
                    );
            return assignment.withType(multiVariable.getType());
        }
        return super.visitVariableDeclarations(multiVariable, p);
    }
}
