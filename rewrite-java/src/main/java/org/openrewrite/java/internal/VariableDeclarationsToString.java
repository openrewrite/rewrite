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
import org.openrewrite.java.tree.Space;

import java.util.stream.Collectors;

public class VariableDeclarationsToString {
    public static String toString(J.VariableDeclarations v) {
        //noinspection ConstantConditions
        return VARIABLE_PRINTER.print(v, null);
    }

    private static final JavaPrinter<Void> VARIABLE_PRINTER = new JavaPrinter<Void>(TreePrinter.identity()) {
        @Override
        public J visitVariableDeclarations(J.VariableDeclarations multiVariable, Void unused) {
            visitModifiers(Space.formatFirstPrefix(multiVariable.getModifiers(), Space.EMPTY), unused);
            StringBuilder acc = getPrinter();
            if (!multiVariable.getModifiers().isEmpty()) {
                acc.append(' ');
            }
            if (multiVariable.getTypeExpression() != null) {
                acc.append(multiVariable.getTypeExpression().printTrimmed())
                        .append(' ');
            }
            acc.append(multiVariable.getDimensionsBeforeName().stream()
                    .map(d -> "[]")
                    .collect(Collectors.joining("")));
            if (multiVariable.getVarargs() != null) {
                acc.append("...");
            }
            acc.append(multiVariable.getVariables().stream()
                    .map(J.VariableDeclarations.NamedVariable::getSimpleName)
                    .collect(Collectors.joining(", ")));
            return multiVariable;
        }
    };
}
