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

import java.time.Instant;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * config/version-policy/test/index.ts of pnpm 11.2.2.
 */
class PackageVersionPolicyTest {

    @Test
    void createPackageVersionPolicy() {
        Function<String, Object> match = PackageVersionPolicy.createPackageVersionPolicy(singletonList("axios@1.12.2"));
        assertThat(match.apply("axios")).isEqualTo(singletonList("1.12.2"));

        match = PackageVersionPolicy.createPackageVersionPolicy(singletonList("is-*"));
        assertThat(match.apply("is-odd")).isEqualTo(true);
        assertThat(match.apply("is-even")).isEqualTo(true);
        assertThat(match.apply("lodash")).isEqualTo(false);

        match = PackageVersionPolicy.createPackageVersionPolicy(singletonList("@babel/core@7.20.0"));
        assertThat(match.apply("@babel/core")).isEqualTo(singletonList("7.20.0"));

        match = PackageVersionPolicy.createPackageVersionPolicy(singletonList("@babel/core"));
        assertThat(match.apply("@babel/core")).isEqualTo(true);

        match = PackageVersionPolicy.createPackageVersionPolicy(singletonList("axios@1.12.2"));
        assertThat(match.apply("is-odd")).isEqualTo(false);

        match = PackageVersionPolicy.createPackageVersionPolicy(asList("axios@1.12.2", "lodash@4.17.21", "is-*"));
        assertThat(match.apply("axios")).isEqualTo(singletonList("1.12.2"));
        assertThat(match.apply("lodash")).isEqualTo(singletonList("4.17.21"));
        assertThat(match.apply("is-odd")).isEqualTo(true);

        assertThatThrownBy(() -> PackageVersionPolicy.createPackageVersionPolicy(singletonList("lodash@^4.17.0")))
          .hasMessageContaining("Invalid versions union");
        assertThatThrownBy(() -> PackageVersionPolicy.createPackageVersionPolicy(singletonList("lodash@~4.17.0")))
          .hasMessageContaining("Invalid versions union");
        assertThatThrownBy(() -> PackageVersionPolicy.createPackageVersionPolicy(singletonList("react@>=18.0.0")))
          .hasMessageContaining("Invalid versions union");
        assertThatThrownBy(() -> PackageVersionPolicy.createPackageVersionPolicy(singletonList("is-*@1.0.0")))
          .hasMessageContaining("Name patterns are not allowed");

        match = PackageVersionPolicy.createPackageVersionPolicy(singletonList("axios@1.12.0 || 1.12.1"));
        assertThat(match.apply("axios")).isEqualTo(asList("1.12.0", "1.12.1"));

        match = PackageVersionPolicy.createPackageVersionPolicy(singletonList("@scope/pkg@1.0.0 || 1.0.1"));
        assertThat(match.apply("@scope/pkg")).isEqualTo(asList("1.0.0", "1.0.1"));

        match = PackageVersionPolicy.createPackageVersionPolicy(singletonList("pkg@1.0.0||1.0.1  ||  1.0.2"));
        assertThat(match.apply("pkg")).isEqualTo(asList("1.0.0", "1.0.1", "1.0.2"));
    }

    @Test
    void createPackageVersionPolicyOrThrowRewrapsParserErrorsWithInvalidKey() {
        assertThatThrownBy(() -> PackageVersionPolicy.createPackageVersionPolicyOrThrow(
          singletonList("lodash@^4.17.0"), "minimumReleaseAgeExclude"))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_MINIMUM_RELEASE_AGE_EXCLUDE");
              assertThat(e.getMessage()).contains("Invalid value in minimumReleaseAgeExclude:");
          });
        assertThatThrownBy(() -> PackageVersionPolicy.createPackageVersionPolicyOrThrow(
          singletonList("is-*@1.0.0"), "trustPolicyExclude"))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_TRUST_POLICY_EXCLUDE"));
    }

    @Test
    void createPackageVersionPolicyOrThrowReturnsAWorkingPolicyForValidInput() {
        Function<String, Object> policy = PackageVersionPolicy.createPackageVersionPolicyOrThrow(
          singletonList("axios@1.12.2"), "minimumReleaseAgeExclude");
        assertThat(policy.apply("axios")).isEqualTo(singletonList("1.12.2"));
        assertThat(policy.apply("lodash")).isEqualTo(false);
    }

    @Test
    void getPublishedByPolicyReturnsUndefinedFieldsWhenNoConfigIsSet() {
        PackageVersionPolicy.PublishedByPolicy policy = PackageVersionPolicy.getPublishedByPolicy(null, null,
          Instant.now());
        assertThat(policy.getPublishedBy()).isNull();
        assertThat(policy.getPublishedByExclude()).isNull();
    }

    @Test
    void getPublishedByPolicyDerivesPublishedByFromMinimumReleaseAgeInMinutes() {
        Instant now = Instant.parse("2026-10-08T12:00:00.123Z");
        PackageVersionPolicy.PublishedByPolicy policy = PackageVersionPolicy.getPublishedByPolicy(24.0 * 60, null,
          now);
        assertThat(policy.getPublishedByExclude()).isNull();
        assertThat(policy.getPublishedBy()).isEqualTo(Instant.parse("2026-10-07T12:00:00.123Z"));
    }

    @Test
    void getPublishedByPolicyBuildsPublishedByExcludePolicyWhenMinimumReleaseAgeExcludeIsSet() {
        PackageVersionPolicy.PublishedByPolicy policy = PackageVersionPolicy.getPublishedByPolicy(24.0 * 60,
          singletonList("pnpm@9.1.0"), Instant.now());
        assertThat(policy.getPublishedByExclude().apply("pnpm")).isEqualTo(singletonList("9.1.0"));
        assertThat(policy.getPublishedByExclude().apply("axios")).isEqualTo(false);
    }

    @Test
    void getPublishedByPolicyRewrapsInvalidExcludePatterns() {
        assertThatThrownBy(() -> PackageVersionPolicy.getPublishedByPolicy(24.0 * 60, singletonList("pnpm@^9.0.0"),
          Instant.now()))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_MINIMUM_RELEASE_AGE_EXCLUDE"));
    }
}
