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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

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
     * @return The text that a parser which carries on past a syntax error, as Java's does, couldn't read in
     * {@code sourceFile}, or null when it read all of it.
     */
    default @Nullable String syntaxError(SourceFile sourceFile) {
        return null;
    }

    /**
     * @return A parser of the format alone, for text that a source's own parser rejects because it checks more than
     * the format, as Maven's does with a POM's coordinates, or null when the source's parser checks only the format.
     */
    default Parser.@Nullable Builder syntaxParser() {
        return null;
    }

    /**
     * @return A parser that builds the same tree as {@code parser}, which may leave out type attribution, for when the
     * check compares no types.
     */
    default Parser.Builder untypedParser(Parser.Builder parser) {
        return parser;
    }

    /**
     * @param sources The sources the test parsed and the recipe's output, whose classes the recipe may have changed
     *                since {@code parser} typed them.
     * @return A parser for when the check compares types, which types text as {@code parser} does. Where it can, it
     * reuses the types {@code parser} holds, except those of the classes {@code sources} declare, and adds none to it.
     */
    default Parser.Builder typedParser(Parser.Builder parser, List<SourceFile> sources) {
        return parser;
    }

    /**
     * @return A copy of {@code parser} for when the check compares types, which reads no types that another parser
     * mapped. It is for a parser that didn't parse the test's sources, as the types it would read can be those of
     * the classpath the sources were parsed with.
     */
    default Parser.Builder typedParser(Parser.Builder parser) {
        return parser;
    }

    /**
     * @return How the types of {@code tree} differ from those of {@code parsed}, the same node in a parse of the text
     * it prints, each such as "the variable int x where the parser gives long x". A type either of them doesn't
     * resolve isn't compared.
     */
    default List<String> typeDifferences(Tree tree, Tree parsed) {
        return emptyList();
    }

    /**
     * Has the parsers of the language that parse with {@code parseCtx} keep the errors of the language's compiler for
     * {@link #compilationErrors} to return. Does nothing for a language whose parser doesn't compile what it reads.
     */
    default void collectCompilationErrors(ExecutionContext parseCtx) {
    }

    /**
     * @return The errors of the language's compiler in the sources that were parsed with {@code parseCtx} since
     * {@link #collectCompilationErrors} prepared it, by the path of the input they are in. Each message of the
     * compiler, such as "incompatible types", has the lines of source it is about, such as {@code int i = "";}.
     */
    default Map<Path, Map<String, Set<String>>> compilationErrors(ExecutionContext parseCtx) {
        return emptyMap();
    }

    /**
     * @return Whether {@code parser} reads the version of the language that {@code sourceFile} is written in, which
     * a parser for an older version doesn't.
     */
    default boolean reads(Parser.Builder parser, SourceFile sourceFile) {
        return true;
    }
}
