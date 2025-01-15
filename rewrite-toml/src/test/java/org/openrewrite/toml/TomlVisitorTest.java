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
package org.openrewrite.toml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.*;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.toml.Assertions.toml;

class TomlVisitorTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite-spring/issues/665")
    @Test
    void visitMarkupErrorMarkers() {
        List<RuntimeException> exceptions = new ArrayList<>();
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new TreeVisitor<>() {
              @Override
              public Tree preVisit(Tree tree, ExecutionContext ctx) {
                  // Mimics what we do in the rewrite-gradle-plugin
                  tree.getMarkers().findFirst(Markup.Error.class).ifPresent(e -> {
                      Optional<SourceFile> sourceFile = Optional.ofNullable(getCursor().firstEnclosing(SourceFile.class));
                      String sourcePath = sourceFile.map(SourceFile::getSourcePath).map(Path::toString).orElse("<unknown>");
                      exceptions.add(new RuntimeException("Error while visiting " + sourcePath + ": " + e.getDetail()));
                  });
                  return tree;
              }
          })),
          toml(
            """
              [versions]
              jackson = '2.14.2'
              
              [libraries]
              jackson-annotations = { module = 'com.fasterxml.jackson.core:jackson-annotations', version.ref = 'jackson' }
              jackson-core = { module = 'com.fasterxml.jackson.core:jackson-core', version.ref = 'jackson' }
              
              [bundles]
              jackson = ['jackson-annotations', 'jackson-core']
              """
          )
        );
        assertThat(exceptions).isEmpty();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void traefikExample() {
        rewriteRun(
          toml(
            """
              ## CODE GENERATED AUTOMATICALLY
              ## THIS FILE MUST NOT BE EDITED BY HAND
              [http]
                [http.routers]
                  [http.routers.Router0]
                    entryPoints = ["foobar", "foobar"]
                    middlewares = ["foobar", "foobar"]
                    service = "foobar"
                    rule = "foobar"
                    ruleSyntax = "foobar"
                    priority = 42
                    [http.routers.Router0.tls]
                      options = "foobar"
                      certResolver = "foobar"
              
                      [[http.routers.Router0.tls.domains]]
                        main = "foobar"
                        sans = ["foobar", "foobar"]
              
                      [[http.routers.Router0.tls.domains]]
                        main = "foobar"
                        sans = ["foobar", "foobar"]
                    [http.routers.Router0.observability]
                      accessLogs = true
                      tracing = true
                      metrics = true
                  [http.routers.Router1]
                    entryPoints = ["foobar", "foobar"]
                    middlewares = ["foobar", "foobar"]
                    service = "foobar"
                    rule = "foobar"
                    ruleSyntax = "foobar"
                    priority = 42
                    [http.routers.Router1.tls]
                      options = "foobar"
                      certResolver = "foobar"
              
                      [[http.routers.Router1.tls.domains]]
                        main = "foobar"
                        sans = ["foobar", "foobar"]
              
                      [[http.routers.Router1.tls.domains]]
                        main = "foobar"
                        sans = ["foobar", "foobar"]
                    [http.routers.Router1.observability]
                      accessLogs = true
                      tracing = true
                      metrics = true
                [http.services]
                  [http.services.Service01]
                    [http.services.Service01.failover]
                      service = "foobar"
                      fallback = "foobar"
                      [http.services.Service01.failover.healthCheck]
                  [http.services.Service02]
                    [http.services.Service02.loadBalancer]
                      passHostHeader = true
                      serversTransport = "foobar"
                      [http.services.Service02.loadBalancer.sticky]
                        [http.services.Service02.loadBalancer.sticky.cookie]
                          name = "foobar"
                          secure = true
                          httpOnly = true
                          sameSite = "foobar"
                          maxAge = 42
                          path = "foobar"
              
                      [[http.services.Service02.loadBalancer.servers]]
                        url = "foobar"
                        weight = 42
                        preservePath = true
              
                      [[http.services.Service02.loadBalancer.servers]]
                        url = "foobar"
                        weight = 42
                        preservePath = true
                      [http.services.Service02.loadBalancer.healthCheck]
                        scheme = "foobar"
                        mode = "foobar"
                        path = "foobar"
                        method = "foobar"
                        status = 42
                        port = 42
                        interval = "42s"
                        timeout = "42s"
                        hostname = "foobar"
                        followRedirects = true
                        [http.services.Service02.loadBalancer.healthCheck.headers]
                          name0 = "foobar"
                          name1 = "foobar"
                      [http.services.Service02.loadBalancer.responseForwarding]
                        flushInterval = "42s"
                  [http.services.Service03]
                    [http.services.Service03.mirroring]
                      service = "foobar"
                      mirrorBody = true
                      maxBodySize = 42
              
                      [[http.services.Service03.mirroring.mirrors]]
                        name = "foobar"
                        percent = 42
              
                      [[http.services.Service03.mirroring.mirrors]]
                        name = "foobar"
                        percent = 42
                      [http.services.Service03.mirroring.healthCheck]
                  [http.services.Service04]
                    [http.services.Service04.weighted]
              
                      [[http.services.Service04.weighted.services]]
                        name = "foobar"
                        weight = 42
              
                      [[http.services.Service04.weighted.services]]
                        name = "foobar"
                        weight = 42
                      [http.services.Service04.weighted.sticky]
                        [http.services.Service04.weighted.sticky.cookie]
                          name = "foobar"
                          secure = true
                          httpOnly = true
                          sameSite = "foobar"
                          maxAge = 42
                          path = "foobar"
                      [http.services.Service04.weighted.healthCheck]
                [http.middlewares]
                  [http.middlewares.Middleware01]
                    [http.middlewares.Middleware01.addPrefix]
                      prefix = "foobar"
                  [http.middlewares.Middleware02]
                    [http.middlewares.Middleware02.basicAuth]
                      users = ["foobar", "foobar"]
                      usersFile = "foobar"
                      realm = "foobar"
                      removeHeader = true
                      headerField = "foobar"
                  [http.middlewares.Middleware03]
                    [http.middlewares.Middleware03.buffering]
                      maxRequestBodyBytes = 42
                      memRequestBodyBytes = 42
                      maxResponseBodyBytes = 42
                      memResponseBodyBytes = 42
                      retryExpression = "foobar"
                  [http.middlewares.Middleware04]
                    [http.middlewares.Middleware04.chain]
                      middlewares = ["foobar", "foobar"]
                  [http.middlewares.Middleware05]
                    [http.middlewares.Middleware05.circuitBreaker]
                      expression = "foobar"
                      checkPeriod = "42s"
                      fallbackDuration = "42s"
                      recoveryDuration = "42s"
                      responseCode = 42
                  [http.middlewares.Middleware06]
                    [http.middlewares.Middleware06.compress]
                      excludedContentTypes = ["foobar", "foobar"]
                      includedContentTypes = ["foobar", "foobar"]
                      minResponseBodyBytes = 42
                      encodings = ["foobar", "foobar"]
                      defaultEncoding = "foobar"
                  [http.middlewares.Middleware07]
                    [http.middlewares.Middleware07.contentType]
                      autoDetect = true
                  [http.middlewares.Middleware08]
                    [http.middlewares.Middleware08.digestAuth]
                      users = ["foobar", "foobar"]
                      usersFile = "foobar"
                      removeHeader = true
                      realm = "foobar"
                      headerField = "foobar"
                  [http.middlewares.Middleware09]
                    [http.middlewares.Middleware09.errors]
                      status = ["foobar", "foobar"]
                      service = "foobar"
                      query = "foobar"
                  [http.middlewares.Middleware10]
                    [http.middlewares.Middleware10.forwardAuth]
                      address = "foobar"
                      trustForwardHeader = true
                      authResponseHeaders = ["foobar", "foobar"]
                      authResponseHeadersRegex = "foobar"
                      authRequestHeaders = ["foobar", "foobar"]
                      addAuthCookiesToResponse = ["foobar", "foobar"]
                      headerField = "foobar"
                      forwardBody = true
                      maxBodySize = 42
                      preserveLocationHeader = true
                      [http.middlewares.Middleware10.forwardAuth.tls]
                        ca = "foobar"
                        cert = "foobar"
                        key = "foobar"
                        insecureSkipVerify = true
                        caOptional = true
                  [http.middlewares.Middleware11]
                    [http.middlewares.Middleware11.grpcWeb]
                      allowOrigins = ["foobar", "foobar"]
                  [http.middlewares.Middleware12]
                    [http.middlewares.Middleware12.headers]
                      accessControlAllowCredentials = true
                      accessControlAllowHeaders = ["foobar", "foobar"]
                      accessControlAllowMethods = ["foobar", "foobar"]
                      accessControlAllowOriginList = ["foobar", "foobar"]
                      accessControlAllowOriginListRegex = ["foobar", "foobar"]
                      accessControlExposeHeaders = ["foobar", "foobar"]
                      accessControlMaxAge = 42
                      addVaryHeader = true
                      allowedHosts = ["foobar", "foobar"]
                      hostsProxyHeaders = ["foobar", "foobar"]
                      stsSeconds = 42
                      stsIncludeSubdomains = true
                      stsPreload = true
                      forceSTSHeader = true
                      frameDeny = true
                      customFrameOptionsValue = "foobar"
                      contentTypeNosniff = true
                      browserXssFilter = true
                      customBrowserXSSValue = "foobar"
                      contentSecurityPolicy = "foobar"
                      contentSecurityPolicyReportOnly = "foobar"
                      publicKey = "foobar"
                      referrerPolicy = "foobar"
                      permissionsPolicy = "foobar"
                      isDevelopment = true
                      featurePolicy = "foobar"
                      sslRedirect = true
                      sslTemporaryRedirect = true
                      sslHost = "foobar"
                      sslForceHost = true
                      [http.middlewares.Middleware12.headers.customRequestHeaders]
                        name0 = "foobar"
                        name1 = "foobar"
                      [http.middlewares.Middleware12.headers.customResponseHeaders]
                        name0 = "foobar"
                        name1 = "foobar"
                      [http.middlewares.Middleware12.headers.sslProxyHeaders]
                        name0 = "foobar"
                        name1 = "foobar"
                  [http.middlewares.Middleware13]
                    [http.middlewares.Middleware13.ipAllowList]
                      sourceRange = ["foobar", "foobar"]
                      rejectStatusCode = 42
                      [http.middlewares.Middleware13.ipAllowList.ipStrategy]
                        depth = 42
                        excludedIPs = ["foobar", "foobar"]
                        ipv6Subnet = 42
                  [http.middlewares.Middleware14]
                    [http.middlewares.Middleware14.ipWhiteList]
                      sourceRange = ["foobar", "foobar"]
                      [http.middlewares.Middleware14.ipWhiteList.ipStrategy]
                        depth = 42
                        excludedIPs = ["foobar", "foobar"]
                        ipv6Subnet = 42
                  [http.middlewares.Middleware15]
                    [http.middlewares.Middleware15.inFlightReq]
                      amount = 42
                      [http.middlewares.Middleware15.inFlightReq.sourceCriterion]
                        requestHeaderName = "foobar"
                        requestHost = true
                        [http.middlewares.Middleware15.inFlightReq.sourceCriterion.ipStrategy]
                          depth = 42
                          excludedIPs = ["foobar", "foobar"]
                          ipv6Subnet = 42
                  [http.middlewares.Middleware16]
                    [http.middlewares.Middleware16.passTLSClientCert]
                      pem = true
                      [http.middlewares.Middleware16.passTLSClientCert.info]
                        notAfter = true
                        notBefore = true
                        sans = true
                        serialNumber = true
                        [http.middlewares.Middleware16.passTLSClientCert.info.subject]
                          country = true
                          province = true
                          locality = true
                          organization = true
                          organizationalUnit = true
                          commonName = true
                          serialNumber = true
                          domainComponent = true
                        [http.middlewares.Middleware16.passTLSClientCert.info.issuer]
                          country = true
                          province = true
                          locality = true
                          organization = true
                          commonName = true
                          serialNumber = true
                          domainComponent = true
                  [http.middlewares.Middleware17]
                    [http.middlewares.Middleware17.plugin]
                      [http.middlewares.Middleware17.plugin.PluginConf0]
                        name0 = "foobar"
                        name1 = "foobar"
                      [http.middlewares.Middleware17.plugin.PluginConf1]
                        name0 = "foobar"
                        name1 = "foobar"
                  [http.middlewares.Middleware18]
                    [http.middlewares.Middleware18.rateLimit]
                      average = 42
                      period = "42s"
                      burst = 42
                      [http.middlewares.Middleware18.rateLimit.sourceCriterion]
                        requestHeaderName = "foobar"
                        requestHost = true
                        [http.middlewares.Middleware18.rateLimit.sourceCriterion.ipStrategy]
                          depth = 42
                          excludedIPs = ["foobar", "foobar"]
                          ipv6Subnet = 42
                  [http.middlewares.Middleware19]
                    [http.middlewares.Middleware19.redirectRegex]
                      regex = "foobar"
                      replacement = "foobar"
                      permanent = true
                  [http.middlewares.Middleware20]
                    [http.middlewares.Middleware20.redirectScheme]
                      scheme = "foobar"
                      port = "foobar"
                      permanent = true
                  [http.middlewares.Middleware21]
                    [http.middlewares.Middleware21.replacePath]
                      path = "foobar"
                  [http.middlewares.Middleware22]
                    [http.middlewares.Middleware22.replacePathRegex]
                      regex = "foobar"
                      replacement = "foobar"
                  [http.middlewares.Middleware23]
                    [http.middlewares.Middleware23.retry]
                      attempts = 42
                      initialInterval = "42s"
                  [http.middlewares.Middleware24]
                    [http.middlewares.Middleware24.stripPrefix]
                      prefixes = ["foobar", "foobar"]
                      forceSlash = true
                  [http.middlewares.Middleware25]
                    [http.middlewares.Middleware25.stripPrefixRegex]
                      regex = ["foobar", "foobar"]
              """
          )
        );
    }
}
