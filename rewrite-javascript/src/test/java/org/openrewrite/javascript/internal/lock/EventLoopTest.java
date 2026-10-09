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
package org.openrewrite.javascript.internal.lock;

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.EventLoop.Promise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventLoopTest {

    private final EventLoop loop = new EventLoop();
    private final List<String> log = new ArrayList<>();

    // a package requested at one level, whose children are requested once the request completes
    private Promise<Void> resolve(String name, List<String> children) {
        return loop.request(() -> {
            log.add(name);
            return name;
        }).then(n -> {
            List<Promise<Void>> resolving = new ArrayList<>();
            for (String child : children) {
                resolving.add(resolve(child, new ArrayList<>()));
            }
            return Promise.all(loop, resolving).then(done -> Promise.resolved(loop, null));
        });
    }

    @Test
    void siblingsSubtreesAdvanceTogetherAsUnderPromiseAll() {
        Promise<List<Void>> root = Promise.all(loop, Arrays.asList(
          resolve("cli", Arrays.asList("config", "create")),
          resolve("core", Arrays.asList("resolve", "runner"))));
        loop.await(root);
        assertThat(log).containsExactly("cli", "core", "config", "create", "resolve", "runner");
    }

    @Test
    void microtasksDrainBeforeTheNextRequestCompletes() {
        Promise<String> first = loop.request(() -> {
            log.add("first");
            return "first";
        });
        Promise<String> second = loop.request(() -> {
            log.add("second");
            return "second";
        });
        Promise<String> afterFirst = first.then(v -> Promise.resolved(loop, v))
          .then(v -> Promise.resolved(loop, v)).then(v -> {
              log.add("after " + v);
              return Promise.resolved(loop, v);
          });
        loop.await(Promise.all(loop, Arrays.asList(afterFirst, second)));
        assertThat(log).containsExactly("first", "after first", "second");
    }

    @Test
    void allKeepsTheOrderItWasGiven() {
        Promise<String> late = loop.request(() -> "late");
        Promise<String> early = Promise.resolved(loop, "early");
        assertThat(loop.await(Promise.all(loop, Arrays.asList(late, early)))).containsExactly("late", "early");
    }

    @Test
    void allRejectsWithTheFirstRejection() {
        Promise<String> second = loop.request(() -> {
            throw new IllegalArgumentException("second");
        });
        Promise<String> first = Promise.async(loop, () -> {
            throw new IllegalArgumentException("first");
        });
        assertThatThrownBy(() -> loop.await(Promise.all(loop, Arrays.asList(second, first))))
          .hasMessage("first");
    }

    @Test
    void recoverCatchesWhatTheAwaitedPromiseRejectsWith() {
        Promise<String> failed = loop.request(() -> {
            throw new IllegalStateException("404");
        });
        assertThat(loop.await(failed.recover(e -> Promise.resolved(loop, "skipped " + e.getMessage()))))
          .isEqualTo("skipped 404");
    }

    @Test
    void aPromiseNothingSettlesIsReported() {
        assertThatThrownBy(() -> loop.await(new Promise<String>(loop)))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("the promise never settles");
    }
}
