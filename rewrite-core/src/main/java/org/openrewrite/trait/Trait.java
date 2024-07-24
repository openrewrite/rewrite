/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.trait;

import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;

/**
 * A trait captures semantic information related to a syntax element.
 *
 * @param <T> The type of the tree that this trait is related to. When
 *            multiple specific types of tree are possible, this should
 *            be the lowest common super-type of all the types.
 */
@Incubating(since = "8.30.0")
public interface Trait<T extends Tree> {
    Cursor getCursor();

    default T getTree() {
        return getCursor().getValue();
    }
}
