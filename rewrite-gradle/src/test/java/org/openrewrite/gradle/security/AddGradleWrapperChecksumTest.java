/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.gradle.security;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.time.Duration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.openrewrite.properties.Assertions.properties;

class AddGradleWrapperChecksumTest implements RewriteTest {

    private static final String WRAPPER_PROPERTIES = "gradle/wrapper/gradle-wrapper.properties";

    /**
     * Not the real published sum; only the stub below serves it.
     */
    private static final String SHA_8_14 = "1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90a";

    @Override
    public void defaults(RecipeSpec spec) {
        HttpSender stub = request -> {
            if ("https://services.gradle.org/distributions/gradle-8.14-bin.zip.sha256"
                    .equals(request.getUrl().toString())) {
                return new HttpSender.Response(200, new ByteArrayInputStream(SHA_8_14.getBytes(UTF_8)), () -> {
                });
            }
            return new HttpSender.Response(404, new ByteArrayInputStream(new byte[0]), () -> {
            });
        };
        spec.recipe(new AddGradleWrapperChecksum())
                .executionContext(HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
                        .setHttpSender(stub)
                        .setLargeFileHttpSender(stub));
    }

    @DocumentExample("Pin the distribution checksum without changing the Gradle version")
    @Test
    void addsChecksumWhenMissing() {
        rewriteRun(
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-8.14-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-8.14-bin.zip
              distributionSha256Sum=1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90a
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path(WRAPPER_PROPERTIES)
          )
        );
    }

    @Test
    void noChangeWhenChecksumAlreadyPresent() {
        rewriteRun(
          properties(
            """
              distributionUrl=https\\://services.gradle.org/distributions/gradle-8.14-bin.zip
              distributionSha256Sum=0000000000000000000000000000000000000000000000000000000000000000
              """,
            spec -> spec.path(WRAPPER_PROPERTIES)
          )
        );
    }

    @Test
    void noChangeWhenChecksumCannotBeFetched() {
        rewriteRun(
          properties(
            """
              distributionUrl=https\\://internal.example.com/dists/gradle-8.14-bin.zip
              """,
            spec -> spec.path(WRAPPER_PROPERTIES)
          )
        );
    }

    @Test
    void noChangeWhenDistributionUrlIsMissing() {
        rewriteRun(
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              """,
            spec -> spec.path(WRAPPER_PROPERTIES)
          )
        );
    }

    /**
     * `services.gradle.org` answers the checksum URL with a 301 to `downloads.gradle.org`, and
     * {@link org.openrewrite.Checksum#fromUri} treats any non-2xx as a failure, so a client that did not follow
     * the redirect would silently leave every wrapper untouched. Stubbing the redirect would only prove the stub
     * follows it, so this one goes over the network.
     */
    @Test
    void followsTheRedirectToTheDownloadsHost() {
        HttpUrlConnectionSender sender = new HttpUrlConnectionSender(Duration.ofSeconds(30), Duration.ofMinutes(1));
        rewriteRun(
          spec -> spec.executionContext(HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
            .setHttpSender(sender)
            .setLargeFileHttpSender(sender)),
          properties(
            """
              distributionUrl=https\\://services.gradle.org/distributions/gradle-8.14-bin.zip
              """,
            """
              distributionUrl=https\\://services.gradle.org/distributions/gradle-8.14-bin.zip
              distributionSha256Sum=61ad310d3c7d3e5da131b76bbf22b5a4c0786e9d892dae8c1658d4b484de3caa
              """,
            spec -> spec.path(WRAPPER_PROPERTIES)
          )
        );
    }

    @Test
    void insertsWhenFileHasNoTrailingNewline() {
        rewriteRun(
          properties(
            "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.14-bin.zip",
            "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.14-bin.zip\n" +
            "distributionSha256Sum=" + SHA_8_14,
            spec -> spec.path(WRAPPER_PROPERTIES)
          )
        );
    }

    @Test
    void noChangeToOtherPropertiesFiles() {
        rewriteRun(
          properties(
            """
              distributionUrl=https\\://services.gradle.org/distributions/gradle-8.14-bin.zip
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }
}
