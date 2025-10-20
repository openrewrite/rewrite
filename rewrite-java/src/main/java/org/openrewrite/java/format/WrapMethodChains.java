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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.style.LineWrapSetting;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Value
@EqualsAndHashCode(callSuper = false)
public class WrapMethodChains<P> extends JavaIsoVisitor<P> {

    WrappingAndBracesStyle style;

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P ctx) {
        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

        try {
            // styles are parent loaded, so the getters may or may not be present and they may or may not return null
            if (style != null && style.getChainedMethodCalls() != null && style.getChainedMethodCalls().getWrap() == LineWrapSetting.WrapAlways) {
                List<MethodMatcher> matchers = style.getChainedMethodCalls().getBuilderMethods().stream()
                        .map(name -> String.format("*..* %s(..)", name))
                        .map(MethodMatcher::new)
                        .collect(toList());
                J.MethodInvocation chainStarter = findChainStarterInChain(m, matchers);
                // If there is no chain starter in the chain, or the current method is the actual chain starter call (current chain starter call does not need newline)
                if (chainStarter == null || chainStarter == m || m.getPadding().getSelect() == null) {
                    return m;
                }

                Space after = m.getPadding().getSelect().getAfter();
                //Already on a new line
                if (after.getLastWhitespace().contains("\n")) {
                    return m;
                }

                //Only update the whitespace, preserving comments
                if (after.getComments().isEmpty()) {
                    after = after.withWhitespace("\n");
                } else {
                    after = after.withComments(ListUtils.mapLast(after.getComments(), comment -> comment == null ? null : comment.withSuffix("\n")));
                }
                if (after != m.getPadding().getSelect().getAfter()) {
                    m = m.getPadding().withSelect(m.getPadding().getSelect().withAfter(after))
                            .withArguments(ListUtils.map(m.getArguments(), arg -> arg.withPrefix(Space.EMPTY)));
                }
            }
        } catch (NoSuchMethodError ignore) {
            // Styles are parent-first loaded and this can happen if the style is from a older version of the runtime. Can be removed in future releases.
        }

        return m;
    }

    private J.@Nullable MethodInvocation findChainStarterInChain(J.MethodInvocation method, List<MethodMatcher> matchers) {
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
}
