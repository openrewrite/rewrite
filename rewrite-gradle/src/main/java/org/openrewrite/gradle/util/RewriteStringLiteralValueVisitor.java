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
package org.openrewrite.gradle.util;

import lombok.AllArgsConstructor;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.java.tree.J;

@AllArgsConstructor
public class RewriteStringLiteralValueVisitor<P> extends GroovyVisitor<P> {

    private final J.Literal target;
    private final String newValue;

    @Override
    public J visitLiteral(J.Literal literal, P p) {
        J.Literal l = (J.Literal) super.visitLiteral(literal, p);
        if (l == target) {
            String oldValue = (String) l.getValue();
            if (oldValue.equals(newValue)) {
                return l;
            }
            String valueSource = l.getValueSource();
            String delimiter = (valueSource == null) ? "'"
                : valueSource.substring(0, valueSource.indexOf(oldValue));
            l = l.withValue(newValue).withValueSource(delimiter + newValue + delimiter);
        }
        return l;
    }
}
