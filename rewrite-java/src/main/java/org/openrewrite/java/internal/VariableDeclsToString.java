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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;

import java.util.stream.Collectors;

public class VariableDeclsToString {
    public static String toString(J.VariableDecls v) {
        //noinspection ConstantConditions
        return VARIABLE_PRINTER.print(v, null);
    }

    private static final JavaPrinter<Void> VARIABLE_PRINTER = new JavaPrinter<Void>(TreePrinter.identity()) {
        @Override
        public J visitMultiVariable(J.VariableDecls multiVariable, Void unused) {
            visitModifiers(multiVariable.getModifiers(), unused);
            StringBuilder acc = getPrinterAcc();
            if (multiVariable.getModifiers().isEmpty()) {
                acc.append(' ');
            }
            if (multiVariable.getTypeExpr() != null) {
                acc.append(multiVariable.getTypeExpr().printTrimmed())
                        .append(' ');
            }
            acc.append(multiVariable.getDimensionsBeforeName().stream()
                    .map(d -> "[]")
                    .collect(Collectors.joining("")));
            if (multiVariable.getVarargs() != null) {
                acc.append("...");
            }
            acc.append(multiVariable.getVars().stream()
                    .map(JRightPadded::getElem)
                    .map(J.VariableDecls.NamedVar::getSimpleName)
                    .collect(Collectors.joining(", ")));
            return multiVariable;
        }
    };
}
