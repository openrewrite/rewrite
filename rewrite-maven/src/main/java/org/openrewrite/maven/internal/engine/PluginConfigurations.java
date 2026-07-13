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
package org.openrewrite.maven.internal.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.engine.shaded.org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Iterator;
import java.util.Map;

/**
 * Bidirectional bridge between a Maven {@code <configuration>} {@link Xpp3Dom} (what the shaded model builder hands back
 * from {@code Plugin#getConfiguration()}) and the Jackson {@link JsonNode} shape {@code MavenXmlMapper} produces for
 * {@link org.openrewrite.maven.tree.Plugin#getConfiguration()}: an {@link ObjectNode} keyed by child element name, leaf
 * text as a trimmed {@link TextNode}, repeated siblings as an {@link ArrayNode}, attributes as plain sibling fields
 * (namespace-unaware, so {@code combine.children}/{@code combine.self} surface as fields).
 */
final class PluginConfigurations {

    private static final JsonNodeFactory F = JsonNodeFactory.instance;

    private PluginConfigurations() {
    }

    /** The {@code configuration} element itself maps to the ObjectNode of its children, matching Jackson's element deserialization. */
    static @Nullable JsonNode toJson(@Nullable Object configuration) {
        if (!(configuration instanceof Xpp3Dom)) {
            return null;
        }
        Xpp3Dom dom = (Xpp3Dom) configuration;
        if (dom.getChildCount() == 0 && dom.getAttributeNames().length == 0) {
            return null;
        }
        return toObject(dom);
    }

    private static JsonNode toNode(Xpp3Dom element) {
        if (element.getChildCount() == 0 && element.getAttributeNames().length == 0) {
            String value = element.getValue();
            return value == null ? F.nullNode() : TextNode.valueOf(value.trim());
        }
        return toObject(element);
    }

    private static ObjectNode toObject(Xpp3Dom element) {
        ObjectNode object = F.objectNode();
        for (String attribute : element.getAttributeNames()) {
            object.set(attribute, TextNode.valueOf(element.getAttribute(attribute)));
        }
        for (Xpp3Dom child : element.getChildren()) {
            String name = child.getName();
            JsonNode value = toNode(child);
            JsonNode existing = object.get(name);
            if (existing == null) {
                object.set(name, value);
            } else if (existing.isArray()) {
                ((ArrayNode) existing).add(value);
            } else {
                ArrayNode array = F.arrayNode();
                array.add(existing);
                array.add(value);
                object.set(name, array);
            }
        }
        return object;
    }

    /** Reverse: a {@code Plugin#getConfiguration()} JsonNode back into an {@code <configuration>} {@link Xpp3Dom}. */
    static @Nullable Xpp3Dom toDom(@Nullable JsonNode configuration) {
        if (configuration == null || !configuration.isObject()) {
            return null;
        }
        Xpp3Dom root = new Xpp3Dom("configuration");
        appendChildren(root, (ObjectNode) configuration);
        return root;
    }

    private static void appendChildren(Xpp3Dom parent, ObjectNode object) {
        Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode value = field.getValue();
            if (value.isArray()) {
                for (JsonNode element : value) {
                    parent.addChild(toDomElement(field.getKey(), element));
                }
            } else {
                parent.addChild(toDomElement(field.getKey(), value));
            }
        }
    }

    private static Xpp3Dom toDomElement(String name, JsonNode value) {
        Xpp3Dom element = new Xpp3Dom(name);
        if (value.isObject()) {
            appendChildren(element, (ObjectNode) value);
        } else if (!value.isNull()) {
            element.setValue(value.asText());
        }
        return element;
    }
}
