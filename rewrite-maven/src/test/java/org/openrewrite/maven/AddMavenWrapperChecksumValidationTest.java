/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;

public class AddMavenWrapperChecksumValidationTest implements RewriteTest {

    private static final String WRAPPER_URL = "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar";
    private static final String WRAPPER_SHA256_SUM = "e63a53cfb9c4d291ebe3c2b0edacb7622bbc480326beaa5a0456e412f52f066a";
    private static final String DISTRIBUTION_URL = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.1/apache-maven-3.9.1-bin.zip";
    private static final String DISTRIBUTION_SHA256_SUM = "10b13517951362d435a3256222efd8b71524d9335d6dca4e78648a67ef71da41";

    @Test
    void doNotModifyIrrelevantFiles() {
        rewriteRun(
          spec -> spec.recipe(new AddMavenWrapperChecksumValidation(
          )),
          properties(
            """
              distributionUrl=%s""".formatted(DISTRIBUTION_URL),
            spec -> spec.path(".mvn/wrapper/gradle-wrapper.properties")
          )
        );
    }

    @Test
    void addDistributionSha256Sum() {
        rewriteRun(
          spec -> spec.recipe(new AddMavenWrapperChecksumValidation(
          )),
          properties(
            """
              distributionUrl=%s""".formatted(DISTRIBUTION_URL),
            """
              distributionUrl=%s
              distributionSha256Sum=%s
              """.formatted(DISTRIBUTION_URL, DISTRIBUTION_SHA256_SUM),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          )
        );
    }

    @Test
    void doNotAddDistributionSha256SumPropertyTwice() {
        rewriteRun(
          spec -> spec.recipe(new AddMavenWrapperChecksumValidation(
          )),
          properties(
            """
              distributionUrl=%s
              distributionSha256Sum=%s
              """.formatted(DISTRIBUTION_URL, DISTRIBUTION_SHA256_SUM),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          )
        );
    }

    @Test
    void doNotOverwriteDistributionSha256SumPropertyTwice() {
        rewriteRun(
          spec -> spec.recipe(new AddMavenWrapperChecksumValidation(
          )),
          properties(
            """
              distributionUrl=%s
              distributionSha256Sum=%s
              """.formatted(DISTRIBUTION_URL, "1"),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          )
        );
    }

    @Test
    void addWrapperSha256Sum() {
        rewriteRun(
          spec -> spec.recipe(new AddMavenWrapperChecksumValidation(
          )),
          properties(
            """
              wrapperUrl=%s""".formatted(WRAPPER_URL),
            """
              wrapperUrl=%s
              wrapperSha256Sum=%s
              """.formatted(WRAPPER_URL, WRAPPER_SHA256_SUM),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          )
        );
    }

    @Test
    void doNotAddWrapperSha256SumPropertyTwice() {
        rewriteRun(
          spec -> spec.recipe(new AddMavenWrapperChecksumValidation(
          )),
          properties(
            """
              wrapperUrl=%s
              wrapperSha256Sum=%s
              """.formatted(WRAPPER_URL, WRAPPER_SHA256_SUM),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          )
        );
    }

    @Test
    void doNotOverwriteWrapperSha256SumPropertyTwice() {
        rewriteRun(
          spec -> spec.recipe(new AddMavenWrapperChecksumValidation(
          )),
          properties(
            """
              wrapperUrl=%s
              wrapperSha256Sum=%s
              """.formatted(WRAPPER_URL, "1"),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          )
        );
    }
}
