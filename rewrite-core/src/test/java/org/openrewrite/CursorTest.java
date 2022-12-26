/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.marker.Markers;
import org.openrewrite.text.PlainText;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class CursorTest {
    @Test
    void peekMessages() {
        var t = new PlainText(randomId(), Paths.get("test.txt"), Markers.EMPTY, null, false, null, null, "test");
        var cursor = new Cursor(null, t);

        cursor.putMessage("key", 1);
        assertThat(cursor.<Integer>getNearestMessage("key")).isEqualTo(1);

        var child = new Cursor(cursor, t);
        assertThat(child.<Integer>getNearestMessage("key")).isEqualTo(1);
    }

    @Test
    void pollMessages() {
        var t = new PlainText(randomId(), Paths.get("test.txt"), Markers.EMPTY, null, false, null, null, "test");
        var cursor = new Cursor(null, t);

        cursor.putMessage("key", 1);
        assertThat(cursor.<Integer>pollNearestMessage("key")).isEqualTo(1);
        assertThat(cursor.<Integer>pollNearestMessage("key")).isNull();

        cursor.putMessage("key", 1);
        var child = new Cursor(cursor, t);
        assertThat(child.<Integer>getNearestMessage("key")).isEqualTo(1);
    }

    @Test
    void pathPredicates() {
        var t = new PlainText(randomId(), Paths.get("test.txt"), Markers.EMPTY, null, false, null, null, "test");
        var cursor = new Cursor(new Cursor(new Cursor(null, 1), t), 2);
        assertThat(cursor.getPath(v -> v instanceof PlainText).next()).isSameAs(t);
    }

    @Test
    void pathAsStreamPredicates() {
        var t = new PlainText(randomId(), Paths.get("test.txt"), Markers.EMPTY, null, false, null, null, "test");
        var cursor = new Cursor(new Cursor(new Cursor(null, 1), t), 2);
        assertThat(cursor.getPathAsStream(v -> v instanceof PlainText).toList()).containsExactly(t);
    }
}
