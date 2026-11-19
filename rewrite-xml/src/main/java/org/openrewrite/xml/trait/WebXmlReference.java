/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.xml.trait;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import javax.lang.model.SourceVersion;

/**
 * A class name in a servlet deployment descriptor or web fragment.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class WebXmlReference extends XmlReference {
    Cursor cursor;

    @Override
    public Kind getKind() {
        return Kind.TYPE;
    }

    public static class Provider extends AbstractProvider<WebXmlReference> {
        private static final SimpleTraitMatcher<WebXmlReference> matcher = new SimpleTraitMatcher<WebXmlReference>() {
            private final XPathMatcher filter = new XPathMatcher("/*/*[local-name()='filter']/*");
            private final XPathMatcher servlet = new XPathMatcher("/*/*[local-name()='servlet']/*");
            private final XPathMatcher listener = new XPathMatcher("/*/*[local-name()='listener']/*");
            private final XPathMatcher errorPage = new XPathMatcher("/*/*[local-name()='error-page']/*");

            @Override
            protected @Nullable WebXmlReference test(Cursor cursor) {
                if (!(cursor.getValue() instanceof Xml.Tag)) {
                    return null;
                }
                Xml.Tag tag = cursor.getValue();
                XPathMatcher parent;
                switch (Namespaced.extractLocalName(tag.getName())) {
                    case "filter-class":
                        parent = filter;
                        break;
                    case "servlet-class":
                        parent = servlet;
                        break;
                    case "listener-class":
                        parent = listener;
                        break;
                    case "exception-type":
                        parent = errorPage;
                        break;
                    default:
                        return null;
                }
                if (parent.matches(cursor) && tag.getValue().filter(SourceVersion::isName).isPresent()) {
                    return new WebXmlReference(cursor);
                }
                return null;
            }
        };

        @Override
        public boolean isAcceptable(SourceFile sourceFile) {
            if (!(sourceFile instanceof Xml.Document) || !sourceFile.getSourcePath().toString().endsWith(".xml")) {
                return false;
            }
            Xml.Document doc = (Xml.Document) sourceFile;
            Xml.Tag root = doc.getRoot();
            //noinspection ConstantValue
            if (root == null) {
                return false;
            }
            String name = Namespaced.extractLocalName(root.getName());
            if (!"web-app".equals(name) && !"web-fragment".equals(name)) {
                return false;
            }
            String namespace = new Namespaced(new Cursor(null, root)).getNamespaceUri().orElse(null);
            if (namespace == null) {
                return "web-app".equals(root.getName()) && doc.getProlog().getMisc().stream()
                        .filter(Xml.DocTypeDecl.class::isInstance)
                        .map(Xml.DocTypeDecl.class::cast)
                        .anyMatch(decl -> "web-app".equals(decl.getName().getName()));
            }
            return "http://java.sun.com/xml/ns/j2ee".equals(namespace) ||
                    "http://java.sun.com/xml/ns/javaee".equals(namespace) ||
                    "http://xmlns.jcp.org/xml/ns/javaee".equals(namespace) ||
                    "https://jakarta.ee/xml/ns/jakartaee".equals(namespace);
        }

        @Override
        public SimpleTraitMatcher<WebXmlReference> getMatcher() {
            return matcher;
        }
    }
}
