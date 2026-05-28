/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.golang.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.service.ImportService;

/**
 * Go-specific import service. Translates Java-style import requests
 * (packageName + typeName) to Go import paths.
 * <p>
 * In Go's FQN model, the package path IS the import path:
 * <ul>
 *   <li>{@code "main.Point"} → packageName="main", typeName="Point" → same-package, no import needed</li>
 *   <li>{@code "fmt.Stringer"} → packageName="fmt", typeName="Stringer" → add {@code import "fmt"}</li>
 *   <li>{@code "net/http.Handler"} → packageName="net/http", typeName="Handler" → add {@code import "net/http"}</li>
 * </ul>
 */
public class GolangImportService extends ImportService {

    @Override
    public <P> JavaVisitor<P> addImportVisitor(@Nullable String packageName,
                                               String typeName,
                                               @Nullable String member,
                                               @Nullable String alias,
                                               boolean onlyIfReferenced) {
        if (packageName == null) {
            return new JavaVisitor<P>() {};
        }
        return new GolangAddImport<>(packageName, alias, onlyIfReferenced);
    }
}
