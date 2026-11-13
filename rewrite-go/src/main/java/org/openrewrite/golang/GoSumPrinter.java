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
import org.openrewrite.golang.tree.GoSum;
import org.openrewrite.golang.tree.GoSumTree;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

/**
 * Prints {@code go.sum} LSTs. A port of the native printer in {@code pkg/printer/gosum_printer.go},
 * with which it must agree byte for byte.
 */
public class GoSumPrinter<P> extends GoSumVisitor<PrintOutputCapture<P>> {

    @Override
    public GoSumTree visitGoSum(GoSum goSum, PrintOutputCapture<P> p) {
        beforeSyntax(goSum.getPrefix(), goSum.getMarkers(), p);
        for (JRightPadded<GoSum.Line> line : goSum.getLines()) {
            visit(line.getElement(), p);
            visitSpace(line.getAfter(), p);
        }
        afterSyntax(goSum.getMarkers(), p);
        visitSpace(goSum.getEof(), p);
        return goSum;
    }

    @Override
    public GoSumTree visitLine(GoSum.Line line, PrintOutputCapture<P> p) {
        beforeSyntax(line.getPrefix(), line.getMarkers(), p);
        p.append(line.getModulePath());
        p.append(' ');
        p.append(line.getVersion());
        if (line.isGoMod()) {
            p.append("/go.mod");
        }
        p.append(' ');
        p.append(line.getHash());
        afterSyntax(line.getMarkers(), p);
        return line;
    }

    @Override
    public Space visitSpace(Space space, PrintOutputCapture<P> p) {
        GolangPrinter.printSpace(space, getCursor(), p);
        return space;
    }

    private void beforeSyntax(Space prefix, Markers markers, PrintOutputCapture<P> p) {
        GolangPrinter.beforeSyntax(prefix, markers, getCursor(), p);
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        GolangPrinter.afterSyntax(markers, getCursor(), p);
    }
}
