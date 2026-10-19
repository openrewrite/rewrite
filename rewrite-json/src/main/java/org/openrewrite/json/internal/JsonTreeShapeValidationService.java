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
package org.openrewrite.json.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.TreeShapeValidationService;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonKey;

import java.util.*;
import java.util.regex.Pattern;

public class JsonTreeShapeValidationService implements TreeShapeValidationService {
    private static final Pattern STRICT_NUMBER = Pattern.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?");

    @Override
    public @Nullable Object value(Tree tree) {
        if (tree instanceof Json.Literal) {
            Object value = ((Json.Literal) tree).getValue();
            return value == null ? "the value null" :
                    "the " + value.getClass().getSimpleName() + " value `" + value + "`";
        } else if (tree instanceof Json.Identifier) {
            return "the name `" + ((Json.Identifier) tree).getName() + "`";
        }
        return null;
    }

    @Override
    public boolean skip(Tree tree) {
        // the parser adds an empty element to an empty object or array, where recipes often leave none
        return tree instanceof Json.Empty;
    }

    @Override
    public List<String> invalid(SourceFile sourceFile) {
        List<String> invalid = new ArrayList<>();
        new JsonIsoVisitor<List<String>>() {
            @Override
            public Json.JsonObject visitObject(Json.JsonObject obj, List<String> found) {
                Set<String> keys = new HashSet<>();
                for (Json member : obj.getMembers()) {
                    if (member instanceof Json.Member) {
                        String key = key(((Json.Member) member).getKey());
                        if (!keys.add(key)) {
                            found.add("the key `" + key + "` appears twice in one object");
                        }
                    }
                }
                return super.visitObject(obj, found);
            }

            @Override
            public Json.Member visitMember(Json.Member member, List<String> found) {
                if (member.getKey() instanceof Json.Identifier) {
                    found.add("the key `" + ((Json.Identifier) member.getKey()).getName() +
                              "` has no quotes, which JSON doesn't allow");
                }
                return super.visitMember(member, found);
            }

            @Override
            public Json.Literal visitLiteral(Json.Literal literal, List<String> found) {
                String source = literal.getSource();
                Object value = literal.getValue();
                if (source.startsWith("'")) {
                    found.add("`" + source + "` is in single quotes, which JSON doesn't allow");
                } else if (value instanceof Number && !STRICT_NUMBER.matcher(source).matches()) {
                    found.add("`" + source + "` isn't a number JSON allows");
                }
                return super.visitLiteral(literal, found);
            }
        }.visit(sourceFile, invalid);
        return invalid;
    }

    private static String key(JsonKey key) {
        if (key instanceof Json.Identifier) {
            return ((Json.Identifier) key).getName();
        } else if (key instanceof Json.Literal) {
            return String.valueOf(((Json.Literal) key).getValue());
        }
        return key.toString();
    }
}
