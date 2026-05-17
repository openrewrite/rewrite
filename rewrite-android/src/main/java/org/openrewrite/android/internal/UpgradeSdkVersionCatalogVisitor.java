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
package org.openrewrite.android.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlKey;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Updates entries in {@code gradle/libs.versions.toml} (or any project-local
 * version catalog) whose keys back an SDK assignment via
 * {@code libs.versions.X.get().toInt()}.
 * <p>
 * Restricted to the {@code [versions]} table. Accepts both kebab-case and
 * camelCase key variants since Gradle normalises catalog accessors.
 */
public class UpgradeSdkVersionCatalogVisitor extends TomlIsoVisitor<ExecutionContext> {

    private final Set<String> catalogKeyCandidates;
    private final int newValue;
    private boolean inVersionsTable;

    public UpgradeSdkVersionCatalogVisitor(Set<String> rawCatalogKeys, int newValue) {
        this.catalogKeyCandidates = new HashSet<>();
        for (String raw : rawCatalogKeys) {
            List<String> variants = SdkVersionValueSourceResolver.versionCatalogTomlKeyCandidates(raw);
            this.catalogKeyCandidates.addAll(variants);
        }
        this.newValue = newValue;
    }

    @Override
    public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
        if (!document.getSourcePath().toString().endsWith("libs.versions.toml")) {
            return document;
        }
        return super.visitDocument(document, ctx);
    }

    @Override
    public Toml.Table visitTable(Toml.Table table, ExecutionContext ctx) {
        boolean entered = false;
        Toml.Identifier name = table.getName();
        if (name != null && "versions".equals(name.getName())) {
            inVersionsTable = true;
            entered = true;
        }
        try {
            return super.visitTable(table, ctx);
        } finally {
            if (entered) {
                inVersionsTable = false;
            }
        }
    }

    @Override
    public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, ExecutionContext ctx) {
        if (!inVersionsTable) {
            return keyValue;
        }
        TomlKey key = keyValue.getKey();
        if (!(key instanceof Toml.Identifier)) {
            return keyValue;
        }
        String keyName = ((Toml.Identifier) key).getName();
        if (!catalogKeyCandidates.contains(keyName)) {
            return keyValue;
        }
        if (!(keyValue.getValue() instanceof Toml.Literal)) {
            return keyValue;
        }
        Toml.Literal lit = (Toml.Literal) keyValue.getValue();
        Integer current = parseIntFromValue(lit);
        if (current != null && current >= newValue) {
            return keyValue;
        }
        // Preserve quoting: catalog versions are typically quoted strings.
        String existingSource = lit.getSource();
        String newSource;
        String quote = pickQuote(existingSource);
        if (quote == null) {
            // Was a bare int — keep bare.
            newSource = String.valueOf(newValue);
            return keyValue.withValue(lit.withSource(newSource).withValue((long) newValue));
        }
        newSource = quote + newValue + quote;
        return keyValue.withValue(lit.withSource(newSource).withValue(String.valueOf(newValue)));
    }

    private static @Nullable String pickQuote(@Nullable String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        char c = source.charAt(0);
        if (c == '"' || c == '\'') {
            return String.valueOf(c);
        }
        return null;
    }

    private static @Nullable Integer parseIntFromValue(Toml.Literal lit) {
        Object v = lit.getValue();
        if (v instanceof Long) {
            return ((Long) v).intValue();
        }
        if (v instanceof Integer) {
            return (Integer) v;
        }
        if (v instanceof String) {
            try {
                return Integer.parseInt(((String) v).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
