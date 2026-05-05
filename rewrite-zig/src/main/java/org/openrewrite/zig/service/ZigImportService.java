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
package org.openrewrite.zig.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.service.ImportService;

/**
 * Zig-specific import service. Translates import requests for Zig's
 * {@code @import("module")} syntax.
 */
public class ZigImportService extends ImportService {

    @Override
    public <P> JavaVisitor<P> addImportVisitor(@Nullable String packageName,
                                               String typeName,
                                               @Nullable String member,
                                               @Nullable String alias,
                                               boolean onlyIfReferenced) {
        // Return a no-op visitor so cross-language recipes gracefully skip Zig files
        // rather than crashing. Full import management to be implemented later.
        return new JavaVisitor<P>() {};
    }
}
