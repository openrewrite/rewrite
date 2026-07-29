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
package org.openrewrite.javascript;

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

/**
 * JavaScript-ecosystem configuration carried on the {@link ExecutionContext},
 * following the {@code MavenExecutionContextView} pattern. Hosts embedding
 * OpenRewrite use this to point npm dependency recipes at private registries
 * and to supply credentials, instead of relying on {@code .npmrc} files
 * captured from the repository.
 */
public class JavaScriptExecutionContextView extends DelegatingExecutionContext {
    private static final String NPM_DEFAULT_REGISTRY = "org.openrewrite.javascript.npmDefaultRegistry";
    private static final String NPM_SCOPED_REGISTRIES = "org.openrewrite.javascript.npmScopedRegistries";
    private static final String NPM_REGISTRY_CREDENTIALS = "org.openrewrite.javascript.npmRegistryCredentials";

    public JavaScriptExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static JavaScriptExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof JavaScriptExecutionContextView) {
            return (JavaScriptExecutionContextView) ctx;
        }
        return new JavaScriptExecutionContextView(ctx);
    }

    /**
     * Override the default npm registry for all packages, taking precedence over any
     * {@code registry=} entry in the project's {@code .npmrc}.
     */
    public JavaScriptExecutionContextView setNpmDefaultRegistry(@Nullable String registryUrl) {
        putMessage(NPM_DEFAULT_REGISTRY, registryUrl);
        return this;
    }

    public @Nullable String getNpmDefaultRegistry() {
        return getMessage(NPM_DEFAULT_REGISTRY);
    }

    /**
     * Override registries per scope (e.g. {@code "@myorg" -> "https://npm.example.com/"}),
     * taking precedence over {@code @scope:registry=} entries in the project's {@code .npmrc}.
     */
    public JavaScriptExecutionContextView setNpmScopedRegistries(@Nullable Map<String, String> scopeToRegistryUrl) {
        putMessage(NPM_SCOPED_REGISTRIES, scopeToRegistryUrl);
        return this;
    }

    public @Nullable Map<String, String> getNpmScopedRegistries() {
        return getMessage(NPM_SCOPED_REGISTRIES);
    }

    /**
     * Credentials merged into discovered registries by hostname, taking precedence
     * over credentials found in {@code .npmrc} files.
     */
    public JavaScriptExecutionContextView setRegistryCredentials(List<NpmRegistryCredentials> credentials) {
        putMessage(NPM_REGISTRY_CREDENTIALS, credentials);
        return this;
    }

    public List<NpmRegistryCredentials> getRegistryCredentials() {
        return getMessage(NPM_REGISTRY_CREDENTIALS, emptyList());
    }
}
