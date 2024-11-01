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
package org.openrewrite.internal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.SourceFile;
import org.openrewrite.trait.TypeReference;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class TypeReferences {
    private final SourceFile sourceFile;
    private final Set<TypeReference> typeReferences;

    public static TypeReferences build(SourceFile sourceFile) {
        Set<TypeReference> typeReferences = new HashSet<>();
        ServiceLoader<TypeReference.TypeReferenceProvider> loader = ServiceLoader.load(TypeReference.TypeReferenceProvider.class);
        loader.forEach(provider -> {
            if (provider.isAcceptable(sourceFile)) {
                typeReferences.addAll(provider.getTypeReferences(sourceFile));
            }
        });
        return new TypeReferences(sourceFile, typeReferences);
    }
}
