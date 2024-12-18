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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

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
          yaml(
            """
              image: maven:latest
              
              variables:
                MAVEN_CLI_OPTS: "-s .m2/settings.xml --batch-mode"
                MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
              
              cache:
                paths:
                  - .m2/repository/
                  - target/
              
              build:
                stage: build
                script:
                  - mvn $MAVEN_CLI_OPTS compile
              
              test:
                stage: test
                script:
                  - mvn $MAVEN_CLI_OPTS test
              
              deploy:
                stage: deploy
                script:
                  - mvn $MAVEN_CLI_OPTS deploy
                only:
                  - master
              """,
            """
              ~~(maven:latest)~~>image: maven:latest
              
              variables:
                MAVEN_CLI_OPTS: "-s .m2/settings.xml --batch-mode"
                MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
              
              cache:
                paths:
                  - .m2/repository/
                  - target/
              
              build:
                stage: build
                script:
                  - mvn $MAVEN_CLI_OPTS compile
              
              test:
                stage: test
                script:
                  - mvn $MAVEN_CLI_OPTS test
              
              deploy:
                stage: deploy
                script:
                  - mvn $MAVEN_CLI_OPTS deploy
                only:
                  - master
              """,
            spec -> spec.path(".gitlab-ci")
          )
        );
    }

    @Test
    void dockerFile() {
        rewriteRun(
          text(
            //language=Dockerfile
            """
              FROM golang:1.7.3 as builder
              WORKDIR /go/src/github.com/alexellis/href-counter/
              RUN go get -d -v golang.org/x/net/html
              COPY app.go .
              RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o app .
              
              FROM alpine:latest
              RUN apk --no-cache add ca-certificates
              WORKDIR /root/
              COPY --from=builder /go/src/github.com/alexellis/href-counter/app .
              CMD ["./app"]
              """,
            """
              ~~(golang:1.7.3)~~>FROM golang:1.7.3 as builder
              WORKDIR /go/src/github.com/alexellis/href-counter/
              RUN go get -d -v golang.org/x/net/html
              COPY app.go .
              RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o app .
              
              ~~(alpine:latest)~~>FROM alpine:latest
              RUN apk --no-cache add ca-certificates
              WORKDIR /root/
              COPY --from=builder /go/src/github.com/alexellis/href-counter/app .
              CMD ["./app"]
              """,
            spec -> spec.path("Dockerfile")
          )
        );
    }
}
