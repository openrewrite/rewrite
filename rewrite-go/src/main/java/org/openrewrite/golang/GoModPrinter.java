/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.golang;

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.golang.tree.GoMod;
import org.openrewrite.golang.tree.GoModTree;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.List;

/**
 * Prints {@code go.mod} LSTs. A port of the native printer in {@code pkg/printer/gomod_printer.go},
 * with which it must agree byte for byte.
 */
public class GoModPrinter<P> extends GoModVisitor<PrintOutputCapture<P>> {

    @Override
    public GoModTree visitGoMod(GoMod goMod, PrintOutputCapture<P> p) {
        beforeSyntax(goMod.getPrefix(), goMod.getMarkers(), p);
        printStatements(goMod.getStatements(), p);
        afterSyntax(goMod.getMarkers(), p);
        visitSpace(goMod.getEof(), p);
        return goMod;
    }

    @Override
    public GoModTree visitDirective(GoMod.Directive directive, PrintOutputCapture<P> p) {
        beforeSyntax(directive.getPrefix(), directive.getMarkers(), p);
        p.append(directive.getKeyword());
        for (GoMod.Value value : directive.getValues()) {
            visit(value, p);
        }
        afterSyntax(directive.getMarkers(), p);
        return directive;
    }

    @Override
    public GoModTree visitBlock(GoMod.Block block, PrintOutputCapture<P> p) {
        beforeSyntax(block.getPrefix(), block.getMarkers(), p);
        p.append(block.getKeyword());
        visitSpace(block.getBeforeLParen(), p);
        p.append('(');
        printStatements(block.getEntries(), p);
        visitSpace(block.getBeforeRParen(), p);
        p.append(')');
        afterSyntax(block.getMarkers(), p);
        return block;
    }

    @Override
    public GoModTree visitValue(GoMod.Value value, PrintOutputCapture<P> p) {
        beforeSyntax(value.getPrefix(), value.getMarkers(), p);
        p.append(value.getText());
        afterSyntax(value.getMarkers(), p);
        return value;
    }

    @Override
    public Space visitSpace(Space space, PrintOutputCapture<P> p) {
        GolangPrinter.printSpace(space, getCursor(), p);
        return space;
    }

    private void printStatements(List<JRightPadded<GoMod.GoModStatement>> statements, PrintOutputCapture<P> p) {
        for (JRightPadded<GoMod.GoModStatement> statement : statements) {
            visit(statement.getElement(), p);
            visitSpace(statement.getAfter(), p);
        }
    }

    private void beforeSyntax(Space prefix, Markers markers, PrintOutputCapture<P> p) {
        GolangPrinter.beforeSyntax(prefix, markers, getCursor(), p);
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        GolangPrinter.afterSyntax(markers, getCursor(), p);
    }
}
