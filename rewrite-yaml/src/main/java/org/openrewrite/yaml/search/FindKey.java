/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.yaml.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.RecipeSearchResult;
import org.openrewrite.yaml.XPathMatcher;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.HashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindKey extends Recipe {

    @Option(displayName = "Key path",
            description = "XPath expression used to find matching keys.",
            example = "/subjects/kind")
    String key;

    @Override
    public String getDisplayName() {
        return "Find YAML entries";
    }

    @Override
    public String getDescription() {
        return "Find YAML entries by XPath expression.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher xPathMatcher = new XPathMatcher(key);
        return new YamlVisitor<ExecutionContext>() {
            @Override
            public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = (Yaml.Mapping.Entry) super.visitMappingEntry(entry, ctx);
                if (xPathMatcher.matches(getCursor())) {
                    e = e.withMarker(new RecipeSearchResult(FindKey.this));
                }
                return e;
            }
        };
    }

    public static Set<Yaml.Mapping.Entry> find(Yaml y, String xPath) {
        XPathMatcher xPathMatcher = new XPathMatcher(xPath);
        YamlVisitor<Set<Yaml.Mapping.Entry>> findVisitor = new YamlVisitor<Set<Yaml.Mapping.Entry>>() {
            @Override
            public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, Set<Yaml.Mapping.Entry> es) {
                Yaml.Mapping.Entry e = (Yaml.Mapping.Entry) super.visitMappingEntry(entry, es);
                if (xPathMatcher.matches(getCursor())) {
                    es.add(e);
                }
                return e;
            }
        };

        Set<Yaml.Mapping.Entry> es = new HashSet<>();
        findVisitor.visit(y, es);
        return es;
    }
}
