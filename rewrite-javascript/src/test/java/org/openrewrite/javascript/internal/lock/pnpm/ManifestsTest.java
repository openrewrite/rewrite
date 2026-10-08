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

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;

/**
 * {@link Manifests#parseMembers} reads what {@code JSON.parse} and the property reads after it give.
 */
class ManifestsTest {

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
