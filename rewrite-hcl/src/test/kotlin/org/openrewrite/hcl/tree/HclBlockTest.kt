/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.hcl.tree

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.hcl.Assertions.hcl
import org.openrewrite.test.RewriteTest

class HclBlockTest : RewriteTest {

    /**
     * Doesn't seem to be documented in HCL spec, but in use in plenty of places in terragoat.
     */
    @Test
    fun blockExpression() = rewriteRun(
        hcl(
            """
              tags = {
                git_file = "terraform/aws/ec2.tf"
                git_repo = "terragoat"
              }
            """
        )
    )

    @Test
    fun blockUnquotedLabel() = rewriteRun(
        hcl(
            """
                resource azurerm_monitor_log_profile "logging_profile" {
                  device_name = "/dev/sdh"
                }
            """
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1506")
    @Test
    fun binaryOperator() = rewriteRun(
        hcl(
            """
               create_vnic_details {
                 assign_public_ip = (var.instance_visibility == "Private") ? false : true
               }
            """
        )
    )

    @Test
    fun block() = rewriteRun(
        hcl(
            """
                resource "aws_volume_attachment" "ebs_att" {
                  device_name = "/dev/sdh"
                  volume_id   = "aws_ebs_volume.web_host_storage.id"
                  instance_id = "aws_instance.web_host.id"
                }
                
                resource "aws_route_table_association" "rtbassoc2" {
                  subnet_id      = aws_subnet.web_subnet2.id
                  route_table_id = aws_route_table.web_rtb.id
                }
            """
        )
    )

    @Test
    fun oneLineBlock() = rewriteRun(
        hcl(
            """
                resource "aws_volume_attachment" "ebs_att" { device_name = "/dev/sdh" }
            """
        )
    )
}
