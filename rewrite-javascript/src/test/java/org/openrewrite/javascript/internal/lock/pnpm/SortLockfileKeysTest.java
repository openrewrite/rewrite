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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * lockfile/fs/test/sortLockfileKeys.test.ts and object/key-sorting/test/index.test.ts of pnpm 11.2.2.
 */
class SortLockfileKeysTest {

    @Test
    void sortsKeysAlphabetically() {
        Map<String, @Nullable Object> sorted = SortLockfileKeys.sortLockfileKeys(obj(
          "{'lockfileVersion':'9.0','importers':{'foo':{'dependencies':{" +
          "'zzz':{'version':'link:../zzz','specifier':'link:../zzz'}," +
          "'bar':{'version':'link:../bar','specifier':'link:../bar'}," +
          "'aaa':{'version':'link:../aaa','specifier':'link:../aaa'}}}," +
          "'bar':{'dependencies':{'baz':{'version':'link:../baz','specifier':'link:../baz'}}}}," +
          "'patchedDependencies':{'zzz':'bar','bar':'bar','aaa':'bar'}}"));
        assertThat(json(sorted)).isEqualTo(json(obj(
          "{'lockfileVersion':'9.0','patchedDependencies':{'aaa':'bar','bar':'bar','zzz':'bar'}," +
          "'importers':{'bar':{'dependencies':{'baz':{'specifier':'link:../baz','version':'link:../baz'}}}," +
          "'foo':{'dependencies':{'aaa':{'specifier':'link:../aaa','version':'link:../aaa'}," +
          "'bar':{'specifier':'link:../bar','version':'link:../bar'}," +
          "'zzz':{'specifier':'link:../zzz','version':'link:../zzz'}}}}}")));
    }

    @Test
    void sortingDoesNotCareAboutLocale() {
        Map<String, @Nullable Object> sorted = SortLockfileKeys.sortLockfileKeys(obj(
          "{'lockfileVersion':'9.0','importers':{'foo':{'dependencies':{" +
          "'bar':{'version':'link:../bar','specifier':'link:../bar'}," +
          "'href':{'version':'link:../href','specifier':'link:../href'}," +
          "'chmod':{'version':'link:../chmod','specifier':'link:../chmod'}}}}}"));
        assertThat(json(sorted)).isEqualTo(json(obj(
          "{'lockfileVersion':'9.0','importers':{'foo':{'dependencies':{" +
          "'bar':{'specifier':'link:../bar','version':'link:../bar'}," +
          "'chmod':{'specifier':'link:../chmod','version':'link:../chmod'}," +
          "'href':{'specifier':'link:../href','version':'link:../href'}}}}}")));
    }

    @Test
    void sortKeysByPriority() {
        Map<String, Integer> priority = new HashMap<>();
        priority.put("foo", 1);
        priority.put("bar", 2);
        priority.put("qar", 3);
        assertThat(KeySorting.sortKeysByPriority(priority, false,
          obj("{'a':'a','qar':'qar','b':'b','foo':'foo','c':'c','bar':'bar'}")).keySet())
          .containsExactly("foo", "bar", "qar", "a", "b", "c");
    }
}
