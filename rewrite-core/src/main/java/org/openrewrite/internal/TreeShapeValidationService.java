/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Lets the test framework check that a recipe's output is the tree its parser makes of the text it prints.
 */
public interface TreeShapeValidationService {

    /**
     * @return What tells {@code tree} apart from another node of its class with the same children, such as an
     * identifier's name or a literal's value, or null when nothing does.
     */
    @Nullable
    Object value(Tree tree);

    /**
     * @return Whether the comparison leaves out {@code tree} and what it holds, as with an empty element the parser
     * adds or leaves out without the text changing.
     */
    default boolean skip(Tree tree) {
        return false;
    }

    /**
     * @return Whether {@code tree} and {@code parsed}, which differ in kind or in their children, are two forms of the
     * same code that the parser and recipes both produce, such as two places for a type annotation.
     */
    default boolean equivalent(Tree tree, Tree parsed) {
        return false;
    }

    /**
     * @return What the text of {@code sourceFile} has that its parser accepts but its language doesn't, such as a
     * duplicate key.
     */
    default List<String> invalid(SourceFile sourceFile) {
        return emptyList();
    }

    /**
     * @return A parser of the format alone, for text that a source's own parser rejects because it checks more than
     * the format, as Maven's does with a POM's coordinates, or null when the source's parser checks only the format.
     */
    default Parser.@Nullable Builder syntaxParser() {
        return null;
    }

    /**
     * @return Whether {@code parser} reads the version of the language that {@code sourceFile} is written in, which
     * a parser for an older version doesn't.
     */
    default boolean reads(Parser.Builder parser, SourceFile sourceFile) {
        return true;
    }
}
