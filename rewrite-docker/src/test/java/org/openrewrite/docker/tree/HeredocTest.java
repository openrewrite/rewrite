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
package org.openrewrite.docker.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.docker.Assertions.docker;

class HeredocTest implements RewriteTest {

    @Test
    void heredocWithShellPath() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<EOF
              addgroup -S docker
              adduser -S --shell /bin/bash --ingroup docker vscode
              EOF
              """
          )
        );
    }

    @Test
    void heredocWithMultipleLines() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<EOF
              apt-get update
              apt-get install -y curl
              EOF
              """
          )
        );
    }

    @Test
    void heredocWithDash() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              RUN <<-FILE_END
              echo "Hello World"
              echo "Another line"
              FILE_END
              """
          )
        );
    }
}
