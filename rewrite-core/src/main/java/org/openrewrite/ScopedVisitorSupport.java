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
