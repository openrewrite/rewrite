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
package org.openrewrite.java.utilities;

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AbstractJavaSourceVisitor;
import org.openrewrite.java.tree.J;

import java.util.Spliterators;

import static java.util.stream.StreamSupport.stream;

class SpansMultipleLines extends AbstractJavaSourceVisitor<Boolean> {
    private final J scope;

    @Nullable
    private final J skip;

    SpansMultipleLines(J scope, @Nullable J skip) {
        this.scope = scope;
        this.skip = skip;
        setCursoringOn();
    }

    @Override
    public Boolean defaultTo(Tree t) {
        return false;
    }

    @Override
    public Boolean visitTree(Tree tree) {
        if (scope.isScope(tree)) {
            if (tree instanceof J.Block && ((J.Block<?>) tree).getEnd().getPrefix().contains("\n")) {
                return true;
            }

            // don't look at the prefix of the scope that we are testing, we are interested in its contents
            return super.visitTree(tree);
        } else if (getCursor().isScopeInPath(scope) && !isSkipInCursorPath()) {
            if (tree instanceof J.Block && ((J.Block<?>) tree).getEnd().getPrefix().contains("\n")) {
                return true;
            }

            return tree != null && tree.getFormatting().getPrefix().contains("\n") || super.visitTree(tree);
        } else {
            return false;
        }
    }

    private boolean isSkipInCursorPath() {
        Tree t = getCursor().getTree();
        return skip != null && ((t != null && t.getId().equals(skip.getId())) ||
                stream(Spliterators.spliteratorUnknownSize(getCursor().getPath(), 0), false)
                        .anyMatch(p -> p.getId().equals(skip.getId())));
    }
}
