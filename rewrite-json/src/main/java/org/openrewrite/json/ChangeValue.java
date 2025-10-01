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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonValue;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeValue extends Recipe {
    @Option(displayName = "Key path",
            description = "A [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expression to locate a JSON entry.",
            example = "$.subjects.kind")
    String oldKeyPath;

    @Option(displayName = "New value",
            description = "The new value to set for the key identified by oldKeyPath.",
            example = "'Deployment'")
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
        return new JsonIsoVisitor<ExecutionContext>() {
            @Override
            public Json.Member visitMember(Json.Member member, ExecutionContext ctx) {
                Json.Member m = super.visitMember(member, ctx);
                if (!matcher.matches(getCursor())) {
                    return m;
                }

                String targetValue = value;
                String withoutQoutes = targetValue;
                if (targetValue.startsWith("\"") || targetValue.startsWith("'")) {
                    withoutQoutes = withoutQoutes.substring(1, withoutQoutes.length() - 1);
                }
                String withQuotes = targetValue;
                if (!(targetValue.startsWith("\"") || targetValue.startsWith("'"))) {
                    withQuotes = "\"" + withQuotes + "\"";
                }

                if (m.getValue() instanceof Json.Literal &&
                        (((Json.Literal) m.getValue()).getSource().equals(targetValue) ||
                                ((Json.Literal) m.getValue()).getSource().equals(withoutQoutes) ||
                                ((Json.Literal) m.getValue()).getSource().equals(withQuotes))) {
                    return m;
                }
                Optional<JsonValue> jsonValue = JsonParser.builder().build()
                        .parse(withoutQoutes, targetValue, withQuotes)
                        .filter(it -> it instanceof Json.Document)
                        .findFirst()
                        .map(Json.Document.class::cast)
                        .map(Json.Document::getValue);
                if (jsonValue.isPresent()) {
                    return m.withValue(jsonValue.get().withPrefix(m.getValue().getPrefix()));
                }
                return m;
            }
        };
    }
}
