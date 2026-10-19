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
package org.openrewrite.yaml.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.TreeShapeValidationService;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;

public class YamlTreeShapeValidationService implements TreeShapeValidationService {

    @Override
    public @Nullable Object value(Tree tree) {
        if (tree instanceof Yaml.Scalar) {
            Yaml.Scalar scalar = (Yaml.Scalar) tree;
            String value = scalar.getValue();
            // a block scalar's value holds the whitespace up to the next node, which the parser splits differently
            if (scalar.getStyle() == Yaml.Scalar.Style.LITERAL || scalar.getStyle() == Yaml.Scalar.Style.FOLDED) {
                value = value.replaceAll("\\s+$", "");
            }
            return "the " + scalar.getStyle() + " value `" + value + "`";
        } else if (tree instanceof Yaml.Anchor) {
            return "the anchor `" + ((Yaml.Anchor) tree).getKey() + "`";
        } else if (tree instanceof Yaml.Alias) {
            return "the alias of `" + ((Yaml.Alias) tree).getAnchor().getKey() + "`";
        }
        return null;
    }

    @Override
    public List<String> invalid(SourceFile sourceFile) {
        List<String> invalid = new ArrayList<>();
        new YamlIsoVisitor<List<String>>() {
            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, List<String> found) {
                Set<String> keys = new HashSet<>();
                for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
                    String key = entry.getKey().getValue();
                    if (!keys.add(key)) {
                        found.add("the key `" + key + "` appears twice in one mapping, which YAML doesn't allow");
                    }
                }
                return super.visitMapping(mapping, found);
            }
        }.visit(sourceFile, invalid);
        return invalid;
    }
}
