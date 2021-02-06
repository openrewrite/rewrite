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
package org.openrewrite.java.internal.template;

import com.fasterxml.jackson.annotation.JacksonAnnotation;
import lombok.RequiredArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.List;

public class ExtractTrees2 {
    public static List<Object> extract(J.CompilationUnit cu, Cursor insertionScope, JavaCoordinates<?> coordinates) {
        long insertionDepth = insertionScope.getPathAsStream(v -> v instanceof J).count();

        List<Object> extracted = new ArrayList<>();
        new ExtractTemplatedCode(insertionDepth, coordinates).visit(cu, extracted);
        return extracted;
    }

    @RequiredArgsConstructor
    private static class ExtractTemplatedCode extends JavaIsoVisitor<List<Object>> {
        private final long insertionDepth;
        private final JavaCoordinates<?> coordinates;

        private long startDepth = -1;
        private boolean foundEnd;

        @Override
        public Space visitSpace(Space space, Space.Location loc, List<Object> js) {
            long depth = getCursor().getPathAsStream(v -> v instanceof J).count();
            if (findComment(space, JavaTemplatePrinter.SNIPPET_MARKER_START) != null) {
                startDepth = depth;
            }
            if (findComment(space, JavaTemplatePrinter.SNIPPET_MARKER_END) != null) {
                foundEnd = true;
            }

//            /*START*/
//            @JacksonAnnotation
//            /*END*/
//            void method /*START*/<T>/*END*/() {
//            }

            // int n = 1 == 2 ? /*START*/1/*END*/ : 2

            // this handles all the before cases?
            if (startDepth >= 0 && insertionDepth == startDepth) {
                Object o = getCursor().getValue();
                switch (coordinates.getSpaceLocation()) {
                    case ANNOTATION_PREFIX:
                        if (o instanceof J.Annotation) {
                            maybeAdd(js, o);
                        }
                        break;
                    case BLOCK_PREFIX:
                    default:
                        maybeAdd(js, o);
                }
            }

            return super.visitSpace(space, loc, js);
        }

        private boolean maybeAdd(List<Object> js, Object o) {
            // do while loop thing on each js
            return js.add(o);
        }

        @Override
        public <T> JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, JLeftPadded.Location loc, List<Object> js) {
            if (startDepth >= 0 && insertionDepth == startDepth &&
                loc.getBeforeLocation().equals(coordinates.getSpaceLocation())) {
                maybeAdd(js, left);
            }
            return super.visitLeftPadded(left, loc, js);
        }

        @Override
        public <J2 extends J> JContainer<J2> visitContainer(JContainer<J2> container, JContainer.Location loc, List<Object> js) {
            if (startDepth >= 0 && insertionDepth == startDepth &&
                    loc.getBeforeLocation().equals(coordinates.getSpaceLocation())) {
                maybeAdd(js, container);
            }
            return super.visitContainer(container, loc, js);
        }

        @Nullable
        private Comment findComment(@Nullable Space space, String comment) {
            if (space == null) {
                return null;
            }
            return space.getComments().stream()
                    .filter(c -> Comment.Style.BLOCK.equals(c.getStyle()))
                    .filter(c -> c.getText().equals(comment))
                    .findAny()
                    .orElse(null);
        }
    }
}
