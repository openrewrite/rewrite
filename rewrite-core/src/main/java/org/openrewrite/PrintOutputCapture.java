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
package org.openrewrite;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markup;
import org.openrewrite.marker.SearchResult;

import java.util.function.UnaryOperator;

public class PrintOutputCapture<P> implements Cloneable {
    private final P p;
    private final MarkerPrinter markerPrinter;
    public final StringBuilder out = new StringBuilder();

    public PrintOutputCapture(P p) {
        this.p = p;
        this.markerPrinter = MarkerPrinter.DEFAULT;
    }

    public PrintOutputCapture(P p, MarkerPrinter markerPrinter) {
        this.p = p;
        this.markerPrinter = markerPrinter;
    }

    public P getContext() {
        return p;
    }

    public MarkerPrinter getMarkerPrinter() {
        return markerPrinter;
    }

    public String getOut() {
        return out.toString();
    }

    public PrintOutputCapture<P> append(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return this;
        }
        out.append(text);
        return this;
    }

    public PrintOutputCapture<P> append(char c) {
        out.append(c);
        return this;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public PrintOutputCapture<P> clone() {
        return new PrintOutputCapture<>(p, markerPrinter);
    }

    public interface MarkerPrinter {

        @Incubating(since = "8.41.4")
        @RequiredArgsConstructor
        enum MarkerMode {
            /**
             * Does not print any markers.
             */
            NONE(MarkerPrinter.NONE),
            /**
             * Prints a squiggly line arrow in front of the marked element wrapped in a comment.
             * /*~~>* /Thing thing
             */
            DEFAULT(MarkerPrinter.DEFAULT),
            /**
             * Prints a squiggly line arrow in front of the marked element wrapped in a comment.
             * Possibly adding verbose information, depending on the marker
             * /*~~>(this is some verbose information)* /Thing thing
             */
            VERBOSE(MarkerPrinter.VERBOSE),
            /**
             * Prints a squiggly arrow in front and after the marked element. It only includes {@link SearchResult}
             * /*~~>* /Thing thing /**~~>* /
             */
            SEARCH_ONLY(MarkerPrinter.SEARCH_ONLY),
            /**
             * Prints a fenced marker ID in front and after the marked element. It only includes {@link Markup} and {@link SearchResult}
             * /*{{3e2a36bb-7c16-4b03-bdde-bffda08838e7}}* /Thing thing /*{{3e2a36bb-7c16-4b03-bdde-bffda08838e7}}* /
             */
            FENCED_MARKUP_AND_SEARCH(MarkerPrinter.FENCED_MARKUP_AND_SEARCH);
            @Getter
            private final MarkerPrinter printer;
        }

        MarkerPrinter NONE = new MarkerPrinter() {
        };

        MarkerPrinter DEFAULT = new MarkerPrinter() {
            @Override
            public String beforeSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
                return marker.print(cursor, commentWrapper, false);
            }
        };

        MarkerPrinter VERBOSE = new MarkerPrinter() {
            @Override
            public String beforeSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
                return marker.print(cursor, commentWrapper, true);
            }
        };

        MarkerPrinter FENCED_MARKUP_AND_SEARCH = new MarkerPrinter() {
            @Override
            public String beforeSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
                return marker instanceof SearchResult || marker instanceof Markup ? "{{" + marker.getId() + "}}" : "";
            }

            @Override
            public String afterSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
                return marker instanceof SearchResult || marker instanceof Markup ? "{{" + marker.getId() + "}}" : "";
            }
        };

        MarkerPrinter SEARCH_ONLY = new MarkerPrinter() {
            @Override
            public String beforeSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
                return marker instanceof SearchResult ? marker.print(cursor, commentWrapper, false) : "";
            }

            @Override
            public String afterSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
                return marker instanceof SearchResult ? marker.print(cursor, commentWrapper, false) : "";
            }
        };

        default String beforePrefix(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
            return "";
        }

        default String beforeSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
            return "";
        }

        default String afterSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
            return "";
        }
    }
}
