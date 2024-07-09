/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.trait;

import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;

import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

@Incubating(since = "8.30.0")
public abstract class SimpleTraitMatcher<U extends Trait<?>> implements TraitMatcher<U> {

    @Override
    public Optional<U> get(Cursor cursor) {
        return Optional.ofNullable(test(cursor));
    }

    @Override
    public Stream<U> higher(Cursor cursor) {
        Stream.Builder<U> stream = Stream.builder();
        Iterator<Cursor> cursors = cursor.getPathAsCursors();
        while (cursors.hasNext()) {
            Cursor c = cursors.next();
            if (c != cursor) {
                U u = test(c);
                if (u != null) {
                    stream.add(u);
                }
            }
        }
        return stream.build();
    }

    @Override
    public Stream<U> lower(Cursor cursor) {
        Stream.Builder<U> stream = Stream.builder();
        this.<Stream.Builder<U>>asVisitor((va, sb) -> {
            sb.add(test(va.getCursor()));
            return va.getTree();
        }).visit(cursor.getValue(), stream, cursor.getParentOrThrow());
        return stream.build();
    }

    /**
     * This method is called on every tree. For more performant matching, traits should override
     * and provide a narrower visitor that only calls {@link #test(Cursor)} against tree types that
     * could potentially match.
     *
     * @param visitor Called for each match of the trait. The function is passed the trait, the cursor at which the
     *                trait was found, and a context object.
     * @param <P>     The type of the context object.
     * @return A visitor that can be used to locate trees matching the trait.
     */
    @Override
    public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<U, P> visitor) {
        return new TreeVisitor<Tree, P>() {
            @Override
            public @Nullable Tree preVisit(Tree tree, P p) {
                U u = test(getCursor());
                return u != null ?
                        visitor.visit(u, p) :
                        super.visit(tree, p);
            }
        };
    }

    @Nullable
    protected abstract U test(Cursor cursor);
}
