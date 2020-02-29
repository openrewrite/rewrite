/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

import java.util.Spliterators;
import java.util.UUID;

import static java.util.stream.StreamSupport.stream;

public interface ScopedVisitorSupport {
    UUID getScope();

    Cursor getCursor();

    default boolean isScope() {
        return isScope(getCursor().getTree());
    }

    default boolean isScope(@Nullable Tree t) {
        return t != null && getScope().equals(t.getId());
    }

    default boolean isScopeInCursorPath() {
        Tree t = getCursor().getTree();
        return (t != null && t.getId().equals(getScope())) ||
                stream(Spliterators.spliteratorUnknownSize(getCursor().getPath(), 0), false)
                        .anyMatch(p -> p.getId().equals(getScope()));
    }
}
