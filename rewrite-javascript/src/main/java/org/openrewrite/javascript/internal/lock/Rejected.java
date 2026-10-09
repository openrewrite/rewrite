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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;

/**
 * What the package manager does with an edit instead of writing a lock, with the package manager's own message.
 */
@Getter
public final class Rejected extends RuntimeException {
    private final Kind kind;
    private final Reason reason;
    private final @Nullable String packageName;

    public Rejected(Kind kind, Reason reason, @Nullable String packageName, String message) {
        super(message);
        this.kind = kind;
        this.reason = reason;
        this.packageName = packageName;
    }

    public static Rejected rejects(Reason reason, String message) {
        return new Rejected(Kind.PM_REJECTS, reason, null, message);
    }

    public static Rejected rejects(SharedError error, @Nullable String packageName, String message) {
        return new Rejected(Kind.PM_REJECTS, error.getReason(), packageName, message);
    }

    public static Rejected fails(Reason reason, String message) {
        return new Rejected(Kind.PM_FAILS, reason, null, message);
    }

    public static Rejected discardsLock(String message) {
        return new Rejected(Kind.DISCARDS_LOCK, Reason.MALFORMED_LOCK, null, message);
    }
}
