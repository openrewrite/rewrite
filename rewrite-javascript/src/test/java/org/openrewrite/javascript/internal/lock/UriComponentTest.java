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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.UpstreamCalls.Call;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Node 24.19.0's {@code encodeURIComponent}, {@code decodeURIComponent} and {@code decodeURI}
 * ({@code generate/node-builtins.js}), a URIError being an IllegalArgumentException.
 */
class UriComponentTest {

    static Stream<Call> calls() {
        return UpstreamCalls.read(EcmaScriptTest.BUILTINS).stream()
          .filter(call -> call.getFn().endsWith("URIComponent") || "decodeURI".equals(call.getFn()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("calls")
    void answersAsNodeDoes(Call call) {
        Function<String, String> fn = "encodeURIComponent".equals(call.getFn()) ?
          UriComponent::encodeURIComponent : "decodeURI".equals(call.getFn()) ? UriComponent::decodeURI :
          UriComponent::decodeURIComponent;
        String arg = (String) Objects.requireNonNull(call.arg(0));
        if (call.getThrew() != null) {
            assertThatThrownBy(() -> fn.apply(arg)).isInstanceOf(IllegalArgumentException.class)
              .hasMessage((String) call.getThrew().get("message"));
            return;
        }
        assertThat(fn.apply(arg)).isEqualTo(call.getOut());
    }
}
