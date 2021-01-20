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

import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

public class WrappingAndBracesProcessor<P> extends JavaIsoProcessor<P> {
    private final WrappingAndBracesStyle style;

    public WrappingAndBracesProcessor(WrappingAndBracesStyle style) {
        this.style = style;
        setCursoringOn();
    }

    @Override
    public Statement visitStatement(Statement statement, P p) {
        Statement j = super.visitStatement(statement, p);
        J parentTree = getCursor().getParentOrThrow().getTree();
        if(parentTree instanceof J.Block) {
            if(!j.getPrefix().getWhitespace().contains("\n")) {
                j = j.withPrefix(j.getPrefix().withWhitespace("\n" + j.getPrefix().getWhitespace()));
            }
        }

        return j;
    }
}
