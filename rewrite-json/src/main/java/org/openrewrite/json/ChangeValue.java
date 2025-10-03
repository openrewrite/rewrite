/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.json;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;

import java.util.Optional;
import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeValue extends Recipe {
    @Option(displayName = "Key path",
            description = "A [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expression to locate a JSON entry.",
            example = "$.subjects.kind")
    String oldKeyPath;

    @Option(displayName = "New value",
            description = "The new JSON value to set for the key identified by oldKeyPath.",
            example = "'Deployment'")
    @Language("json")
    String value;

    @Override
    public String getDisplayName() {
        return "Change value";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", oldKeyPath, value);
    }

    @Override
    public String getDescription() {
        return "Change a JSON mapping entry value leaving the key intact.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(oldKeyPath);
        // Parse the value once here, outside the visitor
        // Try as keyword/number/array/object first, fallback to string
        Optional<JsonValue> jsonValue = parseValues(firstParserInput(), '"' + value + '"');
        return new JsonIsoVisitor<ExecutionContext>() {
            @Override
            public Json.Member visitMember(Json.Member member, ExecutionContext ctx) {
                Json.Member m = super.visitMember(member, ctx);
                if (matcher.matches(getCursor()) && !m.getMarkers().findFirst(Changed.class).isPresent()) {
                    if (!jsonValue.isPresent()) {
                        return SearchResult.found(m, "Could not parse value: " + value);
                    }
                    JsonValue parsedValue = jsonValue.get();
                    return m.withValue(parsedValue.withPrefix(m.getValue().getPrefix()))
                            .withMarkers(m.getMarkers().add(new Changed(Tree.randomId())));
                }
                return m;
            }
        };
    }

    private String firstParserInput() {
        if (value.startsWith("'") && value.endsWith("'")) {
            // Our parser tolerates single quotes for strings, which we want to convert to double quotes.
            if (value.startsWith("'\"") && value.endsWith("\"'") && value.length() > 3) {
                return "\"'\\\"" + value.substring(2, value.length() - 2) + "\\\"'\"";
            }
            return '"' + value + '"';
        }
        return value;
    }

    private static Optional<JsonValue> parseValues(@Language("json") String... values) {
        return JsonParser.builder().build()
                .parse(values)
                .filter(Json.Document.class::isInstance)
                .map(Json.Document.class::cast)
                .findFirst()
                .map(Json.Document::getValue);
    }

    @Value
    @With
    static class Changed implements Marker {
        UUID id;
    }
}
