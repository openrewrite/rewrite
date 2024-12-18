/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.table.ImageSourceFiles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.yaml.Assertions.yaml;

// TODO: Remove this file, we will use the `FindDockerImageUses` in the rewrite-docker module
class FindImageTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindImage());
    }

    @DocumentExample
    @Test
    void gitlabCIFile() {
        rewriteRun(
          spec -> spec.recipe(new FindImage())
            .dataTable(ImageSourceFiles.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                assertThat(rows.get(0).getValue()).isEqualTo("maven:latest");
            }),
          yaml(
            """
              image: maven:latest
              """,
            """
              ~~(maven:latest)~~>image: maven:latest
              """,
            spec -> spec.path(".gitlab-ci")
          )
        );
    }

    @Test
    void dockerFile() {
        rewriteRun(
          spec -> spec.recipe(new FindImage())
            .dataTable(ImageSourceFiles.Row.class, rows -> {
                assertThat(rows).hasSize(2);
                assertThat(rows.get(0).getValue()).isEqualTo("alpine:latest");
                assertThat(rows.get(1).getValue()).isEqualTo("golang:1.7.3");
            }),
          text(
            //language=Dockerfile
            """
              FROM golang:1.7.3 as builder
              WORKDIR /go/src/github.com/alexellis/href-counter/
              RUN go get -d -v golang.org/x/net/html
              COPY app.go .
              RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o app .
              
              from alpine:latest
              run apk --no-cache add ca-certificates
              workdir /root/
              copy --from=builder /go/src/github.com/alexellis/href-counter/app .
              cmd ["./app"]
              """,
            """
              ~~(alpine:latest|golang:1.7.3)~~>FROM golang:1.7.3 as builder
              WORKDIR /go/src/github.com/alexellis/href-counter/
              RUN go get -d -v golang.org/x/net/html
              COPY app.go .
              RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o app .
              
              from alpine:latest
              run apk --no-cache add ca-certificates
              workdir /root/
              copy --from=builder /go/src/github.com/alexellis/href-counter/app .
              cmd ["./app"]
              """,
            spec -> spec.path("Dockerfile")
          )
        );
    }
}
