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

/**
 * Port of @yarnpkg/plugin-patch sources/PatchFetcher.ts (Yarn 4.10.3).
 */
final class PatchFetcher implements Fetcher {

    @Override
    public boolean supports(Locator locator, FetchOptions opts) {
        return PatchUtils.isPatchLocator(locator);
    }

    @Override
    public @Nullable String fetch(Locator locator, FetchOptions opts) {
        return Cache.fetchPackageFromCache(locator, () -> {
            throw new CannotKnow("the checksum of " + StructUtils.stringifyLocator(locator) + " (Yarn applies the " +
                                 "patch to the source's archive)");
        }, opts.getProject());
    }
}
