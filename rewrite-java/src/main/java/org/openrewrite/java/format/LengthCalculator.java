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
package org.openrewrite.java.format;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import static java.util.Collections.emptyList;

class LengthCalculator {

    public static int computeTreeLineLength(J tree, Cursor cursor) {
        Object cursorValue = cursor.getValue();
        if (cursorValue instanceof J) {
            J j = (J) cursorValue;
            boolean hasNewLine = j.getPrefix().getWhitespace().contains("\n") || j.getComments().stream().anyMatch(c -> c.getSuffix().contains("\n"));
            Cursor parent = cursor.getParentTreeCursor();
            boolean isCompilationUnit = parent.getValue() instanceof J.CompilationUnit;
            if (!hasNewLine && !isCompilationUnit) {
                return computeTreeLineLength(parent.getValue(), parent);
            }
        } else {
            throw new RuntimeException("Unable to calculate length due to unexpected cursor value: " + cursorValue.getClass());
        }

        TreeVisitor<?, PrintOutputCapture<TreeVisitor<?, ?>>> printer = tree.printer(cursor);
        PrintOutputCapture<TreeVisitor<?, ?>> capture = new PrintOutputCapture<>(printer, PrintOutputCapture.MarkerPrinter.SANITIZED);

        printer.visit(trimPrefix(tree), capture, cursor.getParentOrThrow());

        return capture.getOut().length() + getSuffixLength(tree);
    }

    private static int getSuffixLength(J tree) {
        if (tree instanceof Statement && needsSemicolon((Statement) tree)) {
            return 1;
        }
        return 0;
    }

    private static boolean needsSemicolon(Statement statement) {
        return statement instanceof J.MethodInvocation || statement instanceof J.VariableDeclarations || statement instanceof J.Assignment || statement instanceof J.Package;
    }

    private static J trimPrefix(J tree) {
        Space prefix = tree.getPrefix();
        String whitespace = prefix.getLastWhitespace().replaceFirst("^.*\\n*", "");
        prefix = prefix.withComments(emptyList()).withWhitespace(whitespace);
        return tree.withPrefix(prefix);
    }
}
