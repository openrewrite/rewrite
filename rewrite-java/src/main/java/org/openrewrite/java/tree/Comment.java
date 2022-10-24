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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Markers;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@c")
public interface Comment {
    Markers getMarkers();
    <C extends Comment> C withMarkers(Markers markers);

    String getSuffix();
    <C extends Comment> C withSuffix(String margin);

    boolean isMultiline();

    default String printComment(Cursor cursor) {
        PrintOutputCapture<Integer> p = new PrintOutputCapture<>(0);
        printComment(cursor, p);
        return p.getOut();
    }

    <P> void printComment(Cursor cursor, PrintOutputCapture<P> p);
}
