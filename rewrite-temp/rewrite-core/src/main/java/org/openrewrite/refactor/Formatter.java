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
package org.openrewrite.refactor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.Tree;

import java.util.function.Function;

import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

public class Formatter {
    private final Tree root;
    private final Function<Integer, FindIndent> findIndentBuilder;
    private Result wholeSourceIndent;

    public Formatter(Tree root, Function<Integer, FindIndent> findIndentBuilder) {
        this.root = root;
        this.findIndentBuilder = findIndentBuilder;
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

    public Result wholeSourceIndent() {
        if (wholeSourceIndent == null) {
            FindIndent wholeSourceIndentVisitor = findIndentBuilder.apply(0);
            wholeSourceIndentVisitor.visit(root);
            wholeSourceIndent = new Result(0, wholeSourceIndentVisitor.getMostCommonIndent() > 0 ?
                    wholeSourceIndentVisitor.getMostCommonIndent() : 4 /* default to 4 spaces */,
                    wholeSourceIndentVisitor.isIndentedWithSpaces());
        }
        return wholeSourceIndent;
    }

    public Result findIndent(int enclosingIndent, Tree... trees) {
        FindIndent findIndentVisitor = findIndentBuilder.apply(enclosingIndent);
        for (Tree tree : trees) {
            findIndentVisitor.visit(tree);
        }

        int indentToUse = findIndentVisitor.getMostCommonIndent() > 0 ?
                findIndentVisitor.getMostCommonIndent() :
                wholeSourceIndent().getIndentToUse();
        boolean indentedWithSpaces = findIndentVisitor.getTotalLines() > 0 ? findIndentVisitor.isIndentedWithSpaces() :
                wholeSourceIndent().isIndentedWithSpaces();

        return new Result(enclosingIndent, indentToUse, indentedWithSpaces);
    }
}
