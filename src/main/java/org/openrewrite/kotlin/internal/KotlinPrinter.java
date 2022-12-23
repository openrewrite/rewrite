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
package org.openrewrite.kotlin.internal;

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.marker.CompactConstructor;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.kotlin.marker.EmptyBody;
import org.openrewrite.kotlin.marker.MethodClassifier;
import org.openrewrite.kotlin.marker.PropertyClassifier;
import org.openrewrite.kotlin.tree.K;

import java.util.Optional;

public class KotlinPrinter<P> extends KotlinVisitor<PrintOutputCapture<P>> {
    private final KotlinJavaPrinter delegate = new KotlinJavaPrinter();

    @Override
    public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
        if (!(tree instanceof K)) {
            // re-route printing to the java printer
            return delegate.visit(tree, p);
        } else {
            return super.visit(tree, p);
        }
    }

    private class KotlinJavaPrinter extends JavaPrinter<P> {
        @Override
        public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
            if (tree instanceof K) {
                // re-route printing back up to groovy
                return KotlinJavaPrinter.this.visit(tree, p);
            } else {
                return super.visit(tree, p);
            }
        }

        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, PrintOutputCapture<P> p) {
            String kind = "";
            if (classDecl.getKind() == J.ClassDeclaration.Kind.Type.Class || classDecl.getKind() == J.ClassDeclaration.Kind.Type.Enum || classDecl.getKind() == J.ClassDeclaration.Kind.Type.Annotation) {
                kind = "class";
            } else if (classDecl.getKind() == J.ClassDeclaration.Kind.Type.Interface) {
                kind = "interface";
            } else {
                throw new IllegalStateException("Implement me.");
            }

            beforeSyntax(classDecl, Space.Location.CLASS_DECLARATION_PREFIX, p);
            visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
            visit(classDecl.getLeadingAnnotations(), p);
            for (J.Modifier m : classDecl.getModifiers()) {
                visitModifier(m, p);
            }
            visit(classDecl.getAnnotations().getKind().getAnnotations(), p);
            visitSpace(classDecl.getAnnotations().getKind().getPrefix(), Space.Location.CLASS_KIND, p);
            p.append(kind);
            visit(classDecl.getName(), p);
            visitContainer("<", classDecl.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
            // TODO: requires extends and implements
            visitLeftPadded(":", classDecl.getPadding().getExtends(), JLeftPadded.Location.EXTENDS, p);
            visitContainer(":", classDecl.getPadding().getImplements(), JContainer.Location.IMPLEMENTS, ",", null, p);
            if (!classDecl.getBody().getMarkers().findFirst(EmptyBody.class).isPresent()) {
                visit(classDecl.getBody(), p);
            }
            afterSyntax(classDecl, p);
            return classDecl;
        }

        @Override
        public J visitMethodDeclaration(J.MethodDeclaration method, PrintOutputCapture<P> p) {
            beforeSyntax(method, Space.Location.METHOD_DECLARATION_PREFIX, p);
            visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
            visit(method.getLeadingAnnotations(), p);
            for (J.Modifier m : method.getModifiers()) {
                visitModifier(m, p);
            }

            MethodClassifier methodClassifier = method.getMarkers().findFirst(MethodClassifier.class).orElse(null);
            if (methodClassifier != null) {
                p.append(methodClassifier.getPrefix().getWhitespace());
                p.append("fun");
            }

            visit(method.getName(), p);
            if (!method.getMarkers().findFirst(CompactConstructor.class).isPresent()) {
                visitContainer("(", method.getPadding().getParameters(), JContainer.Location.METHOD_DECLARATION_PARAMETERS, ",", ")", p);
            }
            visit(method.getBody(), p);
            afterSyntax(method, p);
            return method;
        }

        @Override
        public J visitVariableDeclarations(J.VariableDeclarations multiVariable, PrintOutputCapture<P> p) {
            beforeSyntax(multiVariable, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
            visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
            visit(multiVariable.getLeadingAnnotations(), p);
            for (J.Modifier m : multiVariable.getModifiers()) {
                visitModifier(m, p);
            }

            PropertyClassifier propertyClassifier = multiVariable.getMarkers().findFirst(PropertyClassifier.class).orElse(null);
            if (propertyClassifier != null) {
                boolean isVal = propertyClassifier.getClassifierType() == PropertyClassifier.ClassifierType.VAL;
                p.append(propertyClassifier.getPrefix().getWhitespace());
                p.append(isVal ? "val" : "var");
            }

            J.VariableDeclarations.NamedVariable variable = multiVariable.getVariables().get(0);
            beforeSyntax(variable, Space.Location.VARIABLE_PREFIX, p);
            visit(variable.getName(), p);

            if (multiVariable.getTypeExpression() != null) {
                p.append(":");
                visit(multiVariable.getTypeExpression(), p);
            }

            visitLeftPadded("=", variable.getPadding().getInitializer(), JLeftPadded.Location.VARIABLE_INITIALIZER, p);
            afterSyntax(variable, p);

            afterSyntax(multiVariable, p);
            return multiVariable;
        }
    }
}
