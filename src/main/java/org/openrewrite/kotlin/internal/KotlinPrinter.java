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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.kotlin.marker.EmptyBody;
import org.openrewrite.kotlin.tree.K;

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
            switch (classDecl.getKind()) {
                case Class:
                    kind = "class";
                    break;
                case Enum:
                    kind = "enum";
                    break;
                case Interface:
                    kind = "interface";
                    break;
                case Annotation:
                    kind = "@interface";
                    break;
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
            visitLeftPadded(":", classDecl.getPadding().getExtends(), JLeftPadded.Location.EXTENDS, p);
            visitContainer(":", classDecl.getPadding().getImplements(), JContainer.Location.IMPLEMENTS, ",", null, p);
            if (!classDecl.getBody().getMarkers().findFirst(EmptyBody.class).isPresent()) {
                visit(classDecl.getBody(), p);
            }
            afterSyntax(classDecl, p);
            return classDecl;
        }
    }
}
