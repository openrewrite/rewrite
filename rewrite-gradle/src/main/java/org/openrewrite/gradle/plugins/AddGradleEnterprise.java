/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle.plugins;

import org.openrewrite.Incubating;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

@Incubating(since = "7.33.0")
public class AddGradleEnterprise extends Recipe {

    @Option(displayName = "Plugin version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "3.x")
    private final String version;

    public AddGradleEnterprise(String version) {
        this.version = version;
        doNext(new AddSettingsPlugin("com.gradle.enterprise", version, null));
        doNext(new UpgradePluginVersion("com.gradle.enterprise", version, null));
    }

    @Override
    public String getDisplayName() {
        return "Add the Gradle Enterprise plugin";
    }

    @Override
    public String getDescription() {
        return "Add the Gradle Enterprise plugin to `settings.gradle(.kts)`.";
    }
}
