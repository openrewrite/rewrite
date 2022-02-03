package org.openrewrite.java;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Position;

import java.util.IdentityHashMap;
import java.util.Map;

import static org.openrewrite.Tree.randomId;

@Incubating(since = "7.18.0")
public class UpdateSourcePositions extends Recipe {
    @Override
    public String getDisplayName() {
        return "Update source positions";
    }

    @Override
    public String getDescription() {
        return "Calculate start position and length for every AST element.";
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        Map<Tree, Position> positionMap = new IdentityHashMap<>();
        PositionPrintOutputCapture ppoc = new PositionPrintOutputCapture();

        JavaPrinter<ExecutionContext> printer = new JavaPrinter<ExecutionContext>() {
            final JavaPrinter<Integer> spacePrinter = new JavaPrinter<>();

            @Override
            public @Nullable J visit(@Nullable Tree tree, PrintOutputCapture<ExecutionContext> outputCapture) {
                if (tree == null) {
                    return null;
                }

                J t = (J) tree;

                PrintOutputCapture<Integer> prefix = new PrintOutputCapture<>(0);
                spacePrinter.visitSpace(t.getPrefix(), Space.Location.ANY, prefix);

                int startPosition = ppoc.pos + prefix.getOut().length();
                t = super.visit(tree, outputCapture);
                int length = ppoc.pos - startPosition;
                positionMap.put(t, new Position(randomId(), startPosition, length));

                return t;
            }
        };

        return new JavaVisitor<ExecutionContext>() {
            boolean firstVisit = true;

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree == null) {
                    return null;
                }
                if (firstVisit) {
                    printer.visit(tree, ppoc);
                    firstVisit = false;
                }

                Position pos = positionMap.get(tree);
                J t = ((J) tree).withMarkers(((J) tree).getMarkers().add(pos));
                return super.visit(t, ctx);
            }
        };
    }

    private static class PositionPrintOutputCapture extends PrintOutputCapture<ExecutionContext> {
        int pos;

        public PositionPrintOutputCapture() {
            super(new InMemoryExecutionContext());
        }

        @Override
        public PrintOutputCapture<ExecutionContext> append(char c) {
            pos++;
            return super.append(c);
        }

        @Override
        public PrintOutputCapture<ExecutionContext> append(@Nullable String text) {
            if (text != null) {
                pos += text.length();
            }
            return super.append(text);
        }
    }
}
