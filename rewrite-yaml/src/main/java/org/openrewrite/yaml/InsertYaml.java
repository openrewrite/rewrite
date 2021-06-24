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
package org.openrewrite.yaml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = true)
@Incubating(since = "7.2.0")
public class InsertYaml extends Recipe {
    @Option(displayName = "Key path",
            description = "XPath expression used to find matching keys.",
            example = "/metadata/labels")
    String key;

    @Option(displayName = "YAML snippet",
            description = "The YAML snippet to insert. The snippet will be indented to match the style of its surroundings.",
            example = "label-one: \"value-one\"")
    String yaml;

    @Incubating(since = "7.8.0")
    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/application-*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Insert YAML snippet";
    }

    @Override
    public String getDescription() {
        return "Insert a YAML snippet at a given key.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        }
        return null;
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher xPathMatcher = new XPathMatcher(key);

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                if (key.equals("/")) {
                    doAfterVisit(new InsertYamlVisitor<>(document, yaml));
                    return document;
                }
                return super.visitDocument(document, ctx);
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                if (xPathMatcher.matches(getCursor())) {
                    doAfterVisit(new InsertYamlVisitor<>(entry, yaml));
                    return entry;
                }
                return super.visitMappingEntry(entry, ctx);
            }
        };
    }
}
