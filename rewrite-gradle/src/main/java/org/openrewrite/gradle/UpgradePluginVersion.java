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
package org.openrewrite.gradle;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.CheckedFunction1;
import lombok.EqualsAndHashCode;
import lombok.Value;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradePluginVersion extends Recipe {
    private static final RetryConfig retryConfig = RetryConfig.custom()
            .retryOnException(throwable -> throwable instanceof SocketTimeoutException ||
                    throwable instanceof TimeoutException)
            .build();

    private static final RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
            .build();

    private static final Retry gradlePluginPortalRetry = retryRegistry.retry("GradlePluginPortal");

    private static final CheckedFunction1<Request, Response> sendRequest = Retry.decorateCheckedFunction(
            gradlePluginPortalRetry,
            request -> httpClient.newCall(request).execute());

    @Option(displayName = "Plugin id",
            description = "The `ID` part of `plugin { ID }`, as a glob expression.",
            example = "com.jfrog.bintray")
    String pluginIdPattern;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X")
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Override
    public String getDisplayName() {
        return "Update a Gradle plugin by id";
    }

    @Override
    public String getDescription() {
        return "Update a Gradle plugin by id to a later version.";
    }

    @Override
    public Validated validate() {
        Validated validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        return validated;
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new IsBuildGradle<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator versionComparator = Semver.validate(newVersion, versionPattern).getValue();
        assert versionComparator != null;

        MethodMatcher pluginMatcher = new MethodMatcher("PluginSpec id(..)", false);
        MethodMatcher versionMatcher = new MethodMatcher("Plugin version(..)", false);
        return new GroovyVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                if (versionMatcher.matches(method) &&
                        method.getSelect() instanceof J.MethodInvocation &&
                        pluginMatcher.matches(method.getSelect())) {
                    List<Expression> pluginArgs = ((J.MethodInvocation) method.getSelect()).getArguments();
                    if (pluginArgs.get(0) instanceof J.Literal) {
                        String pluginId = (String) ((J.Literal) pluginArgs.get(0)).getValue();
                        assert pluginId != null;
                        if (StringUtils.matchesGlob(pluginId, pluginIdPattern)) {
                            List<Expression> versionArgs = method.getArguments();
                            if (versionArgs.get(0) instanceof J.Literal) {
                                String currentVersion = (String) ((J.Literal) versionArgs.get(0)).getValue();
                                if (currentVersion != null) {
                                    return versionComparator.upgrade(currentVersion, availablePluginVersions(pluginId))
                                            .map(upgradeVersion -> method.withArguments(ListUtils.map(versionArgs, v -> {
                                                J.Literal versionLiteral = (J.Literal) v;
                                                assert versionLiteral.getValueSource() != null;
                                                return versionLiteral
                                                        .withValue(upgradeVersion)
                                                        .withValueSource(versionLiteral.getValueSource().replace(currentVersion, upgradeVersion));
                                            })))
                                            .orElse(method);
                                }
                            }
                        }
                    }
                }
                return super.visitMethodInvocation(method, executionContext);
            }
        };
    }

    public static List<String> availablePluginVersions(String pluginId) {
        String uri = "https://plugins.gradle.org/plugin/" + pluginId;

        Request.Builder request = new Request.Builder().url(uri).get();
        try (Response response = sendRequest.apply(request.build())) {
            if (response.isSuccessful() && response.body() != null) {
                @SuppressWarnings("ConstantConditions")
                @Language("xml")
                String responseBody = response.body().string();

                List<String> versions = new ArrayList<>();
                Matcher matcher = Pattern.compile("href=\"/plugin/" + pluginId + "/([^\"]+)\"").matcher(responseBody);
                int lastFind = 0;
                while (matcher.find(lastFind)) {
                    versions.add(matcher.group(1));
                    lastFind = matcher.end();
                }

                matcher = Pattern.compile("Version ([^\\s]+) \\(latest\\)").matcher(responseBody);
                if (matcher.find()) {
                    versions.add(matcher.group(1));
                }

                return versions;
            }
        } catch (Throwable throwable) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
