/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.csharp.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.J;

public class CSharpImportService extends ImportService {
    @Override
    public <P> JavaVisitor<P> addImportVisitor(@Nullable String packageName, String typeName,
                                             @Nullable String member, @Nullable String alias,
                                             boolean onlyIfReferenced) {
        return noOp();
    }

    @Override
    public <P> JavaVisitor<P> removeImportVisitor(String fullyQualifiedName) {
        return noOp();
    }

    @Override
    public JavaVisitor<ExecutionContext> removeUnusedImportsVisitor() {
        return noOp();
    }

    @Override
    public <J2 extends J> JavaVisitor<ExecutionContext> shortenFullyQualifiedTypeReferencesIn(J2 subtree) {
        return shortenAllFullyQualifiedTypeReferences();
    }

    @Override
    public <J2 extends J> JavaVisitor<ExecutionContext> shortenAllFullyQualifiedTypeReferences() {
        return noOp();
    }

    private static <P> JavaVisitor<P> noOp() {
        return new JavaVisitor<P>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, P ctx) {
                return (J) tree;
            }
        };
    }
}
