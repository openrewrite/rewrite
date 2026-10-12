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
package org.openrewrite.javascript.internal.lock.npm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.GitHost;
import org.openrewrite.javascript.internal.lock.Spec;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/consistent-resolve.js (npm 11.17.0).
 */
final class ConsistentResolve {

    private static final GitHost.Opts HOSTED_OPT = GitHost.Opts.of(null, false);

    private ConsistentResolve() {
    }

    static @Nullable Object consistentResolve(@Nullable Object resolved, NpmRelease release) {
        return consistentResolve(resolved, null, null, false, release);
    }

    // a resolved of another JSON type comes back untouched: npa throws on it and the catch returns the input
    static @Nullable Object consistentResolve(@Nullable Object resolved, @Nullable String fromPath,
                                              @Nullable String toPath, boolean relPaths, NpmRelease release) {
        if (!truthy(resolved)) {
            return null;
        }
        try {
            return consistentResolve(Spec.npa(resolved, fromPath, release.npmPackageArg()), toPath, relPaths,
                    release);
        } catch (RuntimeException e) {
            return resolved;
        }
    }

    static @Nullable String consistentResolve(Spec resolved, @Nullable String fromPath,
                                              @Nullable String toPath, boolean relPaths, NpmRelease release) {
        try {
            return consistentResolve(Spec.npa(resolved, fromPath, release.npmPackageArg()), toPath, relPaths,
                    release);
        } catch (RuntimeException e) {
            return resolved.toString();
        }
    }

    /**
     * Arborist 9.0.0's {@code path.replace(/#/g, '%23')} wherever a path becomes a {@code file:} spec, which the
     * npm-package-arg beside it needs to read the path whole.
     */
    static String escapeHash(String path, NpmRelease release) {
        return release.escapesHashOfFilePaths() ? path.replace("#", "%23") : path;
    }

    private static @Nullable String consistentResolve(Spec spec, @Nullable String toPath, boolean relPaths,
                                                      NpmRelease release) {
        Spec.Type type = spec.getType();
        if (type == Spec.Type.FILE || type == Spec.Type.DIRECTORY) {
            String cleanFetchSpec = spec.getFetchSpec() == null ? null : escapeHash(spec.getFetchSpec(), release);
            if (relPaths && toPath != null && !toPath.isEmpty()) {
                return "file:" + Relpath.relpath(toPath, cleanFetchSpec);
            }
            return "file:" + cleanFetchSpec;
        }
        GitHost hosted = spec.getHosted();
        if (hosted != null) {
            // before npm 7.4.3 every hosted git URL resolves to ssh, credentials or not
            boolean withAuth = hosted.getAuth() != null && !hosted.getAuth().isEmpty() &&
                               release.keepsHttpsOfHostedGitWithAuth();
            return "git+" + (withAuth ||
                             "https".equals(hosted.getDefaultRepresentation()) && release.keepsHostedGitHttps() ?
                    hosted.https(HOSTED_OPT) : hosted.sshurl(HOSTED_OPT));
        }
        if (type == Spec.Type.GIT) {
            return spec.getSaveSpec();
        }
        if (!release.returnsRawOfStarSpec()) {
            // always return something.  'foo' is interpreted as 'foo@' otherwise.
            return spec.getRawSpec().isEmpty() && !spec.getRaw().endsWith("@") ? spec.getRaw() : spec.getRawSpec();
        }
        if ("*".equals(spec.getRawSpec())) {
            return spec.getRaw();
        }
        return spec.getRawSpec();
    }
}
