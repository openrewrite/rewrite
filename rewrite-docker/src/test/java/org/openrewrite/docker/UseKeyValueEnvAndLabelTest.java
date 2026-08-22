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
package org.openrewrite.docker;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.docker.Assertions.docker;

class UseKeyValueEnvAndLabelTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseKeyValueEnvAndLabel());
    }

    @DocumentExample
    @Test
    void quoteAValueThatSpansASpace() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ENV JAVA_OPTS -Xmx1g -Xms1g
              """,
            """
              FROM ubuntu:22.04
              ENV JAVA_OPTS="-Xmx1g -Xms1g"
              """
          )
        );
    }

    @Test
    void singleWordValue() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ENV LANG C.UTF-8
              LABEL version 1.0.0
              """,
            """
              FROM ubuntu:22.04
              ENV LANG=C.UTF-8
              LABEL version=1.0.0
              """
          )
        );
    }

    @Test
    void alreadyKeyValue() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ENV a=1 b=2
              ENV JAVA_OPTS="-Xmx1g -Xms1g"
              LABEL maintainer="Jane Doe" version="1.0.0"
              """
          )
        );
    }

    @Test
    void aQuotedValueOnlyLosesItsSpace() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              LABEL description "An example image"
              ENV greeting 'hello world'
              """,
            """
              FROM ubuntu:22.04
              LABEL description="An example image"
              ENV greeting='hello world'
              """
          )
        );
    }

    @Test
    void aQuotedValueHoldingAVariable() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ENV PATH "/opt/bin:$PATH"
              """,
            """
              FROM ubuntu:22.04
              ENV PATH="/opt/bin:$PATH"
              """
          )
        );
    }

    @Test
    void quoteAValueHoldingAVariable() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ENV JAVA_OPTS -Xmx1g -Dname=$APP_NAME
              """,
            """
              FROM ubuntu:22.04
              ENV JAVA_OPTS="-Xmx1g -Dname=$APP_NAME"
              """
          )
        );
    }

    @Test
    void leaveAValueWhoseOwnQuotingWouldChangeAlone() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ENV greeting hello "big world"
              LABEL description a 'quoted phrase'
              ENV escaped one\\ two three
              """
          )
        );
    }

    @Test
    void aValueWrittenHardAgainstItsKeyIsNotTheLegacyForm() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ENV NODE_OPTIONS=--max-old-space-size=4096
              LABEL flags=--verbose
              ENV LIST=[1,2]
              """
          )
        );
    }

    @Test
    void multiStage() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04 AS builder
              ENV BUILD_OPTS -O2 -g

              FROM ubuntu:22.04
              LABEL description An example image
              COPY --from=builder /app /app
              """,
            """
              FROM ubuntu:22.04 AS builder
              ENV BUILD_OPTS="-O2 -g"

              FROM ubuntu:22.04
              LABEL description="An example image"
              COPY --from=builder /app /app
              """
          )
        );
    }
}
