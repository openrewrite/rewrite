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
package org.openrewrite.java.internal;

import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.Collections;

public class FormatFirstClassPrefix<P> extends JavaIsoVisitor<P> {
    private static final J.Block EMPTY_BLOCK = new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
            new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY), Collections.emptyList(), Space.EMPTY);

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = classDecl;
        J.CompilationUnit cu = getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
        if (c == cu.getClasses().get(0)) {
            c = autoFormat(c.withBody(EMPTY_BLOCK).withPrefix(Space.build("\n", c.getComments())), p)
                    .withBody(c.getBody());
        }
        return c;
    }
}
