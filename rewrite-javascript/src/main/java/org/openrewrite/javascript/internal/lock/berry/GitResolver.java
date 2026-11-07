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
import org.openrewrite.javascript.internal.lock.Picomatch;
import org.openrewrite.javascript.internal.lock.V8RegExp;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of @yarnpkg/plugin-git sources/GitResolver.ts and gitUtils.ts {@code isGitUrl} (Yarn 4.10.3), with gitUtils'
 * {@code validateRepoUrl} and utils/normalizeRepoUrl.ts (Yarn 4.14.0).
 */
final class GitResolver implements Resolver {

    private static final Pattern GITHUB_ALIAS = Pattern.compile("^(?:github:|https://github\\.com/|" +
            "git://github\\.com/)?(?!\\.{1,2}/)([a-zA-Z0-9._-]+)/(?!\\.{1,2}(?:#|$))([a-zA-Z0-9._-]+?)(?:\\.git)?" +
            "(#.*)?$");
    private static final Pattern GITHUB_TARBALL = Pattern.compile("^https://github\\.com/(?!\\.{1,2}/)" +
            "([a-zA-Z0-9._-]+)/(?!\\.{1,2}(?:#|$))([a-zA-Z0-9._-]+?)/tarball/(.+)?$");

    // micromatch 4.0.5's makeRe under buildIgnorePattern's options: windows false, dot true
    private static final Picomatch.Options MICROMATCH = new Picomatch.Options(Picomatch.Release.V2_3_1, true, false);

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

    /**
     * {@code resolveUrl}, as far as the engine follows it: {@code lsRemote}'s {@code validateRepoUrl}, which from Yarn
     * 4.14.0 blocks a repository {@code approvedGitRepositories} does not match before git runs.
     */
    @Override
    public List<Locator> getCandidates(Descriptor descriptor, Map<String, Package> dependencies,
                                       ResolveOptions opts) {
        Configuration configuration = opts.getProject().configuration;
        if (configuration.getRelease().approvesGitRepositories()) {
            validateRepoUrl(descriptor.range, configuration);
        }
        throw new CannotKnow("the commit and archive of " + StructUtils.stringifyDescriptor(descriptor) +
                             " (Yarn asks the git repository)");
    }

    /**
     * gitUtils {@code validateRepoUrl} (Yarn 4.14.0) on a host whose network settings enable it: the repository
     * {@code approvedGitRepositories} matches, normalized as git reads it.
     *
     * @throws ReportError YN0080 for a repository it does not match
     */
    static String validateRepoUrl(String url, Configuration configuration) {
        String repo = normalizeRepoUrl(url, false);
        int hashIndex = repo.indexOf('#');
        String normalizedRepoUrl = normalizeRepoUrl(hashIndex == -1 ? repo : repo.substring(0, hashIndex), true);
        Pattern approved = buildIgnorePattern(configuration.get("approvedGitRepositories"));
        if (approved == null || !approved.matcher(normalizedRepoUrl).find()) {
            throw new ReportError(ReportError.NETWORK_DISABLED, "Request to '" + normalizedRepoUrl + "' has been " +
                                                                "blocked because it doesn't match any of the " +
                                                                "patterns in 'approvedGitRepositories'");
        }
        return normalizedRepoUrl;
    }

    /**
     * utils/normalizeRepoUrl.ts: GitHub's shorthands and {@code /tarball/} urls as their repository, and with
     * {@code git} the url as git accepts it.
     */
    static String normalizeRepoUrl(String url, boolean git) {
        String normalized = url.replaceFirst("^git\\+https:", "https:");
        normalized = GITHUB_ALIAS.matcher(normalized).replaceFirst("https://github.com/$1/$2.git$3");
        normalized = GITHUB_TARBALL.matcher(normalized).replaceFirst("https://github.com/$1/$2.git#$3");
        if (git) {
            WhatwgUrl parsedUrl = tryParseGitURL(normalized);
            if (parsedUrl != null) {
                normalized = parsedUrl.getHref();
            }
            normalized = normalized.replaceFirst("^git\\+([^:]+):", "$1:");
        }
        return normalized;
    }

    // hosted-git-info-parse.ts tryParseGitURL: the url, else one with an scp-style colon made a slash
    private static @Nullable WhatwgUrl tryParseGitURL(String gitURL) {
        WhatwgUrl url = WhatwgUrl.tryParse(gitURL);
        return url != null ? url : WhatwgUrl.tryParse(correctURL(gitURL));
    }

    private static String correctURL(String gitURL) {
        int firstAt = lastIndexOfBefore(gitURL, "@", "#");
        int lastColonBeforeHash = lastIndexOfBefore(gitURL, ":", "#");
        String corrected = gitURL;
        if (lastColonBeforeHash > firstAt) {
            corrected = corrected.substring(0, lastColonBeforeHash) + "/" + corrected.substring(lastColonBeforeHash + 1);
        }
        if (lastIndexOfBefore(corrected, ":", "#") == -1 && !corrected.contains("//")) {
            corrected = "ssh://" + corrected;
        }
        return corrected;
    }

    // str.lastIndexOf(char, the index of beforeChar or Infinity)
    private static int lastIndexOfBefore(String str, String c, String beforeChar) {
        int startPosition = str.indexOf(beforeChar);
        return str.lastIndexOf(c, startPosition > -1 ? startPosition : Integer.MAX_VALUE);
    }

    // miscUtils.buildIgnorePattern: the patterns micromatch makes (windows false, dot true) as one alternation, read by
    // String.prototype.match as a RegExp; null for none
    private static @Nullable Pattern buildIgnorePattern(@Nullable Object ignorePatterns) {
        if (!(ignorePatterns instanceof List) || ((List<?>) ignorePatterns).isEmpty()) {
            return null;
        }
        List<String> sources = new ArrayList<>();
        for (Object pattern : (List<?>) ignorePatterns) {
            sources.add("(" + Picomatch.source(Js.string(pattern), MICROMATCH) + ")");
        }
        return V8RegExp.of(String.join("|", sources));
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
