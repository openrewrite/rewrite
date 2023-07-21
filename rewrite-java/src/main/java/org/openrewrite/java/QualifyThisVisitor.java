/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;

public class QualifyThisVisitor extends JavaVisitor<ExecutionContext> {
    @Override
    public J visitIdentifier(J.Identifier ident, ExecutionContext executionContext) {
        if (ident.getSimpleName().equals("this")
                && !isAlreadyQualified(ident)
                && ident.getType() instanceof JavaType.Class) {
            JavaType.Class type = (JavaType.Class) ident.getType();
            return new J.FieldAccess(
                    Tree.randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    new J.Identifier(
                            Tree.randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            emptyList(),
                            type.getClassName(),
                            type,
                            null
                    ),
                    JLeftPadded.build(ident),
                    type
            );
        } else {
            return ident;
        }
    }

    private boolean isAlreadyQualified(J.Identifier ident) {
        Cursor parentCursor = getCursor().getParentTreeCursor();
        if (!(parentCursor.getValue() instanceof J.FieldAccess)) {
            return false;
        }
        J.FieldAccess parent = parentCursor.getValue();
        return parent.getName() == ident;
    }
}
