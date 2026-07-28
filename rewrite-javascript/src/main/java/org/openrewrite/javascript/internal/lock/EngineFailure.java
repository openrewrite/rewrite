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
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;

/**
 * Carries a structured {@link Failure} out of the deep resolution call stack so
 * {@link NativeLockEngine} (and the per-format patchers) can abort loud and the top
 * frame can turn it into a fail-loud {@code Result}. Public because the engine
 * ({@code internal}) and the patchers ({@code internal.lock}) both throw it.
 */
public final class EngineFailure extends RuntimeException {
    public final Failure failure;

    public EngineFailure(Failure failure) {
        super(failure.getDetail());
        this.failure = failure;
    }

    public EngineFailure(Reason reason, @Nullable String packageName, String detail) {
        this(new Failure(reason, packageName, detail));
    }
}
