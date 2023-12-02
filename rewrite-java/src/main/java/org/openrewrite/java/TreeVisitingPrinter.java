/*
 * Copyright 2023 the original author or authors.
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

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.StreamSupport.stream;


/**
 * A visitor to print a tree visiting order and hierarchy in format like below.
 * <pre>
 * ----J.CompilationUnit
 *     |-------J.Import | "..."
 *     |       \---J.FieldAccess | "..."
 *     |           ...
 *     \---J.ClassDeclaration
 *         |---J.Identifier | "..."
 *         \---J.Block
 *              ...
 * </pre>
 */
public class TreeVisitingPrinter extends TreeVisitor<Tree, ExecutionContext> {
    private static final String TAB = "    ";
    private static final String ELEMENT_PREFIX = "\\---";
    private static final String CONTINUE_PREFIX = "----";
    private static final String UNVISITED_PREFIX = "#";
    private static final char BRANCH_CONTINUE_CHAR = '|';
    private static final char BRANCH_END_CHAR = '\\';
    private static final int CONTENT_MAX_LENGTH = 120;

    private List<Object> lastCursorStack;
    private final List<StringBuilder> outputLines;
    private final boolean skipUnvisitedElement;

    protected TreeVisitingPrinter(boolean skipUnvisitedElement) {
        lastCursorStack = new ArrayList<>();
        outputLines = new ArrayList<>();
        this.skipUnvisitedElement = skipUnvisitedElement;
    }

    /**
     * print the entire LST tree with skip unvisited elements by given any cursor in this tree.
     */
    public static String printTree(Cursor cursor) {
        return findRootOfJType(cursor).map(TreeVisitingPrinter::printTree).orElse("");
    }

    /**
     * print tree with skip unvisited elements
     */
    public static String printTree(Tree tree) {
        TreeVisitingPrinter visitor = new TreeVisitingPrinter(true);
        visitor.visit(tree, new InMemoryExecutionContext());
        return visitor.print();
    }

    /**
     * print entire tree including all unvisited elements by given any cursor in this tree.
     */
    public static String printTreeAll(Cursor cursor) {
        return findRootOfJType(cursor).map(TreeVisitingPrinter::printTreeAll).orElse("");
    }

    /**
     * print tree including all unvisited elements
     */
    public static String printTreeAll(Tree tree) {
        TreeVisitingPrinter visitor = new TreeVisitingPrinter(false);
        visitor.visit(tree, new InMemoryExecutionContext());
        return visitor.print();
    }

    private static Optional<Tree> findRootOfJType(Cursor cursor) {
        List<Object> cursorStack =
            stream(Spliterators.spliteratorUnknownSize(cursor.getPath(), 0), false)
                .collect(Collectors.toList());
        Collections.reverse(cursorStack);
        return cursorStack.stream().filter(Tree.class::isInstance)
            .map(Tree.class::cast)
            .findFirst();
    }

    private String print() {
        return String.join("\n", outputLines);
    }

    /**
     * print left padding for a line
     * @param depth, depth starts from 0 (the root)
     */
    private static String leftPadding(int depth) {
        StringBuilder sb = new StringBuilder();
        int tabCount = depth - 1;
        if (tabCount > 0) {
            sb.append(String.join("", Collections.nCopies(tabCount, TAB)));
        }
        // only root has not prefix
        if (depth > 0) {
            sb.append(ELEMENT_PREFIX);
        }
        return sb.toString();
    }

    /**
     * Print a vertical line that connects the current element to the latest sibling.
     * @param depth current element depth
     */
    private void connectToLatestSibling(int depth) {
        if (depth <= 1) {
            return;
        }

        int pos = (depth - 1) * TAB.length();
        for (int i = outputLines.size() - 1; i > 0; i--) {
            StringBuilder line = outputLines.get(i);
            if (pos >= line.length()) {
                break;
            }

            if (line.charAt(pos) != ' ') {
                if (line.charAt(pos) == BRANCH_END_CHAR) {
                    line.setCharAt(pos, BRANCH_CONTINUE_CHAR);
                }
                break;
            }
            line.setCharAt(pos, BRANCH_CONTINUE_CHAR);
        }
    }

    private static String printTreeElement(Tree tree) {
        // skip some specific types printed in the output to make the output looks clean
        if (tree instanceof J.CompilationUnit ||
                tree instanceof J.ClassDeclaration ||
                tree instanceof J.Block ||
                tree instanceof J.Empty ||
                tree instanceof J.Try ||
                tree instanceof J.Try.Catch ||
                tree instanceof J.ForLoop ||
                tree instanceof J.WhileLoop ||
                tree instanceof J.DoWhileLoop ||
                tree instanceof J.Lambda ||
                tree instanceof J.Lambda.Parameters ||
                tree instanceof J.If ||
                tree instanceof J.EnumValueSet ||
                tree instanceof J.TypeParameter
        ) {
            return "";
        }

        if (tree instanceof J.Literal) {
            String s = ((J.Literal) tree).getValueSource();
            return s != null ? s : "";
        }

        String[] lines = tree.toString().split("\n");
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            output.append(lines[i].trim());
            if (i < lines.length - 1) {
                output.append(" ");
            }
        }
        return output.toString();
    }

    private String truncate(String content) {
        if (content.length() > CONTENT_MAX_LENGTH) {
            return content.substring(0, CONTENT_MAX_LENGTH - 3) + "...";
        }
        return content;
    }

    private static String printSpace(Space space) {
        StringBuilder sb = new StringBuilder();
        sb.append(" whitespace=\"")
            .append(space.getWhitespace()).append("\"");
        sb.append(" comments=\"")
            .append(String.join(",", space.getComments().stream().map(c -> c.printComment(new Cursor(null, "root"))).collect(Collectors.toList())))
            .append("\"");;
        return sb.toString().replace("\n", "\\s\n");
    }

    @Override
    public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
        if (tree == null) {
            return super.visit((Tree) null, ctx);
        }

        Cursor cursor = this.getCursor();
        List<Object> cursorStack =
                stream(Spliterators.spliteratorUnknownSize(cursor.getPath(), 0), false)
                        .collect(Collectors.toList());
        Collections.reverse(cursorStack);
        int depth = cursorStack.size();

        // Compare lastCursorStack vs cursorStack, find the fork and print the diff
        int diffPos = -1;
        for (int i = 0; i < cursorStack.size(); i++) {
            if (i >= lastCursorStack.size() || cursorStack.get(i) != lastCursorStack.get(i)) {
                diffPos = i;
                break;
            }
        }

        StringBuilder line = new StringBuilder();

        // print cursor stack diff
        if (diffPos >= 0) {
            for (int i = diffPos; i < cursorStack.size(); i++) {
                Object element = cursorStack.get(i);
                if (skipUnvisitedElement) {
                    // skip unvisited elements, just print indents in the line
                    if (i == diffPos) {
                        line.append(leftPadding(i));
                        connectToLatestSibling(i);
                    } else {
                        line.append(CONTINUE_PREFIX);
                    }
                } else {
                    // print each unvisited element to a line
                    connectToLatestSibling(i);
                    StringBuilder newLine = new StringBuilder()
                        .append(leftPadding(i))
                        .append(UNVISITED_PREFIX)
                        .append(element instanceof String ? element : element.getClass().getSimpleName());

                    if (element instanceof JRightPadded) {
                        JRightPadded rp = (JRightPadded) element;
                        newLine.append(" | ");
                        newLine.append(" after = ").append(printSpace(rp.getAfter()));
                    }

                    if (element instanceof JLeftPadded) {
                        JLeftPadded lp = (JLeftPadded) element;
                        newLine.append(" | ");
                        newLine.append(" before = ").append(printSpace(lp.getBefore()));
                    }

                    outputLines.add(newLine);
                }
            }
        }

        // print current visiting element
        String typeName = tree instanceof J
            ? tree.getClass().getCanonicalName().substring(tree.getClass().getPackage().getName().length() + 1)
            : tree.getClass().getCanonicalName();

        if (skipUnvisitedElement) {
            boolean leftPadded = diffPos >= 0;
            if (leftPadded) {
                line.append(CONTINUE_PREFIX);
            } else {
                connectToLatestSibling(depth);
                line.append(leftPadding(depth));
            }
            line.append(typeName);
        } else {
            connectToLatestSibling(depth);
            line.append(leftPadding(depth)).append(typeName);
        }

        String content = truncate(printTreeElement(tree));
        if (!content.isEmpty()) {
            line.append(" | \"").append(content).append("\"");
        }
        outputLines.add(line);

        cursorStack.add(tree);
        lastCursorStack = cursorStack;
        return super.visit(tree, ctx);
    }
}
