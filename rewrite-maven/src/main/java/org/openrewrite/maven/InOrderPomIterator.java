/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.maven;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.xml.tree.Xml;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class InOrderPomIterator implements Iterable<Xml.Document> {
    /**
     * This list is updated with the results of each iteration step as iteration proceeds.
     */
    @With(AccessLevel.PACKAGE)
    protected final List<Xml.Document> allPoms;

    InOrderPomIterator() {
        this.allPoms = Collections.emptyList();
    }

    @Override
    public final Iterator<Xml.Document> iterator() {
        return new Iterator<Xml.Document>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return allPoms.size() < i;
            }

            @Override
            public Xml.Document next() {
                Xml.Document pom = allPoms.get(i);
                Xml.Document visited = visitPom(pom, pom.getMarkers().findFirst(MavenResolutionResult.class)
                        .orElseThrow(() -> new IllegalStateException("Expected to find a MavenResolutionResult marker.")));
                allPoms.set(i++, visited);
                return visited;
            }
        };
    }

    protected abstract Xml.Document visitPom(Xml.Document pom, MavenResolutionResult resolutionResult);
}
