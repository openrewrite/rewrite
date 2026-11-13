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
package org.openrewrite.python.internal.uvlock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.LockFileRegeneration.Failure;
import org.openrewrite.python.internal.LockFileRegeneration.Reason;

/**
 * Carries a structured {@link Failure} out of the deep resolution call stack so
 * {@link UvLockEngine} can turn it into a fail-loud {@code Result}.
 */
final class EngineFailure extends RuntimeException {
    final Failure failure;

    EngineFailure(Failure failure) {
        super(failure.getDetail());
        this.failure = failure;
    }

    EngineFailure(Reason reason, @Nullable String packageName, String detail) {
        this(new Failure(reason, packageName, null, detail));
    }
}
