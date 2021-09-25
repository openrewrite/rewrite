/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy;

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.groovy.marker.Semicolon;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;

public class GroovyPrinter<P> extends GroovyVisitor<PrintOutputCapture<P>> {
    private final GroovyJavaPrinter delegate = new GroovyJavaPrinter();

    @Override
    public G visitCompilationUnit(G.CompilationUnit cu, PrintOutputCapture<P> p) {
        visitSpace(cu.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p);
        visitMarkers(cu.getMarkers(), p);

        JRightPadded<J.Package> pkg = cu.getPadding().getPackageDeclaration();
        if (pkg != null) {
            delegate.visit(pkg.getElement(), p);
            delegate.visitSpace(pkg.getAfter(), Space.Location.PACKAGE_SUFFIX, p);
        }

        delegate.visit(cu.getStatements(), p);
        visitSpace(cu.getEof(), Space.Location.COMPILATION_UNIT_EOF, p);
        return cu;
    }

    private class GroovyJavaPrinter extends JavaPrinter<P> {
        @Override
        public J visitLambda(J.Lambda lambda, PrintOutputCapture<P> p) {
            p.out.append('{');
            visit(lambda.getParameters(), p);
            if (!lambda.getParameters().getParameters().isEmpty()) {
                visitSpace(lambda.getArrow(), Space.Location.LAMBDA_ARROW_PREFIX, p);
                p.out.append("->");
            }
            if (lambda.getBody() instanceof J.Block) {
                J.Block block = (J.Block) lambda.getBody();
                visit(block.getStatements(), p);
                visitSpace(block.getEnd(), Space.Location.BLOCK_END, p);
            } else {
                visit(lambda.getBody(), p);
            }
            p.out.append('}');
            return lambda;
        }

        @Override
        public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
            if (marker instanceof Semicolon) {
                p.out.append(';');
            }
            return super.visitMarker(marker, p);
        }
    }
}
