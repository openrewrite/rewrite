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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Port of @yarnpkg/core sources/MultiFetcher.ts (Yarn 4.10.3).
 */
final class MultiFetcher implements Fetcher {

    private final List<Fetcher> fetchers;

    MultiFetcher(Fetcher... fetchers) {
        this.fetchers = new ArrayList<>(Arrays.asList(fetchers));
    }

    @Override
    public boolean supports(Locator locator, FetchOptions opts) {
        return tryFetcher(locator, opts) != null;
    }

    @Override
    public @Nullable String fetch(Locator locator, FetchOptions opts) {
        Fetcher fetcher = tryFetcher(locator, opts);
        if (fetcher == null) {
            throw new ReportError(ReportError.FETCHER_NOT_FOUND, StructUtils.stringifyLocator(locator) + " isn't " +
                                                                 "supported by any available fetcher");
        }
        return fetcher.fetch(locator, opts);
    }

    private @Nullable Fetcher tryFetcher(Locator locator, FetchOptions opts) {
        for (Fetcher fetcher : fetchers) {
            if (fetcher.supports(locator, opts)) {
                return fetcher;
            }
        }
        return null;
    }
}
