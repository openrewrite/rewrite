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
package org.openrewrite.java.format;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

public class WrappingAndBracesProcessor<P> extends JavaIsoProcessor<P> {
    private final WrappingAndBracesStyle style;

    @Nullable
    private final List<? extends J> limitToTrees;

    public WrappingAndBracesProcessor(WrappingAndBracesStyle style, @Nullable List<? extends J> limitToTrees) {
        this.style = style;
        this.limitToTrees = limitToTrees;
        setCursoringOn();
    }

    @Override
    public Statement visitStatement(Statement statement, P p) {
        Statement j = super.visitStatement(statement, p);
        if(shouldNotFormat()) {
            return j;
        }

        J parentTree = getCursor().getParentOrThrow().getTree();
        if(parentTree instanceof J.Block) {
            J.Block block = (J.Block) parentTree;
            if(block.getStatements().iterator().next().getElem() == j &&
                    !j.getPrefix().getWhitespace().contains("\n")) {
                j = j.withPrefix(j.getPrefix().withWhitespace("\n" + j.getPrefix().getWhitespace()));
            }
        }

        return j;
    }

    private boolean shouldNotFormat() {
        return limitToTrees != null && limitToTrees.stream().noneMatch(t -> getCursor().isScopeInPath(t));
    }
}
