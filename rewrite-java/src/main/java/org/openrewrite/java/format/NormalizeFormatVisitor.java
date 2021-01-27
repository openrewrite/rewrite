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
package org.openrewrite.java.format;

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

/**
 * Ensures that whitespace is on the outermost AST element possible.
 */
public class NormalizeFormatVisitor<P> extends JavaIsoVisitor<P> {
    @SuppressWarnings("ConstantConditions")
    @Override
    public J.MethodDecl visitMethod(J.MethodDecl method, P p) {
        J.MethodDecl m = super.visitMethod(method, p);

        if (!m.getAnnotations().isEmpty()) {
            m = concatenatePrefix(m, Space.firstPrefix(m.getAnnotations()));
            m = m.withAnnotations(Space.formatFirstPrefix(m.getAnnotations(), Space.EMPTY));
            return m;
        }

        if (!m.getModifiers().isEmpty()) {
            m = concatenatePrefix(m, Space.firstPrefix(m.getModifiers()));
            m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(), Space.EMPTY));
            return m;
        }

        if (m.getTypeParameters() != null && !m.getTypeParameters().getElem().isEmpty()) {
            m = concatenatePrefix(m, m.getTypeParameters().getBefore());
            m = m.withTypeParameters(m.getTypeParameters().withBefore(Space.EMPTY));
            return m;
        }

        if (m.getReturnTypeExpr() != null && m.getReturnTypeExpr().getPrefix().getWhitespace().isEmpty()) {
            m = concatenatePrefix(m, m.getReturnTypeExpr().getPrefix());
            m = m.withReturnTypeExpr(m.getReturnTypeExpr().withPrefix(Space.EMPTY));
            return m;
        }

        m = concatenatePrefix(m, m.getName().getPrefix());
        m = m.withName(m.getName().withPrefix(Space.EMPTY));
        return m;
    }

    private <J2 extends J> J2 concatenatePrefix(J2 j, Space prefix) {
        return j.withPrefix(j.getPrefix().withWhitespace(j.getPrefix().getWhitespace() + prefix.getWhitespace()));
    }
}
