/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.visitor.search;

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Tree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.java.visitor.AstVisitor;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@RequiredArgsConstructor
public class FindFields extends AstVisitor<List<J.VariableDecls>> {
    private final String fullyQualifiedName;

    @Override
    public List<J.VariableDecls> defaultTo(Tree t) {
        return emptyList();
    }

    @Override
    public List<J.VariableDecls> visitMultiVariable(J.VariableDecls multiVariable) {
        if(multiVariable.getTypeExpr() instanceof J.MultiCatch) {
            return emptyList();
        }
        if(multiVariable.getTypeExpr() != null && TypeUtils.hasElementType(multiVariable.getTypeExpr().getType(), fullyQualifiedName)) {
            return singletonList(multiVariable);
        }
        return emptyList();
    }
}
