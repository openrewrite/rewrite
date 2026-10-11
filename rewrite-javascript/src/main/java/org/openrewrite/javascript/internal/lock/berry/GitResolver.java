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

import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of @yarnpkg/plugin-git sources/GitResolver.ts and gitUtils.ts {@code isGitUrl} (Yarn 4.10.3).
 */
final class GitResolver implements Resolver {

    private static final List<Pattern> GIT_PATTERNS = Arrays.asList(
            Pattern.compile("^ssh:"),
            Pattern.compile("^git(?:\\+[^:]+)?:"),
            Pattern.compile("^(?:git\\+)?https?:[^#]+/[^#]+(?:\\.git)(?:#.*)?$"),
            Pattern.compile("^git@[^#]+/[^#]+\\.git(?:#.*)?$"),
            Pattern.compile("^(?:github:|https://github\\.com/)?(?!\\.{1,2}/)([a-zA-Z._0-9-]+)/(?!\\.{1,2}(?:#|$))" +
                            "([a-zA-Z._0-9-]+?)(?:\\.git)?(?:#.*)?$"),
            Pattern.compile("^https://github\\.com/(?!\\.{1,2}/)([a-zA-Z0-9._-]+)/(?!\\.{1,2}(?:#|$))" +
                            "([a-zA-Z0-9._-]+?)/tarball/(.+)?$"));

    static boolean isGitUrl(String url) {
        if (url.isEmpty()) {
            return false;
        }
        for (Pattern pattern : GIT_PATTERNS) {
            if (pattern.matcher(url).find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts) {
        return isGitUrl(descriptor.range);
    }

    @Override
    public boolean supportsLocator(Locator locator, ResolveOptions opts) {
        return isGitUrl(locator.reference);
    }

    @Override
    public boolean shouldPersistResolution(Locator locator, ResolveOptions opts) {
        return true;
    }

    @Override
    public Descriptor bindDescriptor(Descriptor descriptor, Locator fromLocator, ResolveOptions opts) {
        return descriptor;
    }

    @Override
    public Map<String, Descriptor> getResolutionDependencies(Descriptor descriptor, ResolveOptions opts) {
        return Collections.emptyMap();
    }

    @Override
    public List<Locator> getCandidates(Descriptor descriptor, Map<String, Package> dependencies,
                                       ResolveOptions opts) {
        throw new CannotKnow("the commit and archive of " + StructUtils.stringifyDescriptor(descriptor) +
                             " (Yarn asks the git repository)");
    }

    @Override
    public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies,
                                    List<Locator> locators, ResolveOptions opts) {
        return LinkResolver.only(getCandidates(descriptor, dependencies, opts).get(0), locators);
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        throw new CannotKnow("the package.json of " + StructUtils.stringifyLocator(locator) + " (Yarn reads it " +
                             "from the git repository)");
    }
}
