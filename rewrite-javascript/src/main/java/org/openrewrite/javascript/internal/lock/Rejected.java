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

import lombok.AccessLevel;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;

/**
 * What the package manager does with an edit instead of writing a lock, as it words it; {@link LockOnlyInstall#kindOf}
 * names the failure's kind.
 */
@Getter
public final class Rejected extends RuntimeException {

    enum Outcome {
        /**
         * An error of its own, carrying its code or its message.
         */
        VERDICT,

        /**
         * An error with no code of its own: JavaScript's, an exhausted heap or stack, a resolution without end.
         */
        CRASH,

        DISCARDS_LOCK,
        DELETES_LOCK
    }

    @Getter(AccessLevel.PACKAGE)
    private final Outcome outcome;

    private final Reason reason;
    private final @Nullable String packageName;

    /**
     * The package manager's own code, for one that prints its errors by it ({@code ERESOLVE}).
     */
    private final @Nullable String code;

    private Rejected(Outcome outcome, Reason reason, @Nullable String packageName, @Nullable String code,
                     String message) {
        super(message);
        this.outcome = outcome;
        this.reason = reason;
        this.packageName = packageName;
        this.code = code;
    }

    public static Rejected rejects(Reason reason, String message) {
        return rejects(reason, null, message);
    }

    public static Rejected rejects(Reason reason, @Nullable String packageName, String message) {
        return new Rejected(Outcome.VERDICT, reason, packageName, null, message);
    }

    public static Rejected rejects(SharedError error, @Nullable String packageName, String message) {
        return rejects(error.getReason(), packageName, message);
    }

    public static Rejected coded(Reason reason, @Nullable String packageName, String code, String message) {
        return new Rejected(Outcome.VERDICT, reason, packageName, code, message);
    }

    public static Rejected crashes(Reason reason, @Nullable String packageName, String message) {
        return new Rejected(Outcome.CRASH, reason, packageName, null, message);
    }

    public static Rejected discardsLock(String message) {
        return new Rejected(Outcome.DISCARDS_LOCK, Reason.MALFORMED_LOCK, null, null, message);
    }

    public static Rejected deletesLock(String message) {
        return new Rejected(Outcome.DELETES_LOCK, Reason.RESOLUTION_REQUIRED, null, null, message);
    }
}
