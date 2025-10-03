/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.format;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

@Value
@EqualsAndHashCode(callSuper = false)
public class AlwaysWrapMethodChains<P> extends JavaIsoVisitor<P> {

    List<MethodMatcher> matchers;

    public AlwaysWrapMethodChains(@Nullable List<String> methodNames) {
        this.matchers = methodNames == null ? emptyList() : methodNames.stream()
                .map(name -> String.format("*..* %s(..)", name))
                .map(MethodMatcher::new)
                .collect(toList());
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P ctx) {
        if (method.getMarkers().findFirst(FormattedMethodChain.class).isPresent()) {
            return method;
        }
        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

        J.MethodInvocation chainStarter = findChainStarterInChain(m);
        // If there is no chain starter in the chain, or the current method is the actual chain starter call (current chain starter call does not need newline)
        if (chainStarter == null || chainStarter == m) {
            return m;
        }

        Space after = m.getPadding().getSelect().getAfter();
        //Already on a new line
        if (after.getLastWhitespace().contains("\n")) {
            return m.withMarkers(method.getMarkers().add(new FormattedMethodChain()));
        }

        //Only update the whitespace, preserving comments
        if (after.getComments().isEmpty()) {
            after = after.withWhitespace("\n");
        } else {
            after = after.withComments(ListUtils.mapLast(after.getComments(), comment -> comment.withSuffix("\n")));
        }
        if (after != m.getPadding().getSelect().getAfter()) {
            m = m.getPadding().withSelect(m.getPadding().getSelect().withAfter(after))
                    .withArguments(ListUtils.map(m.getArguments(), arg -> arg.withPrefix(Space.EMPTY)));
        }

        return m.withMarkers(method.getMarkers().add(new FormattedMethodChain()));
    }

    private J.@Nullable MethodInvocation findChainStarterInChain(J.MethodInvocation method) {
        Expression current = method;
        while (current instanceof J.MethodInvocation) {
            J.MethodInvocation mi = (J.MethodInvocation) current;
            for (MethodMatcher matcher : matchers) {
                if (matcher.matches(mi)) {
                    return mi;
                }
            }
            Expression select = mi.getSelect();
            if (!(select instanceof J.MethodInvocation)) {
                if (matchers.isEmpty()) {
                    return method;
                }
            }
            current = select;
        }
        return null;
    }

    @Value
    @With
    @AllArgsConstructor
    private static class FormattedMethodChain implements Marker {
        UUID id;

        public FormattedMethodChain() {
            this.id = randomUUID();
        }
    }
}
