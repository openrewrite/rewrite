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
        StringBuilder fullName = new StringBuilder();
        Expression expr = null;
        String nextLeftPad = "";

        StringBuilder segment = new StringBuilder();
        StringBuilder whitespaceBefore = new StringBuilder();
        StringBuilder partBuilder = new StringBuilder();
        StringBuilder whitespaceBeforeNext = new StringBuilder();

        char esc = escape != null ? escape : 0;
        boolean inEscape = false;

        for (int index = 0; index < fullyQualifiedName.length(); index++) {
            char currentChar = fullyQualifiedName.charAt(index);
            if (currentChar == esc) {
                inEscape = !inEscape;
            }
            if (!inEscape && (currentChar == '.' || currentChar == '$') || index == fullyQualifiedName.length() - 1) {
                if (index == fullyQualifiedName.length() - 1) {
                    segment.append(currentChar);
                }
                // Process the segment
                for (int j = 0; j < segment.length(); j++) {
                    char c = segment.charAt(j);
                    if (escape != null && c == escape) {
                        inEscape = !inEscape;
                    }
                    if (!Character.isWhitespace(c) || inEscape) {
                        partBuilder.append(c);
                    } else {
                        if (partBuilder.length() == 0) {
                            whitespaceBefore.append(c);
                        } else {
                            whitespaceBeforeNext.append(c);
                        }
                    }
                }
                String part = partBuilder.toString();
                Markers markers = Markers.EMPTY;
                if (escape != null && part.charAt(0) == esc && part.charAt(part.length() - 1) == esc) {
                    part = part.substring(1, part.length() - 1);
                    markers = markers.addIfAbsent(new Quoted(randomId()));
                }
                Space whitespaceBeforeStr = Space.format(whitespaceBefore.toString());
                if (fullName.length() == 0) {
                    fullName.append(part);
                    expr = new Identifier(randomId(), whitespaceBeforeStr, markers, emptyList(), part, null, null);
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
                                            whitespaceBeforeStr,
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
                // Reset the StringBuilders
                segment.setLength(0);
                whitespaceBefore.setLength(0);
                partBuilder.setLength(0);
                whitespaceBeforeNext.setLength(0);
            } else {
                segment.append(currentChar);
            }
        }
        assert expr != null;
        //noinspection unchecked
        return (T) expr;
    }
}
