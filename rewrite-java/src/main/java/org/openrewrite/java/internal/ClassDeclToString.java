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

import org.openrewrite.java.tree.J;

public class ClassDeclToString {
    public static String toString(J.ClassDecl clazz) {
        return CLASS_DECL_PRINTER.visit(clazz);
    }

    private static final PrintJava CLASS_DECL_PRINTER = new PrintJava() {
        @Override
        public String visitClassDecl(J.ClassDecl classDecl) {
            String modifiers = visitModifiers(classDecl.getModifiers());

            String kind = "";
            if (classDecl.getKind() instanceof J.ClassDecl.Kind.Class) {
                kind = "class ";
            } else if (classDecl.getKind() instanceof J.ClassDecl.Kind.Enum) {
                kind = "enum ";
            } else if (classDecl.getKind() instanceof J.ClassDecl.Kind.Interface) {
                kind = "interface ";
            } else if (classDecl.getKind() instanceof J.ClassDecl.Kind.Annotation) {
                kind = "@interface ";
            }

            return modifiers + kind + classDecl.getName().printTrimmed() +
                    (classDecl.getTypeParameters() == null ? "" : classDecl.getTypeParameters().printTrimmed() + " ") +
                    (classDecl.getExtends() == null ? "" : "extends" + visit(classDecl.getExtends().getFrom()) + " ") +
                    (classDecl.getImplements() == null ? "" : (classDecl.getKind() instanceof J.ClassDecl.Kind.Interface ? "extends " : "implements ") +
                            visit(classDecl.getImplements().getFrom(), ","));
        }
    };
}
