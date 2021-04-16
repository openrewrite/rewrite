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
package org.openrewrite;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@c")
public interface Tree {
    static UUID randomId() {
        return UUID.randomUUID();
    }

    /**
     * An id that can be used to identify a particular AST element, even after transformations have taken place on it
     *
     * @return A unique identifier
     */
    UUID getId();

    /**
     * Supports polymorphic visiting via {@link TreeVisitor#visit(Tree, Object)}. This is useful in cases where an AST
     * type contains a field that is of a type with a hierarchy. The visitor doesn't have to figure out which visit
     * method to call by using instanceof.
     *
     * @param v   visitor
     * @param p   visit context
     * @param <R> visitor return type
     * @param <P> visit context type
     * @return visitor result
     */
    @Nullable
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return v.defaultValue(this, p);
    }

    /**
     * Checks the supplied argument to see if the supplied visitor and its context would be valid arguments
     * to accept().
     * Typically this involves checking that the visitor is of a type that operates on this kind of tree.
     * e.g.: A Java Tree implementation would return true for JavaVisitors and false for MavenVisitors
     *
     * @param <P> the visitor's context argument
     * @return 'true' if the arguments to this function would be valid arguments to accept()
     */
    <P> boolean isAcceptable(TreeVisitor<?, P> v, P p);

    <P> String print(TreePrinter<P> printer, P p);

    default <P> String print(P p) {
        return print(TreePrinter.identity(), p);
    }

    default String print() {
        return print(TreePrinter.identity(), new Object());
    }

    default <P> String printTrimmed(TreePrinter<P> printer, P p) {
        return StringUtils.trimIndent(print(printer, p).trim());
    }

    default <P> String printTrimmed(P p) {
        return printTrimmed(TreePrinter.identity(), p);
    }

    default String printTrimmed() {
        return printTrimmed(TreePrinter.identity(), new Object());
    }

    default boolean isScope(@Nullable Tree tree) {
        return tree != null && tree.getId().equals(getId());
    }
}
