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
package org.openrewrite.java;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.marker.Markers;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;

public class ImplementInterface {
    public static class Scoped extends JavaIsoRefactorVisitor {
        private final J.ClassDecl scope;
        private final String interfaze;
        private final JavaType.Class interfaceType;

        public Scoped(J.ClassDecl scope, String interfaze) {
            this.scope = scope;
            this.interfaze = interfaze;
            this.interfaceType = JavaType.Class.build(interfaze);
        }

        @Override
        public Iterable<Tag> getTags() {
            return Tags.of("from", interfaze);
        }

        @Override
        public J.ClassDecl visitClassDecl(J.ClassDecl classDecl) {
            J.ClassDecl c = super.visitClassDecl(classDecl);
            if (scope.isScope(classDecl) && (classDecl.getImplements() == null ||
                    classDecl.getImplements().getFrom().stream().noneMatch(f -> interfaceType.equals(f.getType())))) {
                maybeAddImport(interfaze);

                J.Ident lifeCycle = J.Ident.buildClassName(interfaze).withFormatting(format(" "));

                if (c.getImplements() == null) {
                    c = c.withImplements(new J.ClassDecl.Implements(randomId(),
                            singletonList(lifeCycle),
                            format(" "),
                            Markers.EMPTY));
                } else {
                    List<TypeTree> implementings = new ArrayList<>(c.getImplements().getFrom());
                    implementings.add(0, lifeCycle);
                    c = c.withImplements(c.getImplements().withFrom(implementings));
                }
            }

            return c;
        }
    }
}
