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
package org.openrewrite.xml.trait;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class SpringXmlReference extends XmlReference {

    Cursor cursor;
    Kind kind;

    public static class Provider extends AbstractProvider<SpringXmlReference> {
        private static final String BEANS_NAMESPACE = "http://www.springframework.org/schema/beans";
        private static final String UTIL_NAMESPACE = "http://www.springframework.org/schema/util";
        private static final String P_NAMESPACE = "http://www.springframework.org/schema/p";
        private static final String C_NAMESPACE = "http://www.springframework.org/schema/c";
        private static final SimpleTraitMatcher<SpringXmlReference> matcher = new SpringMatcher();

        private static class SpringMatcher extends SimpleTraitMatcher<SpringXmlReference> {
            @Override
            protected @Nullable SpringXmlReference test(Cursor cursor) {
                String text;
                boolean typeOnly;
                if (cursor.getValue() instanceof Xml.Attribute) {
                    Xml.Attribute attribute = cursor.getValue();
                    switch (attribute.getKeyAsString()) {
                        case "class":
                        case "type":
                        case "key-type":
                        case "value-type":
                            typeOnly = false;
                            break;
                        case "key":
                            Cursor keyOwner = cursor.getParentTreeCursor();
                            String name = new Namespaced(keyOwner).getLocalName().orElse("");
                            if (!("entry".equals(name) && isBeansNamespace(keyOwner)) &&
                                    !("prop".equals(name) && isPropsEntry(keyOwner))) {
                                return null;
                            }
                            typeOnly = true;
                            break;
                        case "value":
                            Cursor parent = cursor.getParentTreeCursor();
                            String owner = new Namespaced(parent).getLocalName().orElse("");
                            if (!"property".equals(owner) && !"constructor-arg".equals(owner) &&
                                    !"entry".equals(owner)) {
                                return null;
                            }
                            if (!isBeansNamespace(parent) || !"entry".equals(owner) &&
                                    isMessageBundleProperty(((Xml.Tag) parent.getValue()).getAttributes().stream()
                                            .filter(a -> "name".equals(a.getKeyAsString()))
                                            .map(Xml.Attribute::getValueAsString).findFirst().orElse(""))) {
                                return null;
                            }
                            typeOnly = true;
                            break;
                        default:
                            if (!isPropertyShortcut(cursor)) {
                                return null;
                            }
                            typeOnly = true;
                    }
                    text = attribute.getValueAsString();
                } else if (cursor.getValue() instanceof Xml.Tag) {
                    switch (new Namespaced(cursor).getLocalName().orElse("")) {
                        case "value":
                            typeOnly = false;
                            break;
                        case "prop":
                            if (!isPropsEntry(cursor)) {
                                return null;
                            }
                            typeOnly = true;
                            break;
                        default:
                            return null;
                    }
                    if (!isBeansNamespace(cursor)) {
                        return null;
                    }
                    text = ((Xml.Tag) cursor.getValue()).getValue().orElse(null);
                } else {
                    return null;
                }
                Kind kind = referenceKind(text, typeOnly);
                return kind == null ? null : new SpringXmlReference(cursor, kind);
            }
        }

        private static boolean isBeansNamespace(Cursor cursor) {
            Namespaced element = new Namespaced(cursor);
            return element.getNamespaceUri().map(BEANS_NAMESPACE::equals)
                    .orElseGet(() -> !element.getNamespacePrefix().isPresent());
        }

        private static boolean isPropsEntry(Cursor prop) {
            Cursor parent = prop.getParentTreeCursor();
            String parentName = new Namespaced(parent).getLocalName().orElse("");
            return isBeansNamespace(prop) && ("props".equals(parentName) && isBeansNamespace(parent) ||
                    "properties".equals(parentName) &&
                            new Namespaced(parent).getNamespaceUri().filter(UTIL_NAMESPACE::equals).isPresent());
        }

        private static boolean isPropertyShortcut(Cursor attribute) {
            Namespaced namespaced = new Namespaced(attribute);
            String name = namespaced.getLocalName().orElse("");
            return !name.endsWith("-ref") && !isMessageBundleProperty(name) && namespaced.getNamespaceUri()
                    .filter(uri -> P_NAMESPACE.equals(uri) || C_NAMESPACE.equals(uri)).isPresent();
        }

        /**
         * A message source's base names look like class names but name resource bundles, which a package rename
         * does not move.
         */
        private static boolean isMessageBundleProperty(String propertyName) {
            return "basename".equals(propertyName) || "basenames".equals(propertyName);
        }

        @Override
        public boolean isAcceptable(SourceFile sourceFile) {
            if (sourceFile instanceof Xml.Document) {
                Xml.Document doc = (Xml.Document) sourceFile;
                Xml.Tag root = doc.getRoot();
                if (root != null) {
                    Namespaced namespacedRoot = new Namespaced(new Cursor(null, root));
                    if (namespacedRoot.getSchemaLocations().stream().anyMatch(location ->
                            location.getValueAsString().contains("www.springframework.org/schema/beans"))) {
                        return true;
                    }
                    if ("beans".equals(namespacedRoot.getLocalName().orElse("")) &&
                            namespacedRoot.getNamespaceUri().filter(BEANS_NAMESPACE::equals).isPresent()) {
                        return true;
                    }
                    return "beans".equals(root.getName()) && doc.getProlog().getMisc().stream()
                            .filter(Xml.DocTypeDecl.class::isInstance)
                            .map(Xml.DocTypeDecl.class::cast)
                            .anyMatch(Provider::isSpringBeansDtd);
                }
            }
            return false;
        }

        private static boolean isSpringBeansDtd(Xml.DocTypeDecl decl) {
            if (!"beans".equals(decl.getName().getName()) || decl.getExternalId() == null ||
                    decl.getInternalSubset().isEmpty()) {
                return false;
            }
            List<Xml.Ident> ids = decl.getInternalSubset();
            String externalId = decl.getExternalId().getName();
            String systemId;
            if ("PUBLIC".equals(externalId)) {
                String publicId = unquote(ids.get(0));
                if ("-//SPRING//DTD BEAN//EN".equals(publicId) || "-//SPRING//DTD BEAN 2.0//EN".equals(publicId)) {
                    return true;
                }
                if (ids.size() < 2) {
                    return false;
                }
                systemId = unquote(ids.get(1));
            } else if ("SYSTEM".equals(externalId)) {
                systemId = unquote(ids.get(0));
            } else {
                return false;
            }
            return systemId.endsWith(".dtd") && systemId.indexOf("spring-beans", systemId.lastIndexOf('/') + 1) >= 0;
        }

        private static String unquote(Xml.Ident literal) {
            String value = literal.getName();
            return value.substring(1, value.length() - 1);
        }

        @Override
        public SimpleTraitMatcher<SpringXmlReference> getMatcher() {
            return matcher;
        }
    }
}
