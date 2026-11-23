/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.coldfusion;

import org.openrewrite.coldfusion.marker.InferredType;
import org.openrewrite.coldfusion.tree.Cf;

/**
 * Types more of a repository than the parser's own rules do, from what all of its files say together. The parser makes
 * one for each call to {@link ColdFusionParser#parseInputs}, once it has indexed the repository, and calls it in three
 * steps:
 * <ol>
 *     <li>{@link #collect} for each file, with the tree the parser typed. The parser parses and types each file for
 *     this and then drops the tree, so it holds one tree at a time.</li>
 *     <li>{@link #solve}, once.</li>
 *     <li>{@link #attribute} for each file, with the tree the parser typed when it parsed the file again, before it
 *     returns the tree.</li>
 * </ol>
 * The two trees of a file come from two parses, so their elements' ids differ. {@link TypeContext#callKeys} gives each
 * call a key that is the same in both. What an inference throws goes to the context's error handler, and the parser
 * returns the trees as it typed them.
 */
public interface TypeInference {

    /**
     * Makes the inference for one parse, from what the parser knows of the repository's types.
     */
    interface Factory {
        TypeInference create(TypeContext context);
    }

    /**
     * Learns what a file says. The parser drops the tree afterward, so an inference keeps what it learns, not the tree.
     */
    void collect(Cf.CompilationUnit typed);

    /**
     * Infers what the files say together. It is the one step that may give the repository's functions types, with
     * {@link TypeContext#setReturnType} and {@link TypeContext#setParameterType}, since every tree shares them. The
     * parser types the files with them before {@link #attribute} sees them, and marks nothing it types that way.
     */
    void solve();

    /**
     * The tree with the types the inference found, and an {@link InferredType} on every tree whose type rests on the
     * inference, including what the parser typed through the signatures {@link #solve} gave. The tree itself when no
     * type rests on the inference.
     */
    Cf.CompilationUnit attribute(Cf.CompilationUnit typed);
}
