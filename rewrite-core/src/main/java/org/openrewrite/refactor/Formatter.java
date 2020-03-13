package org.openrewrite.refactor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.Tree;

import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

public class Formatter {
    private final Tree root;
    private Result wholeSourceIndent;

    public Formatter(Tree root) {
        this.root = root;
    }

    @RequiredArgsConstructor
    @Getter
    public static class Result {
        private final int enclosingIndent;
        private final int indentToUse;
        private final boolean indentedWithSpaces;

        public String getPrefix() {
            return getPrefix(0);
        }

        public String getPrefix(int offset) {
            return range(0, indentToUse + (indentToUse * offset) + enclosingIndent)
                    .mapToObj(i -> indentedWithSpaces ? " " : "\t")
                    .collect(joining("", "\n", ""));
        }
    }

    protected Result wholeSourceIndent() {
        if (wholeSourceIndent == null) {
            var wholeSourceIndentVisitor = new FindIndentVisitor(0);
            wholeSourceIndentVisitor.visit(root);
            wholeSourceIndent = new Result(0, wholeSourceIndentVisitor.getMostCommonIndent() > 0 ?
                    wholeSourceIndentVisitor.getMostCommonIndent() : 4 /* default to 4 spaces */,
                    wholeSourceIndentVisitor.isIndentedWithSpaces());
        }
        return wholeSourceIndent;
    }

    public Result findIndent(int enclosingIndent, Tree... trees) {
        var findIndentVisitor = new FindIndentVisitor(enclosingIndent);
        for (Tree tree : trees) {
            findIndentVisitor.visit(tree);
        }

        var indentToUse = findIndentVisitor.getMostCommonIndent() > 0 ?
                findIndentVisitor.getMostCommonIndent() :
                wholeSourceIndent().getIndentToUse();
        var indentedWithSpaces = findIndentVisitor.getTotalLines() > 0 ? findIndentVisitor.isIndentedWithSpaces() :
                wholeSourceIndent().isIndentedWithSpaces();

        return new Result(enclosingIndent, indentToUse, indentedWithSpaces);
    }

}
