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

public class LiteralToString {
    public static String toString(J.Literal clazz) {
        //noinspection ConstantConditions
        return LITERAL_PRINTER.print(clazz, null);
    }

    private static final JavaPrinter<Void> LITERAL_PRINTER = new JavaPrinter<Void>(TreePrinter.identity()) {
        @Override
        public J visitLiteral(J.Literal literal, Void unused) {
            StringBuilder acc = getPrinter();
            if(literal.getType() != null) {
                acc.append(literal.getType().getKeyword());
            } else {
                acc.append("<unknown type>");
            }
            acc.append(' ');
            acc.append(literal.getValueSource());
            return literal;
        }
    };
}
