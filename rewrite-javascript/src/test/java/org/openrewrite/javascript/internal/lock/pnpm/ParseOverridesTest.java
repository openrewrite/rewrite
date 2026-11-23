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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.strings;

/**
 * config/parse-overrides/test/index.ts of pnpm 11.2.2.
 */
class ParseOverridesTest {

    private static final Map<String, @Nullable Map<String, @Nullable Object>> NO_CATALOGS = new LinkedHashMap<>();

    private static List<String> parse(String overrides) {
        List<String> out = new ArrayList<>();
        for (ParseOverrides.VersionOverride o : ParseOverrides.parseOverrides(strings(overrides), NO_CATALOGS)) {
            out.add(o.getSelector() + " -> " + o.getNewBareSpecifier() + " parent=" + selector(o.getParentPkg()) +
                    " target=" + selector(o.getTargetPkg()));
        }
        return out;
    }

    private static String selector(ParseOverrides.@Nullable PackageSelector selector) {
        return selector == null ? "-" : selector.getName() +
                                        (selector.getBareSpecifier() == null ? "" : "@" + selector.getBareSpecifier());
    }

    @Test
    void parseOverrides() {
        assertThat(parse("{'foo':'1'}")).containsExactly("foo -> 1 parent=- target=foo");
        assertThat(parse("{'foo@2':'1'}")).containsExactly("foo@2 -> 1 parent=- target=foo@2");
        assertThat(parse("{'foo@>2':'1','foo@3 || >=2':'1'}"))
          .containsExactly("foo@>2 -> 1 parent=- target=foo@>2", "foo@3 || >=2 -> 1 parent=- target=foo@3 || >=2");
        assertThat(parse("{'bar>foo':'2','bar@1>foo':'2','bar>foo@1':'2','bar@1>foo@1':'2'}"))
          .containsExactly("bar>foo -> 2 parent=bar target=foo", "bar@1>foo -> 2 parent=bar@1 target=foo",
            "bar>foo@1 -> 2 parent=bar target=foo@1", "bar@1>foo@1 -> 2 parent=bar@1 target=foo@1");
        assertThat(parse("{'foo@>2>bar@>2':'1','foo@3 || >=2>bar@3 || >=2':'1'}"))
          .containsExactly("foo@>2>bar@>2 -> 1 parent=foo@>2 target=bar@>2",
            "foo@3 || >=2>bar@3 || >=2 -> 1 parent=foo@3 || >=2 target=bar@3 || >=2");
    }

    @Test
    void parseOverridesThrowsAnExceptionOnInvalidSelector() {
        assertThatThrownBy(() -> ParseOverrides.parseOverrides(strings("{'%':'2'}"), NO_CATALOGS))
          .hasMessage("Cannot parse the \"%\" selector");
        assertThatThrownBy(() -> ParseOverrides.parseOverrides(strings("{'foo > bar':'2'}"), NO_CATALOGS))
          .hasMessage("Cannot parse the \"foo > bar\" selector");
    }

    /**
     * The block {@code overrides-ws-ref} records: a {@code $name} value is the root manifest's specifier.
     */
    @Test
    void aReferenceTakesTheRootSpecifier() {
        PnpmOptions options = PnpmOptions.load("overrides:\n  is-number: $is-number\n",
          PnpmTestSupport.obj("{'dependencies':{'is-number':'7.0.0'}}"), null);
        assertThat(ParseOverrides.createOverridesMapFromParsed(ParseOverrides.parseOverrides(options.getOverrides(),
          options.getCatalogs()))).containsEntry("is-number", "7.0.0");
    }
}
