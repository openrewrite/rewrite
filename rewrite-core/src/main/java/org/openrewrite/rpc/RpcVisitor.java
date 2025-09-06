/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.rpc;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

@RequiredArgsConstructor
class RpcVisitor extends TreeVisitor<Tree, ExecutionContext> {
    private final RewriteRpc rpc;
    private final String visitorName;

    @Override
    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
        return rpc.getLanguages().contains(sourceFile.getClass().getName());
    }

    @Override
    public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
        stopAfterPreVisit();
        return rpc.visit((SourceFile) tree, visitorName, ctx);
    }
}
