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
package org.openrewrite.kotlin.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.ShortenFullyQualifiedTypeReferences;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.AddImport;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class KotlinImportService extends ImportService {

    private static final Set<String> KOTLIN_MAPPED_JAVA_UTIL_TYPES = new HashSet<>(asList(
            "java.util.Collection", "java.util.Iterator", "java.util.List",
            "java.util.ListIterator", "java.util.Map", "java.util.Set"));

    @Override
    public <P> JavaVisitor<P> addImportVisitor(@Nullable String packageName, String typeName, @Nullable String member, @Nullable String alias, boolean onlyIfReferenced) {
        return new AddImport<>(packageName, typeName, member, alias, onlyIfReferenced);
    }

    /**
     * Keeps {@code java.lang} and Kotlin-mapped collection types qualified: Kotlin binds names such as
     * {@code String}, {@code Deprecated} and {@code List} to its own types, so a simple name can mean another type.
     */
    @Override
    public <J2 extends J> JavaVisitor<ExecutionContext> shortenFullyQualifiedTypeReferencesIn(J2 subtree) {
        return ShortenFullyQualifiedTypeReferences.modifyOnly(subtree,
                fqn -> fqn.startsWith("java.lang.") || KOTLIN_MAPPED_JAVA_UTIL_TYPES.contains(fqn));
    }
}
