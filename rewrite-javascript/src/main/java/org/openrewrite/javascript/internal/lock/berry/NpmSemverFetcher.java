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

import java.util.Map;
import org.openrewrite.javascript.internal.lock.YarnpkgSemverUtils;

/**
 * Port of @yarnpkg/plugin-npm sources/NpmSemverFetcher.ts (Yarn 4.10.3).
 */
final class NpmSemverFetcher implements Fetcher {

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
        return params == null || !params.containsKey("__archiveUrl");
    }

    @Override
    public @Nullable String fetch(Locator locator, FetchOptions opts) {
        Project project = opts.getProject();
        return Cache.fetchPackageFromCache(locator, () -> {
            byte[] tgz = fetchFromNetwork(locator, opts);
            return () -> Cache.checksum(tgz, locator, project.configuration);
        }, project);
    }

    private static byte[] fetchFromNetwork(Locator locator, FetchOptions opts) {
        String version = YarnpkgSemverUtils.clean(locator.reference.substring(NpmHttpUtils.PROTOCOL.length()));
        String url = NpmHttpUtils.getLocatorUrl(locator, opts.getProject().configuration.getRelease());
        try {
            return NpmHttpUtils.get(url, locator, String.valueOf(version), opts.getProject(), opts.getRegistry());
        } catch (ReportError e) {
            return NpmHttpUtils.get(url.replace("%2f", "/"), locator, String.valueOf(version), opts.getProject(),
                    opts.getRegistry());
        }
    }
}
