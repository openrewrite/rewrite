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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openrewrite.javascript.internal.lock.YarnpkgSemverUtils;

import static org.openrewrite.javascript.internal.lock.V8RegExp.DOT;

/**
 * Port of @yarnpkg/core sources/LegacyMigrationResolver.ts (Yarn 4.10.3).
 */
final class LegacyMigrationResolver implements Resolver {

    /**
     * {@code IMPORTED_PATTERNS}: each pattern with the reference it makes from the entry's version and the match.
     */
    static final List<Map.Entry<Pattern, BiFunction<String, Matcher, String>>> IMPORTED_PATTERNS = Arrays.asList(
            pattern("^(git(?:\\+(?:https|ssh))?://" + DOT + "*(?:\\.git)?)#(" + DOT + "*)\\z",
                    (version, m) -> m.group(1) + "#commit=" + m.group(2)),
            pattern("^https://((?:[^/]+?)@)?codeload\\.github\\.com/([^/]+/[^/]+)/tar\\.gz/([0-9a-f]+)\\z",
                    (version, m) -> "https://" + orEmpty(m.group(1)) + "github.com/" + m.group(2) + ".git#commit=" +
                                    m.group(3)),
            pattern("^https://((?:[^/]+?)@)?github\\.com/([^/]+/[^/]+?)(?:\\.git)?#([0-9a-f]+)\\z",
                    (version, m) -> "https://" + orEmpty(m.group(1)) + "github.com/" + m.group(2) + ".git#commit=" +
                                    m.group(3)),
            pattern("^https?://[^/]+/(?:[^/]+/)*(?:@" + DOT + "+(?:/|(?:%2f)))?([^/]+)/(?:-|download)/\\1-[^/]+\\.tgz" +
                    "(?:#|\\z)", (version, m) -> "npm:" + version),
            pattern("^https://npm\\.pkg\\.github\\.com/download/(?:@[^/]+)/(?:[^/]+)/(?:[^/]+)/(?:[0-9a-f]+)(?:#|\\z)",
                    (version, m) -> "npm:" + version),
            pattern("^https://npm\\.fontawesome\\.com/(?:@[^/]+)/([^/]+)/-/([^/]+)/\\1-\\2" + DOT + "tgz(?:#|\\z)",
                    (version, m) -> "npm:" + version),
            pattern("^https?://[^/]+/" + DOT + "*/(@[^/]+)/([^/]+)/-/\\1/\\2-(?:[.\\d\\w-]+)\\.tgz(?:#|\\z)",
                    (version, m) -> StructUtils.makeRange("npm:", null, version,
                            Collections.singletonMap("__archiveUrl", m.group(0)))),
            pattern("^[^/]+\\.tgz#[0-9a-f]+\\z", (version, m) -> "npm:" + version));

    private @Nullable Map<String, Locator> resolutions;

    private final Resolver resolver;

    LegacyMigrationResolver(Resolver resolver) {
        this.resolver = resolver;
    }

    private static Map.Entry<Pattern, BiFunction<String, Matcher, String>> pattern(
            String regex, BiFunction<String, Matcher, String> matcher) {
        return new AbstractMap.SimpleImmutableEntry<>(Pattern.compile(regex), matcher);
    }

    private static String orEmpty(@Nullable String group) {
        return group == null ? "" : group;
    }

    /**
     * {@code setup}: nothing for a missing or modern lockfile; otherwise every entry with a {@code resolved} URL
     * an imported pattern matches is pinned, and the rest are reported.
     */
    void setup(Project project) {
        Map<String, @Nullable Object> parsed = project.parsedLockfile;
        if (parsed == null) {
            return;
        }
        if (parsed.containsKey("__metadata")) {
            return;
        }
        Map<String, Locator> resolutions = this.resolutions = new LinkedHashMap<>();
        for (String key : Js.keys(parsed)) {
            Descriptor parsedDescriptor = StructUtils.tryParseDescriptor(key);
            if (parsedDescriptor == null) {
                continue;
            }
            Descriptor descriptor = YarnpkgSemverUtils.validRange(parsedDescriptor.range) != null ?
                    StructUtils.makeDescriptor(parsedDescriptor, "npm:" + parsedDescriptor.range) :
                    parsedDescriptor;
            Object entry = parsed.get(key);
            Object version = Js.get(entry, "version");
            Object resolved = Js.get(entry, "resolved");
            if (!Js.truthy(resolved)) {
                continue;
            }
            String reference = null;
            for (Map.Entry<Pattern, BiFunction<String, Matcher, String>> imported : IMPORTED_PATTERNS) {
                Matcher match = imported.getKey().matcher(Js.string(resolved));
                if (match.find()) {
                    reference = imported.getValue().apply(version == Syml.UNDEFINED ? "undefined" :
                            Js.string(version), match);
                    break;
                }
            }
            if (reference == null) {
                continue;
            }
            Ident actualDescriptor = descriptor;
            try {
                StructUtils.Range parsedRange = StructUtils.parseRange(descriptor.range);
                Descriptor potentialDescriptor = StructUtils.tryParseDescriptor(parsedRange.getSelector(), true);
                if (potentialDescriptor != null) {
                    actualDescriptor = potentialDescriptor;
                }
            } catch (RuntimeException ignored) {
                // Yarn's own `catch {}`
            }
            resolutions.put(descriptor.descriptorHash, StructUtils.makeLocator(actualDescriptor, reference));
        }
    }

    @Override
    public boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts) {
        return resolutions != null && resolutions.containsKey(descriptor.descriptorHash);
    }

    @Override
    public boolean supportsLocator(Locator locator, ResolveOptions opts) {
        return false;
    }

    @Override
    public boolean shouldPersistResolution(Locator locator, ResolveOptions opts) {
        throw new JsError(JsError.ERROR, "Assertion failed: This resolver doesn't support resolving locators to " +
                                         "packages");
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
        if (resolutions == null) {
            throw new JsError(JsError.ERROR, "Assertion failed: The resolution store should have been setup");
        }
        Locator resolution = resolutions.get(descriptor.descriptorHash);
        if (resolution == null) {
            throw new JsError(JsError.ERROR, "Assertion failed: The resolution should have been registered");
        }
        Descriptor importedDescriptor = StructUtils.convertLocatorToDescriptor(resolution);
        Descriptor normalizedDescriptor = opts.getProject().configuration.normalizeDependency(importedDescriptor);
        return resolver.getCandidates(normalizedDescriptor, dependencies, opts);
    }

    @Override
    public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies,
                                    List<Locator> locators, ResolveOptions opts) {
        Locator locator = getCandidates(descriptor, dependencies, opts).get(0);
        List<Locator> out = new ArrayList<>();
        for (Locator candidate : locators) {
            if (candidate.locatorHash.equals(locator.locatorHash)) {
                out.add(candidate);
            }
        }
        return new Satisfying(out, false);
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        throw new JsError(JsError.ERROR, "Assertion failed: This resolver doesn't support resolving locators to " +
                                         "packages");
    }
}
