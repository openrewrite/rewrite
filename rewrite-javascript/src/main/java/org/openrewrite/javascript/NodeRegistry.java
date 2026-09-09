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

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

/**
 * A single npm registry after environment-variable expansion and credential resolution.
 * A scoped registry ({@code @scope:registry}) carries its {@code scope}; the default
 * registry has a null scope.
 */
@Value
@With
public class NodeRegistry {

    /**
     * The {@code @scope} this registry serves (e.g. {@code @angular}), or null for the default registry.
     */
    @Nullable
    String scope;

    /**
     * Registry base URL. Any userinfo has been stripped and surfaced as {@link #username}/{@link #password}.
     */
    String url;

    /**
     * Bearer token ({@code _authToken}), if any.
     */
    @Nullable
    String authToken;

    @Nullable
    String username;

    /**
     * Password already decoded from npm's base64 {@code _password}, if any.
     */
    @Nullable
    String password;

    /**
     * Pre-encoded basic credential from npm's {@code _auth}, used verbatim as {@code Authorization: Basic <value>}.
     */
    @Nullable
    String authBase64;

    /**
     * True when {@code always-auth} demands credentials be sent preemptively.
     */
    boolean alwaysAuth;

    /**
     * Path to a custom CA bundle ({@code cafile}). Carried for the engine; honoring it requires a
     * TLS-capable {@code HttpSender}, which the default one is not.
     */
    @Nullable
    String cafile;

    /**
     * npm's {@code strict-ssl}; defaults to true.
     */
    boolean strictSsl;

    /**
     * True when the URL or credentials still contain {@code ${VAR}} placeholders whose variables were
     * unset; using such a registry is a configuration failure and the client refuses to fetch it.
     */
    boolean unresolvedPlaceholders;
}
