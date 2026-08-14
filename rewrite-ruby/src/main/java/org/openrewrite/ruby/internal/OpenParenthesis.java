/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.ruby.internal;

import lombok.Getter;
import org.jruby.ast.Node;
import org.openrewrite.java.tree.Space;

import java.util.ArrayList;
import java.util.List;

public class OpenParenthesis {
    /**
     * We won't know until we see the closing parentheses whether the outermost node,
     * innermost node, or somewhere in between is the one that should be parenthesized.
     */
    private final List<Node> left = new ArrayList<>();

    boolean leftClosed;

    @Getter
    private final Space before;

    public OpenParenthesis(Node left, Space before) {
        this.before = before;
        this.left.add(left);
    }

    public void addLeft(Node left) {
        if (!leftClosed) {
            this.left.add(left);
        }
    }

    public boolean isParenthesized(Node right) {
        return left.contains(right);
    }

    public void closeLeft() {
        this.leftClosed = true;
    }
}
