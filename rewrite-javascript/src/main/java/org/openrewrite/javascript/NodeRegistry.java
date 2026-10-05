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

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * A single npm registry after environment-variable expansion and credential resolution.
 * A scoped registry ({@code @scope:registry}) carries its {@code scope}; the default
 * registry has a null scope.
 */
@Value
@With
@AllArgsConstructor
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
     * True when the URL still contains {@code ${VAR}} placeholders whose variables were unset; there is
     * no usable URL to fall back to, so the client refuses to fetch it.
     */
    boolean unresolvedPlaceholders;

    /**
     * Placeholders, verbatim (e.g. {@code ${NPM_TOKEN}}), in {@code .npmrc} credentials for this registry
     * whose variables were unset. Like npm, those credentials are still sent as written, and the placeholders
     * are reported if the registry rejects them. Typical of an LST built where the variables were set and run
     * where they are not.
     */
    List<String> unresolvedCredentialPlaceholders;

    public NodeRegistry(@Nullable String scope, String url, @Nullable String authToken, @Nullable String username,
                        @Nullable String password, @Nullable String authBase64, boolean alwaysAuth,
                        @Nullable String cafile, boolean strictSsl, boolean unresolvedPlaceholders) {
        this(scope, url, authToken, username, password, authBase64, alwaysAuth, cafile, strictSsl,
                unresolvedPlaceholders, emptyList());
    }
}
