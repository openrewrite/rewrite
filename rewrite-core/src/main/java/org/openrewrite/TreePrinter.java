package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

public interface TreePrinter<T extends Tree, P> {
    /**
     * Transform the AST before a printer prints the subtree.
     *
     * @param tree The subtree to print.
     * @return A tree, possibly mutated for the purpose of printing only. The mutation
     * is discarded after printing.
     */
    @Nullable
    default <T2 extends T> T2 doFirst(T2 tree, P p) {
        return tree;
    }

    /**
     * Transform the printed code.
     *
     *
     * @param tree
     * @param printed The code as printed by the printer.
     * @return The code, possibly mutated.
     */
    default String doLast(Tree tree, String printed, P p) {
        return printed;
    }
}
