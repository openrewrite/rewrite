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
package org.openrewrite.hcl.internal.template;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.tree.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.PropertyPlaceholderHelper;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class Substitutions {
    private static final Pattern PATTERN_COMMENT = Pattern.compile("__p(\\d+)__");

    private final String code;
    private final Object[] parameters;
    private final PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper(
            "#{", "}", null);

    public String substitute() {
        AtomicInteger index = new AtomicInteger(0);
        return propertyPlaceholderHelper.replacePlaceholders(code, key -> {
            int i = index.getAndIncrement();
            Object parameter = parameters[i];
            return substituteSingle(parameter, i);
        });
    }

    private String substituteSingle(Object parameter, int index) {
        if (parameter instanceof Hcl) {
            if (parameter instanceof Expression) {
                return "/*__p" + index + "__*/{}";
            } else {
                throw new IllegalArgumentException("'" + parameter.getClass().getSimpleName() + "' cannot be a parameter to a template.");
            }
        } else if (parameter instanceof HclRightPadded) {
            return substituteSingle(((HclRightPadded<?>) parameter).getElement(), index);
        } else if (parameter instanceof HclLeftPadded) {
            return substituteSingle(((HclLeftPadded<?>) parameter).getElement(), index);
        }
        return parameter.toString();
    }

    public <H extends Hcl> List<H> unsubstitute(List<H> js) {
        return ListUtils.map(js, this::unsubstitute);
    }

    public <H extends Hcl> H unsubstitute(H j) {
        if (parameters.length == 0) {
            return j;
        }

        //noinspection unchecked
        H unsub = (H) new HclVisitor<Integer>() {
            @Override
            public Hcl visitExpression(Expression expression, Integer integer) {
                Hcl param = maybeParameter(expression);
                if (param != null) {
                    return param;
                }
                return super.visitExpression(expression, integer);
            }

            private @Nullable Hcl maybeParameter(Hcl h) {
                Integer param = parameterIndex(h.getPrefix());
                if (param != null) {
                    Hcl h2 = (Hcl) parameters[param];
                    return h2.withPrefix(h2.getPrefix().withWhitespace(h.getPrefix().getWhitespace()));
                }
                return null;
            }

            private @Nullable Integer parameterIndex(Space space) {
                for (Comment comment : space.getComments()) {
                    java.util.regex.Matcher matcher = PATTERN_COMMENT.matcher(comment.getText());
                    if (matcher.matches()) {
                        return Integer.valueOf(matcher.group(1));
                    }
                }
                return null;
            }
        }.visit(j, 0);

        assert unsub != null;
        return unsub;
    }
}
