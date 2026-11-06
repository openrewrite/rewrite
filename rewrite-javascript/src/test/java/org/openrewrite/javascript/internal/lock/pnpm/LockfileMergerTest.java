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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.lockfile;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.objectForm;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.sortedJson;

/**
 * lockfile/merger/test/index.ts of pnpm 11.2.2.
 */
class LockfileMergerTest {

    private static final String INTEGRITY = "sha512-aBVzCAzfyApU0gg36QgCpJixGtYwuQ4djrn11J+DTB5vE4OmBPuZiul" +
                                            "gTCA9ByULgVAyNV2CTpjjvZmxzukSLw==";

    private static String simple(String dependencies, String packages) {
        return "{'importers':{'.':{'dependencies':" + dependencies + ",'specifiers':{'foo':'1.0.0'}}}," +
               "'lockfileVersion':'5.2','packages':" + packages + "}";
    }

    private static final String SIMPLE_PACKAGES = "{'/foo@1.0.0':{'resolution':{'integrity':'" + INTEGRITY + "'}}}";

    @Test
    void picksTheNewerVersionWhenDependenciesDifferInsideImporter() {
        LockfileObject merged = LockfileMerger.mergeLockfileChanges(
          lockfile(simple("{'foo':'1.2.0','bar':'3.0.0(qar@1.0.0)','zoo':'4.0.0(qar@1.0.0)'}", SIMPLE_PACKAGES)),
          lockfile(simple("{'foo':'1.1.0','bar':'4.0.0(qar@1.0.0)','zoo':'3.0.0(qar@1.0.0)'}", SIMPLE_PACKAGES)));
        assertThat(merged.getImporters().get(".").getDependencies())
          .containsEntry("foo", "1.2.0")
          .containsEntry("bar", "4.0.0(qar@1.0.0)")
          .containsEntry("zoo", "4.0.0(qar@1.0.0)");
    }

    @Test
    void picksTheNewerVersionWhenDependenciesDifferInsidePackage() {
        String ours = "{'importers':{'.':{'dependencies':{'a':'1.0.0'},'specifiers':{}}},'lockfileVersion':'5.2'," +
                      "'packages':{'/a@1.0.0':{'dependencies':{'linked':'link:../1','foo':'1.2.0'," +
                      "'bar':'3.0.0(qar@1.0.0)','zoo':'4.0.0(qar@1.0.0)','qar':'1.0.0'}," +
                      "'resolution':{'integrity':''}}," +
                      "'/foo@1.0.0':{'resolution':{'integrity':''}}," +
                      "'/bar@3.0.0(qar@1.0.0)':{'dependencies':{'qar':'1.0.0'},'resolution':{'integrity':''}}," +
                      "'/zoo@4.0.0(qar@1.0.0)':{'dependencies':{'qar':'1.0.0'},'resolution':{'integrity':''}}," +
                      "'/foo@1.2.0':{'resolution':{'integrity':''}},'/qar@1.0.0':{'resolution':{'integrity':''}}}}";
        String theirs = "{'importers':{'.':{'dependencies':{'a':'1.0.0'},'specifiers':{}}},'lockfileVersion':'5.2'," +
                        "'packages':{'/a@1.0.0':{'dependencies':{'linked':'link:../1','foo':'1.1.0'," +
                        "'bar':'4.0.0(qar@1.0.0)','zoo':'3.0.0(qar@1.0.0)','qar':'1.0.0'}," +
                        "'resolution':{'integrity':''}}," +
                        "'/foo@1.0.0':{'resolution':{'integrity':''}}," +
                        "'/bar@4.0.0(qar@1.0.0)':{'dependencies':{'qar':'1.0.0'},'resolution':{'integrity':''}}," +
                        "'/zoo@3.0.0(qar@1.0.0)':{'dependencies':{'qar':'1.0.0'},'resolution':{'integrity':''}}," +
                        "'/foo@1.1.0':{'resolution':{'integrity':''}},'/qar@1.0.0':{'resolution':{'integrity':''}}}}";
        LockfileObject merged = LockfileMerger.mergeLockfileChanges(lockfile(ours), lockfile(theirs));
        assertThat(merged.getPackages().get("/a@1.0.0").getDependencies())
          .containsEntry("linked", "link:../1")
          .containsEntry("foo", "1.2.0")
          .containsEntry("bar", "4.0.0(qar@1.0.0)")
          .containsEntry("zoo", "4.0.0(qar@1.0.0)");
        List<String> keys = new ArrayList<>(merged.getPackages().keySet());
        Collections.sort(keys);
        assertThat(keys).containsExactly("/a@1.0.0", "/bar@3.0.0(qar@1.0.0)", "/bar@4.0.0(qar@1.0.0)", "/foo@1.0.0",
          "/foo@1.1.0", "/foo@1.2.0", "/qar@1.0.0", "/zoo@3.0.0(qar@1.0.0)", "/zoo@4.0.0(qar@1.0.0)");
    }

    @Test
    void prefersOurLockfileResolutionsWhenItHasNewerPackages() {
        String ours = "{'/foo@1.0.0':{'dependencies':{'bar':'1.0.0'}," +
                      "'resolution':{'integrity':'" + INTEGRITY + "'}}," +
                      "'/bar@1.0.0':{'resolution':{'integrity':'" + INTEGRITY + "'}}}";
        String theirs = "{'/foo@1.0.0':{'dependencies':{'bar':'1.1.0'}," +
                        "'resolution':{'integrity':'" + INTEGRITY + "'}}," +
                        "'/bar@1.1.0':{'dependencies':{'qar':'1.0.0'}," +
                        "'resolution':{'integrity':'" + INTEGRITY + "'}}," +
                        "'/qar@1.0.0':{'resolution':{'integrity':'" + INTEGRITY + "'}}}";
        LockfileObject merged = LockfileMerger.mergeLockfileChanges(lockfile(simple("{'foo':'1.0.0'}", ours)),
          lockfile(simple("{'foo':'1.0.0'}", theirs)));
        assertThat(objectForm(merged)).isEqualTo(sortedJson(simple("{'foo':'1.0.0'}",
          "{'/foo@1.0.0':{'dependencies':{'bar':'1.1.0'},'resolution':{'integrity':'" + INTEGRITY + "'}}," +
          "'/bar@1.0.0':{'resolution':{'integrity':'" + INTEGRITY + "'}}," +
          "'/bar@1.1.0':{'dependencies':{'qar':'1.0.0'},'resolution':{'integrity':'" + INTEGRITY + "'}}," +
          "'/qar@1.0.0':{'resolution':{'integrity':'" + INTEGRITY + "'}}}")));
    }

    @Test
    void doesNotCrashWhenMergingNonSemverVersions() {
        String base = "{'importers':{'.':{'dependencies':{'a':'1.0.0'},'specifiers':{}}},'lockfileVersion':'5.2'," +
                      "'packages':{'/a@1.0.0':{'dependencies':{'linked':'link:../pkg1'}," +
                      "'resolution':{'integrity':''}}}}";
        LockfileObject merged = LockfileMerger.mergeLockfileChanges(lockfile(base),
          lockfile(base.replace("link:../pkg1", "link:../pkg2")));
        assertThat(merged.getPackages().get("/a@1.0.0").getDependencies()).containsEntry("linked", "link:../pkg2");
    }
}
