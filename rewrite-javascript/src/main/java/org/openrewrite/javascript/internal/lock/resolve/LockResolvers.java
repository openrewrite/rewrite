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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

/**
 * Selects the {@link LockResolver} for a package manager: the from-scratch resolver tier the native lock engine
 * falls back to when the surgical patch cannot reshape the closure. One (stateless) instance per package manager,
 * mirroring {@link org.openrewrite.javascript.internal.LockFileRegeneration#forPackageManager}.
 */
public final class LockResolvers {

    private LockResolvers() {
    }

    public static @Nullable LockResolver forPackageManager(PackageManager pm) {
        return null;
    }
}
