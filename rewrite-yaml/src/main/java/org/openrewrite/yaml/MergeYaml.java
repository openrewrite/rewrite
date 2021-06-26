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
package org.openrewrite.yaml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.format.AutoFormatVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class MergeYaml extends Recipe {

    @Option(displayName = "Key path",
            description = "XPath expression used to find matching keys.",
            example = "/metadata")
    String key;
    @Option(displayName = "YAML snippet",
            description = "The YAML snippet to insert. The snippet will be indented to match the style of its surroundings.",
            example = "labels: \n\tlabel-one: \"value-one\"")
    @Language("yml")
    String yaml;

    @Override
    public Validated validate() {
        return super.validate()
                .and(Validated.test("yaml", "Must be valid YAML",
                        yaml, y -> {
                            List<Yaml.Documents> docs = new YamlParser().parse(yaml);
                            if (docs.size() == 0) {
                                return false;
                            }
                            Yaml.Documents doc = docs.get(0);
                            if (doc.getDocuments().size() == 0) {
                                return false;
                            }
                            Yaml.Block block = doc.getDocuments().get(0).getBlock();
                            return block instanceof Yaml.Mapping;
                        }));
    }

    @Override
    public String getDisplayName() {
        return "Merge YAML snippet";
    }

    @Override
    public String getDescription() {
        return "Merge a YAML snippet with an existing YAML document.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher matcher = new XPathMatcher(key);
        XPathMatcher parentMatcher;
        if ("/".equals(key)) {
            parentMatcher = new XPathMatcher("/");
        } else {
            parentMatcher = new XPathMatcher(key.substring(0, key.lastIndexOf('/')));
        }
        Yaml.Mapping incoming = (Yaml.Mapping) new YamlParser().parse(yaml).get(0).getDocuments().get(0).getBlock();

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable Yaml postVisit(Yaml tree, ExecutionContext executionContext) {
                if (tree instanceof Yaml.Mapping.Entry && ((Yaml.Mapping.Entry) tree).getKey().getValue().equals("spec")) {
                    System.out.println(tree.print());
                }
                return super.postVisit(tree, executionContext);
            }

            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext executionContext) {
                return super.visitMapping(mapping, executionContext);
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);

                boolean needsFormat = false;
                if (matcher.matches(getCursor())) {
                    doAfterVisit(new MergeYamlVisitor(getCursor().getParentOrThrow().getValue(), incoming));
                    needsFormat = true;
                } else if (parentMatcher.matches(getCursor())
                        && e.getValue() instanceof Yaml.Mapping
                        && !hasChildWithKey(e.getKey().getValue(), (Yaml.Mapping) e.getValue())) {
                    doAfterVisit(new MergeYamlVisitor(e.getValue(), incoming));
                    needsFormat = true;
                }

                if (needsFormat) {
                    doAfterVisit(new AutoFormatVisitor<>(e));
                }

                return e;
            }
        };
    }

    private static boolean hasChildWithKey(String key, Yaml.Mapping mapping) {
        return mapping.getEntries().stream()
                .anyMatch(childEntry -> key.equals(childEntry.getKey().getValue()));
    }

}
