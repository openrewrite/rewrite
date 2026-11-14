/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.yaml.internal;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.openrewrite.Tree.randomId;

/// The indentation that `MergeYamlVisitor` wants `IndentsVisitor` to give the multiline scalars it merges.
///
/// It travels to `IndentsVisitor` as a cursor message for the duration of an `autoFormat` call, so that no marker
/// is left on the tree. One instance is shared by a merge and the merges nested in it, because a later
/// `autoFormat` of an enclosing sequence entry has to indent the same scalars again.
public final class MultilineScalarIndents {
    private static final String CURSOR_MESSAGE = MultilineScalarIndents.class.getName();

    private final Map<UUID, Indent> indents = new HashMap<>();

    /// @return the scalar with an id of its own, as one incoming scalar can be merged into more than one place
    public Yaml.Scalar add(Yaml.Scalar scalar, boolean added, int indent) {
        Yaml.Scalar withOwnId = scalar.withId(randomId());
        indents.put(withOwnId.getId(), new Indent(added, indent));
        return withOwnId;
    }

    public <T> T applyDuring(Cursor cursor, Supplier<T> autoFormat) {
        if (indents.isEmpty()) {
            return autoFormat.get();
        }
        Object previous = cursor.pollMessage(CURSOR_MESSAGE);
        cursor.putMessage(CURSOR_MESSAGE, this);
        try {
            return autoFormat.get();
        } finally {
            cursor.pollMessage(CURSOR_MESSAGE);
            if (previous != null) {
                cursor.putMessage(CURSOR_MESSAGE, previous);
            }
        }
    }

    /// Carries the indents across a [Cursor#fork()], which drops cursor messages.
    public static void forward(Cursor from, Cursor to) {
        MultilineScalarIndents indents = from.getNearestMessage(CURSOR_MESSAGE);
        if (indents != null) {
            to.putMessage(CURSOR_MESSAGE, indents);
        }
    }

    public static @Nullable Indent find(Cursor cursor, Yaml.Scalar scalar) {
        MultilineScalarIndents indents = cursor.getNearestMessage(CURSOR_MESSAGE);
        return indents == null ? null : indents.indents.get(scalar.getId());
    }

    @Value
    public static class Indent {
        boolean added;
        int indent;
    }
}
