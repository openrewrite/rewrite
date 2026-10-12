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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.JsYaml;
import org.openrewrite.javascript.internal.lock.StripBom;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Port of @pnpm/lockfile.types {@code EnvLockfile} and @pnpm/lockfile.fs src/envLockfile.ts (pnpm 11.2.2): the
 * document at the top of pnpm-lock.yaml that holds the config dependencies and pnpm's own release.
 */
final class EnvLockfile {

    /**
     * The document as loaded, every key kept.
     */
    @Getter
    private final Map<String, @Nullable Object> document;

    private EnvLockfile(Map<String, @Nullable Object> document) {
        this.document = document;
    }

    /**
     * The pnpm-lock.yaml an install reads and writes: its env document parsed at the first read, then what each
     * {@code writeEnvLockfile} leaves.
     */
    static final class OnDisk {
        private final @Nullable String text;
        private final PnpmRelease release;
        private @Nullable String envDocument;
        private boolean envWritten;
        private boolean read;
        private @Nullable EnvLockfile envLockfile;

        OnDisk(@Nullable String text) {
            this(text, PnpmRelease.PORTED);
        }

        /**
         * The lock file as {@code release}, the one that runs, reads and writes it.
         */
        OnDisk(@Nullable String text, PnpmRelease release) {
            this.text = text;
            this.release = release;
            this.envDocument = LockfileReader.readFirstYamlDocument(text, release);
        }

        @Nullable
        EnvLockfile readEnvLockfile() {
            if (!read) {
                envLockfile = parse(envDocument);
                read = true;
            }
            return envLockfile;
        }

        void writeEnvLockfile(EnvLockfile lockfile) {
            envDocument = LockfileWriter.lockfileYamlDump(SortLockfileKeys.sortLockfileKeys(lockfile.document));
            envWritten = true;
            envLockfile = lockfile;
            read = true;
        }

        /**
         * The file as it is now; null when there is none.
         */
        @Nullable
        String text() {
            if (!envWritten) {
                return text;
            }
            return LockfileReader.YAML_DOCUMENT_START + envDocument + LockfileReader.YAML_DOCUMENT_SEPARATOR +
                   (text == null ? "" : LockfileReader.extractMainDocument(text, release));
        }

        /**
         * {@code writeWantedLockfile} as {@code release}, the one that installs, writes it: the file with
         * {@code wantedLockfile} as its main document; from 11.14.0 the file as it is where it reads as that already,
         * its BOM stripped and its CRLF read as LF ({@link PnpmRelease#leavesAnUnchangedLockAsItIs}), asked only there.
         */
        String writeWantedLockfile(LockfileObject wantedLockfile, PnpmRelease release) {
            String existing = text();
            String content = LockfileWriter.writeWantedLockfile(wantedLockfile, existing);
            if (existing != null && !existing.equals(content) &&
                StripBom.stripBom(existing).replace("\r\n", "\n").equals(content) &&
                release.leavesAnUnchangedLockAsItIs()) {
                return existing;
            }
            return content;
        }
    }

    static EnvLockfile createEnvLockfile() {
        Map<String, @Nullable Object> document = new LinkedHashMap<>();
        document.put("lockfileVersion", LockfileObject.LOCKFILE_VERSION);
        Map<String, @Nullable Object> importer = new LinkedHashMap<>();
        importer.put("configDependencies", new LinkedHashMap<String, @Nullable Object>());
        Map<String, @Nullable Object> importers = new LinkedHashMap<>();
        importers.put(".", importer);
        document.put("importers", importers);
        document.put("packages", new LinkedHashMap<String, @Nullable Object>());
        document.put("snapshots", new LinkedHashMap<String, @Nullable Object>());
        return new EnvLockfile(document);
    }

    /**
     * {@code readEnvLockfile} of the file's text: null without an env document or with one of another shape.
     */
    static @Nullable EnvLockfile readEnvLockfile(@Nullable String lockText) {
        return parse(LockfileReader.readFirstYamlDocument(lockText));
    }

    @SuppressWarnings("unchecked")
    private static @Nullable EnvLockfile parse(@Nullable String rawContent) {
        if (rawContent == null) {
            return null;
        }
        Object parsed;
        try {
            parsed = JsYaml.load(rawContent);
        } catch (JsYaml.YamlException e) {
            throw new JsError(JsError.ERROR, e.toString());
        }
        if (!(parsed instanceof Map)) {
            return null;
        }
        Map<String, @Nullable Object> lockfile = (Map<String, @Nullable Object>) parsed;
        if (!(lockfile.get("lockfileVersion") instanceof String)) {
            return null;
        }
        for (String section : new String[]{"importers", "packages", "snapshots"}) {
            Object value = lockfile.get(section);
            if (value == null || !"object".equals(Js.typeOf(value))) {
                return null;
            }
            if (!(value instanceof Map)) {
                throw new CannotKnow("what pnpm makes of the env document's " + section + " (an object that is " +
                                     "not a mapping)");
            }
        }
        Map<String, @Nullable Object> importers = (Map<String, @Nullable Object>) lockfile.get("importers");
        Object importer = importers.get(".");
        if (!Js.truthy(importer)) {
            Map<String, @Nullable Object> created = new LinkedHashMap<>();
            created.put("configDependencies", new LinkedHashMap<String, @Nullable Object>());
            importers.put(".", created);
        } else if (!(importer instanceof Map)) {
            throw new CannotKnow("what pnpm makes of the env document's importer . (" + Js.typeOf(importer) +
                                 ", where pnpm reads an object)");
        } else if (!Js.truthy(((Map<String, @Nullable Object>) importer).get("configDependencies"))) {
            ((Map<String, @Nullable Object>) importer).put("configDependencies",
                    new LinkedHashMap<String, @Nullable Object>());
        }
        return new EnvLockfile(lockfile);
    }

    /**
     * {@code writeEnvLockfile}: the file's text with this env document in front of the main document of
     * {@code existing} (the file as it was, null for none).
     */
    static String writeEnvLockfile(@Nullable String existing, EnvLockfile lockfile) {
        String envYaml = LockfileWriter.lockfileYamlDump(SortLockfileKeys.sortLockfileKeys(lockfile.document));
        String mainDoc = existing == null ? "" : LockfileReader.extractMainDocument(existing);
        return LockfileReader.YAML_DOCUMENT_START + envYaml + LockfileReader.YAML_DOCUMENT_SEPARATOR + mainDoc;
    }

    @SuppressWarnings("unchecked")
    Map<String, @Nullable Object> importer() {
        return (Map<String, @Nullable Object>) ((Map<String, @Nullable Object>) document.get("importers")).get(".");
    }

    /**
     * The importer's {@code configDependencies}: name to {@code {specifier, version}}.
     */
    @SuppressWarnings("unchecked")
    Map<String, @Nullable Object> configDependencies() {
        Object configDependencies = importer().get("configDependencies");
        if (!(configDependencies instanceof Map)) {
            throw new CannotKnow("what pnpm makes of the env document's configDependencies (" +
                                 Js.typeOf(configDependencies) + ", where pnpm reads an object)");
        }
        return (Map<String, @Nullable Object>) configDependencies;
    }

    /**
     * The importer's {@code packageManagerDependencies}, null without them.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    Map<String, @Nullable Object> packageManagerDependencies() {
        Object dependencies = importer().get("packageManagerDependencies");
        return dependencies instanceof Map ? (Map<String, @Nullable Object>) dependencies : null;
    }

    @SuppressWarnings("unchecked")
    Map<String, @Nullable Object> packages() {
        return (Map<String, @Nullable Object>) document.get("packages");
    }

    @SuppressWarnings("unchecked")
    Map<String, @Nullable Object> snapshots() {
        return (Map<String, @Nullable Object>) document.get("snapshots");
    }
}
