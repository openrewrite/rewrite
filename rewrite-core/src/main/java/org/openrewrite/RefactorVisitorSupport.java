package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public interface RefactorVisitorSupport {
    @SuppressWarnings("unchecked")
    default <T extends Tree> T refactor(T t, Function<T, Tree> callSuper) {
        return (T) callSuper.apply(t);

//        if(refactored != t) {
//            Logger logger = LoggerFactory.getLogger(Transformer.class);
//            if (logger.isDebugEnabled()) {
//                logger.debug("Refactoring " + t.getClass().getSimpleName() + ". Before:");
//                logger.debug(t.printTrimmed() + "\n");
//                logger.debug("After: ");
//                logger.debug(refactored.print() + "\n");
//            }
//        }
    }

    @SuppressWarnings("unchecked")
    default <T extends Tree> T refactor(@Nullable Tree tree) {
        return (T) visit(tree);
    }

    default <T extends Tree> List<T> refactor(@Nullable List<T> trees) {
        if(trees == null) {
            return null;
        }

        List<T> mutatedTrees = new ArrayList<>(trees.size());
        boolean changed = false;
        for (T tree : trees) {
            T mutated = refactor(tree);
            if(mutated != tree) {
                changed = true;
            }
            mutatedTrees.add(mutated);
        }

        return changed ? mutatedTrees : trees;
    }

    Tree visit(@Nullable Tree tree);
}
