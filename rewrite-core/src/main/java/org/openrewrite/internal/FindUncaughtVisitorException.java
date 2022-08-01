/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.internal;

import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.UncaughtVisitorException;
import org.openrewrite.UncaughtVisitorExceptionResult;
import org.openrewrite.marker.Markers;

import java.lang.reflect.Method;

public class FindUncaughtVisitorException extends TreeVisitor<Tree, Integer> {
    private final UncaughtVisitorException vt;
    private final Tree nearestTree;

    public FindUncaughtVisitorException(UncaughtVisitorException vt) {
        this.vt = vt;
        this.nearestTree = (Tree) vt.getCursor().getPath(Tree.class::isInstance).next();
    }

    @Override
    public Tree preVisit(Tree tree, Integer integer) {
        if (tree == nearestTree) {
            try {
                Method getMarkers = tree.getClass().getDeclaredMethod("getMarkers");
                Method withMarkers = tree.getClass().getDeclaredMethod("withMarkers", Markers.class);
                Markers markers = (Markers) getMarkers.invoke(tree);
                return (Tree) withMarkers.invoke(tree, markers
                        .computeByType(new UncaughtVisitorExceptionResult(vt), (s1, s2) -> s1 == null ? s2 : s1));
            } catch (Throwable ignored) {
            }
        }
        return tree;
    }
}
