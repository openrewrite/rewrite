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
package org.openrewrite.maven;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.tree.MavenRepositoryCredentials;
import org.openrewrite.maven.tree.MavenRepositoryMirror;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.internal.StringUtils.isNullOrEmpty;
import static org.openrewrite.maven.tree.MavenRepository.MAVEN_CENTRAL;
import static org.openrewrite.maven.tree.MavenRepository.MAVEN_LOCAL_DEFAULT;

/**
 * Loads {@code ~/.m2/settings.xml} (and {@code $M2_HOME/conf/settings.xml}) into the
 * default {@link ExecutionContext} for every {@link RewriteTest} in this module, so a
 * configured Maven mirror or repository is honored by recipes that resolve artifacts.
 * Auto-registered via {@code META-INF/services/org.junit.jupiter.api.extension.Extension}.
 * Tests that construct their own {@link ExecutionContext} and bypass {@code rewriteRun()}
 * should opt in explicitly via {@link MavenSettings#readMavenSettingsFromDisk(ExecutionContext)}.
 */
public class MavenSettingsAutoLoadingExtension implements BeforeAllCallback {

    private static final String MIRROR_ID = "rewrite-mirror";

    @Override
    public void beforeAll(ExtensionContext context) {
        RewriteTest.defaultExecutionContextCustomizers.putIfAbsent(
                MavenSettingsAutoLoadingExtension.class,
                MavenSettingsAutoLoadingExtension::loadMavenSettings);
    }

    private static void loadMavenSettings(ExecutionContext ctx) {
        MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
        boolean nothingConfigured = mctx.getSettings() == null &&
                                    mctx.getLocalRepository().equals(MAVEN_LOCAL_DEFAULT) &&
                                    mctx.getRepositories().isEmpty() &&
                                    mctx.getActiveProfiles().isEmpty() &&
                                    mctx.getMirrors().isEmpty();
        if (nothingConfigured) {
            mctx.setMavenSettings(MavenSettings.readMavenSettingsFromDisk(mctx));
            withMavenCentralMirror(mctx);
        }
    }

    /**
     * Adds the mirror of Maven Central that MAVEN_CENTRAL_MIRROR_URL, _USERNAME and _PASSWORD name, if they are set
     * and nothing mirrors Central yet. For tests that set up their own context, which this extension leaves alone.
     */
    public static <T extends ExecutionContext> T withMavenCentralMirror(T ctx) {
        MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
        String url = System.getenv("MAVEN_CENTRAL_MIRROR_URL");
        String username = System.getenv("MAVEN_CENTRAL_MIRROR_USERNAME");
        String password = System.getenv("MAVEN_CENTRAL_MIRROR_PASSWORD");
        MavenSettings settings = mctx.getSettings();
        if (isNullOrEmpty(url) || isNullOrEmpty(username) || isNullOrEmpty(password) ||
            mctx.getMirrors(settings).stream().anyMatch(mirror -> mirror.matches(MAVEN_CENTRAL))) {
            return ctx;
        }
        if (settings != null) {
            // Where there are settings, the downloader takes its mirrors from them
            mctx.setMavenSettings(settings.merge(new MavenSettings(null, null, null,
                    new MavenSettings.Mirrors(singletonList(new MavenSettings.Mirror(MIRROR_ID, url, "central", null, null))),
                    new MavenSettings.Servers(singletonList(new MavenSettings.Server(MIRROR_ID, username, password, null))))));
        } else {
            // Beside the repositories and local repository the test set, which settings would replace
            List<MavenRepositoryMirror> mirrors = new ArrayList<>(mctx.getMirrors());
            mirrors.add(new MavenRepositoryMirror(MIRROR_ID, url, "central", null, null, null));
            mctx.setMirrors(mirrors);
            List<MavenRepositoryCredentials> credentials = new ArrayList<>(mctx.getCredentials());
            credentials.add(new MavenRepositoryCredentials(MIRROR_ID, username, password));
            mctx.setCredentials(credentials);
        }
        return ctx;
    }
}
