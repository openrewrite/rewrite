/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.polyglot;

import org.openrewrite.Cursor;
import org.openrewrite.TreePrinter;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;

public class PolyglotPrinter<T> extends TreeVisitor<PolyglotTree, T> {

    private static final String PRINTER_ACC_KEY = "printed";

    private final TreePrinter<T> treePrinter;

    public PolyglotPrinter(TreePrinter<T> treePrinter) {
        this.treePrinter = treePrinter;
    }

    public TreePrinter<T> getTreePrinter() {
        return treePrinter;
    }

    public String print(PolyglotTree tree, T obj) {
        setCursor(new Cursor(null, "EPSILON"));
        visit(tree, obj);
        return getPrinter().toString();
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

}
