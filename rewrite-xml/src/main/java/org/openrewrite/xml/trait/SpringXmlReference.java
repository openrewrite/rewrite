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
                            String name = localTagName(keyOwner);
                            if (!("entry".equals(name) || "prop".equals(name)) || !isBeansNamespace(keyOwner)) {
                                return null;
                            }
                            typeOnly = true;
                            break;
                        case "value":
                            Cursor parent = cursor.getParentTreeCursor();
                            String owner = localTagName(parent);
                            if (!"property".equals(owner) && !"constructor-arg".equals(owner) &&
                                    !"entry".equals(owner)) {
                                return null;
                            }
                            if (!isBeansNamespace(parent)) {
                                return null;
                            }
                            typeOnly = true;
                            break;
                        default:
                            return null;
                    }
                    text = attribute.getValueAsString();
                } else if (cursor.getValue() instanceof Xml.Tag) {
                    switch (localTagName(cursor)) {
                        case "value":
                            typeOnly = false;
                            break;
                        case "prop":
                            Cursor parent = cursor.getParentTreeCursor();
                            String parentName = localTagName(parent);
                            if (!("props".equals(parentName) && isBeansNamespace(parent)) &&
                                    !("properties".equals(parentName) && new Namespaced(parent).getNamespaceUri()
                                            .filter(UTIL_NAMESPACE::equals).isPresent())) {
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

        private static String localTagName(Cursor cursor) {
            if (!(cursor.getValue() instanceof Xml.Tag)) {
                return "";
            }
            return Namespaced.extractLocalName(((Xml.Tag) cursor.getValue()).getName());
        }

        private static boolean isBeansNamespace(Cursor cursor) {
            Namespaced element = new Namespaced(cursor);
            return element.getNamespaceUri().map(BEANS_NAMESPACE::equals)
                    .orElseGet(() -> !element.getNamespacePrefix().isPresent());
        }

        @Override
        public boolean isAcceptable(SourceFile sourceFile) {
            if (sourceFile instanceof Xml.Document) {
                Xml.Document doc = (Xml.Document) sourceFile;
                Xml.Tag root = doc.getRoot();
                if (root != null) {
                    for (Xml.Attribute attrib : root.getAttributes()) {
                        if ("xsi:schemaLocation".equals(attrib.getKeyAsString()) &&
                                attrib.getValueAsString().contains("www.springframework.org/schema/beans")) {
                            return true;
                        }
                    }
                    if ("beans".equals(Namespaced.extractLocalName(root.getName())) &&
                            new Namespaced(new Cursor(null, root)).getNamespaceUri().filter(BEANS_NAMESPACE::equals)
                                    .isPresent()) {
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
