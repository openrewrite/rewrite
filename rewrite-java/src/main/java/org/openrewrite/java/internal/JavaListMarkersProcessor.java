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
package org.openrewrite.java.internal;

import org.openrewrite.java.JavaProcessor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Marker;

import java.util.Set;

public class JavaListMarkersProcessor<T> extends JavaProcessor<Set<T>> {
    private final Class<? extends Marker> markerType;

    public JavaListMarkersProcessor(Class<? extends Marker> markerType) {
        this.markerType = markerType;
    }

    @Override
    public J visitEach(J j, Set<T> ts) {
        if (j.getMarkers().findFirst(markerType).isPresent()) {
            //noinspection unchecked
            ts.add((T) j);
        }
        return super.visitEach(j, ts);
    }
}
