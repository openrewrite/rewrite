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
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;

public class YamlPrinter<P> implements YamlVisitor<String, P> {

    private final TreePrinter<P> treePrinter;

    public YamlPrinter(TreePrinter<P> treePrinter) {
        this.treePrinter = treePrinter;
    }

    @Override
    public String defaultValue(@Nullable Tree tree, P p) {
        return "";
    }

    @Override
    public String visit(Tree tree, P p) {
        if (tree == null) {
            return defaultValue(null, p);
        }

        Yaml t = treePrinter.doFirst((Yaml) tree, p);
        if (t == null) {
            return defaultValue(null, p);
        }

        return treePrinter.doLast(tree, t.accept(this, p), p);
    }

    public String visit(@Nullable List<? extends Yaml> nodes, P p) {
        if (nodes == null) {
            return "";
        }

        StringBuilder acc = new StringBuilder();
        for (Yaml node : nodes) {
            acc.append(visit(node, p));
        }
        return acc.toString();
    }

    @Override
    public String visitDocument(Yaml.Document document, P p) {
        return fmt(document, (document.isExplicit() ? "---" : "") + visit(document.getBlocks(), p));
    }

    @Override
    public String visitDocuments(Yaml.Documents documents, P p) {
        return fmt(documents, visit(documents.getDocuments(), p));
    }

    @Override
    public String visitSequenceEntry(Yaml.Sequence.Entry entry, P p) {
        return fmt(entry, "-" + visit(entry.getBlock(), p));
    }

    @Override
    public String visitSequence(Yaml.Sequence sequence, P p) {
        // todo, something isn't right with just not accessing the element prefix
        return visit(sequence.getEntries(), p);
    }

    @Override
    public String visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        return fmt(entry, visit(entry.getKey(), p) + entry.getBeforeMappingValueIndicator() + ":" + visit(entry.getValue(), p));
    }

    @Override
    public String visitMapping(Yaml.Mapping mapping, P p) {
        // todo, something isn't right with just not accessing the element prefix
        return visit(mapping.getEntries(), p);
    }

    @Override
    public String visitScalar(Yaml.Scalar scalar, P p) {
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

    private String fmt(@Nullable Yaml tree, @Nullable String code) {
        return tree == null || code == null ? "" : tree.getPrefix() + code;
    }
}
