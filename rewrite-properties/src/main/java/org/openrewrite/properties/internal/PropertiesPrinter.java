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
package org.openrewrite.properties.internal;

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;

import java.util.List;

public class PropertiesPrinter<P> extends PropertiesVisitor<P> {

    private static final String PRINTER_ACC_KEY = "printed";

    private final TreePrinter<P> treePrinter;

    public PropertiesPrinter(TreePrinter<P> treePrinter) {
        this.treePrinter = treePrinter;
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

    public String print(Properties properties, P p) {
        setCursor(new Cursor(null, "EPSILON"));
        visit(properties, p);
        return getPrinter().toString();
    }

    @Override
    @Nullable
    public Properties visit(@Nullable Tree tree, P p) {

        if (tree == null) {
            return defaultValue(null, p);
        }

        StringBuilder printer = getPrinter();
        treePrinter.doBefore(tree, printer, p);
        tree = super.visit(tree, p);
        if (tree != null) {
            treePrinter.doAfter(tree, printer, p);
        }
        return (Properties) tree;
    }

    public void visit(@Nullable List<? extends Properties> nodes, P p) {
        if (nodes != null) {
            for (Properties node : nodes) {
                visit(node, p);
            }
        }
    }

    @Override
    public Properties visitFile(Properties.File file, P p) {
        StringBuilder acc = getPrinter();
        acc.append(file.getPrefix());
        visit(file.getContent(), p);
        acc.append(file.getEof());
        return file;
    }

    @Override
    public Properties visitEntry(Properties.Entry entry, P p) {
        StringBuilder acc = getPrinter();
        acc.append(entry.getPrefix())
                .append(entry.getKey())
                .append(entry.getBeforeEquals())
                .append('=')
                .append(entry.getValue().getPrefix())
                .append(entry.getValue().getText());
        return entry;
    }

    @Override
    public Properties visitComment(Properties.Comment comment, P p) {
        StringBuilder acc = getPrinter();
        acc.append(comment.getPrefix())
                .append('#')
                .append(comment.getMessage());
        return comment;
    }
}
