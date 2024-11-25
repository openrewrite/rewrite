/*
 * Copyright 2024 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.trait.Reference;

import java.util.Map;

@Value
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ReferenceUpdateVisitor extends TreeVisitor<Tree, ExecutionContext> {
    Map<Tree, Reference> matches;
    Reference.ReferenceUpdateProvider updateProvider;
    String newValue;
    @NonFinal
    Reference.@Nullable ReferenceUpdateFunction updateFunction;

    @Override
    public @Nullable Tree preVisit(@Nullable Tree tree, ExecutionContext ctx) {
        Reference reference = matches.get(tree);
        if (reference != null && updateProvider.acceptsReferenceForUpdate(reference)) {
            if (updateFunction == null) {
                updateFunction = updateProvider.getReferenceUpdateFunction();
            }
            return updateFunction.apply(reference, newValue).getTree();
        }
        return tree;
    }
}
