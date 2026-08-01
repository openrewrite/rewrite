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
package org.openrewrite.javascript.internal.lock.resolve;

import org.openrewrite.javascript.internal.lock.EngineFailure;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

/**
 * Resolves the whole dependency closure for an edited set of manifests and produces the new lock file content,
 * byte-for-byte identical to what a real {@code install} would write — or throws {@link EngineFailure} to defer.
 * <p>
 * This is the resolver tier of native lock regeneration (ADR 0012): the deeper complement to the surgical
 * patchers, run when a closure-reshaping edit is beyond a mechanical patch. Each package manager provides one
 * implementation. All version and constraint work is delegated to rewrite-core's node-semver; the resolver adds
 * only the {@link ResolutionGraph} and the package-manager-specific resolve/layout algorithm.
 * <p>
 * The accuracy contract is absolute: an implementation either returns a lock a real install agrees with exactly,
 * or fails loud. It never returns a lock a real install would disagree with.
 */
public interface LockResolver {

    PackageManager packageManager();

    /**
     * Resolve the closure for {@code request}'s edited manifests and return the new lock content, byte-exact.
     *
     * @throws EngineFailure to defer (the closure cannot be reproduced byte-exact); the old lock is left untouched.
     */
    String resolve(ResolveRequest request);
}
