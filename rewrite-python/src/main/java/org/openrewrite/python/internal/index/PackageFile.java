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
package org.openrewrite.python.internal.index;

import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * One distribution file of a package as listed by the Simple Repository API.
 */
@Value
public class PackageFile {
    String filename;

    /**
     * Absolute download URL, resolved against the listing page.
     */
    String url;

    @Nullable
    String sha256;

    @Nullable
    String requiresPython;

    /**
     * Whether a PEP 658/714 metadata sidecar is available; null when the index does not say.
     */
    @Nullable
    Boolean coreMetadataAvailable;

    boolean yanked;

    /**
     * PEP 700 file size in bytes; null on PEP 503 HTML listings.
     */
    @Nullable
    Long size;

    /**
     * PEP 700 {@code upload-time}, an ISO 8601 timestamp; null on PEP 503 HTML listings.
     */
    @Nullable
    String uploadTime;
}
