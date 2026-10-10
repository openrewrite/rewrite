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

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;

/**
 * {@link Manifests#parseMembers} reads what {@code JSON.parse} and the property reads after it give, and pnpm
 * 11.2.2's workspace/project-manifest-reader/test/index.ts cases of the conversion a read applies.
 */
class ManifestsTest {

    @Test
    void readProjectManifestConvertsDevEnginesRuntimeToDevDependencies() {
        assertThat(json(Manifests.readProjectManifest("{\"devEngines\":{\"runtime\":{\"name\":\"node\"," +
                                                      "\"version\":\"24\",\"onFail\":\"download\"}}}", "package.json")))
          .isEqualTo("{'devEngines':{'runtime':{'name':'node','version':'24','onFail':'download'}}," +
                     "'devDependencies':{'node':'runtime:24'}}");
    }

    @Test
    void readProjectManifestConvertsEnginesRuntimeToDependencies() {
        assertThat(json(Manifests.readProjectManifest("{\"engines\":{\"runtime\":{\"name\":\"node\"," +
                                                      "\"version\":\"24\",\"onFail\":\"download\"}}}", "package.json")))
          .isEqualTo("{'engines':{'runtime':{'name':'node','version':'24','onFail':'download'}}," +
                     "'dependencies':{'node':'runtime:24'}}");
    }

    /**
     * A package.yaml as read-yaml-file 3.0.0 reads it (@zkochan/js-yaml, the byte order mark stripped), converted as
     * a package.json is; text that is not YAML is pnpm's ERR_PNPM_YAML_PARSE.
     */
    @Test
    void readPackageYaml() {
        String yaml = '\uFEFF' + "name: c\nversion: 1.0.0\nengines:\n  runtime:\n    name: node\n    version: '24'\n" +
                      "    onFail: download\n";
        assertThat(json(Manifests.readPackageYaml(yaml, "packages/c/package.yaml")))
          .isEqualTo("{'name':'c','version':'1.0.0','engines':{'runtime':{'name':'node','version':'24'," +
                     "'onFail':'download'}},'dependencies':{'node':'runtime:24'}}");
        assertThatThrownBy(() -> Manifests.readPackageYaml("name: [c\n", "packages/c/package.yaml"))
          .isInstanceOfSatisfying(PnpmError.class, e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_YAML_PARSE"))
          .hasMessageEndingWith("\nin packages/c/package.yaml");
        assertThatThrownBy(() -> Manifests.readPackageYaml("- c\n", "packages/c/package.yaml"))
          .isInstanceOf(CannotKnow.class);
    }

    private static final Set<String> MODIFIED = Collections.singleton("modified");
    private static final Set<String> VERSIONS = Collections.singleton("versions");

    private static String members(String text) {
        return json(Manifests.parseMembers(text.replace('\'', '"'), "doc", MODIFIED, VERSIONS));
    }

    @Test
    void keepsTheNamedMembersAndTheKeysOfAnObject() {
        assertThat(members("{'name':'a','versions':{'2':{'x':[1,{}]},'1.0.0':{},'1':null},'modified':'m','time':{}}"))
          .isEqualTo("{'versions':{'1':true,'2':true,'1.0.0':true},'modified':'m'}");
    }

    @Test
    void theLastOfDuplicateMembersWinsAtTheFirstOnesPlace() {
        assertThat(members("{'modified':1,'versions':[],'modified':'m','versions':{'1.0.0':{}}}"))
          .isEqualTo("{'modified':'m','versions':{'1.0.0':true}}");
    }

    @Test
    void aMemberThatIsNotAnObjectIsKeptWhole() {
        assertThat(members("{'versions':['1.0.0'],'modified':{'a':1}}"))
          .isEqualTo("{'versions':['1.0.0'],'modified':{'a':1}}");
    }

    @Test
    void aDocumentThatIsNotAnObject() {
        assertThat(members("null")).isEqualTo("null");
        assertThat(members("[{'modified':'m'}]")).isEqualTo("{}");
        assertThat(members("'modified'")).isEqualTo("{}");
    }

    @Test
    void textThatIsNotJsonFails() {
        assertThatThrownBy(() -> members("{'modified':'m'} x")).isInstanceOf(PnpmError.class);
        assertThatThrownBy(() -> members("{'modified':'m',")).isInstanceOf(PnpmError.class);
        assertThatThrownBy(() -> members("{'versions':{'a':}}")).isInstanceOf(PnpmError.class);
    }
}
