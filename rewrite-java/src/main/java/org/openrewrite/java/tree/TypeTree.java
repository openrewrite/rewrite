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
package org.openrewrite.java.tree;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.Quoted;
import org.openrewrite.marker.Markers;

import java.util.Scanner;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

/**
 * A tree identifying a type (e.g. a simple or fully qualified class name, a primitive,
 * array, or parameterized type).
 */
public interface TypeTree extends NameTree {

    static <T extends TypeTree & Expression> T build(String fullyQualifiedName) {
        return TypeTree.build(fullyQualifiedName, null);
    }

    static <T extends TypeTree & Expression> T build(String fullyQualifiedName, @Nullable Character escape) {
        Scanner scanner = new Scanner(fullyQualifiedName);
        scanner.useDelimiter("[.$]");

        StringBuilder fullName = new StringBuilder();
        Expression expr = null;
        String nextLeftPad = "";
        for (int i = 0; scanner.hasNext(); i++) {
            StringBuilder whitespaceBefore = new StringBuilder();
            StringBuilder partBuilder = null;
            StringBuilder whitespaceBeforeNext = new StringBuilder();

            String segment = scanner.next();
            for (int j = 0; j < segment.length(); j++) {
                char c = segment.charAt(j);
                if (!Character.isWhitespace(c)) {
                    if (partBuilder == null) {
                        partBuilder = new StringBuilder();
                    }
                    partBuilder.append(c);
                } else {
                    if (partBuilder == null) {
                        whitespaceBefore.append(c);
                    } else {
                        whitespaceBeforeNext.append(c);
                    }
                }
            }

            assert partBuilder != null;
            String part = partBuilder.toString();
            Markers markers = Markers.EMPTY;
            if (escape != null) {
                String esc = String.valueOf(escape);
                if (!esc.isEmpty() && part.startsWith(esc) && part.endsWith(esc)) {
                    part = part.substring(1, part.length() - 1);
                    markers = markers.addIfAbsent(new Quoted(randomId()));
                }
            }

            if (i == 0) {
                fullName.append(part);
                expr = new Identifier(randomId(), Space.format(whitespaceBefore.toString()), markers, emptyList(), part, null, null);
            } else {
                fullName.append('.').append(part);
                expr = new J.FieldAccess(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        expr,
                        new JLeftPadded<>(
                                Space.format(nextLeftPad),
                                new Identifier(
                                        randomId(),
                                        Space.format(whitespaceBefore.toString()),
                                        markers,
                                        emptyList(),
                                        part,
                                        null,
                                        null
                                ),
                                Markers.EMPTY
                        ),
                        Character.isUpperCase(part.charAt(0)) ?
                                JavaType.ShallowClass.build(fullName.toString()) :
                                null
                );
            }

            nextLeftPad = whitespaceBeforeNext.toString();
        }

        assert expr != null;

        //noinspection unchecked
        return (T) expr;
    }
}
