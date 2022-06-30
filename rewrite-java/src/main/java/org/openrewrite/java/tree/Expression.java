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
package org.openrewrite.java.tree;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.dataflow2.ProgramPoint;

import java.util.List;

import static java.util.Collections.emptyList;

public interface Expression extends J, ProgramPoint {
    @Nullable
    JavaType getType();

    <T extends J> T withType(@Nullable JavaType type);

    /**
     * @return A list of the side effects emitted by the statement, if the statement was decomposed.
     * So for a binary operation, there are up to two potential side effects (the left and right side) and as
     * few as zero if both sides of the expression are something like constants or variable references.
     */
    default List<J> getSideEffects() {
        return emptyList();
    }
}
