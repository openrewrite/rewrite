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
package org.openrewrite.xml;

import org.openrewrite.JavaTypeReferenceProvider;
import org.openrewrite.SourceFile;
import org.openrewrite.trait.JavaTypeReference;
import org.openrewrite.xml.trait.SpringJavaTypeReference;
import org.openrewrite.xml.tree.Xml;

import java.util.HashSet;
import java.util.Set;

public class SpringJavaTypeReferenceProvider implements JavaTypeReferenceProvider {

    @Override
    public Set<JavaTypeReference> getTypeReferences(SourceFile sourceFile) {
        Set<JavaTypeReference> typeReferences = new HashSet<>();
        new SpringJavaTypeReference.Matcher().asVisitor(reference -> {
            typeReferences.add(reference);
            return reference.getTree();
        }).visit(sourceFile, null);
        return typeReferences;
    }

    @Override
    public boolean isAcceptable(SourceFile sourceFile) {
        if (sourceFile instanceof Xml.Document) {
            Xml.Document doc = (Xml.Document) sourceFile;
            for (Xml.Attribute attrib : doc.getRoot().getAttributes()) {
                if (attrib.getKeyAsString().equals("xsi:schemaLocation") && attrib.getValueAsString().contains("www.springframework.org/schema/beans")) {
                    return true;
                }
            }
        }
        return false;
    }
}
