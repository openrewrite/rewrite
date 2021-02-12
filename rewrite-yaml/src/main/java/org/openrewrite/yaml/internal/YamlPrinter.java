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

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;

public class YamlPrinter<P> extends YamlVisitor<P> {

    private static final String PRINTER_ACC_KEY = "printed";

    private final TreePrinter<P> treePrinter;

    public YamlPrinter(TreePrinter<P> treePrinter) {
        this.treePrinter = treePrinter;
        setCursoringOn();
    }

    @NonNull
    protected StringBuilder getPrinter() {
        StringBuilder acc = getCursor().getRoot().getMessage(PRINTER_ACC_KEY);
        if (acc == null) {
            acc = new StringBuilder();
            getCursor().getRoot().putMessage(PRINTER_ACC_KEY, acc);
        }
        return acc;
    }

    public String print(Yaml yaml, P p) {
        setCursor(new Cursor(null, "EPSILON"));
        visit(yaml, p);
        return getPrinter().toString();
    }

    @Override
    @Nullable
    public Yaml visit(@Nullable Tree tree, P p) {

        if (tree == null) {
            return defaultValue(null, p);
        }

        StringBuilder printerAcc = getPrinter();
        treePrinter.doBefore(tree, printerAcc, p);
        tree = super.visit(tree, p);
        if (tree != null) {
            treePrinter.doAfter(tree, printerAcc, p);
        }
        return (Yaml) tree;
    }

    public void visit(@Nullable List<? extends Yaml> nodes, P p) {
        if (nodes != null) {
            for (Yaml node : nodes) {
                visit(node, p);
            }
        }
    }

    @Override
    public Yaml visitDocument(Yaml.Document document, P p) {
        StringBuilder acc = getPrinter();
        acc.append(document.getPrefix());
        if (document.isExplicit()) {
            acc.append("---");
        }
        visit(document.getBlocks(), p);
        if (document.getEnd() != null) {
            acc.append(document.getEnd().getPrefix()).append("...");
        }
        return document;
    }

    @Override
    public Yaml visitDocuments(Yaml.Documents documents, P p) {
        getPrinter().append(documents.getPrefix());
        return super.visitDocuments(documents, p);
    }

    @Override
    public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry, P p) {
        StringBuilder acc = getPrinter();
        acc.append(entry.getPrefix()).append('-');
        visit(entry.getBlock(), p);
        return entry;
    }

    @Override
    public Yaml visitSequence(Yaml.Sequence sequence, P p) {
        getPrinter().append(sequence.getPrefix());
        return super.visitSequence(sequence, p);
    }

    @Override
    public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        StringBuilder acc = getPrinter();
        acc.append(entry.getPrefix());
        visit(entry.getKey(), p);
        acc.append(entry.getBeforeMappingValueIndicator())
                .append(':');
        visit(entry.getValue(), p);
        return entry;
    }

    @Override
    public Yaml visitMapping(Yaml.Mapping mapping, P p) {
        return super.visitMapping(mapping, p);
    }

    @Override
    public Yaml visitScalar(Yaml.Scalar scalar, P p) {
        StringBuilder acc = getPrinter();
        acc.append(scalar.getPrefix());
        switch (scalar.getStyle()) {
            case DOUBLE_QUOTED:
                acc.append('"')
                        .append(scalar.getValue())
                        .append('"');
                break;
            case SINGLE_QUOTED:
                acc.append('\'')
                        .append(scalar.getValue())
                        .append('\'');
                break;
            case LITERAL:
                acc.append('|')
                        .append(scalar.getValue());
                break;
            case FOLDED:
                acc.append('>')
                        .append(scalar.getValue());
                break;
            case PLAIN:
            default:
                acc.append(scalar.getValue());
                break;

        }
        return scalar;
    }
}
