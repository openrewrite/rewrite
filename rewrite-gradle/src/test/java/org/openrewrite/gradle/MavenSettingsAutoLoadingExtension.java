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
package org.openrewrite.gradle;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.tree.MavenRepository.MAVEN_LOCAL_DEFAULT;

/**
 * Loads {@code ~/.m2/settings.xml} (and {@code $M2_HOME/conf/settings.xml}) into the
 * default {@link ExecutionContext} for every {@link RewriteTest} in this module, so a
 * configured Maven mirror or repository is honored by recipes that resolve artifacts.
 * Auto-registered via {@code META-INF/services/org.junit.jupiter.api.extension.Extension};
 * Gradle does not normally read the user's Maven settings, so this is opt-in at the
 * module test classpath level.
 */
public class MavenSettingsAutoLoadingExtension implements BeforeAllCallback {

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
        }
    }
}
