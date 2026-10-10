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
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.function.Supplier;

/**
 * Port of @yarnpkg/core sources/Cache.ts {@code getCacheKey} and {@code fetchPackageFromCache} (Yarn 4.10.3).
 */
final class Cache {

    static final int CACHE_VERSION = 10;

    private Cache() {
    }

    static String getCacheKey(Configuration options) {
        Object compressionLevel = options.get("compressionLevel");
        String cacheSpec = !"mixed".equals(compressionLevel) ? "c" + Js.string(compressionLevel) : "";
        return CACHE_VERSION + cacheSpec;
    }

    /**
     * What a fetcher's loader brings back from the network: the archive, whose checksum the cache computes.
     */
    interface Archive {
        String checksum();
    }

    /**
     * {@code fetchPackageFromCache} for a locator without a stored checksum: a disabled package is mocked, a new
     * conditional one loaded with its checksum hidden (a locked one's entry stands in), any other one checksummed.
     */
    static @Nullable String fetchPackageFromCache(Locator locator, Supplier<Archive> loader, Project project) {
        if (project.disabledLocators.contains(locator.locatorHash)) {
            return null;
        }
        if (project.conditionalLocators.contains(locator.locatorHash)) {
            if (!project.lockfileHeld(locator)) {
                loadConditional(locator, loader, project);
            }
            return null;
        }
        return loader.get().checksum();
    }

    // where the archive fails, another machine the host could be would mock the package instead
    private static void loadConditional(Locator locator, Supplier<Archive> loader, Project project) {
        try {
            loader.get();
        } catch (ReportError | JsError e) {
            if (!project.hostDependentLocators.contains(locator.locatorHash)) {
                throw e;
            }
            Package pkg = project.storedPackages.get(locator.locatorHash);
            throw new CannotKnow("whether Yarn fetches " + StructUtils.stringifyLocator(locator) + " (conditions " +
                                 (pkg == null ? "" : Js.string(pkg.conditions)) + ", on a platform the host does " +
                                 "not give), whose archive fails: " + e.getMessage());
        }
    }

    /**
     * {@code <cacheKey>/<sha512>} of the archive {@code NpmSemverFetcher} makes from the tarball: the package under
     * {@code node_modules/<ident>}, the tarball's first path component stripped.
     */
    static String checksum(byte[] tgz, Ident ident, Configuration options) {
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
