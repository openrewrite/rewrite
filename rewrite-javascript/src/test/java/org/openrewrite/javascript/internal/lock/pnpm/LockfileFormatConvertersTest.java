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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.lockfile;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.objectForm;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.sortedJson;

/**
 * lockfile/fs/test/normalizeLockfile.test.ts and lockfileV6Converters.test.ts of pnpm 11.2.2.
 */
class LockfileFormatConvertersTest {

    private static String file(LockfileObject lockfile) {
        return json(KeySorting.sortDeepKeys(LockfileFormatConverters.convertToLockfileFile(lockfile)));
    }

    @Test
    void emptyOverridesAreRemovedDuringLockfileNormalization() {
        assertThat(file(lockfile("{'lockfileVersion':'9.0','overrides':{},'patchedDependencies':{},'packages':{}," +
                                 "'importers':{'foo':{'dependencies':{'bar':'link:../bar'}," +
                                 "'specifiers':{'bar':'link:../bar'}}}}")))
          .isEqualTo(sortedJson("{'lockfileVersion':'9.0','importers':{'foo':{'dependencies':{'bar':" +
                                "{'version':'link:../bar','specifier':'link:../bar'}}}}}"));
    }

    @Test
    void redundantFieldsAreRemovedFromTime() {
        assertThat(file(lockfile("{'lockfileVersion':'9.0','packages':{},'importers':{'foo':{" +
                                 "'dependencies':{'bar':'1.0.0'},'devDependencies':{'foo':'1.0.0(react@18.0.0)'}," +
                                 "'optionalDependencies':{'qar':'1.0.0'}," +
                                 "'specifiers':{'bar':'1.0.0','foo':'1.0.0','qar':'1.0.0'}}}," +
                                 "'time':{'bar@1.0.0':'2021-02-11T22:54:29.120Z'," +
                                 "'foo@1.0.0':'2021-02-11T22:54:29.120Z','qar@1.0.0':'2021-02-11T22:54:29.120Z'," +
                                 "'zoo@1.0.0':'2021-02-11T22:54:29.120Z'}}")))
          .isEqualTo(sortedJson("{'lockfileVersion':'9.0','importers':{'foo':{" +
                                "'dependencies':{'bar':{'version':'1.0.0','specifier':'1.0.0'}}," +
                                "'devDependencies':{'foo':{'version':'1.0.0(react@18.0.0)','specifier':'1.0.0'}}," +
                                "'optionalDependencies':{'qar':{'version':'1.0.0','specifier':'1.0.0'}}}}," +
                                "'time':{'bar@1.0.0':'2021-02-11T22:54:29.120Z'," +
                                "'foo@1.0.0':'2021-02-11T22:54:29.120Z','qar@1.0.0':'2021-02-11T22:54:29.120Z'}}"));
    }

    private static final String LOCKFILE_OBJECT = "{'lockfileVersion':'9.0','importers':{'project1':{" +
      "'specifiers':{'foo':'^1.0.0','bar':'^1.0.0','qar':'^1.0.0','tarball':'^1.0.0'}," +
      "'dependencies':{'foo':'1.0.0','tarball':'@registry.npmjs.org/is-positive/-/is-positive-1.0.0.tgz'}," +
      "'devDependencies':{'bar':'/@bar/bar@1.0.0(@babel/core@2.0.0)'}," +
      "'optionalDependencies':{'qar':'reg.com/qar@1.0.0'}}}," +
      "'packages':{'/foo@1.0.0':{'resolution':{'integrity':''}}," +
      "'/@bar/bar@1.0.0(@babel/core@2.0.0)':{'resolution':{'integrity':''}}," +
      "'reg.com/qar@1.0.0':{'resolution':{'integrity':''}}," +
      "'@registry.npmjs.org/is-positive/-/is-positive-1.0.0.tgz':{'resolution':{'integrity':''}}}}";

    private static final String LOCKFILE_FILE = "{'lockfileVersion':'9.0','importers':{'project1':{" +
      "'dependencies':{'foo':{'specifier':'^1.0.0','version':'1.0.0'}," +
      "'tarball':{'specifier':'^1.0.0','version':'@registry.npmjs.org/is-positive/-/is-positive-1.0.0.tgz'}}," +
      "'devDependencies':{'bar':{'specifier':'^1.0.0','version':'/@bar/bar@1.0.0(@babel/core@2.0.0)'}}," +
      "'optionalDependencies':{'qar':{'specifier':'^1.0.0','version':'reg.com/qar@1.0.0'}}}}," +
      "'packages':{'/foo@1.0.0':{'resolution':{'integrity':''}},'/@bar/bar@1.0.0':{'resolution':{'integrity':''}}," +
      "'reg.com/qar@1.0.0':{'resolution':{'integrity':''}}," +
      "'@registry.npmjs.org/is-positive/-/is-positive-1.0.0.tgz':{'resolution':{'integrity':''}}}," +
      "'snapshots':{'/foo@1.0.0':{},'/@bar/bar@1.0.0(@babel/core@2.0.0)':{},'reg.com/qar@1.0.0':{}," +
      "'@registry.npmjs.org/is-positive/-/is-positive-1.0.0.tgz':{}}}";

    @Test
    void convertToLockfileFile() {
        assertThat(file(lockfile(LOCKFILE_OBJECT))).isEqualTo(sortedJson(LOCKFILE_FILE));
        assertThat(objectForm(LockfileFormatConverters.convertToLockfileObject(obj(LOCKFILE_FILE))))
          .isEqualTo(objectForm(lockfile(LOCKFILE_OBJECT)));
    }

    @Test
    void convertToLockfileObjectReconstructsADroppedDirectoryResolutionButNeverForATarball() {
        LockfileObject lockfile = LockfileFormatConverters.convertToLockfileObject(obj(
          "{'lockfileVersion':'9.0','importers':{},'snapshots':{'dir@file:packages/dir(peer@1.0.0)':{}," +
          "'tar@file:vendor/tar-1.0.0.tgz(peer@1.0.0)':{},'upper@file:vendor/upper-1.0.0.TGZ(peer@1.0.0)':{}," +
          "'mixed@file:vendor/mixed-1.0.0.Tar.Gz(peer@1.0.0)':{}}}"));
        assertThat(json(lockfile.getPackages().get("dir@file:packages/dir(peer@1.0.0)").getResolution()))
          .isEqualTo("{'directory':'packages/dir','type':'directory'}");
        assertThat(lockfile.getPackages().get("tar@file:vendor/tar-1.0.0.tgz(peer@1.0.0)").getResolution()).isNull();
        assertThat(lockfile.getPackages().get("upper@file:vendor/upper-1.0.0.TGZ(peer@1.0.0)").getResolution())
          .isNull();
        assertThat(lockfile.getPackages().get("mixed@file:vendor/mixed-1.0.0.Tar.Gz(peer@1.0.0)").getResolution())
          .isNull();
    }
}
