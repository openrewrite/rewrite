/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrewrite.yaml;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.yaml.internal.grammar.JsonPath;
import org.openrewrite.yaml.internal.grammar.JsonPathLexer;
import org.openrewrite.yaml.tree.JsonPathYamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.disjoint;

public class JsonPathMatcher {

    private final String jsonPath;

    public JsonPathMatcher(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public Optional<Object> find(Cursor cursor) {
        JsonPath jsonPath = new JsonPath(new CommonTokenStream(new JsonPathLexer(CharStreams.fromString(this.jsonPath))));
        LinkedList<Tree> cursorPath = cursor.getPathAsStream()
                .filter(o -> o instanceof Tree)
                .map(Tree.class::cast)
                .collect(Collectors.toCollection(LinkedList::new));
        if (cursorPath.isEmpty()) {
            return Optional.empty();
        }
        Collections.reverse(cursorPath);

        JsonPath.JsonpathContext ctx = jsonPath.jsonpath();
        Object result = new JsonPathYamlVisitor(cursorPath).visit(ctx);
        return Optional.ofNullable(result);
    }

    public boolean matches(Cursor cursor) {
        return find(cursor).map(o -> {
            if (o instanceof List) {
                List<Object> cursorPath = cursor.getPathAsStream().collect(Collectors.toList());
                //noinspection unchecked
                return !disjoint((List<Yaml>) o, cursorPath);
            } else {
                return Objects.equals(o, cursor.getValue());
            }
        }).orElse(false);
    }

    public boolean encloses(Cursor cursor) {
        return false;
    }

}
