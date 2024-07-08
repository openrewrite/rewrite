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

import org.openrewrite.*;

import java.util.Optional;
import java.util.stream.Stream;

@Incubating(since = "8.30.0")
public interface TraitMatcher<U extends Trait<?>> {

    Optional<U> get(Cursor cursor);

    Stream<U> higher(Cursor cursor);

    Stream<U> lower(Cursor cursor);

    default Stream<U> lower(SourceFile sourceFile) {
        return lower(new Cursor(new Cursor(null, Cursor.ROOT_VALUE), sourceFile));
    }

    /**
     * @param visitor Called for each match of the trait. The function is passed the trait.
     * @param <P>     The type of context object passed to the visitor.
     * @return A visitor that can be used to inspect or modify trees matching the trait.
     */
    default <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction<U> visitor) {
        return asVisitor((u, cursor, p) -> visitor.visit(u));
    }

    /**
     * @param visitor Called for each match of the trait. The function is passed the trait
     *                and a context object.
     * @param <P>     The type of context object passed to the visitor.
     * @return A visitor that can be used to inspect or modify trees matching the trait.
     */
    default <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<U, P> visitor) {
        return asVisitor((u, cursor, p) -> visitor.visit(u, p));
    }

    /**
     * @param visitor Called for each match of the trait. The function is passed the trait, the cursor at which the
     *                trait was found, and a context object.
     * @param <P>     The type of context object passed to the visitor.
     * @return A visitor that can be used to inspect or modify trees matching the trait.
     */
    <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction3<U, P> visitor);
}
