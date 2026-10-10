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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openrewrite.javascript.internal.lock.JsError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * yarn 1.22.22's __tests__/normalize-manifest.js over the fixtures whose fields the port normalizes; the test
 * resources' {@code generate/bundle-rows.js} prints the rows that hold yarn's own answers.
 */
class NormalizeManifestTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            "bundledDependencies alias|true|{\"bundledDependencies\": [\"foobar\"]}|" +
            "{\"name\": \"\", \"version\": \"\", \"bundleDependencies\": [\"foobar\"]}|",
            "clean version|true|{\"version\": \" v1.0.0\"}|{\"name\": \"\", \"version\": \"1.0.0\"}|",
            "clean version with v|true|{\"version\": \"v1.0.0\"}|{\"name\": \"\", \"version\": \"1.0.0\"}|",
            "dedupe all trivial dependencies|true|" +
            "{\"devDependencies\": {\"foobar\": \"*\"}, \"dependencies\": {\"foobar\": \"*\"}}|" +
            "{\"devDependencies\": {}, \"dependencies\": {\"foobar\": \"*\"}, \"name\": \"\", \"version\": \"\"}|",
            "dedupe dependencies|false|" +
            "{\"devDependencies\": {\"foobar\": \"1.0.0\"}, \"dependencies\": {\"foobar\": \"2.0.0\"}}|" +
            "{\"devDependencies\": {}, \"dependencies\": {\"foobar\": \"2.0.0\"}, \"name\": \"\", \"version\": \"\"}|",
            "dedupe dependencies root|true|" +
            "{\"devDependencies\": {\"foobar\": \"1.0.0\"}, \"dependencies\": {\"foobar\": \"2.0.0\"}}|" +
            "{\"devDependencies\": {}, \"dependencies\": {\"foobar\": \"2.0.0\"}, \"name\": \"\", \"version\": \"\"}|" +
            "\"dependencies\" has dependency \"foobar\" with range \"2.0.0\" that collides with a dependency in " +
            "\"devDependencies\" of the same name with version \"1.0.0\"",
            "dedupe optionalDependencies|false|" +
            "{\"optionalDependencies\": {\"foobar\": \"1.0.0\"}, \"dependencies\": {\"foobar\": \"2.0.0\"}}|" +
            "{\"optionalDependencies\": {\"foobar\": \"1.0.0\"}, \"dependencies\": {}, \"name\": \"\", " +
            "\"version\": \"\"}|",
            "dedupe optionalDependencies root|true|" +
            "{\"optionalDependencies\": {\"foobar\": \"1.0.0\"}, \"dependencies\": {\"foobar\": \"2.0.0\"}}|" +
            "{\"optionalDependencies\": {\"foobar\": \"1.0.0\"}, \"dependencies\": {}, \"name\": \"\", " +
            "\"version\": \"\"}|\"optionalDependencies\" has dependency \"foobar\" with range \"1.0.0\" that " +
            "collides with a dependency in \"dependencies\" of the same name with version \"2.0.0\"",
            "dedupe single trivial dependency|true|" +
            "{\"devDependencies\": {\"foobar1\": \"*\", \"foobar2\": \"2.0.0\", \"foobar4\": \"*\"}, " +
            "\"dependencies\": {\"foobar1\": \"1.0.0\", \"foobar2\": \"*\", \"foobar3\": \"3.0.0\"}, " +
            "\"optionalDependencies\": {\"foobar3\": \"*\", \"foobar4\": \"4.0.0\"}}|" +
            "{\"devDependencies\": {}, \"dependencies\": {\"foobar1\": \"1.0.0\", \"foobar2\": \"2.0.0\"}, " +
            "\"optionalDependencies\": {\"foobar3\": \"3.0.0\", \"foobar4\": \"4.0.0\"}, \"name\": \"\", " +
            "\"version\": \"\"}|",
            "engines array|true|{\"engines\": [\"node >=0.12\"]}|" +
            "{\"name\": \"\", \"version\": \"\", \"engines\": {\"node\": \">=0.12\"}}|",
            "name|true|{\"name\": \"foobar\"}|{\"name\": \"foobar\", \"version\": \"\"}|",
            "version|true|{\"version\": \"1.2.3\"}|{\"name\": \"\", \"version\": \"1.2.3\"}|",
    })
    void normalizes(String name, boolean isRoot, String actual, String expected, String collision) {
        List<String> warnings = new ArrayList<>();
        Manifest info = new Manifest(object(actual));
        NormalizeManifest.normalizeManifest(info, "", "", isRoot, true, warnings);
        assertThat(info.data).isEqualTo(object(expected));
        if (collision == null) {
            assertThat(warnings).isEmpty();
        } else {
            assertThat(warnings).containsExactly(collision);
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            "throw name blacklist|{\"name\": \"node_modules\"}|node_modules: Name is blacklisted",
            "throw name illegal character|{\"name\": \"foo%bar\"}|foo%bar: Name contains illegal characters",
            "throw name non-string|{\"name\": {}}|\"name\" is not a string",
            "throw name start dot|{\"name\": \".foobar\"}|.foobar: Name can't start with a dot",
            "throw name url encode required|{\"name\": \"foo$bar\"}|foo$bar: Name contains illegal characters",
    })
    void rejects(String name, String actual, String error) {
        Manifest info = new Manifest(object(actual));
        assertThatThrownBy(() -> NormalizeManifest.normalizeManifest(info, "", "", true, true, new ArrayList<>()))
                .isInstanceOf(MessageError.class)
                .hasMessage(error);
    }

    /**
     * As yarn 1.22.22's own normalize-manifest from its bundle answered: only a non-empty name prefixes what yarn
     * reports.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            "empty name|{\"name\": \"\", \"version\": 1}|\"version\" is not a string",
            "no name|{\"version\": 1}|\"version\" is not a string",
            "name|{\"name\": \"x\", \"version\": 1}|x: \"version\" is not a string",
    })
    void prefixesTheNameWhenThereIsOne(String name, String actual, String error) {
        Manifest info = new Manifest(object(actual));
        assertThatThrownBy(() -> NormalizeManifest.normalizeManifest(info, "", "", true, true, new ArrayList<>()))
                .isInstanceOf(MessageError.class)
                .hasMessage(error);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            "empty name||\"dependencies\" has dependency \"a\" with range \"1\" that collides with a dependency in " +
            "\"devDependencies\" of the same name with version \"2\"",
            "name|x|x@1.0.0: \"dependencies\" has dependency \"a\" with range \"1\" that collides with a dependency " +
            "in \"devDependencies\" of the same name with version \"2\"",
    })
    void prefixesTheCollisionWarningWhenThereIsAName(String label, @Nullable String name, String warning) {
        Manifest info = new Manifest(object("{\"name\": \"" + (name == null ? "" : name) + "\", " +
                                            "\"version\": \"1.0.0\", \"dependencies\": {\"a\": \"1\"}, " +
                                            "\"devDependencies\": {\"a\": \"2\"}}"));
        List<String> warnings = new ArrayList<>();
        NormalizeManifest.normalizeManifest(info, "", "", true, true, warnings);
        assertThat(warnings).containsExactly(warning);
    }

    /**
     * What yarn 1.22.22 printed for these dependency values ({@code open-manifest-dep-number}, and the same probe
     * with the other values): {@code path.isAbsolute} rejects each, before anything resolves.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            "number|1|type number (1)",
            "fraction|1.5|type number (1.5)",
            "boolean|true|type boolean (true)",
            "object|{}|[Object: null prototype] {}",
            "array|[\"x\"]|an instance of Array",
    })
    void rejectsADependencyValueThatIsNotAString(String label, String value, String received) {
        Manifest info = new Manifest(object("{\"name\": \"r\", \"version\": \"1.0.0\", \"dependencies\": {\"ms\": " +
                                            value + "}}"));
        assertThatThrownBy(() -> NormalizeManifest.normalizeManifest(info, "", "", true, true, new ArrayList<>()))
                .isInstanceOfSatisfying(JsError.class, e -> assertThat(e.getName() + ": " + e.getMessage())
                        .isEqualTo("TypeError [ERR_INVALID_ARG_TYPE]: The \"path\" argument must be of type " +
                                   "string. Received " + received));
    }

    @Test
    void readsADependencyArrayByItsIndices() {
        Manifest info = new Manifest(object("{\"name\": \"r\", \"version\": \"1.0.0\", \"dependencies\": " +
                                            "[\"ms\", null]}"));
        NormalizeManifest.normalizeManifest(info, "", "", true, true, new ArrayList<>());
        Object dependencies = info.get("dependencies");
        assertThat(Js.keys(dependencies)).containsExactly("0", "1");
        assertThat(Js.get(dependencies, "0")).isEqualTo("ms");
        assertThat(Js.get(dependencies, "1")).isEqualTo("");
    }

    /**
     * As yarn 1.22.22's own resolve-relative.js from its bundle answered, the project at {@code /a/b/c/proj}: a
     * target outside the project keeps its climb.
     */
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            "``|file:../x|file:../x",
            "``|file:../../x|file:../../x",
            "``|file:./x/|file:./x",
            "``|file:.|file:.",
            "packages/a|file:../b|file:packages/b",
            "packages/a|link:./../b|link:./packages/b",
            "packages/a|link:../../..|link:..",
    })
    void resolvesARelativeTarget(String moduleLoc, String value, String expected) {
        Manifest info = new Manifest(object("{\"dependencies\": {\"x\": \"" + value + "\"}}"));
        NormalizeManifest.resolveRelative(info, moduleLoc, "");
        assertThat(Js.get(info.get("dependencies"), "x")).isEqualTo(expected);
    }

    @Test
    void cannotKnowWhereTheProjectSitsForAnAbsoluteTarget() {
        Manifest info = new Manifest(object("{\"dependencies\": {\"x\": \"/opt/lib\"}}"));
        NormalizeManifest.resolveRelative(info, "", "");
        Object target = Js.get(info.get("dependencies"), "x");
        assertThat(Js.typeOf(target)).isEqualTo("string");
        assertThatThrownBy(() -> Js.string(target))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("where the project sits on disk")
                .hasMessageContaining("x@file:/opt/lib");
    }

    @Test
    void aCacheRelativeDevDependencyThatCleanDependenciesMovesStaysUnknown() {
        Manifest info = new Manifest(object("{\"name\": \"t\", \"version\": \"1.0.0\", \"dependencies\": {\"x\": " +
                                            "\"*\"}, \"devDependencies\": {\"x\": \"file:../x\"}, " +
                                            "\"peerDependencies\": {\"y\": \"link:../y\"}}"));
        NormalizeManifest.normalizeManifest(info, null, "", false, false, new ArrayList<>());
        assertThat(info.get("devDependencies")).isEqualTo(new LinkedHashMap<>());
        assertThatThrownBy(() -> Js.string(Js.get(info.get("dependencies"), "x")))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("where yarn's cache sits, which the devDependencies entry x@file:../x of t");
        assertThat(Js.get(info.get("peerDependencies"), "y")).isInstanceOf(UnknownPath.class);
    }

    @Test
    void cannotRewriteADependencyStringByIndex() {
        Manifest info = new Manifest(object("{\"dependencies\": \"/x\"}"));
        assertThatThrownBy(() -> NormalizeManifest.resolveRelative(info, "", ""))
                .isInstanceOfSatisfying(JsError.class, e -> assertThat(e.getName() + ": " + e.getMessage())
                        .isEqualTo("TypeError: Cannot assign to read only property '0' of string '/x'"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(String json) {
        return (Map<String, Object>) Js.jsonParse(json);
    }
}
