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
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.Map;
import org.openrewrite.javascript.internal.lock.YarnpkgSemverUtils;

/**
 * Port of @yarnpkg/plugin-npm sources/NpmHttpFetcher.ts (Yarn 4.10.3).
 */
final class NpmHttpFetcher implements Fetcher {

    @Override
    public boolean supports(Locator locator, FetchOptions opts) {
        if (!locator.reference.startsWith(NpmHttpUtils.PROTOCOL)) {
            return false;
        }
        StructUtils.Range range = StructUtils.parseRange(locator.reference);
        if (!YarnpkgSemverUtils.valid(range.getSelector())) {
            return false;
        }
        Map<String, Object> params = range.getParams();
        return params != null && params.get("__archiveUrl") instanceof String;
    }

    @Override
    public @Nullable String fetch(Locator locator, FetchOptions opts) {
        Project project = opts.getProject();
        return Cache.fetchPackageFromCache(locator, () -> {
            StructUtils.Range range = StructUtils.parseRange(locator.reference);
            Object archiveUrl = range.getParams() == null ? null : range.getParams().get("__archiveUrl");
            if (!(archiveUrl instanceof String)) {
                throw new JsError(JsError.ERROR, "Assertion failed: The archiveUrl querystring parameter should " +
                                                 "have been available");
            }
            byte[] tgz = NpmHttpUtils.get((String) archiveUrl, locator, range.getSelector(), project,
                    opts.getRegistry());
            return () -> Cache.checksum(tgz, locator, project.configuration);
        }, project);
    }
}
