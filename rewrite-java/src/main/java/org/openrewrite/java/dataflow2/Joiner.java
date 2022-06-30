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
package org.openrewrite.java.dataflow2;

import java.util.Arrays;
import java.util.Collection;

/**
 * @param <T> Must be a lattice verifying the bounded scale property, namely:
 *            - elements in T are partially ordered
 *            - there exists an upper bound B such that for all values v, v &leq; B
 *            - there does not exist any loop in ordering such as v1 &lt; v2 &lt; .. &lt; vN &lt; v1
 *            - join(v1, .., vN) &geq; vi : the join operation must be monotonically increasing
 */
public abstract class Joiner<T> {
    public abstract T join(Collection<T> values);

    @SafeVarargs
    public final T join(T... outs) {
        return join(Arrays.asList(outs));
    }

    public abstract T lowerBound();

    public abstract T defaultInitialization();
}
