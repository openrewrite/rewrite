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

class ChangeValueTest implements RewriteTest {

    @DocumentExample
    @Test
    void changeStringValueInBlock() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.resource.aws_db_instance.engine_version", "\"8.0\"", null, null)),
          hcl(
            """
              resource "aws_db_instance" "default" {
                engine         = "mysql"
                engine_version = "5.7"
              }
              """,
            """
              resource "aws_db_instance" "default" {
                engine         = "mysql"
                engine_version = "8.0"
              }
              """
          )
        );
    }

    @Test
    void changeValueInBlocksSelectedByType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.resource.aws_db_instance.engine_version", "\"8.0\"", null, null)),
          hcl(
            """
              resource "aws_db_instance" "primary" {
                engine_version = "5.7"
              }
              resource "aws_db_instance" "replica" {
                engine_version = "5.7"
              }
              resource "aws_rds_cluster" "default" {
                engine_version = "5.7"
              }
              """,
            """
              resource "aws_db_instance" "primary" {
                engine_version = "8.0"
              }
              resource "aws_db_instance" "replica" {
                engine_version = "8.0"
              }
              resource "aws_rds_cluster" "default" {
                engine_version = "5.7"
              }
              """
          )
        );
    }

    @Test
    void changeValueInBlockSelectedByLabels() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.resource.aws_db_instance.replica.multi_az", "true", null, null)),
          hcl(
            """
              resource "aws_db_instance" "primary" {
                multi_az = false
              }
              resource "aws_db_instance" "replica" {
                multi_az = false
              }
              """,
            """
              resource "aws_db_instance" "primary" {
                multi_az = false
              }
              resource "aws_db_instance" "replica" {
                multi_az = true
              }
              """
          )
        );
    }

    @Test
    void changeValueInNestedBlock() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.provider.features.key_vault.purge_soft_delete_on_destroy", "false", null, null)),
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
                    purge_soft_delete_on_destroy = false
                  }
                }
                purge_soft_delete_on_destroy = true
              }
              """
          )
        );
    }

    @Test
    void changeAllMatchesWithRecursiveDescent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$..encrypted", "true", null, null)),
          hcl(
            """
              resource "aws_ebs_volume" "data" {
                encrypted = false
              }
              resource "aws_launch_configuration" "web" {
                root_block_device {
                  encrypted = "false"
                }
              }
              """,
            """
              resource "aws_ebs_volume" "data" {
                encrypted = true
              }
              resource "aws_launch_configuration" "web" {
                root_block_device {
                  encrypted = true
                }
              }
              """
          )
        );
    }

    @Test
    void changeValueInBlocksSelectedByFilter() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.resource.aws_db_instance[?(@.engine == 'mysql')].engine_version", "\"8.0\"", null, null)),
          hcl(
            """
              resource "aws_db_instance" "primary" {
                engine         = "mysql"
                engine_version = "5.7"
              }
              resource "aws_db_instance" "analytics" {
                engine         = "postgres"
                engine_version = "13"
              }
              """,
            """
              resource "aws_db_instance" "primary" {
                engine         = "mysql"
                engine_version = "8.0"
              }
              resource "aws_db_instance" "analytics" {
                engine         = "postgres"
                engine_version = "13"
              }
              """
          )
        );
    }

    @Test
    void changeTopLevelAttributeByRootPath() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.region", "\"eu-west-1\"", null, null)),
          hcl(
            """
              region = "us-east-1"
              """,
            """
              region = "eu-west-1"
              """
          )
        );
    }

    @Test
    void changeTopLevelAttribute() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$..region", "\"eu-west-1\"", null, null)),
          hcl(
            """
              region = "us-east-1"
              """,
            """
              region = "eu-west-1"
              """
          )
        );
    }

    @Test
    void changeObjectKeyValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$..Env", "\"staging\"", null, null)),
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
                  Name = "web"
                  Env  = "staging"
                }
              }
              """
          )
        );
    }

    @Test
    void changeQuotedObjectKeyValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$..['kubernetes.io/role/elb']", "0", null, null)),
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
                  "kubernetes.io/role/elb" = 0
                }
              }
              """
          )
        );
    }

    @Test
    void changeValueToExpression() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.resource.instance_type", "var.instance_type", null, null)),
          hcl(
            """
              resource "aws_instance" "web" {
                instance_type = "t2.micro"
              }
              """,
            """
              resource "aws_instance" "web" {
                instance_type = var.instance_type
              }
              """
          )
        );
    }

    @Test
    void changeValueToList() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.resource.enabled_cloudwatch_logs_exports", "[\"audit\", \"error\"]", null, null)),
          hcl(
            """
              resource "aws_db_instance" "default" {
                enabled_cloudwatch_logs_exports = []
              }
              """,
            """
              resource "aws_db_instance" "default" {
                enabled_cloudwatch_logs_exports = ["audit", "error"]
              }
              """
          )
        );
    }

    @Test
    void changeValueToMultilineObject() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.resource.tags", "{\nName = \"web\"\nEnv = \"prod\"\n}", null, null)),
          hcl(
            """
              resource "aws_instance" "web" {
                tags = {}
              }
              """,
            """
              resource "aws_instance" "web" {
                tags = {
                  Name = "web"
                  Env  = "prod"
                }
              }
              """
          )
        );
    }

    @Test
    void changeValueToHeredoc() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.resource.user_data", "<<EOT\n#!/bin/bash\necho hello\nEOT", null, null)),
          hcl(
            """
              resource "aws_instance" "web" {
                user_data = ""
              }
              """,
            """
              resource "aws_instance" "web" {
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
    void preserveTrailingComment() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.resource.engine_version", "\"8.0\"", null, null)),
          hcl(
            """
              resource "aws_db_instance" "default" {
                engine_version = "5.7" # pinned
              }
              """,
            """
              resource "aws_db_instance" "default" {
                engine_version = "8.0" # pinned
              }
              """
          )
        );
    }

    @Test
    void changeValueOfComputedObjectKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$..['(var.key)']", "\"new\"", null, null)),
          hcl(
            """
              locals {
                tags = {
                  (var.key) = "old"
                }
              }
              """,
            """
              locals {
                tags = {
                  (var.key) = "new"
                }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenValueIsTheSame() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.resource.engine_version", "\"8.0\"", null, null)),
          hcl(
            """
              resource "aws_db_instance" "default" {
                engine_version = "8.0"
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenPathDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.resource.engine_version", "\"8.0\"", null, null)),
          hcl(
            """
              data "aws_db_instance" "default" {
                engine_version = "5.7"
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenPathMatchesBlock() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.resource.lifecycle", "true", null, null)),
          hcl(
            """
              resource "aws_instance" "web" {
                lifecycle {
                  create_before_destroy = true
                }
              }
              """
          )
        );
    }

    @Test
    void changeOnlyValuesEqualToOldValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.resource.aws_db_instance.engine_version", "\"8.0\"", "5.7", null)),
          hcl(
            """
              resource "aws_db_instance" "old" {
                engine_version = "5.7"
              }
              resource "aws_db_instance" "patched" {
                engine_version = "5.7.44"
              }
              resource "aws_db_instance" "new" {
                engine_version = "8.4"
              }
              """,
            """
              resource "aws_db_instance" "old" {
                engine_version = "8.0"
              }
              resource "aws_db_instance" "patched" {
                engine_version = "5.7.44"
              }
              resource "aws_db_instance" "new" {
                engine_version = "8.4"
              }
              """
          )
        );
    }

    @Test
    void oldValueMatchesQuotedAndUnquotedValues() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$..port", "5432", "3306", null)),
          hcl(
            """
              resource "aws_db_instance" "a" {
                port = 3306
              }
              resource "aws_db_instance" "b" {
                port = "3306"
              }
              """,
            """
              resource "aws_db_instance" "a" {
                port = 5432
              }
              resource "aws_db_instance" "b" {
                port = 5432
              }
              """
          )
        );
    }

    @Test
    void oldValueMatchesBooleans() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$..multi_az", "true", "false", null)),
          hcl(
            """
              resource "aws_db_instance" "default" {
                multi_az = false
              }
              """,
            """
              resource "aws_db_instance" "default" {
                multi_az = true
              }
              """
          )
        );
    }

    @Test
    void regexReplacesWholeMatchedValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$..engine_version", "8.0", "^5\\.7(\\..*)?$", true)),
          hcl(
            """
              resource "aws_db_instance" "a" {
                engine_version = "5.7.44"
              }
              resource "aws_db_instance" "b" {
                engine_version = "8.4"
              }
              """,
            """
              resource "aws_db_instance" "a" {
                engine_version = "8.0"
              }
              resource "aws_db_instance" "b" {
                engine_version = "8.4"
              }
              """
          )
        );
    }

    @Test
    void regexReplacesMatchingPartsWithCapturingGroups() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$..instance_class", "db.t3.$1", "db\\.t2\\.(\\w+)", true)),
          hcl(
            """
              resource "aws_db_instance" "default" {
                instance_class = "db.t2.micro"
              }
              """,
            """
              resource "aws_db_instance" "default" {
                instance_class = "db.t3.micro"
              }
              """
          )
        );
    }

    @Test
    void regexKeepsUnquotedValuesUnquoted() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$..backup_retention_period", "7", "^[0-6]$", true)),
          hcl(
            """
              resource "aws_db_instance" "a" {
                backup_retention_period = 1
              }
              resource "aws_db_instance" "b" {
                backup_retention_period = 14
              }
              """,
            """
              resource "aws_db_instance" "a" {
                backup_retention_period = 7
              }
              resource "aws_db_instance" "b" {
                backup_retention_period = 14
              }
              """
          )
        );
    }

    @Test
    void oldValueNeverMatchesReferencesOrInterpolations() {
        rewriteRun(
          spec -> spec.recipes(
            new ChangeValue("$..engine_version", "\"8.0\"", "var.engine_version", null),
            new ChangeValue("$..engine_version", "8.0", ".*", true)
          ),
          hcl(
            """
              resource "aws_db_instance" "a" {
                engine_version = var.engine_version
              }
              resource "aws_db_instance" "b" {
                engine_version = "${var.major}.7"
              }
              resource "aws_db_instance" "c" {
                engine_version = <<EOT
              5.7
              EOT
              }
              """
          )
        );
    }

    @Test
    void changeValueOfRenamedKey() {
        rewriteRun(
          spec -> spec.recipes(
            new ChangeKey("$.resource.name", "db_name"),
            new ChangeValue("$.resource.db_name", "\"newdb\"", null, null)
          ),
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
                db_name = "newdb"
              }
              """
          )
        );
    }

    @Test
    void declarativeRecipe() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            //language=yaml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: com.example.UpgradeDbInstance
              displayName: Upgrade DB instance
              description: Rename `name` to `db_name` and upgrade the engine.
              recipeList:
                - org.openrewrite.hcl.ChangeKey:
                    oldKeyPath: $.resource.name
                    newKey: db_name
                - org.openrewrite.hcl.ChangeValue:
                    keyPath: $.resource.engine_version
                    value: '"8.0"'
                    oldValue: '5.7'
                - org.openrewrite.hcl.ChangeValue:
                    keyPath: $.resource.storage_encrypted
                    value: true
              """,
            "com.example.UpgradeDbInstance"
          ),
          hcl(
            """
              resource "aws_db_instance" "default" {
                name              = "mydb"
                engine_version    = "5.7"
                storage_encrypted = false
              }
              """,
            """
              resource "aws_db_instance" "default" {
                db_name           = "mydb"
                engine_version    = "8.0"
                storage_encrypted = true
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\"unterminated", "1\nother = 2", "1 2"})
    void invalidValue(String value) {
        // when
        var validated = new ChangeValue("$.resource.engine_version", value, null, null).validate();

        // then
        assertThat(validated.isInvalid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "5.7["})
    void invalidRegex(String oldValue) {
        // when
        var validated = new ChangeValue("$.resource.engine_version", "8.0", oldValue, true).validate();

        // then
        assertThat(validated.isInvalid()).isTrue();
    }

    @Test
    void regexValueIsReplacementNotHclExpression() {
        // when
        var validated = new ChangeValue("$.resource.engine_version", "8.0.$1", "5\\.7\\.(\\d+)", true).validate();

        // then
        assertThat(validated.isValid()).isTrue();
    }
}
