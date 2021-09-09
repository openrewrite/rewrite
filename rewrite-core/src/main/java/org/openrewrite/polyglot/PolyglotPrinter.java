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
