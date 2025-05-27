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
package org.openrewrite.json.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.JsonPathMatcher;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonKey;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindKey extends Recipe {
    @Option(displayName = "Key path",
            description = "A JsonPath expression used to find matching keys.",
            example = "$.subjects.kind")
    String key;

    @Override
    public String getDisplayName() {
        return "Find JSON object members";
    }

    @Override
    public String getDescription() {
        return "Find JSON object members by JsonPath expression.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(key);
        return new JsonIsoVisitor<ExecutionContext>() {
            @Override
            public Json.Member visitMember(Json.Member member, ExecutionContext ctx) {
                Json.Member m = super.visitMember(member, ctx);
                if (matcher.matches(getCursor())) {
                    return m.withKey(SearchResult.found(m.getKey()));
                }
                return m;
            }
        };
    }

    public static Set<JsonKey> find(Json j, String key) {
        JsonPathMatcher matcher = new JsonPathMatcher(key);
        Set<JsonKey> ks = new HashSet<>();
        new JsonIsoVisitor<Set<JsonKey>>() {
            @Override
            public Json.Member visitMember(Json.Member member, Set<JsonKey> ks) {
                Json.Member m = super.visitMember(member, ks);
                if (matcher.matches(getCursor())) {
                    ks.add(m.getKey());
                }
                return m;
            }
        }.visit(j, ks);
        return ks;
    }
}
