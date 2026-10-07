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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * yarn 1.22.22's __tests__/normalize-manifest.js over the fixtures (__tests__/fixtures/normalize-manifest) whose
 * fields the port normalizes: name, version, the dependency maps, engines and bundleDependencies. The rest (people,
 * readme, license, bin, man, scripts, repository, homepage, keywords, and the util.* cases) concern fields that
 * never reach a lock. A fixture without {@code _root: false} is a root manifest; of its warnings the dependency
 * collision is ported, not the license one.
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(String json) {
        return (Map<String, Object>) Js.jsonParse(json);
    }
}
