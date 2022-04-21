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
package org.openrewrite.dataflow;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.openrewrite.Incubating;

import java.util.List;

/**
 * A maximal sequence of statements I_0, I_1, I_2, ..., I_n such that
 * if I_j and I_j+1 are two adjacent statements in this
 * sequence, then
 *  - The execution of I_j is always immediately followed by the
 *    execution of I_j+1.
 *  - The execution of I_j+1 is always immediate preceded by
 *    the execution of I_j.
 * Edges between basic blocks represent potential flow of control.
 *
 * @param <S> The statement type of a particular language family.
 *
 */
@Value
@Incubating(since = "7.22.0")
public class BasicBlock<S> {
    @NonFinal
    List<S> statements;

    public void unsafeSetStatements(List<S> statements) {
        this.statements = statements;
    }
}
