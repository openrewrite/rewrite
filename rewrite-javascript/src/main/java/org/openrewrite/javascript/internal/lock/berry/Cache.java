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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.function.Supplier;

/**
 * Port of @yarnpkg/core sources/Cache.ts {@code getCacheKey} and {@code fetchPackageFromCache} (Yarn 4.10.3).
 */
final class Cache {

    static final int CACHE_VERSION = 10;

    private Cache() {
    }

    static String getCacheKey(BerryOptions options) {
        Object compressionLevel = options.get("compressionLevel");
        String cacheSpec = !"mixed".equals(compressionLevel) ? "c" + Js.string(compressionLevel) : "";
        return CACHE_VERSION + cacheSpec;
    }

    /**
     * {@code fetchPackageFromCache} for a locator the lockfile has no checksum for: a package whose presence is
     * conditional has its checksum hidden, any other gets the checksum of the archive the loader makes.
     */
    static @Nullable String fetchPackageFromCache(Locator locator, Supplier<String> loader, Project project) {
        if (project.conditionalLocators.contains(locator.locatorHash)) {
            return null;
        }
        return loader.get();
    }

    /**
     * {@code <cacheKey>/<sha512>} of the archive {@code NpmSemverFetcher} makes from the tarball: the package under
     * {@code node_modules/<ident>}, the tarball's first path component stripped.
     */
    static String checksum(byte[] tgz, Ident ident, BerryOptions options) {
        Object compressionLevel = options.get("compressionLevel");
        if (!Integer.valueOf(0).equals(compressionLevel)) {
            String name = ident instanceof Locator ? StructUtils.stringifyLocator((Locator) ident) :
                    StructUtils.stringifyIdent(ident);
            throw new CannotKnow("the checksum of " + name + " under compressionLevel " + Js.string(compressionLevel) +
                                 " (zlib-ng deflate)");
        }
        byte[] zip = TgzUtils.convertToZip(tgz, StructUtils.getIdentVendorPath(ident), 1).toByteArray();
        return getCacheKey(options) + "/" + HashUtils.sha512Hex(zip);
    }
}
