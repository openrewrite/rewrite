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
package org.openrewrite.javascript.internal.lock.berry;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Port of @yarnpkg/plugin-file sources/fileUtils.ts (Yarn 4.10.3), up to the files it reads.
 */
final class FileUtils {

    private FileUtils() {
    }

    @Value
    static class Spec {
        @Nullable
        Locator parentLocator;

        String path;
    }

    static Spec parseSpec(String spec) {
        StructUtils.Range range = StructUtils.parseRange(spec);
        Object locator = range.getParams() == null ? null : range.getParams().get("locator");
        return new Spec(locator instanceof String ? StructUtils.parseLocator((String) locator) : null,
                range.getSelector());
    }

    static String makeSpec(@Nullable Locator parentLocator, String path, @Nullable String hash, String protocol) {
        Map<String, String> params = new LinkedHashMap<>();
        if (hash != null) {
            params.put("hash", hash);
        }
        if (parentLocator != null) {
            params.put("locator", StructUtils.stringifyLocator(parentLocator));
        }
        return StructUtils.makeRange(protocol, path, path, params);
    }

    static Locator makeLocator(Ident ident, @Nullable Locator parentLocator, String path, @Nullable String hash,
                               String protocol) {
        return StructUtils.makeLocator(ident, makeSpec(parentLocator, path, hash, protocol));
    }

    /**
     * The resolvers' {@code getCandidates} up to the archive: the bound spec, then the bindings
     * {@code fetchArchiveFromLocator} and {@code makeArchiveFromLocator} read before the file or folder.
     */
    static void readBindings(Descriptor descriptor, @Nullable String hash, String protocol) {
        Spec spec = parseSpec(descriptor.range);
        if (spec.getParentLocator() == null) {
            throw new JsError(JsError.ERROR, "Assertion failed: The descriptor should have been bound");
        }
        StructUtils.parseFileStyleRange(makeLocator(descriptor, spec.getParentLocator(), spec.getPath(), hash,
                protocol).reference, protocol);
    }
}
