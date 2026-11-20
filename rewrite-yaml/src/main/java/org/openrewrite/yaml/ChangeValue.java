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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.trait.BlockScalar;
import org.openrewrite.yaml.tree.Yaml;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeValue extends Recipe {
    @Option(displayName = "Key path",
            description = "A [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expression to locate a YAML entry.",
            example = "$.subjects.kind")
    String keyPath;

    @Option(displayName = "New value",
            description = "The new value to set for the key identified by the `oldKeyPath`.",
            example = "Deployment")
    String value;


    @Option(displayName = "File pattern",
            description = "A glob expression representing a file path to search for (relative to the project root). Blank/null matches all.",
            required = false,
            example = ".github/workflows/*.yml")
    @Nullable
    String filePattern;

    String displayName = "Change value";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", keyPath, value);
    }

    String description = "Change a YAML mapping entry value while leaving the key intact.";

    @Override
    public Validated<Object> validate() {
        return super.validate().and(JsonPathMatcher.validate("keyPath", keyPath));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(keyPath);
        return Preconditions.check(new FindSourceFiles(filePattern), new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                if (!matcher.matches(getCursor())) {
                    return e;
                }
                BlockScalar block = new BlockScalar.Matcher().get(new Cursor(getCursor(), e.getValue())).orElse(null);
                if (block != null) {
                    if (!block.getBody().equals(value)) {
                        e = e.withValue(block.withBody(value));
                    }
                } else if (!(e.getValue() instanceof Yaml.Scalar) || !((Yaml.Scalar) e.getValue()).getValue().equals(value)) {
                    Yaml.Anchor anchor = (e.getValue() instanceof Yaml.Scalar) ? ((Yaml.Scalar) e.getValue()).getAnchor() : null;
                    Yaml.Tag tag = (e.getValue() instanceof Yaml.Scalar) ? ((Yaml.Scalar) e.getValue()).getTag() : null;
                    String prefix = e.getValue() instanceof Yaml.Sequence ? ((Yaml.Sequence) e.getValue()).getOpeningBracketPrefix() : e.getValue().getPrefix();
                    Yaml.Block parsed = parsed(prefix, anchor, tag);
                    if (parsed == null) {
                        e = e.withValue(
                                new Yaml.Scalar(randomId(), prefix, Markers.EMPTY,
                                        Yaml.Scalar.Style.PLAIN, anchor, tag, value)
                        );
                    } else if (!parsed.printTrimmed(getCursor()).equals(e.getValue().printTrimmed(getCursor()))) {
                        e = e.withValue(parsed);
                    }
                }
                return e;
            }

            /**
             * The value as the parser reads it, such as a flow sequence for `[a, b]` or a quoted scalar for `"*"`, or
             * null when it reads the value as the plain scalar it is.
             */
            private Yaml.@Nullable Block parsed(@Nullable String prefix, Yaml.@Nullable Anchor anchor,
                                                Yaml.@Nullable Tag tag) {
                Yaml.Block parsed = new YamlParser().parse(new InMemoryExecutionContext(),
                                "key:" + (prefix == null || prefix.isEmpty() ? " " : prefix) + value)
                        .filter(documents -> documents instanceof Yaml.Documents &&
                                             !((Yaml.Documents) documents).getDocuments().isEmpty())
                        .map(documents -> ((Yaml.Documents) documents).getDocuments().get(0).getBlock())
                        .filter(Yaml.Mapping.class::isInstance)
                        .map(mapping -> ((Yaml.Mapping) mapping).getEntries())
                        .filter(entries -> entries.size() == 1)
                        .map(entries -> entries.get(0).getValue())
                        .findFirst()
                        .orElse(null);
                if (parsed instanceof Yaml.Scalar) {
                    Yaml.Scalar scalar = (Yaml.Scalar) parsed;
                    return scalar.getStyle() == Yaml.Scalar.Style.PLAIN ? null : scalar.withAnchor(anchor).withTag(tag);
                } else if (parsed instanceof Yaml.Sequence) {
                    return ((Yaml.Sequence) parsed).withAnchor(anchor).withTag(tag);
                } else if (parsed instanceof Yaml.Mapping) {
                    return ((Yaml.Mapping) parsed).withAnchor(anchor).withTag(tag);
                }
                return null;
            }

            @Override
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, ExecutionContext ctx) {
                Yaml.Scalar s = super.visitScalar(scalar, ctx);
                if (matcher.matches(getCursor())) {
                    BlockScalar block = new BlockScalar.Matcher().get(getCursor()).orElse(null);
                    if (block != null) {
                        if (!block.getBody().equals(value)) {
                            s = block.withBody(value);
                        }
                    } else if (!s.getValue().equals(value)) {
                        s = s.withValue(value);
                    }
                }
                return s;
            }
        });
    }


}
