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
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.TreeVisitor;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

public class MarkersTest {
    @Test
    public void addPreventsDuplicates() {
        Markers markers = Markers.EMPTY;
        markers = markers.add(new TextMarker(randomId(), "test"));
        markers = markers.add(new TextMarker(randomId(), "test"));
        assertThat(markers.findAll(TextMarker.class)).hasSize(1);
    }

    @Test
    public void addAcceptsNonDuplicates() {
        Markers markers = Markers.EMPTY;
        markers = markers.add(new TextMarker(randomId(), "thing1"));
        markers = markers.add(new TextMarker(randomId(), "thing2"));
        assertThat(markers.findAll(TextMarker.class)).hasSize(2);
    }

    private static class TextMarker implements Marker {
        private final UUID id;
        private final String text;

        private TextMarker( UUID id, String text) {
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

        @Override
        public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
            return false;
        }

        @Override
        public <P> String print(TreePrinter<P> printer, P p) {
            return "";
        }
    }
}
