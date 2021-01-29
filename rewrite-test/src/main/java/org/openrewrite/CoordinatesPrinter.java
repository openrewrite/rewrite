package org.openrewrite;

import org.junit.jupiter.api.parallel.Execution;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.internal.JavaPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

import java.util.HashMap;
import java.util.Map;

public class CoordinatesPrinter {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_FOREGROUND_WHITE = "\u001B[97m";
    public static final String ANSI_FOREGROUND_GREY = "\u001B[90m";
    public static final String ANSI_BACKGROUND_GREEN = "\u001B[42m";


    public static String printCoordinates(J.CompilationUnit cu, Class<? extends J> cursorFilter) {
        cu = new MapSpaces(cursorFilter, false).visitCompilationUnit(cu, ExecutionContext.builder().build());
        return cu.print();
    }

    public static String printCoordinatesWithColor(J.CompilationUnit cu, Class<? extends J> cursorFilter) {
        cu = new MapSpaces(cursorFilter, true).visitCompilationUnit(cu, ExecutionContext.builder().build());
        return cu.print();
    }

    private static class MapSpaces extends JavaIsoVisitor<ExecutionContext> {

        private final Class<? extends J> cursorFilter;
        private final boolean useColor;
        public MapSpaces(Class<? extends J> cursorFilter, boolean useColor) {
            this.cursorFilter = cursorFilter;
            this.useColor = useColor;
            setCursoringOn();
        }

        @Override
        public Space visitSpace(Space space, Space.Location loc, ExecutionContext context) {
            space = super.visitSpace(space, loc, context);
            StringBuilder spaceOut = new StringBuilder();

            if (cursorFilter == null) {
                J tree = getCursor().firstEnclosing(J.class);
                spaceOut.append(space.getWhitespace());

                if(useColor) {
                    spaceOut.append(ANSI_RESET).append(ANSI_FOREGROUND_GREY);
                }
                spaceOut
                        .append("<<(")
                        .append(tree == null ? "None" : tree.getClass().getSimpleName())
                        .append(",")
                        .append(loc)
                        .append(">>");
                        if (useColor) {
                            spaceOut.append(ANSI_BACKGROUND_GREEN).append(ANSI_FOREGROUND_WHITE);
                        }
            } else {
                J tree = getCursor().firstEnclosing(J.class);
                if (tree != null && tree.getClass() == cursorFilter) {
                    if (useColor) {
                        spaceOut
                                .append(space.getWhitespace())
                                .append(ANSI_BACKGROUND_GREEN).append(ANSI_FOREGROUND_WHITE)
                                .append("<<").append(loc).append(">>")
                                .append(ANSI_RESET).append(ANSI_FOREGROUND_GREY);
                    } else {
                        spaceOut
                                .append(space.getWhitespace())
                                .append("<<").append(loc).append(">>");
                    }
                } else {
                    return space;
                }
            }
            return space.withWhitespace(spaceOut.toString());
        }
    }
}