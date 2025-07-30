/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.json.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;

/**
 * A JSON element that could have trailing space.
 *
 * @param <T>
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Data
public class JsonRightPadded<T extends Json> {
    @With
    T element;

    @With
    Space after;

    @With
    Markers markers;

    public JsonRightPadded<T> map(UnaryOperator<T> map) {
        return withElement(map.apply(element));
    }

    public static <T extends Json> List<T> getElements(List<JsonRightPadded<T>> ls) {
        List<T> list = new ArrayList<>();
        for (JsonRightPadded<T> l : ls) {
            T elem = l.getElement();
            list.add(elem);
        }
        return list;
    }

    public static <J2 extends Json> List<JsonRightPadded<J2>> withElements(List<JsonRightPadded<J2>> before, List<J2> elements) {
        // a cheaper check for the most common case when there are no changes
        boolean hasSizeChanged = elements.size() != before.size();
        if (!hasSizeChanged) {
            boolean hasChanges = false;
            for (int i = 0; i < before.size(); i++) {
                if (before.get(i).getElement() != elements.get(i)) {
                    hasChanges = true;
                    break;
                }
            }
            if (!hasChanges) {
                return before;
            }
        }

        List<JsonRightPadded<J2>> after = new ArrayList<>(elements.size());
        Map<UUID, JsonRightPadded<J2>> beforeById = before.stream().collect(toMap(j -> j.getElement().getId(), Function.identity()));

        int counter = 0;
        String paddingAfterLastElement = "\n";
        if (!before.isEmpty()) {
            JsonRightPadded<J2> previousFinalElement = before.get(before.size() - 1);
            paddingAfterLastElement = previousFinalElement.getAfter().getWhitespace();
        }
        for (J2 t : elements) {
            counter++;
            if (beforeById.get(t.getId()) != null) {
                JsonRightPadded<J2> found = beforeById.get(t.getId());
                if (counter == before.size() && hasSizeChanged) {
                    paddingAfterLastElement = found.getAfter().getWhitespace();
                    found = found.withAfter(Space.EMPTY);
                }
                after.add(found.withElement(t));
            } else {
                Space space = counter == elements.size() ? Space.build(paddingAfterLastElement, emptyList()) : Space.EMPTY;
                after.add(new JsonRightPadded<>(t, space, Markers.EMPTY));
            }
        }

        return after;
    }

    public static <T extends Json> JsonRightPadded<T> build(T element) {
        return new JsonRightPadded<>(element, Space.EMPTY, Markers.EMPTY);
    }

    @Override
    public String toString() {
        return "JsonRightPadded(element=" + element.getClass().getSimpleName() + ", after=" + after + ')';
    }
}
