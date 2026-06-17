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

import java.util.HashSet;
import java.util.Set;

@Incubating(since = "8.39.0")
public interface Reference extends Trait<Tree> {

    enum Kind {
        TYPE,
        PACKAGE,
        IMAGE
    }

    Kind getKind();

    String getValue();

    default boolean matches(Matcher matcher) {
        return matcher.matchesReference(this);
    }

    default boolean supportsRename() {
        return false;
    }

    /**
     * Applies the {@link org.openrewrite.trait.Reference.Renamer} to the provided cursor to compute a new reference value.
     * Note that this method only provides the logic for computing a new {@link org.openrewrite.Tree} with a renamed value,
     * specific to the reference type. Hence, the reference itself is not changed, and the new value is computed from the
     * provided {@code cursor} (which the caller supplies fresh during its own traversal), not from the reference's own
     * {@link org.openrewrite.trait.Reference#getTree()} or {@link org.openrewrite.trait.Reference#getValue()}. References
     * are cursor-free: {@link Trait#getCursor()} is not available on a cached reference.
     */
    default Tree rename(Renamer renamer, Cursor cursor, ExecutionContext ctx) {
        throw new UnsupportedOperationException();
    }

    interface Provider {

        Set<Reference> getReferences(SourceFile sourceFile);

        boolean isAcceptable(SourceFile sourceFile);
    }

    abstract class AbstractProvider<U extends Reference> implements Provider {
        protected abstract SimpleTraitMatcher<U> getMatcher();

        @Override
        public Set<Reference> getReferences(SourceFile sourceFile) {
            Set<Reference> references = new HashSet<>();
            getMatcher().asVisitor(reference -> {
                references.add(reference);
                return reference.getTree();
            }).visit(sourceFile, 0);
            return references;
        }
    }

    interface Matcher {

        boolean matchesReference(Reference value);

        default Renamer createRenamer(String newName) {
            throw new UnsupportedOperationException();
        }
    }

    @FunctionalInterface
    interface Renamer {
        /**
         * Compute a renamed value for a reference.
         */
        String rename(Reference reference);
    }
}
