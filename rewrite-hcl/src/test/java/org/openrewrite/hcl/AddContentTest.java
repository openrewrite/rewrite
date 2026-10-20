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
package org.openrewrite.hcl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.hcl.Assertions.hcl;

class AddContentTest implements RewriteTest {

    @DocumentExample
    @Test
    void addAttributeToSelectedBlocks() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_db_instance", "publicly_accessible = false", null)),
          hcl(
            """
              resource "aws_db_instance" "db" {
                engine = "mysql"
              }
              resource "aws_rds_cluster" "cluster" {
                engine = "aurora-mysql"
              }
              """,
            """
              resource "aws_db_instance" "db" {
                engine              = "mysql"
                publicly_accessible = false
              }
              resource "aws_rds_cluster" "cluster" {
                engine = "aurora-mysql"
              }
              """
          )
        );
    }

    @Test
    void addAttributeToBlockSelectedByAllLabels() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_db_instance.primary", "publicly_accessible = false", null)),
          hcl(
            """
              resource "aws_db_instance" "primary" {
                engine = "mysql"
              }
              resource "aws_db_instance" "replica" {
                engine = "mysql"
              }
              """,
            """
              resource "aws_db_instance" "primary" {
                engine              = "mysql"
                publicly_accessible = false
              }
              resource "aws_db_instance" "replica" {
                engine = "mysql"
              }
              """
          )
        );
    }

    @Test
    void existingAttributeIsLeftUnchanged() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_db_instance", "publicly_accessible = false", null)),
          hcl(
            """
              resource "aws_db_instance" "db" {
                engine              = "mysql"
                publicly_accessible = true
              }
              """
          )
        );
    }

    @Test
    void existingBlockIsLeftUnchanged() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance",
            """
              root_block_device {
                encrypted = true
              }
              """, null)),
          hcl(
            """
              resource "aws_instance" "web" {
                ami = "ami-123"
                root_block_device {
                  volume_size = 8
                }
              }
              """
          )
        );
    }

    @Test
    void attributeIsNotAddedWhenBlockOfSameNameExists() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_s3_bucket", "versioning = {}", null)),
          hcl(
            """
              resource "aws_s3_bucket" "b" {
                versioning {
                  enabled = true
                }
              }
              """
          )
        );
    }

    @Test
    void addBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance",
            """
              root_block_device {
                encrypted = true
              }
              """, null)),
          hcl(
            """
              resource "aws_instance" "web" {
                ami = "ami-123"
              }
              """,
            """
              resource "aws_instance" "web" {
                ami = "ami-123"
                root_block_device {
                  encrypted = true
                }
              }
              """
          )
        );
    }

    @Test
    void addLabelledBlockNextToBlockWithOtherLabels() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance",
            """
              provisioner "local-exec" {
                command = "echo done"
              }
              """, null)),
          hcl(
            """
              resource "aws_instance" "web" {
                provisioner "file" {
                  source = "conf"
                }
              }
              resource "aws_instance" "app" {
                provisioner "local-exec" {
                  command = "echo app"
                }
              }
              """,
            """
              resource "aws_instance" "web" {
                provisioner "file" {
                  source = "conf"
                }
                provisioner "local-exec" {
                  command = "echo done"
                }
              }
              resource "aws_instance" "app" {
                provisioner "local-exec" {
                  command = "echo app"
                }
              }
              """
          )
        );
    }

    @Test
    void addToExistingNestedBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance.root_block_device", "encrypted = true", null)),
          hcl(
            """
              resource "aws_instance" "web" {
                ami = "ami-123"
                root_block_device {
                  volume_size = 8
                }
              }
              """,
            """
              resource "aws_instance" "web" {
                ami = "ami-123"
                root_block_device {
                  volume_size = 8
                  encrypted   = true
                }
              }
              """
          )
        );
    }

    @Test
    void addToEmptyBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance.metadata_options", "http_tokens = \"required\"", null)),
          hcl(
            """
              resource "aws_instance" "web" {
                metadata_options {}
              }
              """,
            """
              resource "aws_instance" "web" {
                metadata_options {
                  http_tokens = "required"
                }
              }
              """
          )
        );
    }

    @Test
    void addToSingleLineBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance.metadata_options", "http_tokens = \"required\"", null)),
          hcl(
            """
              resource "aws_instance" "web" {
                metadata_options { http_endpoint = "enabled" }
              }
              """,
            """
              resource "aws_instance" "web" {
                metadata_options {
                  http_endpoint = "enabled"
                  http_tokens   = "required"
                }
              }
              """
          )
        );
    }

    @Test
    void indentLikeSiblings() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance",
            """
              root_block_device {
                encrypted = true
              }
              """, null)),
          hcl(
            """
              resource "aws_instance" "web" {
                  ami = "ami-123"
              }
              """,
            """
              resource "aws_instance" "web" {
                  ami = "ami-123"
                  root_block_device {
                    encrypted = true
                  }
              }
              """
          )
        );
    }

    @Test
    void keepLeadingComment() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance",
            """
              # required by policy
              root_block_device {
                encrypted = true
              }
              """, null)),
          hcl(
            """
              resource "aws_instance" "web" {
                ami = "ami-123"
              }
              """,
            """
              resource "aws_instance" "web" {
                ami = "ami-123"
                # required by policy
                root_block_device {
                  encrypted = true
                }
              }
              """
          )
        );
    }

    @Test
    void prepend() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_db_instance", "publicly_accessible = false", true)),
          hcl(
            """
              resource "aws_db_instance" "db" {
                # the engine
                engine = "mysql"
              }
              """,
            """
              resource "aws_db_instance" "db" {
                publicly_accessible = false
                # the engine
                engine              = "mysql"
              }
              """
          )
        );
    }

    @Test
    void realignOnlyAlignedAttributes() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_db_instance", "publicly_accessible = false", null)),
          hcl(
            """
              resource "aws_db_instance" "db" {
                engine = "mysql"
                instance_class   = "db.t3.micro"
              }
              """,
            """
              resource "aws_db_instance" "db" {
                engine = "mysql"
                instance_class   = "db.t3.micro"
                publicly_accessible = false
              }
              """
          )
        );
    }

    @Test
    void separateAttributeGroupsKeepTheirAlignment() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_db_instance", "publicly_accessible = false", null)),
          hcl(
            """
              resource "aws_db_instance" "db" {
                engine = "mysql"

                tags = {}
              }
              """,
            """
              resource "aws_db_instance" "db" {
                engine = "mysql"

                tags                = {}
                publicly_accessible = false
              }
              """
          )
        );
    }

    @Test
    void addToTopLevelOfFile() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$", "instance_type = \"t3.micro\"", null)),
          hcl(
            """
              region = "eu-west-1"
              ami    = "ami-123"
              """,
            """
              region        = "eu-west-1"
              ami           = "ami-123"
              instance_type = "t3.micro"
              """,
            spec -> spec.path("terraform.tfvars")
          )
        );
    }

    @Test
    void rootPathDoesNotAddToTopLevelBlocks() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$",
            """
              terraform {
                required_version = ">= 1.5"
              }
              """, null)),
          hcl(
            """
              provider "aws" {
                region = "eu-west-1"
              }
              """,
            """
              provider "aws" {
                region = "eu-west-1"
              }
              terraform {
                required_version = ">= 1.5"
              }
              """
          )
        );
    }

    @Test
    void prependToTopLevelOfFile() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$", "instance_type = \"t3.micro\"", true)),
          hcl(
            """
              region = "eu-west-1"
              """,
            """
              instance_type = "t3.micro"
              region        = "eu-west-1"
              """
          )
        );
    }

    @Test
    void addToEmptyFile() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$", "region = \"eu-west-1\"", null)),
          hcl(
            "",
            """
              region = "eu-west-1"
              """
          )
        );
    }

    @Test
    void addToMultilineObject() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance.tags", "Owner = \"platform\"", null)),
          hcl(
            """
              resource "aws_instance" "web" {
                tags = {
                  Name = "web"
                }
              }
              """,
            """
              resource "aws_instance" "web" {
                tags = {
                  Name  = "web"
                  Owner = "platform"
                }
              }
              """
          )
        );
    }

    @Test
    void addToMultilineObjectWithCommas() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance.tags", "Owner = \"platform\"", null)),
          hcl(
            """
              resource "aws_instance" "web" {
                tags = {
                  Name = "web",
                  Env  = "prod",
                }
              }
              """,
            """
              resource "aws_instance" "web" {
                tags = {
                  Name  = "web",
                  Env   = "prod",
                  Owner = "platform",
                }
              }
              """
          )
        );
    }

    @Test
    void addQuotedKeyToObject() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance.tags", "\"kubernetes.io/role\" = \"node\"", null)),
          hcl(
            """
              resource "aws_instance" "web" {
                tags = {
                  Name = "web"
                }
              }
              """,
            """
              resource "aws_instance" "web" {
                tags = {
                  Name                 = "web"
                  "kubernetes.io/role" = "node"
                }
              }
              """
          )
        );
    }

    @Test
    void addToSingleLineObject() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance.tags", "Owner = \"platform\"", null)),
          hcl(
            """
              resource "aws_instance" "web" {
                tags = { Name = "web" }
              }
              """,
            """
              resource "aws_instance" "web" {
                tags = { Name = "web", Owner = "platform" }
              }
              """
          )
        );
    }

    @Test
    void prependToSingleLineObject() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance.tags", "Owner = \"platform\"", true)),
          hcl(
            """
              resource "aws_instance" "web" {
                tags = {Name = "web"}
              }
              """,
            """
              resource "aws_instance" "web" {
                tags = {Owner = "platform", Name = "web"}
              }
              """
          )
        );
    }

    @Test
    void addToEmptyObject() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance.tags", "Owner = \"platform\"", null)),
          hcl(
            """
              resource "aws_instance" "web" {
                tags = {}
              }
              """,
            """
              resource "aws_instance" "web" {
                tags = {
                  Owner = "platform"
                }
              }
              """
          )
        );
    }

    @Test
    void existingObjectKeyIsLeftUnchanged() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance.tags", "Owner = \"platform\"", null)),
          hcl(
            """
              resource "aws_instance" "web" {
                tags = {
                  Owner = "team"
                }
              }
              """
          )
        );
    }

    @Test
    void heredocContentIsKeptVerbatim() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance",
            """
              user_data = <<EOT
              #!/bin/bash
                echo hello
              EOT
              """, null)),
          hcl(
            """
              resource "aws_instance" "web" {
                ami = "ami-123"
              }
              """,
            """
              resource "aws_instance" "web" {
                ami = "ami-123"
                user_data = <<EOT
              #!/bin/bash
                echo hello
              EOT
              }
              """
          )
        );
    }

    @Test
    void ensureValueTogetherWithChangeValue() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            //language=yaml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: com.example.PrivateDbInstance
              displayName: Private DB instance
              description: Ensure that DB instances aren't publicly accessible.
              recipeList:
                - org.openrewrite.hcl.AddContent:
                    blockPath: $.resource.aws_db_instance
                    content: publicly_accessible = false
                - org.openrewrite.hcl.ChangeValue:
                    keyPath: $.resource.aws_db_instance.publicly_accessible
                    value: false
              """,
            "com.example.PrivateDbInstance"
          ),
          hcl(
            """
              resource "aws_db_instance" "missing" {
                engine = "mysql"
              }
              resource "aws_db_instance" "public" {
                engine              = "mysql"
                publicly_accessible = true
              }
              resource "aws_db_instance" "private" {
                engine              = "mysql"
                publicly_accessible = false
              }
              """,
            """
              resource "aws_db_instance" "missing" {
                engine              = "mysql"
                publicly_accessible = false
              }
              resource "aws_db_instance" "public" {
                engine              = "mysql"
                publicly_accessible = false
              }
              resource "aws_db_instance" "private" {
                engine              = "mysql"
                publicly_accessible = false
              }
              """
          )
        );
    }

    @Test
    void trailingCommentStaysWithLastAttributeOfBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_db_instance", "publicly_accessible = false", null)),
          hcl(
            """
              resource "aws_db_instance" "db" {
                engine = "mysql" # the engine
              }
              """,
            """
              resource "aws_db_instance" "db" {
                engine              = "mysql" # the engine
                publicly_accessible = false
              }
              """
          )
        );
    }

    @Test
    void commentAfterOpeningBraceStaysThereOnPrepend() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_db_instance", "publicly_accessible = false", true)),
          hcl(
            """
              resource "aws_db_instance" "db" { # the database
                engine = "mysql"
              }
              """,
            """
              resource "aws_db_instance" "db" { # the database
                publicly_accessible = false
                engine              = "mysql"
              }
              """
          )
        );
    }

    @Test
    void trailingCommentStaysWithLastElementOfObject() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance.tags", "Owner = \"platform\"", null)),
          hcl(
            """
              resource "aws_instance" "web" {
                tags = {
                  Name = "web" # the name
                }
              }
              """,
            """
              resource "aws_instance" "web" {
                tags = {
                  Name  = "web" # the name
                  Owner = "platform"
                }
              }
              """
          )
        );
    }

    @Test
    void commentInEmptyObjectIsKept() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$.resource.aws_instance.tags", "Owner = \"platform\"", null)),
          hcl(
            """
              resource "aws_instance" "web" {
                tags = {
                  # to be completed
                }
              }
              """,
            """
              resource "aws_instance" "web" {
                tags = {
                  Owner = "platform"
                  # to be completed
                }
              }
              """
          )
        );
    }

    @Test
    void trailingCommentStaysWithLastAttributeOfFile() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$", "instance_type = \"t3.micro\"", null)),
          hcl(
            """
              region = "eu-west-1" # the region
              """,
            """
              region        = "eu-west-1" # the region
              instance_type = "t3.micro"
              """
          )
        );
    }

    @Test
    void addToFileWithOnlyAComment() {
        rewriteRun(
          spec -> spec.recipe(new AddContent("$", "region = \"eu-west-1\"", null)),
          hcl(
            "# variables",
            """
              # variables
              region = "eu-west-1"
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a = 1\nb = 2", "a = ", "a = 1 }", "block {", "a = 1 # comment", "{ a = 1 }"})
    void invalidContent(String content) {
        // when
        var validated = new AddContent("$.resource", content, null).validate();

        // then
        assertThat(validated.isInvalid()).isTrue();
    }
}
