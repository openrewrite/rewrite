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
package org.openrewrite.maven.internal.engine;

import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.management.PluginManagementInjector;

/**
 * Stock Maven 3.9 {@link DefaultModelBuilderFactory} except for a no-op {@link PluginManagementInjector}: rewrite's
 * output contract keeps {@code <pluginManagement>} separate from the effective {@code <plugins>} list (DESIGN §4.1), so
 * management must not be folded into plugins. Every other pipeline component is wired by the stock factory via plain
 * {@code new} (no DI container).
 */
public class EngineModelBuilderFactory extends DefaultModelBuilderFactory {

    @Override
    protected PluginManagementInjector newPluginManagementInjector() {
        // Leave build/pluginManagement and build/plugins as authored; DependencyGraphMapper (B2) reads them separately.
        return (model, request, problems) -> {
        };
    }
}
