/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

public class CountLinesVisitor extends JavaVisitor<AtomicInteger> {

    @Override
    public J visit(@Nullable Tree tree, AtomicInteger count) {
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
            // Skip compilation unit prefix and EOF
            if (cu.getPackageDeclaration() != null) {
                // Package often does not have a prefix to count newlines within
                count.incrementAndGet();
            }
            count.getAndAdd(cu.getImports().size());
            for (J.ClassDeclaration c : cu.getClasses()) {
                visit(c, count);
            }
            return cu;
        }
        return super.visit(tree, count);
    }

    @Override
    public J visitBlock(J.Block block, AtomicInteger count) {
        // Skip whitespace related to opening and closing braces
        // Lines of code count shouldn't change based on curly brace placement
        for (Statement s : block.getStatements()) {
            visit(s, count);
        }
        return block;
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, AtomicInteger count) {
        if (space.getWhitespace().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitSpace(space, loc, count);
    }

    public static int countLines(Tree tree) {
        return new CountLinesVisitor().reduce(tree, new AtomicInteger()).get();
    }
}
