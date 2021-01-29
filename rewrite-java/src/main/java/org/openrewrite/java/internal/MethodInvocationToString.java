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
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;

import java.util.stream.Collectors;

public class MethodInvocationToString {
    public static String toString(J.MethodInvocation method) {
        //noinspection ConstantConditions
        return METHOD_PRINTER.print(method, null);
    }

    private static final JavaPrinter<Void> METHOD_PRINTER = new JavaPrinter<Void>(TreePrinter.identity()) {
        @Override
        public J visitMethodInvocation(J.MethodInvocation method, Void unused) {
            StringBuilder acc = getPrinterAcc();
            JavaType.Method type = method.getType();
            if (type == null) {
                acc.append("<unknown receiver type>.");
                acc.append(method.getSimpleName());
                acc.append("(?)");
            } else {
                acc.append(type.getDeclaringType().getFullyQualifiedName()
                        .replaceFirst("^java\\.lang\\.", ""));
                acc.append('.');
                visit("<", method.getTypeParameters(), ",", ">", unused);
                acc.append(type.getName());
                acc.append('(');
                acc.append(String.join(",", type.getParamNames()));
                acc.append(')');
            }
            return method;
        }
    };
}
