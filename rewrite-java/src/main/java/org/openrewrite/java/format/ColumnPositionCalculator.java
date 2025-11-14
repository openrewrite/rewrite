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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.java.tree.J;

import java.util.concurrent.CancellationException;

/**
 * Utility class for computing column positions of elements within a Java AST.
 */
class ColumnPositionCalculator {

    /**
     * Computes the column position of a target element within a tree by printing the tree
     * up to the target element and counting characters from the last newline.
     *
     * @param tree          The tree to print
     * @param targetElement The element whose column position we want to find
     * @param cursor        The cursor context
     * @return The column position of the target element
     */
    public static int computeColumnPosition(J tree, J targetElement, Cursor cursor) {
        TreeVisitor<?, PrintOutputCapture<TreeVisitor<?, ?>>> printer = tree.printer(cursor);
        PrintOutputCapture<TreeVisitor<?, ?>> capture = new PrintOutputCapture<TreeVisitor<?, ?>>(printer) {
            @Override
            public PrintOutputCapture<TreeVisitor<?, ?>> append(@Nullable String text) {
                if (targetElement.isScope(getContext().getCursor().getValue())) {
                    throw new CancellationException();
                }
                return super.append(text);
            }
        };
        try {
            printer.visit(tree, capture, cursor.getParentOrThrow());
            throw new IllegalStateException("Target element not found in tree");
        } catch (CancellationException ignored) {
            String out = capture.getOut();
            int lineBreakIndex = out.lastIndexOf('\n');
            return (out.length() - (lineBreakIndex == -1 ? 0 : lineBreakIndex)) - 1;
        } catch (RecipeRunException e) {
            if (e.getCause() instanceof CancellationException) {
                String out = capture.getOut();
                int lineBreakIndex = out.lastIndexOf('\n');
                return (out.length() - (lineBreakIndex == -1 ? 0 : lineBreakIndex)) - 1;
            }
            throw e;
        }
    }
}
