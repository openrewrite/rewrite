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
package org.openrewrite.marker;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class MarkersTest {

    @Test
    void computeThatDoesntChangeReference() {
        TestMarker marker = new TestMarker();
        Markers markers = Markers.build(singletonList(marker));
        assertThat(markers).isSameAs(markers.addIfAbsent(marker));
    }

    @Test
    void computeThatAddsNewMarker() {
        TestMarker marker = new TestMarker();
        Markers markers = Markers.EMPTY;
        assertThat(markers).isNotSameAs(markers.addIfAbsent(marker));
    }

    @Test
    void addPreventsDuplicates() {
        Markers markers = Markers.EMPTY;
        markers = markers.add(new TextMarker(randomId(), "test"));
        markers = markers.add(new TextMarker(randomId(), "test"));
        assertThat(markers.findAll(TextMarker.class)).hasSize(1);
    }

    @Test
    void addAcceptsNonDuplicates() {
        Markers markers = Markers.EMPTY;
        markers = markers.add(new TextMarker(randomId(), "thing1"));
        markers = markers.add(new TextMarker(randomId(), "thing2"));
        assertThat(markers.findAll(TextMarker.class)).hasSize(2);
    }

    /**
     * A stored LST holds null where a marker once could not be decoded, and its printer still reads it.
     */
    @Test
    void nullEntryStaysWhereItIs() {
        TextMarker text = new TextMarker(randomId(), "text");
        Markers markers = Markers.build(Arrays.asList(null, text));
        SearchResult found = new SearchResult(randomId(), "found");
        TextMarker replaced = new TextMarker(randomId(), "replaced");

        assertThat(markers.add(found).getMarkers()).containsExactly(null, text, found);
        assertThat(markers.addIfAbsent(found).getMarkers()).containsExactly(null, text, found);
        assertThat(markers.setByType(found).getMarkers()).containsExactly(null, text, found);
        assertThat(markers.setByType(replaced).getMarkers()).containsExactly(null, replaced);
        assertThat(markers.compute(replaced, (a, b) -> b).getMarkers()).containsExactly(null, text, replaced);
        assertThat(markers.removeByType(TextMarker.class).getMarkers()).hasSize(1).containsOnlyNulls();
        assertThat(markers.findAll(TextMarker.class)).containsExactly(text);
    }

    @Test
    void visitorReplacingAMarkerLeavesANullEntryInPlace() {
        TextMarker text = new TextMarker(randomId(), "text");
        TextMarker replaced = new TextMarker(randomId(), "replaced");
        PlainText before = PlainText.builder()
          .sourcePath(Paths.get("a.txt"))
          .markers(Markers.build(Arrays.asList(null, text)))
          .build();

        PlainText after = new PlainTextVisitor<Integer>() {
            @Override
            public <M extends Marker> M visitMarker(Marker marker, Integer p) {
                //noinspection unchecked
                return (M) (marker.getId().equals(text.getId()) ? replaced : marker);
            }
        }.visitText(before, 0);

        assertThat(after.getMarkers().getMarkers()).containsExactly(null, replaced);
    }

    @Test
    void markupWithoutDetailPrintsItsMessageWhenVerbose() {
        Cursor cursor = new Cursor(null, Cursor.ROOT_VALUE);

        assertThat(new Markup.Info(randomId(), "message", null).print(cursor, UnaryOperator.identity(), true))
          .isEqualTo("(message)");
        assertThat(new Markup.Info(randomId(), "message", "detail").print(cursor, UnaryOperator.identity(), true))
          .isEqualTo("(detail)");
    }

    private static class TextMarker implements Marker {
        private final UUID id;
        private final String text;

        private TextMarker(UUID id, String text) {
            this.text = text;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TextMarker that = (TextMarker) o;
            return text.equals(that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text);
        }

        @Override
        public UUID getId() {
            return id;
        }

        @SuppressWarnings("unchecked")
        @Override
        public TextMarker withId(UUID id) {
            return new TextMarker(id, text);
        }
    }

    private static class TestMarker implements Marker {
        @Override
        public UUID getId() {
            return randomId();
        }

        @Override
        public <M extends Marker> M withId(UUID id) {
            throw new UnsupportedOperationException();
        }
    }
}
