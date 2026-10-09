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

    static @Nullable Object consistentResolve(@Nullable Object resolved) {
        return consistentResolve(resolved, null, null, false);
    }

    // a resolved of another JSON type comes back untouched: npa throws on it and the catch returns the input
    static @Nullable Object consistentResolve(@Nullable Object resolved, @Nullable String fromPath,
                                              @Nullable String toPath, boolean relPaths) {
        if (!truthy(resolved)) {
            return null;
        }
        try {
            return consistentResolve(Spec.npa(resolved, fromPath), toPath, relPaths);
        } catch (RuntimeException e) {
            return resolved;
        }
    }

    static @Nullable String consistentResolve(Spec resolved, @Nullable String fromPath,
                                              @Nullable String toPath, boolean relPaths) {
        try {
            return consistentResolve(Spec.npa(resolved, fromPath), toPath, relPaths);
        } catch (RuntimeException e) {
            return resolved.toString();
        }
    }

    private static @Nullable String consistentResolve(Spec spec, @Nullable String toPath, boolean relPaths) {
        Spec.Type type = spec.getType();
        if (type == Spec.Type.FILE || type == Spec.Type.DIRECTORY) {
            if (relPaths && toPath != null && !toPath.isEmpty()) {
                return "file:" + Relpath.relpath(toPath, spec.getFetchSpec());
            }
            return "file:" + spec.getFetchSpec();
        }
        GitHost hosted = spec.getHosted();
        if (hosted != null) {
            return "git+" + (hosted.getAuth() != null && !hosted.getAuth().isEmpty() ?
                    hosted.https(HOSTED_OPT) : hosted.sshurl(HOSTED_OPT));
        }
        if (type == Spec.Type.GIT) {
            return spec.getSaveSpec();
        }
        if ("*".equals(spec.getRawSpec())) {
            return spec.getRaw();
        }
        return spec.getRawSpec();
    }
}
