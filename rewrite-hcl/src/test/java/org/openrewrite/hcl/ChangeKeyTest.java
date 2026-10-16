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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.hcl.Assertions.hcl;

class ChangeKeyTest implements RewriteTest {

    @DocumentExample
    @Test
    void renameAttributeInBlock() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$.resource.name", "db_name")),
          hcl(
            """
              resource "aws_db_instance" "default" {
                engine = "mysql"
                name   = "mydb"
              }
              """,
            """
              resource "aws_db_instance" "default" {
                engine  = "mysql"
                db_name = "mydb"
              }
              """
          )
        );
    }

    @Test
    void renameAttributeInNestedBlock() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$.provider.features.key_vault.purge_soft_delete_on_destroy", "purge_on_destroy")),
          hcl(
            """
              provider "azurerm" {
                features {
                  key_vault {
                    purge_soft_delete_on_destroy = true
                  }
                }
                purge_soft_delete_on_destroy = true
              }
              """,
            """
              provider "azurerm" {
                features {
                  key_vault {
                    purge_on_destroy = true
                  }
                }
                purge_soft_delete_on_destroy = true
              }
              """
          )
        );
    }

    @Test
    void renameWithRecursiveDescent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$..ami", "image_id")),
          hcl(
            """
              resource "aws_instance" "web" {
                ami = "ami-123"
              }
              resource "aws_launch_template" "web" {
                ami = "ami-456"
              }
              """,
            """
              resource "aws_instance" "web" {
                image_id = "ami-123"
              }
              resource "aws_launch_template" "web" {
                image_id = "ami-456"
              }
              """
          )
        );
    }

    @Test
    void renameTopLevelAttribute() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$..region", "aws_region")),
          hcl(
            """
              region = "us-east-1"
              """,
            """
              aws_region = "us-east-1"
              """
          )
        );
    }

    @Test
    void renameObjectKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$..Name", "DisplayName")),
          hcl(
            """
              resource "aws_instance" "web" {
                tags = {
                  Name = "web"
                  Env  = "prod"
                }
              }
              """,
            """
              resource "aws_instance" "web" {
                tags = {
                  DisplayName = "web"
                  Env         = "prod"
                }
              }
              """
          )
        );
    }

    @Test
    void renameQuotedObjectKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$..['kubernetes.io/role/elb']", "kubernetes.io/role/internal-elb")),
          hcl(
            """
              resource "aws_subnet" "public" {
                tags = {
                  "kubernetes.io/role/elb" = 1
                }
              }
              """,
            """
              resource "aws_subnet" "public" {
                tags = {
                  "kubernetes.io/role/internal-elb" = 1
                }
              }
              """
          )
        );
    }

    @Test
    void quoteObjectKeyWhenNewKeyIsNotAnIdentifier() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$..Role", "kubernetes.io/role")),
          hcl(
            """
              resource "aws_subnet" "public" {
                tags = {
                  Role = "elb"
                }
              }
              """,
            """
              resource "aws_subnet" "public" {
                tags = {
                  "kubernetes.io/role" = "elb"
                }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenNewKeyIsNotAnIdentifierForBlockAttribute() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$.resource.name", "db/name")),
          hcl(
            """
              resource "aws_db_instance" "default" {
                name = "mydb"
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenSiblingAlreadyHasNewKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$.resource.name", "db_name")),
          hcl(
            """
              resource "aws_db_instance" "default" {
                name    = "mydb"
                db_name = "mydb"
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenPathDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$.resource.name", "db_name")),
          hcl(
            """
              data "aws_db_instance" "default" {
                name = "mydb"
              }
              """
          )
        );
    }

    @Test
    void noChangeForComputedObjectKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$..['(var.key)']", "renamed")),
          hcl(
            """
              locals {
                tags = {
                  (var.key) = "value"
                }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenSiblingBlockHasNewKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$.resource.foo", "lifecycle")),
          hcl(
            """
              resource "aws_instance" "web" {
                foo = 1
                lifecycle {
                  create_before_destroy = true
                }
              }
              """
          )
        );
    }

    @Test
    void renameOnlyFirstOfSeveralMatchingSiblings() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$.resource['foo','bar']", "baz")),
          hcl(
            """
              resource "aws_instance" "web" {
                foo = 1
                bar = 2
              }
              """,
            """
              resource "aws_instance" "web" {
                baz = 1
                bar = 2
              }
              """
          )
        );
    }

    @Test
    void doNotAlignInlineObject() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$..Name", "DisplayName")),
          hcl(
            """
              resource "aws_instance" "web" {
                tags = { Name = "web", Env = "prod" }
              }
              """,
            """
              resource "aws_instance" "web" {
                tags = { DisplayName = "web", Env = "prod" }
              }
              """
          )
        );
    }

    @Test
    void doNotRealignUnrelatedAttributeGroup() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$.resource.name", "db_name")),
          hcl(
            """
              resource "aws_db_instance" "default" {
                name = "mydb"

                a = 1
                longer = 2
              }
              """,
            """
              resource "aws_db_instance" "default" {
                db_name = "mydb"

                a = 1
                longer = 2
              }
              """
          )
        );
    }

    @Test
    void quoteForObjectKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$..Name", "for")),
          hcl(
            """
              locals {
                tags = {
                  Name = "web"
                }
              }
              """,
            """
              locals {
                tags = {
                  "for" = "web"
                }
              }
              """
          )
        );
    }
}
