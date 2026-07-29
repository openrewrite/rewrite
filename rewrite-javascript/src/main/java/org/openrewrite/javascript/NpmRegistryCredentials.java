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
import org.jspecify.annotations.Nullable;

/**
 * Credentials for an npm registry host, supplied by the host application via
 * {@link JavaScriptExecutionContextView#setRegistryCredentials}. Matched to
 * registries by hostname, the same pattern as Maven's settings credentials.
 * Either a bearer {@code token} or a {@code username}/{@code password} pair.
 */
@Value
public class NpmRegistryCredentials {
    String host;
    @Nullable String token;
    @Nullable String username;
    @Nullable String password;

    public static NpmRegistryCredentials token(String host, String token) {
        return new NpmRegistryCredentials(host, token, null, null);
    }

    public static NpmRegistryCredentials usernamePassword(String host, String username, String password) {
        return new NpmRegistryCredentials(host, null, username, password);
    }
}
