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
package org.openrewrite.trait.reference;

import org.openrewrite.*;
import org.openrewrite.trait.Trait;

import java.util.Set;

@Incubating(since = "8.39.0")
public interface Reference extends Trait<Tree> {

    enum Kind {
        TYPE,
        PACKAGE
    }

    Kind getKind();

    String getValue();

    default boolean supportsRename() {
        return false;
    }

    default TreeVisitor<Tree, ExecutionContext> rename(String oldValue, String newValue, boolean recursive) {
        throw new UnsupportedOperationException();
    }

    default boolean matches(Matcher matcher) {
        return matcher.matchesReference(getValue());
    }

    interface Provider {

        Set<Reference> getTypeReferences(SourceFile sourceFile);

        boolean isAcceptable(SourceFile sourceFile);
    }

    interface Matcher {
        boolean matchesReference(String value);
    }
}
