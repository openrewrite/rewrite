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
package org.openrewrite.javascript.internal.lock.npm;

/**
 * A root package.json arborist cannot build a node from (npm writes an empty lock), or a {@code $name} override
 * naming nothing on an edge the build reaches (npm writes none); the message names what was refused.
 */
public final class MalformedManifest extends IllegalStateException {
    public MalformedManifest(String message) {
        super(message);
    }

    public MalformedManifest(String message, Throwable cause) {
        super(message, cause);
    }
}
