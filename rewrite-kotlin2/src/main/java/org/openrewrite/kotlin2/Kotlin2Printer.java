/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.kotlin2;

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin2.tree.Kt;
import org.openrewrite.marker.Marker;

/**
 * Printer for Kotlin 2 language constructs.
 * Converts K2 AST elements back to Kotlin source code.
 */
public class Kotlin2Printer<P> extends Kotlin2Visitor<PrintOutputCapture<P>> {
    
    private final JavaPrinter<P> javaPrinter = new JavaPrinter<P>() {
        @Override
        public J visit(J tree, PrintOutputCapture<P> p) {
            if (tree instanceof Kt) {
                return Kotlin2Printer.this.visit(tree, p);
            }
            return super.visit(tree, p);
        }
    };

    @Override
    public J visitCompilationUnit(Kt.CompilationUnit cu, PrintOutputCapture<P> p) {
        beforeSyntax(cu, p);
        
        if (cu.getPackageDeclaration() != null) {
            visit(cu.getPackageDeclaration(), p);
            p.append("\n\n");
        }
        
        for (J.Import import_ : cu.getImports()) {
            visit(import_, p);
            p.append("\n");
        }
        
        if (!cu.getImports().isEmpty()) {
            p.append("\n");
        }
        
        for (J statement : cu.getStatements()) {
            visit(statement, p);
            if (statement instanceof J.ClassDeclaration || statement instanceof J.MethodDeclaration) {
                p.append("\n\n");
            } else {
                p.append("\n");
            }
        }
        
        visitSpace(cu.getEof(), p);
        afterSyntax(cu, p);
        
        return cu;
    }

    @Override
    public J visitContextReceiver(Kt.ContextReceiver contextReceiver, PrintOutputCapture<P> p) {
        beforeSyntax(contextReceiver, p);
        p.append("context(");
        visit(contextReceiver.getContext(), p);
        p.append(")");
        afterSyntax(contextReceiver, p);
        return contextReceiver;
    }

    @Override
    public J visitDefinitelyNonNullableType(Kt.DefinitelyNonNullableType type, PrintOutputCapture<P> p) {
        beforeSyntax(type, p);
        visit(type.getBaseType(), p);
        p.append("!!");
        afterSyntax(type, p);
        return type;
    }

    @Override
    public J visit(J tree, PrintOutputCapture<P> p) {
        if (tree instanceof K2) {
            return super.visit(tree, p);
        }
        return javaPrinter.visit(tree, p);
    }

    private void beforeSyntax(J j, PrintOutputCapture<P> p) {
        visitSpace(j.getPrefix(), p);
        visitMarkers(j.getMarkers(), p);
    }

    private void afterSyntax(J j, PrintOutputCapture<P> p) {
        // Hook for any post-syntax processing
    }
}