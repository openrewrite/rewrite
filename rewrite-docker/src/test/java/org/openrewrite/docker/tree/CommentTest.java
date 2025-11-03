/*
 * Copyright 2025 the original author or authors.
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

import static org.openrewrite.docker.Assertions.dockerfile;

class CommentTest implements RewriteTest {

    @Test
    void commentsAtTop() {
        rewriteRun(
          dockerfile(
            """
              # This is a comment
              # Another comment line
              FROM ubuntu:20.04
              """
          )
        );
    }

    @Test
    void commentsInline() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04  # Base image
              RUN apt-get update  # Update packages
              """
          )
        );
    }

    @Test
    void commentsBetweenInstructions() {
        rewriteRun(
          dockerfile(
            """
              FROM ubuntu:20.04
              # Update and install dependencies
              RUN apt-get update
              # Install curl
              RUN apt-get install -y curl
              """
          )
        );
    }

    @Test
    void emptyLinesAndComments() {
        rewriteRun(
          dockerfile(
            """
              # Base image
              FROM ubuntu:20.04
              
              # System updates
              RUN apt-get update
              
              # Install packages
              RUN apt-get install -y curl wget
              """
          )
        );
    }

    @Test
    void trailingCommentAfterImage() {
        rewriteRun(
          dockerfile(
            """
              FROM 'ubuntu:22.04' # Trailing comment
              RUN apt-get update
              """
          )
        );
    }
}
