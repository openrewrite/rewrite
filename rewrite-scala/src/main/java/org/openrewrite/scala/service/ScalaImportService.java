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
package org.openrewrite.scala.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.scala.AddImport;
import org.openrewrite.scala.RemoveUnusedImports;

/**
 * Adds and removes imports the way Scala writes and scopes them, where an import may sit between or inside
 * definitions, select several names in braces, or be used only implicitly.
 */
public class ScalaImportService extends ImportService {

    @Override
    public <P> JavaVisitor<P> addImportVisitor(@Nullable String packageName, String typeName, @Nullable String member,
                                               @Nullable String alias, boolean onlyIfReferenced) {
        return new AddImport<>(packageName, typeName, member, alias, onlyIfReferenced);
    }

    @Override
    public <P> JavaVisitor<P> removeImportVisitor(String fullyQualifiedName) {
        return RemoveUnusedImports.removing(fullyQualifiedName);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> removeUnusedImportsVisitor() {
        return new RemoveUnusedImports().getVisitor();
    }

    /**
     * {@link org.openrewrite.java.tree.JavaSourceFile#getImports()} lists only the single-name and wildcard imports
     * at the top level of a Scala file, so the others are only reached through this service.
     */
    @Override
    public boolean usesStatementBasedImports() {
        return true;
    }
}
