/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.openrewrite.javascript.internal.lock.EventLoop;
import org.openrewrite.javascript.internal.lock.EventLoop.Promise;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * One I/O operation on the event loop, with the operations that completed before it was issued as far as the request
 * issuing it knows: which of two racing requests can see the other's result.
 */
final class Io<T> {
    final int id;
    final Promise<T> promise;
    final Set<Integer> causal;

    private Io(int id, Promise<T> promise, Set<Integer> causal) {
        this.id = id;
        this.promise = promise;
        this.causal = causal;
    }

    static <T> Io<T> issue(EventLoop loop, Set<Integer> causal, Supplier<T> perform) {
        int id = loop.issued();
        return new Io<>(id, loop.request(perform), new LinkedHashSet<>(causal));
    }
}
