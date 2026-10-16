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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsYamlDumper;

import java.util.Map;

/**
 * Port of @pnpm/lockfile.fs src/write.ts (pnpm 11.2.2): {@code writeWantedLockfile}'s text, the whole file
 * rendered from the lock, behind the env document the existing file starts with.
 */
final class LockfileWriter {

    private LockfileWriter() {
    }

    /**
     * The text {@code writeWantedLockfile} writes over {@code existing} (the file as it was, null for none).
     */
    static String writeWantedLockfile(LockfileObject wantedLockfile, @Nullable String existing) {
        String yamlDoc = yamlStringify(LockfileFormatConverters.convertToLockfileFile(wantedLockfile));
        String envDoc = LockfileReader.readFirstYamlDocument(existing);
        String envPrefix = envDoc != null ?
                LockfileReader.YAML_DOCUMENT_START + envDoc + LockfileReader.YAML_DOCUMENT_SEPARATOR : "";
        return envPrefix + yamlDoc;
    }

    static String yamlStringify(Map<String, @Nullable Object> lockfile) {
        return lockfileYamlDump(SortLockfileKeys.sortLockfileKeys(lockfile));
    }

    static String lockfileYamlDump(Object obj) {
        return JsYamlDumper.dump(obj);
    }

    static boolean isEmptyLockfile(LockfileObject lockfile) {
        for (ProjectSnapshot importer : lockfile.getImporters().values()) {
            if (!importer.getSpecifiers().isEmpty() ||
                importer.getDependencies() != null && !importer.getDependencies().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
