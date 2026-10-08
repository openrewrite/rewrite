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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.lockfile;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.strings;

/**
 * lockfile/verification/test/satisfiesPackageManifest.ts and diffFlatRecords.test.ts of pnpm 11.2.2.
 */
class VerificationTest {

    private static Verification.Satisfies satisfies(boolean autoInstallPeers, boolean excludeLinks,
                                                    @Nullable String importer, String pkg) {
        ProjectSnapshot snapshot = importer == null ? null :
                lockfile("{'importers':{'.':" + importer + "}}").getImporters().get(".");
        return Verification.satisfiesPackageManifest(autoInstallPeers, excludeLinks, snapshot,
                obj("{'name':'project','version':'1.0.0'" + (pkg.isEmpty() ? "" : "," + pkg) + "}"));
    }

    private static void satisfied(boolean autoInstallPeers, boolean excludeLinks, String importer, String pkg) {
        Verification.Satisfies result = satisfies(autoInstallPeers, excludeLinks, importer, pkg);
        assertThat(result.isSatisfies()).as(String.valueOf(result.getDetailedReason())).isTrue();
        assertThat(result.getDetailedReason()).isNull();
    }

    private static void satisfied(String importer, String pkg) {
        satisfied(false, false, importer, pkg);
    }

    private static void unsatisfied(@Nullable String importer, String pkg, String reason) {
        Verification.Satisfies result = satisfies(false, false, importer, pkg);
        assertThat(result.isSatisfies()).isFalse();
        assertThat(result.getDetailedReason()).isEqualTo(reason);
    }

    @Test
    void satisfiesPackageManifest() {
        satisfied("{'dependencies':{'foo':'1.0.0'},'specifiers':{'foo':'^1.0.0'}}",
          "'dependencies':{'foo':'^1.0.0'}");
        satisfied("{'dependencies':{'foo':'1.0.0'},'devDependencies':{},'specifiers':{'foo':'^1.0.0'}}",
          "'dependencies':{'foo':'^1.0.0'}");
        satisfied("{'devDependencies':{'foo':'1.0.0'},'specifiers':{'foo':'^1.0.0'}}",
          "'devDependencies':{'foo':'^1.0.0'}");
        satisfied("{'optionalDependencies':{'foo':'1.0.0'},'specifiers':{'foo':'^1.0.0'}}",
          "'optionalDependencies':{'foo':'^1.0.0'}");
        unsatisfied("{'dependencies':{'foo':'1.0.0'},'specifiers':{'foo':'^1.0.0'}}",
          "'optionalDependencies':{'foo':'^1.0.0'}",
          "\"optionalDependencies\" in the lockfile ({}) doesn't match the same field in package.json " +
          "({\"foo\":\"^1.0.0\"})");
        unsatisfied("{'dependencies':{'foo':'1.0.0'},'specifiers':{'foo':'^1.0.0'}}",
          "'dependencies':{'foo':'^1.1.0'}",
          "specifiers in the lockfile don't match specifiers in package.json:\n" +
          "* 1 dependencies are mismatched:\n  - foo (lockfile: ^1.0.0, manifest: ^1.1.0)\n");
        unsatisfied("{'dependencies':{'foo':'1.0.0'},'specifiers':{'foo':'^1.0.0'}}",
          "'dependencies':{'foo':'^1.0.0','bar':'2.0.0'}",
          "specifiers in the lockfile don't match specifiers in package.json:\n" +
          "* 1 dependencies were added: bar@2.0.0\n");
        unsatisfied("{'dependencies':{'foo':'1.0.0'},'specifiers':{'foo':'^1.0.0','bar':'2.0.0'}}",
          "'dependencies':{'foo':'^1.0.0','bar':'2.0.0'}",
          "\"dependencies\" in the lockfile ({\"foo\":\"1.0.0\"}) doesn't match the same field in package.json " +
          "({\"foo\":\"^1.0.0\",\"bar\":\"2.0.0\"})");
        satisfied("{'dependencies':{'foo':'1.0.0'},'optionalDependencies':{'bar':'2.0.0'}," +
                  "'specifiers':{'bar':'2.0.0','foo':'^1.0.0'}}",
          "'dependencies':{'bar':'2.0.0','foo':'^1.0.0'},'optionalDependencies':{'bar':'2.0.0'}");
        unsatisfied("{'dependencies':{'bar':'2.0.0','qar':'1.0.0'},'specifiers':{'bar':'2.0.0','qar':'^1.0.0'}}",
          "'dependencies':{'bar':'2.0.0'}",
          "specifiers in the lockfile don't match specifiers in package.json:\n" +
          "* 1 dependencies were removed: qar@^1.0.0\n");
        unsatisfied("{'dependencies':{'bar':'2.0.0','qar':'1.0.0'},'specifiers':{'bar':'2.0.0'}}",
          "'dependencies':{'bar':'2.0.0'}",
          "\"dependencies\" in the lockfile ({\"bar\":\"2.0.0\",\"qar\":\"1.0.0\"}) doesn't match the same field in " +
          "package.json ({\"bar\":\"2.0.0\"})");
        satisfied("{'dependencies':{'foo':'1.0.0','linked':'link:../linked'},'specifiers':{'foo':'^1.0.0'}}",
          "'dependencies':{'foo':'^1.0.0'}");
        unsatisfied(null, "'dependencies':{'foo':'^1.0.0'}", "no importer");
        satisfied("{'dependencies':{'foo':'1.0.0'},'specifiers':{'foo':'1.0.0'}}",
          "'dependencies':{'foo':'1.0.0'},'devDependencies':{'foo':'1.0.0'}");
        satisfied("{'dependencies':{'foo':'1.0.0'},'specifiers':{'foo':'1.0.0'}}",
          "'dependencies':{'foo':'1.0.0'},'devDependencies':{'foo':'1.0.0'},'dependenciesMeta':{}");
        satisfied(true, false, "{'dependencies':{'foo':'1.0.0','bar':'1.0.0'}," +
                               "'specifiers':{'foo':'1.0.0','bar':'^1.0.0'}}",
          "'dependencies':{'foo':'1.0.0'},'peerDependencies':{'bar':'^1.0.0'}");
        satisfied(true, false, "{'dependencies':{'qar':'1.0.0'},'optionalDependencies':{'bar':'1.0.0'}," +
                               "'devDependencies':{'foo':'1.0.0'}," +
                               "'specifiers':{'foo':'1.0.0','bar':'1.0.0','qar':'1.0.0'}}",
          "'dependencies':{'qar':'1.0.0'},'optionalDependencies':{'bar':'1.0.0'}," +
          "'devDependencies':{'foo':'1.0.0'},'peerDependencies':{'foo':'^1.0.0','bar':'^1.0.0','qar':'^1.0.0'}");
        satisfied("{'dependencies':{'foo':'1.0.0'},'specifiers':{'foo':'1.0.0'},'publishDirectory':'dist'}",
          "'dependencies':{'foo':'1.0.0'},'publishConfig':{'directory':'dist'}");
        unsatisfied("{'dependencies':{'foo':'1.0.0'},'specifiers':{'foo':'1.0.0'},'publishDirectory':'dist'}",
          "'dependencies':{'foo':'1.0.0'},'publishConfig':{'directory':'lib'}",
          "\"publishDirectory\" in the lockfile (dist) doesn't match \"publishConfig.directory\" in package.json " +
          "(lib)");
        satisfied(false, true, "{'dependencies':{'foo':'1.0.0'},'specifiers':{'foo':'1.0.0'}}",
          "'dependencies':{'foo':'1.0.0','bar':'link:../bar'}");
        Verification.Satisfies broken = Verification.satisfiesPackageManifest(false, false,
          lockfile("{'importers':{'.':{'dependencies':{'@apollo/client':'3.13.8(@types/react@18.3.23)" +
                   "(graphql@15.8.0)(react-dom@17.0.2(react@17.0.2))(react@17.0.2)" +
                   "(subscriptions-transport-ws@0.11.0(graphql@15.8.0))'}," +
                   "'specifiers':{'@apollo/client':'3.3.7'}}}}").getImporters().get("."),
          obj("{'dependencies':{'@apollo/client':'3.3.7'}}"));
        assertThat(broken.isSatisfies()).isFalse();
        assertThat(broken.getDetailedReason())
          .isEqualTo("The importer resolution is broken at dependency \"@apollo/client\": version \"3.13.8\" " +
                     "doesn't satisfy range \"3.3.7\"");
    }

    @Test
    void diffFlatRecords() {
        Verification.Diff diff = Verification.diffFlatRecords(strings("{'is-positive':'1.0.0','is-negative':'2.0.0'}"),
          strings("{'is-negative':'2.1.0','is-odd':'1.0.0'}"));
        assertThat(diff.getAdded()).containsExactly(new String[]{"is-odd", "1.0.0"});
        assertThat(diff.getRemoved()).containsExactly(new String[]{"is-positive", "1.0.0"});
        assertThat(diff.getModified()).containsExactly(new String[]{"is-negative", "2.0.0", "2.1.0"});
    }
}
