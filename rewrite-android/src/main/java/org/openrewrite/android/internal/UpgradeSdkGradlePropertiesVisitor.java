/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.android.internal;

import org.openrewrite.ExecutionContext;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;

import java.util.Set;

/**
 * Updates entries in {@code gradle.properties} (or any sibling
 * {@code *.properties} file in the project) whose keys back an SDK assignment
 * via {@code providers.gradleProperty("key").get().toInt()} or similar.
 */
public class UpgradeSdkGradlePropertiesVisitor extends PropertiesVisitor<ExecutionContext> {

    private final Set<String> propertyKeys;
    private final int newValue;

    public UpgradeSdkGradlePropertiesVisitor(Set<String> propertyKeys, int newValue) {
        this.propertyKeys = propertyKeys;
        this.newValue = newValue;
    }

    @Override
    public Properties visitFile(Properties.File file, ExecutionContext ctx) {
        if (!file.getSourcePath().toString().endsWith("gradle.properties")) {
            return file;
        }
        return super.visitFile(file, ctx);
    }

    @Override
    public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
        if (!propertyKeys.contains(entry.getKey())) {
            return entry;
        }
        String currentText = entry.getValue().getText();
        Integer current = parseIntOrNull(currentText);
        if (current != null && current >= newValue) {
            return entry;
        }
        return entry.withValue(entry.getValue().withText(String.valueOf(newValue)));
    }

    private static @org.jspecify.annotations.Nullable Integer parseIntOrNull(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
