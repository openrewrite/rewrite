/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.format;

import lombok.AllArgsConstructor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.internal.KotlinTreeParserVisitor;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
public class TrailingCommaVisitor<P> extends KotlinIsoVisitor<P> {
    private final boolean useTrailingComma;

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);
        return m.getPadding().withParameters(handleTrailingComma(m.getPadding().getParameters()));
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = super.visitMethodInvocation(method, p);
        return m.getPadding().withArguments(handleTrailingComma(m.getPadding().getArguments()));
    }

    private <T extends J> JContainer<T> handleTrailingComma(JContainer<T> container) {
        List<JRightPadded<T>> rps = container.getPadding().getElements();

        if (!rps.isEmpty()) {
            JRightPadded<T> last = rps.get(rps.size() - 1);
            JRightPadded<T> updated = last;
            Markers markers = last.getMarkers();
            Optional<TrailingComma> maybeTrailingComma = markers.findFirst(TrailingComma.class);

            if (!useTrailingComma && maybeTrailingComma.isPresent()) {
                markers = markers.removeByType(TrailingComma.class);
                updated = last.withMarkers(markers).withAfter(KotlinTreeParserVisitor.merge(last.getAfter(), maybeTrailingComma.get().getSuffix()));
            }

            if (useTrailingComma && !maybeTrailingComma.isPresent()) {
                markers = markers.add(new TrailingComma(UUID.randomUUID(), last.getAfter()));
                updated = last.withMarkers(markers).withAfter(Space.EMPTY);
            }

            if (updated != last) {
                JRightPadded<T> finalUpdated = updated;
                rps = ListUtils.mapLast(rps, x -> finalUpdated);
                container = container.getPadding().withElements(rps);
            }
        }

        return container;
    }
}
