/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.service;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.ShortenFullyQualifiedTypeReferences;
import org.openrewrite.java.tree.J;

@Incubating(since = "8.2.0")
public class ImportService {

    public <P> JavaVisitor<P> addImportVisitor(@Nullable String packageName,
                                               String typeName,
                                               @Nullable String member,
                                               @Nullable String alias,
                                               boolean onlyIfReferenced) {
        return new AddImport<>(packageName, typeName, member,  alias, onlyIfReferenced);
    }

    public <J2 extends J> JavaVisitor<ExecutionContext> shortenAllFullyQualifiedTypeReferences() {
        return new ShortenFullyQualifiedTypeReferences().getVisitor();
    }

    public <J2 extends J> JavaVisitor<ExecutionContext> shortenFullyQualifiedTypeReferencesIn(J2 subtree) {
        return ShortenFullyQualifiedTypeReferences.modifyOnly(subtree);
    }
}
