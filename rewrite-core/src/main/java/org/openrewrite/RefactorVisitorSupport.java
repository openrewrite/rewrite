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
