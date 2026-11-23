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
package org.openrewrite.scala.marker;

import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Marker applied to an import (or export) statement that is the continuation
 * of a comma-separated group: the second {@code b._} in
 * {@code import a._, b._}. The printer emits a {@code ,} instead of the
 * {@code import} (or {@code export}) keyword for marked statements.
 * <p>
 * The marker carries no payload — whitespace before the comma is the
 * statement's own prefix; whitespace after the comma is the qualifier's
 * leading prefix.
 */
public class CommaContinuation implements Marker {
    private final UUID id;

    public CommaContinuation(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public CommaContinuation withId(UUID id) {
        return new CommaContinuation(id);
    }
}
