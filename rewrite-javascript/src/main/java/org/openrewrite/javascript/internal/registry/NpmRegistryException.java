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
package org.openrewrite.javascript.internal.registry;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * A structured failure talking to an npm registry, mapping onto the lock
 * regeneration failure reasons rather than surfacing as raw IO errors.
 */
@Getter
public class NpmRegistryException extends RuntimeException {

    public enum Reason {
        AUTH_FAILED,
        NOT_FOUND,
        UNREACHABLE,
        CONFIG
    }

    private final Reason reason;
    private final @Nullable String registryUrl;

    public NpmRegistryException(Reason reason, @Nullable String registryUrl, String message) {
        super(message);
        this.reason = reason;
        this.registryUrl = registryUrl;
    }

    public NpmRegistryException(Reason reason, @Nullable String registryUrl, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.registryUrl = registryUrl;
    }
}
