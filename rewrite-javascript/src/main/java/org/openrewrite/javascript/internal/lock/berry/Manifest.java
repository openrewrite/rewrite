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
package org.openrewrite.javascript.internal.lock.berry;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openrewrite.javascript.internal.lock.YarnpkgSemverUtils;

/**
 * Port of @yarnpkg/core sources/Manifest.ts for the fields a lock-only install reads (Yarn 4.10.3).
 */
final class Manifest {

    private static final Pattern INDENT = Pattern.compile("^[ \\t]+", Pattern.MULTILINE);
    private static final Pattern NOT_BANG = Pattern.compile("[^!]");

    @Value
    static class WorkspaceDefinition {
        String pattern;
    }

    @Value
    static class ResolutionEntry {
        Resolution pattern;
        String reference;
    }

    String indent = "  ";

    @Nullable
    Ident name;

    @Nullable
    String version;

    @Nullable
    List<String> os;

    @Nullable
    List<String> cpu;

    @Nullable
    List<String> libc;

    @Nullable
    String packageManager;

    boolean isPrivate;

    @Nullable
    String languageName;

    Map<String, String> bin = new LinkedHashMap<>();
    Map<String, String> scripts = new LinkedHashMap<>();
    Map<String, Descriptor> dependencies = new LinkedHashMap<>();
    Map<String, Descriptor> devDependencies = new LinkedHashMap<>();
    Map<String, Descriptor> peerDependencies = new LinkedHashMap<>();
    List<WorkspaceDefinition> workspaceDefinitions = new ArrayList<>();
    Map<String, Map<@Nullable String, Map<String, Object>>> dependenciesMeta = new LinkedHashMap<>();
    Map<String, Map<String, Object>> peerDependenciesMeta = new LinkedHashMap<>();
    List<ResolutionEntry> resolutions = new ArrayList<>();

    @Nullable
    Object raw = new LinkedHashMap<>();

    /**
     * Problems {@code load} found and stepped over; Yarn reports them as warnings.
     */
    List<String> errors = new ArrayList<>();

    /**
     * {@code Manifest.fromText}: a package.json's text, a byte order mark dropped and an empty file read as {@code {}}.
     * @throws Js.SyntaxError for text that is not JSON, the message naming it as Yarn does.
     */
    static Manifest fromText(String text) {
        Manifest manifest = new Manifest();
        String stripped = stripBOM(text);
        Object data;
        try {
            data = Js.jsonParse(stripped.isEmpty() ? "{}" : stripped);
        } catch (Js.SyntaxError e) {
            throw new Js.SyntaxError(e.getMessage() + " (when parsing " + text + ")");
        }
        manifest.load(data, false);
        manifest.indent = getIndent(text);
        return manifest;
    }

    /**
     * @param yamlCompatibilityMode whether a boolean meta field may be spelled as a string ({@code "true"}), as
     *                              js-yaml's failsafe schema reads every scalar of a lockfile
     */
    @SuppressWarnings("unchecked")
    void load(@Nullable Object data, boolean yamlCompatibilityMode) {
        if (!(data instanceof Map) && !(data instanceof List)) {
            throw new JsError(JsError.ERROR, "Utterly invalid manifest data (" + Js.string(data) + ")");
        }
        this.raw = data;
        List<String> errors = new ArrayList<>();

        this.name = null;
        Object name = Js.get(data, "name");
        if (name instanceof String) {
            try {
                this.name = StructUtils.parseIdent((String) name);
            } catch (JsError e) {
                errors.add("Parsing failed for the 'name' field");
            }
        }

        Object version = Js.get(data, "version");
        this.version = version instanceof String ? (String) version : null;

        this.os = stringList(Js.get(data, "os"), "os", errors);
        this.cpu = stringList(Js.get(data, "cpu"), "cpu", errors);
        this.libc = stringList(Js.get(data, "libc"), "libc", errors);

        Object packageManager = Js.get(data, "packageManager");
        this.packageManager = packageManager instanceof String ? (String) packageManager : null;

        Object isPrivate = Js.get(data, "private");
        this.isPrivate = isPrivate instanceof Boolean && (Boolean) isPrivate;

        Object languageName = Js.get(data, "languageName");
        this.languageName = languageName instanceof String ? (String) languageName : null;

        this.bin = new LinkedHashMap<>();
        Object bin = Js.get(data, "bin");
        if (bin instanceof String) {
            if (((String) bin).trim().isEmpty()) {
                errors.add("Invalid bin field");
            } else if (this.name != null) {
                this.bin.put(this.name.name, normalizeSlashes((String) bin));
            } else {
                errors.add("String bin field, but no attached package name");
            }
        } else if (Js.isObject(bin)) {
            for (Map.Entry<String, @Nullable Object> entry : Js.entries(bin)) {
                Object value = entry.getValue();
                if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
                    errors.add("Invalid bin definition for '" + entry.getKey() + "'");
                    continue;
                }
                Ident binaryIdent = StructUtils.parseIdent(entry.getKey());
                this.bin.put(binaryIdent.name, normalizeSlashes((String) value));
            }
        }

        this.scripts = new LinkedHashMap<>();
        Object scripts = Js.get(data, "scripts");
        if (Js.isObject(scripts)) {
            for (Map.Entry<String, @Nullable Object> entry : Js.entries(scripts)) {
                if (!(entry.getValue() instanceof String)) {
                    errors.add("Invalid script definition for '" + entry.getKey() + "'");
                    continue;
                }
                this.scripts.put(entry.getKey(), (String) entry.getValue());
            }
        }

        this.dependencies = dependencyMap(Js.get(data, "dependencies"), errors);
        this.devDependencies = dependencyMap(Js.get(data, "devDependencies"), errors);

        this.peerDependencies = new LinkedHashMap<>();
        Object peerDependencies = Js.get(data, "peerDependencies");
        if (Js.isObject(peerDependencies)) {
            for (Map.Entry<String, @Nullable Object> entry : Js.entries(peerDependencies)) {
                Ident ident;
                try {
                    ident = StructUtils.parseIdent(entry.getKey());
                } catch (JsError e) {
                    errors.add("Parsing failed for the dependency name '" + entry.getKey() + "'");
                    continue;
                }
                Object range = entry.getValue();
                if (!(range instanceof String) || (!((String) range).startsWith(WorkspaceResolver.PROTOCOL) &&
                                                   YarnpkgSemverUtils.validRange((String) range) == null)) {
                    errors.add("Invalid dependency range for '" + entry.getKey() + "'");
                    range = "*";
                }
                Descriptor descriptor = StructUtils.makeDescriptor(ident, (String) range);
                this.peerDependencies.put(descriptor.identHash, descriptor);
            }
        }

        Object workspacesField = Js.get(data, "workspaces");
        if (workspacesField instanceof Map && Js.truthy(Js.get(workspacesField, "nohoist"))) {
            errors.add("'nohoist' is deprecated, please use 'installConfig.hoistingLimits' instead");
        }
        Object workspaces = workspacesField instanceof List ? workspacesField :
                workspacesField instanceof Map && Js.get(workspacesField, "packages") instanceof List ?
                        Js.get(workspacesField, "packages") : new ArrayList<>();
        this.workspaceDefinitions = new ArrayList<>();
        for (Object entry : (List<?>) workspaces) {
            if (!(entry instanceof String)) {
                errors.add("Invalid workspace definition for '" + Js.string(entry) + "'");
                continue;
            }
            this.workspaceDefinitions.add(new WorkspaceDefinition((String) entry));
        }

        this.dependenciesMeta = new LinkedHashMap<>();
        Object dependenciesMeta = Js.get(data, "dependenciesMeta");
        if (Js.isObject(dependenciesMeta)) {
            for (Map.Entry<String, @Nullable Object> entry : Js.entries(dependenciesMeta)) {
                Object meta = entry.getValue();
                if (!Js.isObject(meta)) {
                    errors.add("Invalid meta field for '" + entry.getKey());
                    continue;
                }
                Descriptor descriptor = StructUtils.parseDescriptor(entry.getKey());
                Map<String, Object> dependencyMeta = ensureDependencyMeta(descriptor);
                Object built = tryParseOptionalBoolean(Js.get(meta, "built"), yamlCompatibilityMode);
                if (built == null) {
                    errors.add("Invalid built meta field for '" + entry.getKey() + "'");
                    continue;
                }
                Object optional = tryParseOptionalBoolean(Js.get(meta, "optional"), yamlCompatibilityMode);
                if (optional == null) {
                    errors.add("Invalid optional meta field for '" + entry.getKey() + "'");
                    continue;
                }
                Object unplugged = tryParseOptionalBoolean(Js.get(meta, "unplugged"), yamlCompatibilityMode);
                if (unplugged == null) {
                    errors.add("Invalid unplugged meta field for '" + entry.getKey() + "'");
                    continue;
                }
                dependencyMeta.put("built", built);
                dependencyMeta.put("optional", optional);
                dependencyMeta.put("unplugged", unplugged);
            }
        }

        this.peerDependenciesMeta = new LinkedHashMap<>();
        Object peerDependenciesMeta = Js.get(data, "peerDependenciesMeta");
        if (Js.isObject(peerDependenciesMeta)) {
            for (Map.Entry<String, @Nullable Object> entry : Js.entries(peerDependenciesMeta)) {
                Object meta = entry.getValue();
                if (!Js.isObject(meta)) {
                    errors.add("Invalid meta field for '" + entry.getKey());
                    continue;
                }
                Descriptor descriptor = StructUtils.parseDescriptor(entry.getKey());
                Map<String, Object> peerDependencyMeta = ensurePeerDependencyMeta(descriptor);
                Object optional = tryParseOptionalBoolean(Js.get(meta, "optional"), yamlCompatibilityMode);
                if (optional == null) {
                    errors.add("Invalid optional meta field for '" + entry.getKey() + "'");
                    continue;
                }
                peerDependencyMeta.put("optional", optional);
            }
        }

        this.resolutions = new ArrayList<>();
        Object resolutions = Js.get(data, "resolutions");
        if (Js.isObject(resolutions)) {
            for (Map.Entry<String, @Nullable Object> entry : Js.entries(resolutions)) {
                if (!(entry.getValue() instanceof String)) {
                    errors.add("Invalid resolution entry for '" + entry.getKey() + "'");
                    continue;
                }
                try {
                    this.resolutions.add(new ResolutionEntry(Resolution.parseResolution(entry.getKey()),
                            (String) entry.getValue()));
                } catch (JsError e) {
                    errors.add(e.getMessage());
                }
            }
        }

        Object optionalDependencies = Js.get(data, "optionalDependencies");
        if (Js.isObject(optionalDependencies)) {
            for (Map.Entry<String, @Nullable Object> entry : Js.entries(optionalDependencies)) {
                if (!(entry.getValue() instanceof String)) {
                    errors.add("Invalid dependency range for '" + entry.getKey() + "'");
                    continue;
                }
                Ident ident;
                try {
                    ident = StructUtils.parseIdent(entry.getKey());
                } catch (JsError e) {
                    errors.add("Parsing failed for the dependency name '" + entry.getKey() + "'");
                    continue;
                }
                Descriptor realDescriptor = StructUtils.makeDescriptor(ident, (String) entry.getValue());
                this.dependencies.put(realDescriptor.identHash, realDescriptor);
                Descriptor identDescriptor = StructUtils.makeDescriptor(ident, "unknown");
                ensureDependencyMeta(identDescriptor).put("optional", true);
            }
        }

        this.errors = errors;
    }

    private static @Nullable List<String> stringList(@Nullable Object field, String name, List<String> errors) {
        if (!(field instanceof List)) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (Object item : (List<?>) field) {
            if (!(item instanceof String)) {
                errors.add("Parsing failed for the '" + name + "' field");
            } else {
                out.add((String) item);
            }
        }
        return out;
    }

    private static Map<String, Descriptor> dependencyMap(@Nullable Object field, List<String> errors) {
        Map<String, Descriptor> out = new LinkedHashMap<>();
        if (!Js.isObject(field)) {
            return out;
        }
        for (Map.Entry<String, @Nullable Object> entry : Js.entries(field)) {
            if (!(entry.getValue() instanceof String)) {
                errors.add("Invalid dependency range for '" + entry.getKey() + "'");
                continue;
            }
            Ident ident;
            try {
                ident = StructUtils.parseIdent(entry.getKey());
            } catch (JsError e) {
                errors.add("Parsing failed for the dependency name '" + entry.getKey() + "'");
                continue;
            }
            Descriptor descriptor = StructUtils.makeDescriptor(ident, (String) entry.getValue());
            out.put(descriptor.identHash, descriptor);
        }
        return out;
    }

    /**
     * {@code getConditions}: the os, cpu and libc lists as a condition line, null when none is set.
     */
    @Nullable
    String getConditions() {
        List<String> fields = new ArrayList<>();
        if (os != null && !os.isEmpty()) {
            fields.add(toConditionLine("os", os));
        }
        if (cpu != null && !cpu.isEmpty()) {
            fields.add(toConditionLine("cpu", cpu));
        }
        if (libc != null && !libc.isEmpty()) {
            fields.add(toConditionLine("libc", libc));
        }
        return fields.isEmpty() ? null : String.join(" & ", fields);
    }

    Map<String, Object> ensureDependencyMeta(Descriptor descriptor) {
        if (!"unknown".equals(descriptor.range) && !YarnpkgSemverUtils.valid(descriptor.range)) {
            throw new JsError(JsError.ERROR, "Invalid meta field range for '" +
                                             StructUtils.stringifyDescriptor(descriptor) + "'");
        }
        String identString = StructUtils.stringifyIdent(descriptor);
        String range = !"unknown".equals(descriptor.range) ? descriptor.range : null;
        return dependenciesMeta.computeIfAbsent(identString, k -> new LinkedHashMap<>())
                .computeIfAbsent(range, k -> new LinkedHashMap<>());
    }

    Map<String, Object> ensurePeerDependencyMeta(Descriptor descriptor) {
        if (!"unknown".equals(descriptor.range)) {
            throw new JsError(JsError.ERROR, "Invalid meta field range for '" +
                                             StructUtils.stringifyDescriptor(descriptor) + "'");
        }
        return peerDependenciesMeta.computeIfAbsent(StructUtils.stringifyIdent(descriptor),
                k -> new LinkedHashMap<>());
    }

    /**
     * {@code exportTo({}, {compatibilityMode})} of the fields this port loads; the lockfile writer passes
     * {@code false}, so optional dependencies stay in {@code dependencies}.
     */
    Map<String, Object> exportTo(boolean compatibilityMode) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (name != null) {
            data.put("name", StructUtils.stringifyIdent(name));
        }
        if (version != null) {
            data.put("version", version);
        }
        if (os != null) {
            data.put("os", os);
        }
        if (cpu != null) {
            data.put("cpu", cpu);
        }
        if (packageManager != null) {
            data.put("packageManager", packageManager);
        }
        if (isPrivate) {
            data.put("private", true);
        }
        if (languageName != null) {
            data.put("languageName", languageName);
        }
        if (bin.size() == 1 && name != null && bin.containsKey(name.name)) {
            data.put("bin", bin.get(name.name));
        } else if (!bin.isEmpty()) {
            Map<String, Object> sorted = new LinkedHashMap<>();
            List<String> keys = new ArrayList<>(bin.keySet());
            keys.sort(String::compareTo);
            for (String key : keys) {
                sorted.put(key, bin.get(key));
            }
            data.put("bin", sorted);
        }
        if (!workspaceDefinitions.isEmpty()) {
            List<String> patterns = new ArrayList<>();
            for (WorkspaceDefinition definition : workspaceDefinitions) {
                patterns.add(definition.pattern);
            }
            data.put("workspaces", patterns);
        }

        List<Descriptor> regularDependencies = new ArrayList<>();
        List<Descriptor> optionalDependencies = new ArrayList<>();
        for (Descriptor dependency : dependencies.values()) {
            Map<@Nullable String, Map<String, Object>> dependencyMetaSet =
                    dependenciesMeta.get(StructUtils.stringifyIdent(dependency));
            boolean isOptionallyBuilt = false;
            if (compatibilityMode && dependencyMetaSet != null) {
                Map<String, Object> meta = dependencyMetaSet.get(null);
                if (meta != null && Js.truthy(meta.get("optional"))) {
                    isOptionallyBuilt = true;
                }
            }
            (isOptionallyBuilt ? optionalDependencies : regularDependencies).add(dependency);
        }
        if (!regularDependencies.isEmpty()) {
            data.put("dependencies", rangesByIdent(regularDependencies));
        }
        if (!optionalDependencies.isEmpty()) {
            data.put("optionalDependencies", rangesByIdent(optionalDependencies));
        }
        if (!devDependencies.isEmpty()) {
            data.put("devDependencies", rangesByIdent(devDependencies.values()));
        }
        if (!peerDependencies.isEmpty()) {
            data.put("peerDependencies", rangesByIdent(peerDependencies.values()));
        }

        Map<String, Object> exportedMeta = new LinkedHashMap<>();
        for (Map.Entry<String, Map<@Nullable String, Map<String, Object>>> set :
                MiscUtils.sortMap(dependenciesMeta.entrySet(), Map.Entry::getKey)) {
            for (Map.Entry<@Nullable String, Map<String, Object>> entry : MiscUtils.sortMap(set.getValue().entrySet(),
                    e -> e.getKey() != null ? "0" + e.getKey() : "1")) {
                String range = entry.getKey();
                String key = range != null ?
                        StructUtils.stringifyDescriptor(StructUtils.makeDescriptor(
                                StructUtils.parseIdent(set.getKey()), range)) :
                        set.getKey();
                Map<String, Object> metaCopy = new LinkedHashMap<>(entry.getValue());
                if (compatibilityMode && range == null) {
                    metaCopy.remove("optional");
                }
                if (metaCopy.isEmpty()) {
                    continue;
                }
                exportedMeta.put(key, metaCopy);
            }
        }
        if (!exportedMeta.isEmpty()) {
            data.put("dependenciesMeta", exportedMeta);
        }

        if (!peerDependenciesMeta.isEmpty()) {
            Map<String, Object> exportedPeerMeta = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> entry :
                    MiscUtils.sortMap(peerDependenciesMeta.entrySet(), Map.Entry::getKey)) {
                exportedPeerMeta.put(entry.getKey(), entry.getValue());
            }
            data.put("peerDependenciesMeta", exportedPeerMeta);
        }

        if (!resolutions.isEmpty()) {
            Map<String, Object> exportedResolutions = new LinkedHashMap<>();
            for (ResolutionEntry resolution : resolutions) {
                exportedResolutions.put(Resolution.stringifyResolution(resolution.pattern), resolution.reference);
            }
            data.put("resolutions", exportedResolutions);
        }

        if (!scripts.isEmpty()) {
            data.put("scripts", new LinkedHashMap<>(scripts));
        }
        return data;
    }

    private static Map<String, Object> rangesByIdent(Iterable<Descriptor> dependencies) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Descriptor dependency : StructUtils.sortDescriptors(dependencies)) {
            out.put(StructUtils.stringifyIdent(dependency), dependency.range);
        }
        return out;
    }

    private static String getIndent(String content) {
        Matcher indentMatch = INDENT.matcher(content);
        return indentMatch.find() ? indentMatch.group() : "  ";
    }

    private static String stripBOM(String content) {
        return !content.isEmpty() && content.charAt(0) == '\uFEFF' ? content.substring(1) : content;
    }

    private static String normalizeSlashes(String str) {
        return str.replace('\\', '/');
    }

    private static @Nullable Object tryParseOptionalBoolean(@Nullable Object value, boolean yamlCompatibilityMode) {
        if (yamlCompatibilityMode) {
            return MiscUtils.tryParseOptionalBoolean(value);
        }
        if (value == Syml.UNDEFINED || value instanceof Boolean) {
            return value;
        }
        return null;
    }

    private static String toConditionToken(String name, String raw) {
        Matcher index = NOT_BANG.matcher(raw);
        if (!index.find()) {
            return "invalid";
        }
        String prefix = index.start() % 2 == 0 ? "" : "!";
        return prefix + name + "=" + raw.substring(index.start());
    }

    private static String toConditionLine(String name, List<String> rawTokens) {
        if (rawTokens.size() == 1) {
            return toConditionToken(name, rawTokens.get(0));
        }
        List<String> tokens = new ArrayList<>();
        for (String raw : rawTokens) {
            tokens.add(toConditionToken(name, raw));
        }
        return "(" + String.join(" | ", tokens) + ")";
    }
}
