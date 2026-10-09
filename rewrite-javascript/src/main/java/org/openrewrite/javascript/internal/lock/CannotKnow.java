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
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;

/**
 * Raised where the package manager reads something the engine is not given: a tarball's contents, a manifest that is
 * not in the registry, the filesystem, network timing, the host. {@link #getSubject()} names it.
 */
@Getter
public final class CannotKnow extends RuntimeException {
    private final String subject;
    private final Reason reason;

    public CannotKnow(String subject) {
        this(subject, Reason.RESOLUTION_REQUIRED);
    }

    public CannotKnow(String subject, Reason reason) {
        super("the engine cannot know " + subject);
        this.subject = subject;
        this.reason = reason;
    }
}
