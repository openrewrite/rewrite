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
package org.openrewrite.java.refactor;

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeTree;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;

public class ImplementInterface extends ScopedJavaRefactorVisitor {
    private final String interfaze;

    public ImplementInterface(J.ClassDecl scope, String interfaze) {
        super(scope.getId());
        this.interfaze = interfaze;
    }

    @Override
    public String getName() {
        return "core.ImplementInterface{from=" + interfaze + "}";
    }

    @Override
    public J visitClassDecl(J.ClassDecl classDecl) {
        J.ClassDecl c = refactor(classDecl, super::visitClassDecl);
        if (classDecl.getId().equals(getScope())) {
            maybeAddImport(interfaze);

            J.Ident lifeCycle = J.Ident.buildClassName(interfaze).withFormatting(format(" "));

            if (c.getImplements() == null) {
                c = c.withImplements(new J.ClassDecl.Implements(randomId(),
                        singletonList(lifeCycle),
                        format(" ")));
            }
            else {
                List<TypeTree> implementings = new ArrayList<>(c.getImplements().getFrom());
                implementings.add(0, lifeCycle);
                c = c.withImplements(c.getImplements().withFrom(implementings));
            }
        }

        return c;
    }
}
