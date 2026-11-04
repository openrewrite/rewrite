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
import java.util.function.UnaryOperator;

import static java.util.Collections.singletonList;
import static org.openrewrite.internal.StringUtils.isNullOrEmpty;
import static org.openrewrite.maven.tree.MavenRepository.MAVEN_CENTRAL;

/**
 * Sends what recipes download from Maven Central with {@link RewriteTest}'s default {@link ExecutionContext} to the
 * mirror that MAVEN_CENTRAL_MIRROR_URL, _USERNAME and _PASSWORD name, when all three are set. Registered through
 * {@code META-INF/services}, so a test JVM with {@code junit.jupiter.extensions.autodetection.enabled=true} uses it.
 */
public class MavenCentralMirrorExtension implements BeforeAllCallback {

    private static final String MIRROR_ID = "rewrite-mirror";

    @Override
    public void beforeAll(ExtensionContext context) {
        RewriteTest.defaultExecutionContextCustomizers.putIfAbsent(
                MavenCentralMirrorExtension.class,
                MavenCentralMirrorExtension::withMavenCentralMirror);
    }

    /**
     * Adds the mirror of Maven Central that the variables name, if they are set and nothing mirrors Central yet.
     * For tests that set up their own context, which this extension leaves alone.
     */
    public static <T extends ExecutionContext> T withMavenCentralMirror(T ctx) {
        return withMavenCentralMirror(ctx, System::getenv);
    }

    static <T extends ExecutionContext> T withMavenCentralMirror(T ctx, UnaryOperator<String> env) {
        MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
        String url = env.apply("MAVEN_CENTRAL_MIRROR_URL");
        String username = env.apply("MAVEN_CENTRAL_MIRROR_USERNAME");
        String password = env.apply("MAVEN_CENTRAL_MIRROR_PASSWORD");
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
