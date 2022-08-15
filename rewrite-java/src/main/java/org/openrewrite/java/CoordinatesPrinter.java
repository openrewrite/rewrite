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
package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

/**
 * Utility to print a compilation unit with tree coordinates embedded in the output.
 * This can be useful to discover the location within the source code that the coordinate represents.
 */
public class CoordinatesPrinter {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_FOREGROUND_WHITE = "\u001B[97m";
    public static final String ANSI_FOREGROUND_GREY = "\u001B[90m";
    public static final String ANSI_BACKGROUND_GREEN = "\u001B[42m";


    /**
     * This will print out the compilation unit and embed the tree coordinates (at each Space position). If a filter
     * is supplied, the printer will only print coordinates for the tree elements of that given type.
     * <p>
     * See {@link CoordinatesPrinter#printCoordinatesWithColor(J.CompilationUnit, Class)} for a variant of this method
     * that will output the string with ASCII color codes.
     *
     * @param cu           The compilation unit to print
     * @param cursorFilter An optional cursor filter.
     * @return The printed tree.
     */
    public static String printCoordinates(J.CompilationUnit cu, @Nullable Class<? extends J> cursorFilter) {
        cu = new MapSpaces(cursorFilter, false).visitCompilationUnit(cu, new InMemoryExecutionContext());
        return cu.print();
    }

    /**
     * This will print out the compilation unit and embed the tree coordinates (at each Space position). If a filter
     * is supplied, the printer will only print coordinates for the tree elements of that given type. This variant also
     * uses ANSI Color codes to better highlight the differences between the coordinates and the actual source code.
     *
     * @param cu           The compilation unit to print
     * @param cursorFilter An optional cursor filter.
     * @return The printed tree.
     */
    public static String printCoordinatesWithColor(J.CompilationUnit cu, Class<? extends J> cursorFilter) {
        cu = new MapSpaces(cursorFilter, true).visitCompilationUnit(cu, new InMemoryExecutionContext());
        return cu.print();
    }

    private static class MapSpaces extends JavaIsoVisitor<ExecutionContext> {
        @Nullable
        private final Class<? extends J> cursorFilter;

        private final boolean useColor;

        public MapSpaces(@Nullable Class<? extends J> cursorFilter, boolean useColor) {
            this.cursorFilter = cursorFilter;
            this.useColor = useColor;
        }

        @Override
        public Space visitSpace(Space space, Space.Location loc, ExecutionContext context) {
            space = super.visitSpace(space, loc, context);
            StringBuilder spaceOut = new StringBuilder();

            if (cursorFilter == null) {
                J tree = getCursor().firstEnclosing(J.class);
                spaceOut.append(space.getWhitespace());

                if (useColor) {
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
