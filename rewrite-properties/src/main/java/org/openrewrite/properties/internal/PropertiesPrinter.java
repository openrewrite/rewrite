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

import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;

import java.util.List;

public class PropertiesPrinter<P> implements PropertiesVisitor<String, P> {

    private final TreePrinter<P> treePrinter;

    public PropertiesPrinter(TreePrinter<P> treePrinter) {
        this.treePrinter = treePrinter;
    }

    @NonNull
    @Override
    public String defaultValue(@Nullable Tree tree, P p) {
        return "";
    }

    @NonNull
    @Override
    public String visit(@Nullable Tree tree, P p) {
        if (tree == null) {
            return defaultValue(null, p);
        }

        Properties t = treePrinter.doFirst((Properties) tree, p);
        if (t == null) {
            return defaultValue(null, p);
        }

        //noinspection ConstantConditions
        return treePrinter.doLast(tree, t.accept(this, p), p);
    }

    public String visit(@Nullable List<? extends Properties> nodes, P p) {
        if (nodes == null) {
            return "";
        }

        StringBuilder acc = new StringBuilder();
        for (Properties node : nodes) {
            acc.append(visit(node, p));
        }
        return acc.toString();
    }

    @Override
    public String visitFile(Properties.File file, P p) {
        return file.getPrefix() + visit(file.getContent(), p) + file.getEof();
    }

    @Override
    public String visitEntry(Properties.Entry entry, P p) {
        return entry.getPrefix() + entry.getKey() +
                entry.getBeforeEquals() + "=" +
                entry.getValue().getPrefix() + entry.getValue().getText();
    }

    @Override
    public String visitComment(Properties.Comment comment, P p) {
        return comment.getPrefix() + "#" + comment.getMessage();
    }
}
