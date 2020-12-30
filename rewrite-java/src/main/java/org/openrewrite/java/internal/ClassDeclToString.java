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

public class ClassDeclToString {
    public static String toString(J.ClassDecl clazz) {
        return CLASS_DECL_PRINTER.visit(clazz, null);
    }

    private static final PrintJava<Void> CLASS_DECL_PRINTER = new PrintJava<Void>(TreePrinter.identity()) {
        @Override
        public String visitClassDecl(J.ClassDecl classDecl, Void unused) {
            String modifiers = visitModifiers(classDecl.getModifiers()).trim();

            String kind = "";
            switch (classDecl.getKind().getElem()) {
                case Class:
                    kind = "class ";
                    break;
                case Enum:
                    kind = "enum ";
                    break;
                case Interface:
                    kind = "interface ";
                    break;
                case Annotation:
                    kind = "@interface ";
                    break;
            }

            return (modifiers.isEmpty() ? "" : modifiers + " ") +
                    kind + classDecl.getName().printTrimmed() +
                    (classDecl.getTypeParameters() == null ? "" : visit("<", classDecl.getTypeParameters(), ",", ">", unused) + " ") +
                    visit("extends", classDecl.getExtends(), unused) +
                    (classDecl.getImplements() == null ? "" : (J.ClassDecl.Kind.Interface.equals(classDecl.getKind().getElem()) ? "extends" : "implements") +
                            visit("", classDecl.getImplements(), ",", "", unused));
        }
    };
}
