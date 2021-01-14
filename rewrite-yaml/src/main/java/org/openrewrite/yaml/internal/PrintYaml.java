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
package org.openrewrite.yaml.internal;

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.AbstractYamlSourceVisitor;
import org.openrewrite.yaml.YamlSourceVisitor;
import org.openrewrite.yaml.tree.Yaml;

public class PrintYaml extends AbstractYamlSourceVisitor<String> {
    @Override
    public String defaultTo(Tree t) {
        return "";
    }

    @Override
    public String reduce(String r1, String r2) {
        return r1 + r2;
    }

    @Override
    public String visitDocument(Yaml.Document document) {
        return fmt(document, (document.isExplicit() ? "---" : "") + visit(document.getBlocks()));
    }

    @Override
    public String visitSequenceEntry(Yaml.Sequence.Entry entry) {
        return fmt(entry, "-" + visit(entry.getBlock()));
    }

    @Override
    public String visitMappingEntry(Yaml.Mapping.Entry entry) {
        return fmt(entry, visit(entry.getKey()) + ":" + visit(entry.getValue()));
    }

    @Override
    public String visitScalar(Yaml.Scalar scalar) {
        String value;
        switch (scalar.getStyle()) {
            case DOUBLE_QUOTED:
                value = "\"" + scalar.getValue() + "\"";
                break;
            case SINGLE_QUOTED:
                value = "'" + scalar.getValue() + "'";
                break;
            case LITERAL:
                value = "|" + scalar.getValue();
                break;
            case FOLDED:
                value = ">" + scalar.getValue();
                break;
            case PLAIN:
            default:
                value = scalar.getValue();
                break;
        }

        return fmt(scalar, value);
    }

    private String fmt(@Nullable Tree tree, @Nullable String code) {
        return tree == null || code == null ? "" : tree.getPrefix() + code;
    }
}
