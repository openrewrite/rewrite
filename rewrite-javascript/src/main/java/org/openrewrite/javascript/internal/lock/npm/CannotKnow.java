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

import lombok.Getter;

/**
 * Raised where npm reads something this engine has no access to: a tarball's contents, a manifest that is
 * not in the registry, the filesystem, the host. {@link #getSubject()} names it, as in
 * {@code bundled dependencies of x@1.0.0} or {@code git manifest github:a/b#v1}.
 */
@Getter
public final class CannotKnow extends IllegalStateException {
    private final String subject;

    public CannotKnow(String subject) {
        super(subject);
        this.subject = subject;
    }
}
