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
 * A failure talking to an npm registry, carrying enough structure for the lock engine to
 * map it onto a per-package recipe failure. The reason set is kept distinct from the
 * engine's lock-failure taxonomy so the client does not depend on it.
 */
@Getter
public class NodeRegistryException extends RuntimeException {

    public enum Reason {
        UNREACHABLE,
        AUTH_FAILED,
        PACKAGE_NOT_FOUND,
        VERSION_NOT_FOUND,
        MALFORMED_MANIFEST
    }

    private final Reason reason;
    private final String registryUrl;

    @Nullable
    private final String packageName;

    @Nullable
    private final String version;

    public NodeRegistryException(Reason reason, String registryUrl, String message) {
        this(reason, registryUrl, null, null, message, null);
    }

    public NodeRegistryException(Reason reason, String registryUrl, String message, @Nullable Throwable cause) {
        this(reason, registryUrl, null, null, message, cause);
    }

    public NodeRegistryException(Reason reason, String registryUrl, @Nullable String packageName,
                                 @Nullable String version, String message, @Nullable Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.registryUrl = registryUrl;
        this.packageName = packageName;
        this.version = version;
    }
}
