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
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.Space;

public class ClassDeclarationToString {
    public static String toString(J.ClassDeclaration clazz) {
        //noinspection ConstantConditions
        return CLASS_DECL_PRINTER.print(clazz, null);
    }

    private static final JavaPrinter<Void> CLASS_DECL_PRINTER = new JavaPrinter<Void>(TreePrinter.identity()) {
        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, Void unused) {
            visitModifiers(Space.formatFirstPrefix(classDecl.getModifiers(), Space.EMPTY), unused);
            StringBuilder acc = getPrinter();
            if (!classDecl.getModifiers().isEmpty()) {
                acc.append(' ');
            }
            switch (classDecl.getKind()) {
                case Class:
                    acc.append("class ");
                    break;
                case Enum:
                    acc.append("enum ");
                    break;
                case Interface:
                    acc.append("interface ");
                    break;
                case Annotation:
                    acc.append("@interface ");
                    break;
            }
            acc.append(classDecl.getName().printTrimmed());
            if (classDecl.getTypeParameters() != null) {
                visitContainer("<", classDecl.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", unused);
                acc.append(' ');
            }
            visitLeftPadded("extends", classDecl.getPadding().getExtends(), JLeftPadded.Location.EXTENDS, unused);
            if (classDecl.getImplements() != null) {
                if (J.ClassDeclaration.Kind.Type.Interface.equals(classDecl.getKind())) {
                    acc.append("extends");
                } else {
                    acc.append("implements");
                }
            }
            visitContainer("", classDecl.getPadding().getImplements(), JContainer.Location.IMPLEMENTS, ",", "", unused);
            return classDecl;
        }
    };
}
