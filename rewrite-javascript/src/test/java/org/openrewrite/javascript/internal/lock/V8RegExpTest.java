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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.UpstreamCalls.Call;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Node 24.19.0's answers ({@code generate/node-builtins.js}): {@code new RegExp(source)}, with or without the u flag,
 * matched against a subject or V8's SyntaxError, and {@code String.prototype.replace} with its {@code $} patterns.
 */
class V8RegExpTest {

    // a group name used twice, which V8 13.6 allows across alternatives and Java never does, and a modifier group
    private static final List<String> CANNOT_KNOW = Arrays.asList("(?<a>x)|(?<a>y)", "(?i:a)");

    // with the u flag, also a property other than a General_Category
    private static final List<String> UNICODE_CANNOT_KNOW = Arrays.asList("(?<a>x)|(?<a>y)", "\\p{Letter}",
      "\\p{Script=Greek}", "\\p{ASCII}", "\\p{Foo}", "(?i-:a)");

    static Stream<Call> calls() {
        return UpstreamCalls.read(EcmaScriptTest.BUILTINS).stream()
          .filter(call -> "match".equals(call.getFn()) || "replace".equals(call.getFn()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("calls")
    void answersAsV8Does(Call call) {
        String source = (String) Objects.requireNonNull(call.arg(1));
        if ("replace".equals(call.getFn())) {
            assertThat(V8RegExp.replace((String) Objects.requireNonNull(call.arg(0)), V8RegExp.of(source),
              Boolean.TRUE.equals(call.arg(2)), (String) Objects.requireNonNull(call.arg(3)))).isEqualTo(call.getOut());
            return;
        }
        String regExp = (String) Objects.requireNonNull(call.arg(0));
        if ("u".equals(call.arg(2))) {
            answersAsV8DoesWithTheUFlag(call, regExp, source);
            return;
        }
        if (CANNOT_KNOW.contains(regExp)) {
            assertThatThrownBy(() -> V8RegExp.compile(regExp)).isInstanceOf(CannotKnow.class);
            return;
        }
        Pattern pattern = V8RegExp.compile(regExp);
        if (call.getThrew() != null) {
            assertThat(pattern).isNull();
            return;
        }
        assertThat(pattern).isNotNull();
        assertThat(match(pattern, source)).isEqualTo(call.getOut());
    }

    private static void answersAsV8DoesWithTheUFlag(Call call, String regExp, String subject) {
        Map<String, @Nullable Object> threw = call.getThrew();
        if (UNICODE_CANNOT_KNOW.contains(regExp)) {
            assertThatThrownBy(() -> V8RegExp.compileUnicode(regExp)).isInstanceOf(CannotKnow.class);
        } else if (threw != null) {
            assertThatThrownBy(() -> V8RegExp.compileUnicode(regExp)).isInstanceOf(JsError.class)
              .hasToString(threw.get("name") + ": " + threw.get("message"));
        } else {
            assertThat(match(V8RegExp.compileUnicode(regExp), subject)).isEqualTo(call.getOut());
        }
    }

    // String.prototype.match without the g flag: the match and its groups, undefined for one that did not take part
    private static @Nullable List<@Nullable Object> match(Pattern pattern, String subject) {
        Matcher m = pattern.matcher(subject);
        if (!m.find()) {
            return null;
        }
        List<@Nullable Object> out = new ArrayList<>();
        for (int i = 0; i <= m.groupCount(); i++) {
            out.add(m.group(i) == null ? EcmaScript.UNDEFINED : m.group(i));
        }
        return out;
    }
}
