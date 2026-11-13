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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.openrewrite.javascript.internal.lock.CannotKnow;

/**
 * A string yarn makes relative to where the project or its cache sits on disk: the engine cannot know its text, so
 * reading it is cannot-know, and holding it is not.
 */
final class UnknownPath {

    private final String subject;

    UnknownPath(String subject) {
        this.subject = subject;
    }

    CannotKnow read() {
        return new CannotKnow(subject);
    }

    @Override
    public String toString() {
        return subject;
    }
}
