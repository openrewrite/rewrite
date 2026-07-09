/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scheduling;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.text.ChangeText;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecipePathTest {

    @Test
    void rootPath() {
        Recipe root = new ChangeText("a");
        RecipePath path = new RecipePath(root);

        assertThat(path.size()).isEqualTo(1);
        assertThat(path.leaf()).isSameAs(root);
        assertThat(path.getFirst()).isSameAs(root);
        assertThat(path).containsExactly(root);
    }

    @Test
    void childIsRootFirstLeafLast() {
        Recipe root = new ChangeText("root");
        Recipe a = new ChangeText("a");
        Recipe b = new ChangeText("b");

        RecipePath path = new RecipePath(root).child(a).child(b);

        assertThat(path.size()).isEqualTo(3);
        assertThat(path.leaf()).isSameAs(b);
        // Ordered root-first, leaf-last, mirroring the previous Stack representation.
        assertThat(path).containsExactly(root, a, b);
        assertThat(path.get(0)).isSameAs(root);
        assertThat(path.get(1)).isSameAs(a);
        assertThat(path.get(2)).isSameAs(b);
    }

    @Test
    void childSharesParentWithoutMutatingIt() {
        Recipe root = new ChangeText("root");
        RecipePath parent = new RecipePath(root);

        RecipePath childA = parent.child(new ChangeText("a"));
        RecipePath childB = parent.child(new ChangeText("b"));

        // Deriving children must not affect the parent or siblings.
        assertThat(parent.size()).isEqualTo(1);
        assertThat(childA.size()).isEqualTo(2);
        assertThat(childB.size()).isEqualTo(2);
        assertThat(childA.getFirst()).isSameAs(root);
        assertThat(childB.getFirst()).isSameAs(root);
    }

    @Test
    void subListYieldsParentPath() {
        Recipe root = new ChangeText("root");
        Recipe a = new ChangeText("a");
        Recipe b = new ChangeText("b");
        RecipePath path = new RecipePath(root).child(a).child(b);

        assertThat(path.subList(0, path.size() - 1)).containsExactly(root, a);
    }

    @Test
    void getOutOfBounds() {
        RecipePath path = new RecipePath(new ChangeText("a"));
        assertThatThrownBy(() -> path.get(1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> path.get(-1)).isInstanceOf(IndexOutOfBoundsException.class);
    }
}
