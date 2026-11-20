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
package org.openrewrite.javascript.internal.lock.npm;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.arr;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;

/**
 * What npm 12.2.0 does with a root's {@code packageExtensions}: every digest, record and message here is one it
 * wrote, into a lock or to its log, for a project whose manifest holds the rules.
 */
class PackageExtensionsTest {

    private static final String IS_ODD = "{\"name\":\"is-odd\",\"version\":\"3.0.1\",\"dependencies\":{\"is-number\":" +
                                         "\"^6.0.0\"},\"engines\":{\"node\":\">=4\"}}";
    private static final String ONCE = "{\"name\":\"once\",\"version\":\"1.4.0\",\"dependencies\":{\"wrappy\":\"1\"}}";

    // npm 12.2.0 bundles node-semver 7.8.5
    private static PackageExtensions read(@Nullable Object packageExtensions) {
        return new PackageExtensions(packageExtensions, NpmRelease.of("12.2.0").semver());
    }

    // the packageExtensionsHash of the lock's root entry
    @ParameterizedTest
    @CsvSource(delimiterString = "=>", value = {
      "{} => sha512-J8dGcK23UHX60FjVzq97IMTneGyDuuijL2Jvl4KvNMmjPCBG72D9Knh403jin+yFGAa72aZ4ePOp8c2kgwdj/Q==",
      "{\"is-odd@3\":{\"dependencies\":{\"isarray\":\"2.0.5\"}}} => " +
      "sha512-h32ouO4a7osRa5vCtTHIhZJpFFdMwv+bf2ausl7cJUTBlfnzQ4bGMBVx6YuYFz3eHrroKgJyt0r/p+zjKT+M7w==",
      "{\"is-odd@3\":{\"dependencies\":{\"isarray\":\"1.0.0\"}}} => " +
      "sha512-dmhehxm5nt77al1xCbi9CCOZQeEbsO+6i1MFuMzTpCZPAOkKlY6IVbaM9l3jSMQi5NMyIDwRU/qagQyxbgbBuw==",
      "{\"once\":{\"peerDependencies\":{\"isarray\":\"^2.0.0\"},\"peerDependenciesMeta\":{\"isarray\":{\"optional\":" +
      "true}}}} => sha512-Ud9p+PPGx0Ww/84AuVJNDdI9ocSbTlWY1MWr+dFH2f2yQI62kieUp2xA4aGbt3Zv0I6BUISjmpLD6pZQVSQw9Q==",
      "{\"once\":{\"peerDependencies\":{\"isarray\":\"^2.0.0\"}}} => " +
      "sha512-wKF+G/yhtvjdeydLuVFAnSnAuHVMrZELZZQejCuZTKyNdspV63Kq3z9+oBgNjnicQfL8nL3tpqD62UAraP446A==",
      "{\"is-odd@>=3 <4\":{\"optionalDependencies\":{\"wrappy\":\"1.0.2\"},\"peerDependencies\":{\"isarray\":\"2.x\"" +
      "}}} => sha512-AT8Vlf+x34nV/SR4nmoSsGnLiUpnHyRAKcasYy6z8yE4sSQ4x3KbDrxVSWpLhgwr6+/qBg1y+8Is9mlkpj0ufQ==",
      "{\"b\":{\"dependencies\":{\"x\":\"1\"}},\"a\":{\"peerDependencies\":{\"y\":\"2\"}}} => " +
      "sha512-D4i6rN2E/s0aMR+0eEVM4OaYV7q/YOEFJtqUj8R/NECNL8UpcLf5KcAomflWfC1lCMDKQDe6niv6OKTHh5nz7Q==",
      "{\"@scope/pkg@^1 || >=3\":{\"dependencies\":{\"x\":\"1\"}}} => " +
      "sha512-4NT4QwAvT9ezQY8GWnLRzNdcAnOrbXthh0E0pChCB303fQeqTZK4Wtr4Ehaq3GZl6dcaOb2a9TWUMn3LBkiCDg==",
      "{\"is-odd\":{\"peerDependenciesMeta\":{\"x\":{\"optional\":true}}}} => " +
      "sha512-2UYcdSUuVc7Wsa22oTkBj+1o6jazlcnqF+JyBx954XyTuuLXfqd/m6O3DlPEwarX9wD/4pZXAM9nJpT7Q3DEGQ=="
    })
    void theDigestOfTheRulesIsTheOneNpmRecords(String rules, String hash) {
        assertThat(read(json(rules)).getHash()).isEqualTo(hash);
    }

    @Test
    void aManifestWithoutTheFieldHasNoRulesAndNoDigest() {
        PackageExtensions none = read(Js.UNDEFINED);
        assertThat(none.getHash()).isNull();
        assertThat(none.apply(json(IS_ODD))).isNull();
        assertThat(none.wouldMatch("is-odd", "3.0.1")).isFalse();
    }

    // node_modules/is-odd and node_modules/once in the locks npm wrote: the fields the rule adds, and the
    // packageExtensionsApplied that says which
    @Test
    void theMatchingRuleExtendsACopyOfTheManifest() {
        Map<String, Object> isOdd = json(IS_ODD);
        PackageExtensions.Extended extended = read(json(
          "{\"is-odd@3\":{\"dependencies\":{\"isarray\":\"2.0.5\"}}}")).apply(isOdd);
        assertThat(extended).isNotNull();
        assertThat(extended.getPkg().get("dependencies")).isEqualTo(obj("is-number", "^6.0.0", "isarray", "2.0.5"));
        assertThat(extended.getApplied()).isEqualTo(obj("selector", "is-odd@3", "dependencies", arr("isarray")));
        // the manifest is the cache's, and stays as it was fetched
        assertThat(isOdd).isEqualTo(json(IS_ODD));

        extended = read(json(
          "{\"is-odd@>=3 <4\":{\"optionalDependencies\":{\"wrappy\":\"1.0.2\"},\"peerDependencies\":{\"isarray\":" +
          "\"2.x\"}}}")).apply(isOdd);
        assertThat(extended).isNotNull();
        assertThat(extended.getPkg().get("optionalDependencies")).isEqualTo(obj("wrappy", "1.0.2"));
        assertThat(extended.getPkg().get("peerDependencies")).isEqualTo(obj("isarray", "2.x"));
        assertThat(extended.getApplied()).isEqualTo(obj("selector", "is-odd@>=3 <4", "optionalDependencies",
          arr("wrappy"), "peerDependencies", arr("isarray")));

        extended = read(json(
          "{\"once\":{\"peerDependencies\":{\"isarray\":\"^2.0.0\"},\"peerDependenciesMeta\":{\"isarray\":{" +
          "\"optional\":true}}}}")).apply(json(ONCE));
        assertThat(extended).isNotNull();
        assertThat(extended.getPkg().get("peerDependencies")).isEqualTo(obj("isarray", "^2.0.0"));
        assertThat(extended.getPkg().get("peerDependenciesMeta")).isEqualTo(obj("isarray", obj("optional", true)));
        assertThat(extended.getApplied()).isEqualTo(obj("selector", "once", "peerDependencies", arr("isarray"),
          "peerDependenciesMeta", arr("isarray")));
    }

    // npm writes the lock of a project whose rule selects left-pad as it does without the rule, but for the digest
    @Test
    void aRuleSelectsANameAndTheVersionsInItsRange() {
        PackageExtensions rules = read(json(
          "{\"is-odd@3\":{\"dependencies\":{\"isarray\":\"2.0.5\"}},\"left-pad\":{\"dependencies\":{\"isarray\":" +
          "\"2.0.5\"}}}"));
        assertThat(rules.wouldMatch("is-odd", "3.0.1")).isTrue();
        assertThat(rules.wouldMatch("is-odd", "2.0.0")).isFalse();
        assertThat(rules.wouldMatch("is-odd", "not a version")).isFalse();
        assertThat(rules.wouldMatch("left-pad", "1.3.0")).isTrue();
        assertThat(rules.wouldMatch("left-pad", null)).isTrue();
        assertThat(rules.wouldMatch("once", "1.4.0")).isFalse();
        assertThat(rules.apply(json(ONCE))).isNull();
        assertThat(rules.apply(json("{\"version\":\"3.0.1\"}"))).isNull();
    }

    // the code and the message of npm's error for each rule set, which it reads before it resolves anything
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "[] | EEXTENSIONROOT | packageExtensions must be an object",
      "null | EEXTENSIONROOT | packageExtensions must be an object",
      "\"x\" | EEXTENSIONROOT | packageExtensions must be an object",
      "{\"\":{\"dependencies\":{\"x\":\"1\"}}} | EEXTENSIONSELECTOR | Invalid packageExtensions selector: \"\"",
      "{\"Is Odd\":{\"dependencies\":{\"x\":\"1\"}}} | EEXTENSIONSELECTOR | " +
      "Invalid package name in packageExtensions selector: \"Is Odd\"",
      "{\"@scope\":{\"dependencies\":{\"x\":\"1\"}}} | EEXTENSIONSELECTOR | " +
      "Invalid package name in packageExtensions selector: \"@scope\"",
      "{\"is-odd@\":{\"dependencies\":{\"x\":\"1\"}}} | EEXTENSIONSELECTOR | " +
      "Invalid packageExtensions selector: \"is-odd@\". Use the name only to match every version.",
      "{\"is-odd@ \":{\"dependencies\":{\"x\":\"1\"}}} | EEXTENSIONSELECTOR | " +
      "Invalid packageExtensions selector: \"is-odd@ \". Use the name only to match every version.",
      "{\"is-odd@latest\":{\"dependencies\":{\"isarray\":\"2.0.5\"}}} | EEXTENSIONSELECTOR | Invalid version range " +
      "in packageExtensions selector: \"is-odd@latest\". Selectors accept a package name with an optional semver " +
      "range only.",
      "{\"is-odd@npm:foo@1\":{\"dependencies\":{\"x\":\"1\"}}} | EEXTENSIONSELECTOR | Invalid version range in " +
      "packageExtensions selector: \"is-odd@npm:foo@1\". Selectors accept a package name with an optional semver " +
      "range only.",
      "{\"is-odd\":1} | EEXTENSIONVALUE | packageExtensions[\"is-odd\"] must be an object",
      "{\"is-odd\":null} | EEXTENSIONVALUE | packageExtensions[\"is-odd\"] must be an object",
      "{\"is-odd\":[]} | EEXTENSIONVALUE | packageExtensions[\"is-odd\"] must be an object",
      "{\"is-odd\":{\"scripts\":{}}} | EEXTENSIONFIELD | packageExtensions[\"is-odd\"] has unsupported field " +
      "\"scripts\". Supported fields: dependencies, optionalDependencies, peerDependencies, peerDependenciesMeta.",
      "{\"is-odd\":{\"dependencies\":1}} | EEXTENSIONVALUE | " +
      "packageExtensions[\"is-odd\"].dependencies must be an object",
      "{\"is-odd\":{\"dependencies\":[]}} | EEXTENSIONVALUE | " +
      "packageExtensions[\"is-odd\"].dependencies must be an object",
      "{\"is-odd\":{\"dependencies\":{\"x\":null}}} | EEXTENSIONDELETE | " +
      "packageExtensions[\"is-odd\"].dependencies.x attempts deletion, which is not supported.",
      "{\"is-odd\":{\"optionalDependencies\":{\"x\":false}}} | EEXTENSIONDELETE | " +
      "packageExtensions[\"is-odd\"].optionalDependencies.x attempts deletion, which is not supported.",
      "{\"is-odd\":{\"peerDependencies\":{\"x\":\"-\"}}} | EEXTENSIONDELETE | " +
      "packageExtensions[\"is-odd\"].peerDependencies.x attempts deletion, which is not supported.",
      "{\"is-odd\":{\"peerDependenciesMeta\":{\"x\":1}}} | EEXTENSIONVALUE | " +
      "packageExtensions[\"is-odd\"].peerDependenciesMeta.x must be an object"
    })
    void npmRefusesARuleSetItCannotRead(String rules, String code, String message) {
        assertThatThrownBy(() -> read(json(rules)))
          .isInstanceOfSatisfying(PackageExtensions.ExtensionError.class, e ->
            assertThat(e.getCode()).isEqualTo(code))
          .hasMessage(message);
    }

    // the code and the message of npm's error once is-odd 3.0.1 is the candidate the rules extend
    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '~', value = {
      "{\"is-odd\":{\"dependencies\":{\"is-number\":\"7.0.0\"}}} | EEXTENSIONDUPDEP | " +
      "packageExtensions[\"is-odd\"].dependencies.is-number conflicts with the package's existing " +
      "dependencies.is-number. Use overrides to change a dependency version; packageExtensions only adds missing " +
      "dependencies.",
      "{\"is-odd\":{\"dependencies\":{\"isarray\":\"2.0.5\"}},\"is-odd@3\":{\"dependencies\":{\"wrappy\":\"1.0.2\"}}}" +
      " | EEXTENSIONCONFLICT | Multiple packageExtensions selectors match is-odd@3.0.1: \"is-odd\", \"is-odd@3\". " +
      "Narrow or remove one of the overlapping rules.",
      "{\"is-odd\":{\"peerDependenciesMeta\":{\"x\":{\"optional\":true}}}} | EEXTENSIONORPHANMETA | " +
      "packageExtensions[\"is-odd\"].peerDependenciesMeta.x has no corresponding peerDependencies.x after extension " +
      "application."
    })
    void npmRefusesARuleItCannotApply(String rules, String code, String message) {
        PackageExtensions extensions = read(json(rules));
        assertThatThrownBy(() -> extensions.apply(json(IS_ODD)))
          .isInstanceOfSatisfying(PackageExtensions.ExtensionError.class, e ->
            assertThat(e.getCode()).isEqualTo(code))
          .hasMessage(message);
    }
}
