package org.openrewrite.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Collectors;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;

import static java.util.stream.StreamSupport.*;


/**
 * A visitor to print a tree visiting order and hierarchy in format like below.
 * <pre>
 * #root
 * \---J.CompilationUnit | "..."
 *     |---#JRightPadded
 *     |   \---J.Import | "..."
 *     \---J.ClassDeclaration | "..."
 *         |---J.Identifier | "..."
 *         \---#JRightPadded
 *             \---J.Return | "..."
 * </pre>
 */
public class TreeVisitingPrinter extends JavaIsoVisitor<ExecutionContext> {
    private static final String TAB = "    ";
    private static final String PREFIX = "\\---";
    // this prefix means it will not be visited by a visitX method
    private static final String SKIPPED_PADDED_PREFIX = "#";
    private static final char BRANCH_CONTINUE_CHAR = '|';
    private static final char BRANCH_END_CHAR = '\\';

    private List<Object> lastCursorStack;
    private final List<StringBuilder> outputLines;

    protected TreeVisitingPrinter() {
        lastCursorStack = new ArrayList<>();
        outputLines = new ArrayList<>();
    }

    public static String printTree(J j) {
        TreeVisitingPrinter visitor = new TreeVisitingPrinter();
        visitor.visit(j, new InMemoryExecutionContext());
        return visitor.print();
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
            sb.append(PREFIX);
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
        for (int j = outputLines.size() - 1; j > 0; j--) {
            StringBuilder line = outputLines.get(j);
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
        if (tree instanceof J.CompilationUnit
            || tree instanceof J.ClassDeclaration
            || tree instanceof J.Block
            || tree instanceof J.Empty
            || tree instanceof J.Literal
            || tree instanceof J.Try
            || tree instanceof J.Try.Catch
            || tree instanceof J.WhileLoop
        ) {
            return "";
        }

        return tree.toString();
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
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

        // print new added lines in the cursor stack
        if (diffPos >= 0) {
            for (int i = diffPos; i < cursorStack.size(); i++) {
                Object element = cursorStack.get(i);
                connectToLatestSibling(i);
                StringBuilder newLine = new StringBuilder()
                    .append(leftPadding(i))
                    .append(SKIPPED_PADDED_PREFIX)
                    .append(element instanceof String ? element : element.getClass().getSimpleName());
                outputLines.add(newLine);
            }
        }

        connectToLatestSibling(depth);

        // print current visiting element
        StringBuilder line = new StringBuilder();
        String typeName = tree instanceof J ? tree.getClass()
            .getCanonicalName()
            .substring(tree.getClass().getPackage().getName().length() + 1) : tree.getClass().getCanonicalName();

        line.append(leftPadding(depth)).append(typeName);
        String content = printTreeElement(tree);
        if (!content.isEmpty()) {
            line.append(" | \"").append(content).append("\"");
        }
        outputLines.add(line);

        cursorStack.add(tree);
        lastCursorStack = cursorStack;
        return super.visit(tree, ctx);
    }
}
