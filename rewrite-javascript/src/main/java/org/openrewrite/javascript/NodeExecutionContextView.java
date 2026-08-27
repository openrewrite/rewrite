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
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Lets a host inject npm registries and credentials for native lock-file regeneration, and
 * shares a single {@link NpmRegistryClient} over the run's {@code HttpSender}. Modeled on
 * {@code MavenExecutionContextView}.
 */
public class NodeExecutionContextView extends DelegatingExecutionContext {
    private static final String REGISTRIES = "org.openrewrite.javascript.registries";
    private static final String CREDENTIALS = "org.openrewrite.javascript.registryCredentials";
    private static final String REGISTRY_CLIENT = "org.openrewrite.javascript.registryClient";

    public NodeExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static NodeExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof NodeExecutionContextView) {
            return (NodeExecutionContextView) ctx;
        }
        return new NodeExecutionContextView(ctx);
    }

    /**
     * Host-supplied registries: the full-override channel. When non-empty, discovery is skipped
     * entirely and these registries are used exactly as given, including their credentials.
     */
    public NodeExecutionContextView setRegistries(List<NodeRegistry> registries) {
        putMessage(REGISTRIES, registries);
        return this;
    }

    public List<NodeRegistry> getRegistries() {
        return getMessage(REGISTRIES, emptyList());
    }

    /**
     * Host-supplied credentials, matched by host against discovered registries whose configuration
     * embeds none. Ignored when {@link #setRegistries(List)} supplies the registries outright.
     */
    public NodeExecutionContextView setRegistryCredentials(List<NodeRegistryCredentials> credentials) {
        putMessage(CREDENTIALS, credentials);
        return this;
    }

    public List<NodeRegistryCredentials> getRegistryCredentials() {
        return getMessage(CREDENTIALS, emptyList());
    }

    /**
     * A per-run {@link NpmRegistryClient} over the run's {@code HttpSender}, lazily created and shared
     * so its packument/manifest caches are reused across the execution.
     */
    public NpmRegistryClient getRegistryClient() {
        return (NpmRegistryClient) getMessages().computeIfAbsent(REGISTRY_CLIENT,
                k -> new NpmRegistryClient(HttpSenderExecutionContextView.view(this).getHttpSender()));
    }
}
