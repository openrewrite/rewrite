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
 *   <li>{@code "github.com/x/y"} → packageName="github", typeName="com/x/y" → add {@code import "github.com/x/y"}</li>
 * </ul>
 */
public class GolangImportService extends ImportService {

    @Override
    public <P> JavaVisitor<P> addImportVisitor(@Nullable String packageName,
                                               String typeName,
                                               @Nullable String member,
                                               @Nullable String alias,
                                               boolean onlyIfReferenced) {
        return new GolangAddImport<>(importPath(packageName, typeName), alias, onlyIfReferenced);
    }

    /**
     * {@code maybeAddImport(fqn)} splits at the last dot, which leaves no package for {@code "net/http"} and
     * lands inside {@code "github.com/x/y"} or {@code "gopkg.in/yaml.v3"}; rejoin those.
     */
    private static String importPath(@Nullable String packageName, String typeName) {
        if (packageName == null) {
            return typeName;
        }
        if (typeName.indexOf('/') >= 0 || GolangAddImport.isVersionElement(typeName)) {
            return packageName + "." + typeName;
        }
        return packageName;
    }
}
