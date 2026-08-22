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
package org.openrewrite.docker.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.docker.table.StageDependencies;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.docker.Assertions.docker;
import static org.openrewrite.test.SourceSpecs.text;

class FindStageGraphTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindStageGraph());
    }

    @DocumentExample
    @Test
    void recordTheStageGraphAndMarkTheStagesNothingReaches() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(StageDependencies.class.getName(),
            //language=csv
            """
              sourcePath,stageName,stageIndex,baseImage,registry,referencedBy,buildTarget,reachable
              Dockerfile,build,0,maven:3.9,docker.io,,false,false
              Dockerfile,tools,1,golang:1.22,docker.io,,false,false
              Dockerfile,,2,eclipse-temurin:21-jre,docker.io,,false,true
              """
          ),
          docker(
            """
              FROM maven:3.9 AS build
              RUN mvn package

              FROM golang:1.22 AS tools
              RUN go build ./cmd/lint

              FROM eclipse-temurin:21-jre
              RUN echo done
              """,
            """
              ~~(unused)~~>FROM maven:3.9 AS build
              RUN mvn package

              ~~(unused)~~>FROM golang:1.22 AS tools
              RUN go build ./cmd/lint

              FROM eclipse-temurin:21-jre
              RUN echo done
              """
          )
        );
    }

    @Test
    void recordWhoReachesAStage() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(StageDependencies.class.getName(),
            //language=csv
            """
              sourcePath,stageName,stageIndex,baseImage,registry,referencedBy,buildTarget,reachable
              Dockerfile,base,0,alpine,docker.io,"builder,#2",false,true
              Dockerfile,builder,1,base,,"#2",false,true
              Dockerfile,,2,base,,,false,true
              """
          ),
          docker(
            """
              FROM alpine AS base
              RUN apk add curl

              FROM base AS builder
              RUN make

              FROM base
              COPY --from=builder /out /out
              """
          )
        );
    }

    @Test
    void aBindMountIsAStageReference() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(StageDependencies.class.getName(),
            //language=csv
            """
              sourcePath,stageName,stageIndex,baseImage,registry,referencedBy,buildTarget,reachable
              Dockerfile,certs,0,alpine,docker.io,"#1",false,true
              Dockerfile,,1,alpine,docker.io,,false,true
              """
          ),
          docker(
            """
              FROM alpine AS certs
              RUN update-ca-certificates

              FROM alpine
              RUN --mount=type=bind,from=certs,source=/etc/ssl,target=/ssl cp -r /ssl /etc/ssl
              """
          )
        );
    }

    @Test
    void aCopyReachesAStageDeclaredAfterIt() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(StageDependencies.class.getName(),
            //language=csv
            """
              sourcePath,stageName,stageIndex,baseImage,registry,referencedBy,buildTarget,reachable
              Dockerfile,runtime,0,alpine,docker.io,"#2",false,true
              Dockerfile,builder,1,golang,docker.io,runtime,false,true
              Dockerfile,,2,runtime,,,false,true
              """
          ),
          docker(
            """
              FROM alpine AS runtime
              COPY --from=builder /app /app

              FROM golang AS builder
              RUN go build

              FROM runtime
              CMD ["/app"]
              """
          )
        );
    }

    @Test
    void reportEveryStageAsNeededWhenAReferenceNamesAPosition() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(StageDependencies.class.getName(),
            //language=csv
            """
              sourcePath,stageName,stageIndex,baseImage,registry,referencedBy,buildTarget,reachable
              Dockerfile,unused,0,alpine,docker.io,,false,true
              Dockerfile,assets,1,node:22,docker.io,,false,true
              Dockerfile,,2,nginx:alpine,docker.io,,false,true
              """
          ),
          docker(
            """
              FROM alpine AS unused
              RUN echo nothing

              FROM node:22 AS assets
              RUN npm run build

              FROM nginx:alpine
              COPY --from=1 /dist /usr/share/nginx/html
              """
          )
        );
    }

    @Test
    void recordAStageTheRepositoryBuildsOnItsOwn() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(StageDependencies.class.getName(),
            //language=csv
            """
              sourcePath,stageName,stageIndex,baseImage,registry,referencedBy,buildTarget,reachable
              Dockerfile,lint,0,golangci/golangci-lint,docker.io,,true,true
              Dockerfile,dead,1,alpine,docker.io,,false,false
              Dockerfile,,2,alpine,docker.io,,false,true
              """
          ),
          text(
            """
              target "lint" {
                target = "lint"
              }
              """,
            spec -> spec.path("docker-bake.hcl")
          ),
          docker(
            """
              FROM golangci/golangci-lint AS lint
              RUN golangci-lint run

              FROM alpine AS dead
              RUN echo nothing

              FROM alpine
              RUN echo done
              """,
            """
              FROM golangci/golangci-lint AS lint
              RUN golangci-lint run

              ~~(unused)~~>FROM alpine AS dead
              RUN echo nothing

              FROM alpine
              RUN echo done
              """
          )
        );
    }

    @Test
    void recordTheRegistryAStageIsPulledFrom() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(StageDependencies.class.getName(),
            //language=csv
            """
              sourcePath,stageName,stageIndex,baseImage,registry,referencedBy,buildTarget,reachable
              Dockerfile,build,0,"mcr.microsoft.com/dotnet/sdk:8.0",mcr.microsoft.com,"#1",false,true
              Dockerfile,,1,redhat/ubi9-minimal,docker.io,,false,true
              """
          ),
          docker(
            """
              FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
              RUN dotnet publish

              FROM redhat/ubi9-minimal
              COPY --from=build /app /app
              """
          )
        );
    }

    @Test
    void recordTheDigestOfAPinnedBaseImage() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(StageDependencies.class.getName(),
            //language=csv
            """
              sourcePath,stageName,stageIndex,baseImage,registry,referencedBy,buildTarget,reachable
              Dockerfile,,0,alpine:3.20@sha256:abc,docker.io,,false,true
              """
          ),
          docker(
            """
              FROM alpine:3.20@sha256:abc
              RUN echo hi
              """
          )
        );
    }
}
