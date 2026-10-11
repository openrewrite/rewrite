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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import lombok.Value;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.EventLoop.Promise;

import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.V8RegExp.DOT;

/**
 * Port of yarn src/resolvers/exotics/git-resolver.js, hosted-git-resolver.js (with github, gitlab and bitbucket) and
 * gist-resolver.js (yarn 1.22.22): a locked git pattern is reused, any other needs the repository.
 */
final class GitResolver {

    private static final Pattern STRIP_HASH = Pattern.compile("(" + DOT + "*?)#" + DOT + "*");
    private static final Pattern STRIP_PROTOCOL = Pattern.compile(DOT + "*:(" + DOT + "*)");
    private static final Pattern GIT_SUFFIX = Pattern.compile(DOT + "git\\z");

    private final PackageRequest request;
    private final String fragment;
    private final String remoteType;

    private GitResolver(PackageRequest request, String fragment, String remoteType) {
        this.request = request;
        this.fragment = fragment;
        this.remoteType = remoteType;
    }

    static GitResolver git(PackageRequest request, String fragment) {
        return new GitResolver(request, fragment, "git");
    }

    /**
     * hosted-git-resolver.js: the fragment exploded into user, repo and hash, and a lock entry reused as a tarball.
     */
    static GitResolver hosted(PackageRequest request, String fragment) {
        explodeHostedGitFragment(fragment);
        return new GitResolver(request, fragment, "tarball");
    }

    static GitResolver gist(PackageRequest request, String fragment) {
        String unprefixed = fragment.startsWith("gist:") ? fragment.substring("gist:".length()) : fragment;
        String[] parts = unprefixed.split("#", -1);
        if (parts.length > 2) {
            throw new MessageError(Lang.format("Invalid gist fragment $0.", unprefixed));
        }
        return git(request, "https://gist.github.com/" + parts[0] + ".git#" + (parts.length > 1 ? parts[1] : ""));
    }

    @Value
    static class ExplodedFragment {
        String user;
        String repo;
        String hash;
    }

    static ExplodedFragment explodeHostedGitFragment(String fragment) {
        int hashPosition = fragment.indexOf('#');
        String hash = hashPosition == -1 ? "" : fragment.substring(hashPosition + 1);
        String[] preParts = fragment.split("@", -1);
        if (preParts.length > 2) {
            fragment = preParts[1] + "@" + preParts[2];
        }
        String stripped = STRIP_HASH.matcher(fragment).replaceFirst("$1");
        stripped = STRIP_PROTOCOL.matcher(stripped).replaceFirst("$1");
        stripped = GIT_SUFFIX.matcher(stripped).replaceFirst("");
        String[] parts = stripped.split("/", -1);
        if (parts.length < 2) {
            throw new MessageError(Lang.format("Invalid hosted git fragment $0.", fragment));
        }
        return new ExplodedFragment(parts[parts.length - 2], parts[parts.length - 1], hash);
    }

    Promise<Manifest> resolve() {
        Manifest shrunk = request.getLocked(remoteType);
        if (shrunk != null) {
            return Promise.resolved(request.resolver.loop, shrunk);
        }
        throw new CannotKnow("git " + fragment);
    }
}
