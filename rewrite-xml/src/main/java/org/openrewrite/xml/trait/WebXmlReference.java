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
import org.openrewrite.xml.tree.Xml;

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
            @Override
            protected @Nullable WebXmlReference test(Cursor cursor) {
                if (!(cursor.getValue() instanceof Xml.Tag)) {
                    return null;
                }
                Xml.Tag tag = cursor.getValue();
                Object parent = cursor.getParentTreeCursor().getValue();
                if (parent instanceof Xml.Tag &&
                        isClassElement(Namespaced.extractLocalName(tag.getName()),
                                Namespaced.extractLocalName(((Xml.Tag) parent).getName())) &&
                        isQualifiedName(tag.getValue().orElse(null))) {
                    return new WebXmlReference(cursor);
                }
                return null;
            }
        };

        /**
         * The elements the servlet and Java EE schemas type as {@code fully-qualified-classType}, by parent.
         */
        private static boolean isClassElement(String element, String parent) {
            switch (element) {
                case "filter-class":
                    return "filter".equals(parent);
                case "servlet-class":
                    return "servlet".equals(parent);
                case "listener-class":
                    return "listener".equals(parent);
                case "exception-type":
                    return "error-page".equals(parent);
                case "res-type":
                    return "resource-ref".equals(parent);
                case "resource-env-ref-type":
                    return "resource-env-ref".equals(parent);
                case "service-interface":
                    return "service-ref".equals(parent);
                case "handler-class":
                    return "handler".equals(parent);
                case "injection-target-class":
                    return "injection-target".equals(parent);
                case "lifecycle-callback-class":
                    return "post-construct".equals(parent) || "pre-destroy".equals(parent);
                case "home":
                case "remote":
                    return "ejb-ref".equals(parent);
                case "local-home":
                case "local":
                    return "ejb-local-ref".equals(parent);
                case "message-destination-type":
                    return "message-destination-ref".equals(parent);
                case "service-ref-type":
                    return "service-ref".equals(parent);
                case "service-endpoint-interface":
                    return "port-component-ref".equals(parent);
                case "class-name":
                    return "data-source".equals(parent) || "jms-connection-factory".equals(parent) ||
                            "jms-destination".equals(parent) || "administered-object".equals(parent);
                case "interface-name":
                    return "jms-connection-factory".equals(parent) || "jms-destination".equals(parent) ||
                            "connection-factory".equals(parent) || "administered-object".equals(parent);
                case "store-protocol-class":
                case "transport-protocol-class":
                    return "mail-session".equals(parent);
                default:
                    return false;
            }
        }

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
                return "web-app".equals(root.getName());
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
