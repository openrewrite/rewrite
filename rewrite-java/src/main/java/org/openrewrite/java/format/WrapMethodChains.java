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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.service.SourcePositionService;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
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
            if (style != null && style.getChainedMethodCalls() != null) {
                List<MethodMatcher> matchers = style.getChainedMethodCalls().getBuilderMethods().stream()
                        .map(name -> String.format("*..* %s(..)", name))
                        .map(MethodMatcher::new)
                        .collect(toList());
                J chainStarter = findChainStarterInChain(m);
                // If there is no chain starter in the chain, or the current method is the actual chain starter call (current chain starter call does not need newline)
                if (chainStarter == m || m.getPadding().getSelect() == null) {
                    return m;
                }

                Space after = m.getPadding().getSelect().getAfter();
                //Already on a new line
                if (after.getLastWhitespace().contains("\n")) {
                    return m;
                }

                boolean isBuilderMethod = chainStarter instanceof J.MethodInvocation && matchers.stream().anyMatch(matcher -> matcher.matches((J.MethodInvocation) chainStarter));

                if (isBuilderMethod || (style.getChainedMethodCalls().getWrap() == LineWrapSetting.WrapAlways || style.getChainedMethodCalls().getWrap() == LineWrapSetting.ChopIfTooLong)) {
                    // Not long enough to wrap (always wrap builder methods)
                    JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                    if (!isBuilderMethod &&
                            style.getChainedMethodCalls().getWrap() == LineWrapSetting.ChopIfTooLong &&
                            (sourceFile == null || sourceFile.service(SourcePositionService.class).computeTreeLength(getCursor()) <= style.getHardWrapAt())) {
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
            }
        } catch (NoSuchMethodError | NoSuchFieldError ignore) {
            // Styles are parent-first loaded and this can happen if the style is from a older version of the runtime. Can be removed in future releases.
        }

        return m;
    }

    private J findChainStarterInChain(J.MethodInvocation method) {
        J.MethodInvocation chainStarter = method;
        Expression select = method.getSelect();
        while (select instanceof J.MethodInvocation) {
            chainStarter = (J.MethodInvocation) select;
            select = chainStarter.getSelect();
        }
        return chainStarter;
    }
}
