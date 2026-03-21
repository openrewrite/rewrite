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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

@Issue("https://github.com/moderneinc/customer-requests/issues/1432")
class KotlinParseNestedClassTest implements RewriteTest {

    @Test
    void deeplyNestedClassReferencesInMapOf() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().dependsOn(
            """
              package com.example.proto

              class RouteSpec {
                  class RestEndpoint {
                      enum class RestMethod {
                          GET, POST, PUT, DELETE, PATCH, OPTIONS
                      }
                  }
              }
              """
          )),
          kotlin(
            """
              package com.example.client

              import com.example.proto.RouteSpec

              class RouteSpecClient {
                  companion object {
                      @JvmStatic
                      val methodMap = mapOf<RouteSpec.RestEndpoint.RestMethod, String>(
                          RouteSpec.RestEndpoint.RestMethod.GET to "GET",
                          RouteSpec.RestEndpoint.RestMethod.POST to "POST",
                          RouteSpec.RestEndpoint.RestMethod.PUT to "PUT",
                          RouteSpec.RestEndpoint.RestMethod.DELETE to "DELETE",
                          RouteSpec.RestEndpoint.RestMethod.PATCH to "PATCH",
                          RouteSpec.RestEndpoint.RestMethod.OPTIONS to "OPTIONS",
                      )
                  }
              }
              """
          )
        );
    }

    @Test
    void deeplyNestedClassWithExtensionFunctions() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().dependsOn(
            """
              package com.example.proto

              class RouteSpec {
                  class RestEndpoint {
                      enum class RestMethod {
                          GET, POST, PUT, DELETE, PATCH, OPTIONS
                      }
                      enum class RestVisibility {
                          ADMIN, PUBLIC, PRIVATE
                      }
                      class SecurityClaim(val claim: String, val claimKey: String)
                      class SecurityDemand(val claim: String, val claimKey: String)
                  }
                  class ServiceName(val name: String)
                  class ServiceVersion(val version: String)
                  class ServiceNameAndVersion(val serviceName: ServiceName, val serviceVersion: ServiceVersion)
                  class ServiceRouteSpec
              }
              """,
            """
              package com.example.model

              enum class Visibility {
                  VISIBILITY_ADMIN, VISIBILITY_PUBLIC, VISIBILITY_PRIVATE, VISIBILITY_UNSPECIFIED
              }
              """
          )),
          kotlin(
            """
              package com.example.client

              import com.example.model.Visibility
              import com.example.proto.RouteSpec

              fun RouteSpec.RestEndpoint.SecurityClaim.toBffClaim(): String {
                  return "$claim:$claimKey"
              }

              fun RouteSpec.RestEndpoint.RestVisibility.toBffVisibility(): Visibility {
                  return when (this) {
                      RouteSpec.RestEndpoint.RestVisibility.ADMIN -> Visibility.VISIBILITY_ADMIN
                      RouteSpec.RestEndpoint.RestVisibility.PUBLIC -> Visibility.VISIBILITY_PUBLIC
                      RouteSpec.RestEndpoint.RestVisibility.PRIVATE -> Visibility.VISIBILITY_PRIVATE
                      else -> Visibility.VISIBILITY_UNSPECIFIED
                  }
              }
              """
          )
        );
    }
}
