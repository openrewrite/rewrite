/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.trait.internal;

import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;

import java.util.Iterator;

@Value
public class MaybeParenthesesPair {
    /**
     * Might be a {@link org.openrewrite.java.tree.J.Parentheses} if navigation up the tree was required.
     */
    Tree tree;
    /**
     * The direct parent in the cursor tree that is a {@link Tree}.
     */
    Tree parent;

    public static MaybeParenthesesPair from(Cursor cursor) {
        final Object initial = cursor.getValue();
        if (!(initial instanceof Tree)) {
            throw new IllegalArgumentException("Cursor value must be a Tree");
        }
        Tree tree = cursor.getValue();
        Tree parent;
        Iterator<Object> treeIterable = cursor.getPath(it -> it instanceof Tree && it != initial);
        if (treeIterable.hasNext()) {
            parent = (Tree) treeIterable.next();
        } else {
            throw new IllegalArgumentException("Cursor must have a parent");
        }
        while (parent instanceof J.Parentheses && treeIterable.hasNext()) {
            tree = parent;
            parent = (Tree) treeIterable.next();
        }
        return new MaybeParenthesesPair(tree, parent);
    }
}
