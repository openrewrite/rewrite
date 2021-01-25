package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

public interface TreePrinter<P> {

    static <P> TreePrinter<P> identity() {
        return new TreePrinter<P>() {
        };
    }

    /**
     * Called before tree is printed, allows printing additional output before tree.
     *
     * @param tree AST element that is about to be printed
     * @param printerAcc Printer accumulator, can be appended to, contains everything written for the overall visit
     *                   operation so far
     * @param p visit context
     */
    default void doBefore(@Nullable Tree tree, StringBuilder printerAcc, P p) {}

    /**
     * Called after tree has been printed, allows printing additional output after tree.
     *
     * @param tree AST element that has just been printed
     * @param printerAcc Printer accumulator, can be appended to, contains everything written for the overall visit
     *      *                   operation so far
     * @param p visit context
     */
    default void doAfter(@Nullable Tree tree, StringBuilder printerAcc, P p) {}


}
