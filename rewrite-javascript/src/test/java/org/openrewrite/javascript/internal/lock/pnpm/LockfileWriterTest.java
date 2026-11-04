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
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.lockfile;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.objectForm;

/**
 * lockfile/fs/test/write.test.ts of pnpm 11.2.2 (and its snapshot), each written lock read back as
 * {@code readWantedLockfile} reads it.
 */
class LockfileWriterTest {

    private static final String ENV_DOC = "---\nlockfileVersion: env-1.0\nimporters:\n  .:\n    configDependencies:\n" +
                                          "      typescript: 5.0.0\n\n---\n";

    private static LockfileObject readBack(String text) {
        LockfileObject read = LockfileReader.read(text).getLockfile();
        assertThat(read).isNotNull();
        return read;
    }

    @Test
    void writeLockfiles() {
        String wanted = "{'importers':{'.':{'dependencies':{'is-negative':'1.0.0','is-positive':'1.0.0'}," +
                        "'specifiers':{'is-negative':'^1.0.0','is-positive':'^1.0.0'}}},'lockfileVersion':'9.0'," +
                        "'packages':{'/is-negative@1.0.0':{'os':['darwin'],'dependencies':{'is-positive':'2.0.0'}," +
                        "'cpu':['x86'],'libc':['glibc'],'engines':{'node':'>=10','npm':'\\nfoo\\n'}," +
                        "'resolution':{'integrity':'sha1-ChbBDewTLAqLCzb793Fo5VDvg/g='}}," +
                        "'/is-positive@1.0.0':{'resolution':{'integrity':'sha1-ChbBDewTLAqLCzb793Fo5VDvg/g='}}," +
                        "'/is-positive@2.0.0':{'resolution':{'integrity':'sha1-ChbBDewTLAqLCzb793Fo5VDvg/g='}}}}";
        String written = LockfileWriter.writeWantedLockfile(lockfile(wanted), null);
        assertThat(written).isEqualTo("lockfileVersion: '9.0'\n" +
                                      "\n" +
                                      "importers:\n" +
                                      "\n" +
                                      "  .:\n" +
                                      "    dependencies:\n" +
                                      "      is-negative:\n" +
                                      "        specifier: ^1.0.0\n" +
                                      "        version: 1.0.0\n" +
                                      "      is-positive:\n" +
                                      "        specifier: ^1.0.0\n" +
                                      "        version: 1.0.0\n" +
                                      "\n" +
                                      "packages:\n" +
                                      "\n" +
                                      "  /is-negative@1.0.0:\n" +
                                      "    resolution: {integrity: sha1-ChbBDewTLAqLCzb793Fo5VDvg/g=}\n" +
                                      "    engines: {node: '>=10', npm: \"\\nfoo\\n\"}\n" +
                                      "    cpu: [x86]\n" +
                                      "    os: [darwin]\n" +
                                      "    libc: [glibc]\n" +
                                      "\n" +
                                      "  /is-positive@1.0.0:\n" +
                                      "    resolution: {integrity: sha1-ChbBDewTLAqLCzb793Fo5VDvg/g=}\n" +
                                      "\n" +
                                      "  /is-positive@2.0.0:\n" +
                                      "    resolution: {integrity: sha1-ChbBDewTLAqLCzb793Fo5VDvg/g=}\n" +
                                      "\n" +
                                      "snapshots:\n" +
                                      "\n" +
                                      "  /is-negative@1.0.0:\n" +
                                      "    dependencies:\n" +
                                      "      is-positive: 2.0.0\n" +
                                      "\n" +
                                      "  /is-positive@1.0.0: {}\n" +
                                      "\n" +
                                      "  /is-positive@2.0.0: {}\n");
        assertThat(objectForm(readBack(written))).isEqualTo(objectForm(lockfile(wanted)));
    }

    @Test
    void writeLockfilesWhenNoSpecifiersButDependenciesPresent() {
        String wanted = "{'importers':{'.':{'dependencies':{'is-positive':'link:../is-positive'},'specifiers':{}}}," +
                        "'lockfileVersion':'9.0','packages':{}}";
        String written = LockfileWriter.writeWantedLockfile(lockfile(wanted), null);
        LockfileObject read = readBack(written);
        assertThat(read.getImporters().get(".").getDependencies()).containsEntry("is-positive", "link:../is-positive");
        assertThat(read.getImporters().get(".").getSpecifiers()).containsKey("is-positive");
        assertThat(read.getImporters().get(".").getSpecifiers().get("is-positive")).isNull();
    }

    @Test
    void writeDoesNotUseYamlAnchorsOrAliases() {
        String integrity = "sha512-y9YmnusURc+3KPgvhYKvZ9oCucj51MSZWODyaeV0KFU0cquzA7dCD1g/" +
                           "OIYUKtNoZ+MXtacDngkdud2TklMSjw==";
        String packages = "/react-dnd@2.5.4(react@15.6.1):\n" +
                          "  dependencies:\n" +
                          "    react: 15.6.1\n" +
                          "  dev: false\n" +
                          "  id: registry.npmjs.org/react-dnd/2.5.4\n" +
                          "  peerDependencies: &ref_11\n" +
                          "    react: '1'\n" +
                          "  resolution:\n" +
                          "    integrity: " + integrity + "\n" +
                          "/react-dnd@2.5.4(react@15.6.2):\n" +
                          "  dependencies:\n" +
                          "    react: 15.6.2\n" +
                          "  dev: false\n" +
                          "  id: registry.npmjs.org/react-dnd/2.5.4\n" +
                          "  peerDependencies: *ref_11\n" +
                          "  resolution:\n" +
                          "    integrity: " + integrity + "\n";
        String text = "lockfileVersion: '9.0'\nimporters:\n  .: {}\nsnapshots:\n" + indent(packages);
        String written = LockfileWriter.writeWantedLockfile(readBack(text), null);
        assertThat(written).doesNotContain("&").doesNotContain("*");
    }

    @Test
    void writeLockfilesDoesNotFailIfTheLockfileHasUndefinedProperties() {
        LockfileObject wanted = lockfile("{'importers':{'.':{'dependencies':{'is-negative':'1.0.0'}," +
                                         "'specifiers':{'is-negative':'^1.0.0'}}},'lockfileVersion':'9.0'," +
                                         "'packages':{'/is-negative@1.0.0':{'resolution':" +
                                         "{'integrity':'sha1-ChbBDewTLAqLCzb793Fo5VDvg/g='}}}}");
        wanted.getPackages().get("/is-negative@1.0.0").setDependencies(null);
        assertThat(LockfileWriter.writeWantedLockfile(wanted, null)).contains("/is-negative@1.0.0: {}");
    }

    @Test
    void writeWantedLockfilePreservesEnvDocumentPrefix() {
        LockfileObject wanted = lockfile("{'importers':{'.':{'dependencies':{'is-positive':'1.0.0'}," +
                                         "'specifiers':{'is-positive':'^1.0.0'}}},'lockfileVersion':'9.0'," +
                                         "'packages':{'is-positive@1.0.0':{'resolution':" +
                                         "{'integrity':'sha1-ChbBDewTLAqLCzb793Fo5VDvg/g='}}}}");
        String written = LockfileWriter.writeWantedLockfile(wanted, ENV_DOC + "lockfileVersion: \"9.0\"\n");
        assertThat(written).startsWith("---\n").contains("configDependencies").contains("typescript: 5.0.0");
        assertThat(readBack(written).getImporters().get(".").getDependencies()).containsEntry("is-positive", "1.0.0");
    }

    private static String indent(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n")) {
            out.append("  ").append(line).append('\n');
        }
        return out.toString();
    }
}
