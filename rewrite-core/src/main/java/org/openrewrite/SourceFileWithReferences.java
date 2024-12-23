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
import lombok.ToString;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.trait.Reference;

import java.lang.ref.SoftReference;
import java.util.*;

@Incubating(since = "8.39.0")
public abstract class SourceFileWithReferences implements SourceFile {

    @Nullable
    @NonFinal
    @ToString.Exclude
    transient SoftReference<References> references;

    public References getReferences() {
        References cache;
        if (this.references == null) {
            cache = References.build(this);
            this.references = new SoftReference<>(cache);
        } else {
            cache = this.references.get();
            if (cache == null || cache.getSourceFile() != this) {
                cache = References.build(this);
                this.references = new SoftReference<>(cache);
            }
        }
        return cache;
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class References {
        @Getter(AccessLevel.PRIVATE)
        private final SourceFile sourceFile;
        @Getter
        private final Set<Reference> references;

        public Collection<Reference> findMatches(Reference.Matcher matcher) {
            List<Reference> list = new ArrayList<>();
            for (Reference ref : references) {
                if (ref.matches(matcher)) {
                    list.add(ref);
                }
            }
            return list;
        }

        public static References build(SourceFile sourceFile) {
            Set<Reference> references = new HashSet<>();
            ServiceLoader<Reference.Provider> loader = ServiceLoader.load(Reference.Provider.class);
            loader.forEach(provider -> {
                if (provider.isAcceptable(sourceFile)) {
                    references.addAll(provider.getReferences(sourceFile));
                }
            });
            return new References(sourceFile, references);
        }
    }
}
