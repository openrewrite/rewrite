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
package org.openrewrite;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.trait.reference.Reference;

import java.util.*;

@Incubating(since = "8.39.0")
public interface SourceFileWithReferences extends SourceFile {

    TypeReferences getTypeReferences();

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    class TypeReferences {
        private final SourceFile sourceFile;
        private final Set<Reference> references;

        public Collection<Reference> findMatches(Reference.MatcherMutator matcherMutator) {
            List<Reference> list = new ArrayList<>();
            for (Reference ref : references) {
                if (ref.matches(matcherMutator)) {
                    list.add(ref);
                }
            }
            return list;
        }

        public static TypeReferences build(SourceFile sourceFile) {
            Set<Reference> references = new HashSet<>();
            ServiceLoader<Reference.Provider> loader = ServiceLoader.load(Reference.Provider.class);
            loader.forEach(provider -> {
                if (provider.isAcceptable(sourceFile)) {
                    references.addAll(provider.getTypeReferences(sourceFile));
                }
            });
            return new TypeReferences(sourceFile, references);
        }
    }
}
