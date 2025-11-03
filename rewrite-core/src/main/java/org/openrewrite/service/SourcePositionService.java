package org.openrewrite.service;

import org.openrewrite.Cursor;

public interface SourcePositionService {

    Span positionOf(Cursor cursor);
}
