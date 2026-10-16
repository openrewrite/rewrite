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

/**
 * Port of @yarnpkg/plugin-link sources/LinkFetcher.ts and PortalFetcher.ts (Yarn 4.10.3).
 */
final class LinkFetcher implements Fetcher {

    @Override
    public boolean supports(Locator locator, FetchOptions opts) {
        return locator.reference.startsWith(LinkResolver.PROTOCOL) ||
               locator.reference.startsWith(PortalResolver.PROTOCOL);
    }

    /**
     * The bindings either fetcher parses before the target, which a soft link has no checksum for.
     */
    @Override
    public @Nullable String fetch(Locator locator, FetchOptions opts) {
        StructUtils.parseFileStyleRange(locator.reference, locator.reference.startsWith(LinkResolver.PROTOCOL) ?
                LinkResolver.PROTOCOL : PortalResolver.PROTOCOL);
        return null;
    }
}
