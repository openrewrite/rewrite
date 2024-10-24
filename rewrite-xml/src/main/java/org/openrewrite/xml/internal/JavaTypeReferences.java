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
package org.openrewrite.xml.internal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.xml.trait.JavaTypeReference;
import org.openrewrite.xml.tree.Xml;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class JavaTypeReferences {
    private final Xml.Document cu;
    private final Set<JavaTypeReference> typeReferences;

    public static JavaTypeReferences build(Xml.Document doc) {
        Set<JavaTypeReference> typeReferences = new HashSet<>();
        new JavaTypeReference.Matcher().asVisitor(reference -> {
            typeReferences.add(reference);
            return reference.getTree();
        }).visit(doc, null);
        return new JavaTypeReferences(doc, typeReferences);
    }
}
