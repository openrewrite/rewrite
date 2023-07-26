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

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;

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
