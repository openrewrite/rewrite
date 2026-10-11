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
import org.openrewrite.javascript.internal.lock.Minimatch;
import org.openrewrite.javascript.internal.lock.Spec;

import java.util.List;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/release-age-exclude.js (npm 11.17.0).
 */
final class ReleaseAgeExclude {

    private static final Minimatch.Options OPTIONS = Minimatch.Options.builder(Minimatch.Release.V10_2_5)
            .nonegate(true).nocomment(true).build();

    private ReleaseAgeExclude() {
    }

    static boolean isReleaseAgeExcluded(@Nullable String name, @Nullable List<Object> patterns) {
        if (!truthy(name) || patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (Object pattern : patterns) {
            if (Js.strictEquals(name, pattern) || Minimatch.minimatch(name, Js.string(pattern), OPTIONS)) {
                return true;
            }
        }
        return false;
    }

    static @Nullable String trustedSpecName(@Nullable Spec spec) {
        if (spec == null) {
            return null;
        }
        if (spec.getType() == Spec.Type.ALIAS && spec.getSubSpec() != null && spec.getSubSpec().isRegistry()) {
            return spec.getSubSpec().getName();
        }
        return spec.getName();
    }
}
