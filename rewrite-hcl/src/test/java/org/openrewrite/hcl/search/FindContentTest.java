/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.hcl.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.hcl.Assertions.hcl;

class FindContentTest implements RewriteTest {

    @DocumentExample
    @Test
    void find() {
        rewriteRun(
          spec -> spec.recipe(new FindContent("$.provider")),
          hcl(
            """
              provider {
              }
              """,
            """
              /*~~>*/provider {
              }
              """
          )
        );
    }

    @Test
    void findAttributeOfResourceSelectedByFilter() {
        rewriteRun(
          spec -> spec.recipe(new FindContent("$.resource.aws_db_instance[?(@.engine == 'mysql')].engine_version")),
          hcl(
            """
              resource "aws_db_instance" "a" {
                engine         = "mysql"
                engine_version = "5.7"
              }
              resource "aws_db_instance" "b" {
                engine         = "postgres"
                engine_version = "13"
              }
              """,
            """
              resource "aws_db_instance" "a" {
                engine         = "mysql"
                /*~~>*/engine_version = "5.7"
              }
              resource "aws_db_instance" "b" {
                engine         = "postgres"
                engine_version = "13"
              }
              """
          )
        );
    }
}
