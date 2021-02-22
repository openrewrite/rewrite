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

import org.openrewrite.TreePrinter;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;

public class MethodDeclarationToString {
    public static String toString(J.MethodDeclaration method) {
        //noinspection ConstantConditions
        return METHOD_PRINTER.print(method, null);
    }

    private static final JavaPrinter<Void> METHOD_PRINTER = new JavaPrinter<Void>(TreePrinter.identity()) {
        @Override
        public J visitMethodDeclaration(J.MethodDeclaration method, Void unused) {
            visitModifiers(Space.formatFirstPrefix(method.getModifiers(), Space.EMPTY), unused);
            StringBuilder acc = getPrinter();
            if (!method.getModifiers().isEmpty()) {
                acc.append(' ');
            }
            J.TypeParameters typeParameters = method.getAnnotations().getTypeParameters();
            if (typeParameters != null) {
                visitSpace(typeParameters.getPrefix(), Space.Location.TYPE_PARAMETERS, unused);
                acc.append("<");
                visitRightPadded(typeParameters.getPadding().getTypeParameters(), JRightPadded.Location.TYPE_PARAMETER, ",", unused);
                acc.append(">");
            }
            if (method.getReturnTypeExpression() != null) {
                acc.append(method.getReturnTypeExpression().printTrimmed()).append(' ');
            }
            acc.append(method.getSimpleName());
            visitContainer("(", method.getPadding().getParameters(), JContainer.Location.METHOD_DECLARATION_PARAMETERS, ",", ")", unused);
            visitContainer("throws", method.getPadding().getThrows(), JContainer.Location.THROWS, ",", "", unused);
            return method;
        }
    };
}
