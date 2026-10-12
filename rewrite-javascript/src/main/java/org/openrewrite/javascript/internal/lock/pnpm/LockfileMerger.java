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
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;

/**
 * Port of @pnpm/lockfile.merger src/index.ts (pnpm 11.2.2): the two sides of a lock with git merge conflicts
 * merged into one.
 */
final class LockfileMerger {

    private LockfileMerger() {
    }

    @SuppressWarnings("unchecked")
    static LockfileObject mergeLockfileChanges(LockfileObject ours, LockfileObject theirs) {
        LockfileObject newLockfile = new LockfileObject();
        Object theirVersion = theirs.getLockfileVersion();
        Object ourVersion = ours.getLockfileVersion();
        newLockfile.set("lockfileVersion", gt(LockfileReader.comverToSemver(versionString(theirVersion)),
                LockfileReader.comverToSemver(versionString(ourVersion))) ? theirVersion : ourVersion);
        Object pnpmfileChecksum = ours.getPnpmfileChecksum() != null && ours.getPnpmfileChecksum() != Js.UNDEFINED ?
                ours.getPnpmfileChecksum() : theirs.getPnpmfileChecksum();
        if (Js.truthy(pnpmfileChecksum)) {
            newLockfile.set("pnpmfileChecksum", pnpmfileChecksum);
        }
        Set<@Nullable Object> ignored = new LinkedHashSet<>();
        ignored.addAll(iterable(ours.getIgnoredOptionalDependencies()));
        ignored.addAll(iterable(theirs.getIgnoredOptionalDependencies()));
        if (!ignored.isEmpty()) {
            newLockfile.set("ignoredOptionalDependencies", new ArrayList<>(ignored));
        }

        Set<String> importerIds = new LinkedHashSet<>(Js.keys(ours.getImporters()));
        importerIds.addAll(Js.keys(theirs.getImporters()));
        for (String importerId : importerIds) {
            ProjectSnapshot importer = new ProjectSnapshot();
            ProjectSnapshot our = ours.getImporters().get(importerId);
            ProjectSnapshot their = theirs.getImporters().get(importerId);
            for (String key : new String[]{"dependencies", "devDependencies", "optionalDependencies"}) {
                Map<String, @Nullable String> merged = mergeDict(our == null ? null : our.field(key),
                        their == null ? null : their.field(key), LockfileMerger::mergeVersions);
                importer.setField(key, merged.isEmpty() ? null : merged);
            }
            importer.setSpecifiers(mergeDict(our == null ? null : our.getSpecifiers(),
                    their == null ? null : their.getSpecifiers(), LockfileMerger::takeChangedValue));
            newLockfile.getImporters().put(importerId, importer);
        }

        Map<String, PackageSnapshot> packages = new LinkedHashMap<>();
        Set<String> depPaths = new LinkedHashSet<>();
        if (ours.getPackages() != null) {
            depPaths.addAll(Js.keys(ours.getPackages()));
        }
        if (theirs.getPackages() != null) {
            depPaths.addAll(Js.keys(theirs.getPackages()));
        }
        for (String depPath : depPaths) {
            PackageSnapshot ourPkg = ours.getPackages() == null ? null : ours.getPackages().get(depPath);
            PackageSnapshot theirPkg = theirs.getPackages() == null ? null : theirs.getPackages().get(depPath);
            PackageSnapshot pkg = spread(ourPkg, theirPkg);
            Map<String, @Nullable String> dependencies = mergeDict(ourPkg == null ? null : ourPkg.getDependencies(),
                    theirPkg == null ? null : theirPkg.getDependencies(), LockfileMerger::mergeVersions);
            pkg.setDependencies(dependencies.isEmpty() ? null : dependencies);
            Map<String, @Nullable String> optional = mergeDict(ourPkg == null ? null : ourPkg.getOptionalDependencies(),
                    theirPkg == null ? null : theirPkg.getOptionalDependencies(), LockfileMerger::mergeVersions);
            pkg.setOptionalDependencies(optional.isEmpty() ? null : optional);
            packages.put(depPath, pkg);
        }
        newLockfile.setPackages(packages);
        return newLockfile;
    }

    // {...ourPkg, ...theirPkg}
    private static PackageSnapshot spread(@Nullable PackageSnapshot ours, @Nullable PackageSnapshot theirs) {
        PackageSnapshot pkg = ours == null ? new PackageSnapshot() : ours.copy();
        if (theirs == null) {
            return pkg;
        }
        if (theirs.getId() != null) {
            pkg.setId(theirs.getId());
        }
        if (theirs.getName() != null) {
            pkg.setName(theirs.getName());
        }
        if (theirs.getVersion() != null) {
            pkg.setVersion(theirs.getVersion());
        }
        if (theirs.getResolution() != null) {
            pkg.setResolution(theirs.getResolution());
        }
        if (theirs.getEngines() != null) {
            pkg.setEngines(theirs.getEngines());
        }
        if (theirs.getCpu() != null) {
            pkg.setCpu(theirs.getCpu());
        }
        if (theirs.getOs() != null) {
            pkg.setOs(theirs.getOs());
        }
        if (theirs.getLibc() != null) {
            pkg.setLibc(theirs.getLibc());
        }
        if (theirs.getDeprecated() != null) {
            pkg.setDeprecated(theirs.getDeprecated());
        }
        if (theirs.getHasBin() != null) {
            pkg.setHasBin(theirs.getHasBin());
        }
        if (theirs.getBundledDependencies() != null) {
            pkg.setBundledDependencies(theirs.getBundledDependencies());
        }
        if (theirs.getPeerDependencies() != null) {
            pkg.setPeerDependencies(theirs.getPeerDependencies());
        }
        if (theirs.getPeerDependenciesMeta() != null) {
            pkg.setPeerDependenciesMeta(theirs.getPeerDependenciesMeta());
        }
        if (theirs.getTransitivePeerDependencies() != null) {
            pkg.setTransitivePeerDependencies(theirs.getTransitivePeerDependencies());
        }
        if (theirs.getOptional() != null) {
            pkg.setOptional(theirs.getOptional());
        }
        return pkg;
    }

    private static Map<String, @Nullable String> mergeDict(@Nullable Map<String, @Nullable String> ourDict,
                                                           @Nullable Map<String, @Nullable String> theirDict,
                                                           BinaryOperator<@Nullable String> valueMerger) {
        Map<String, @Nullable String> ours = ourDict == null ? new LinkedHashMap<>() : ourDict;
        Map<String, @Nullable String> theirs = theirDict == null ? new LinkedHashMap<>() : theirDict;
        Map<String, @Nullable String> newDict = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(Js.keys(ours));
        keys.addAll(Js.keys(theirs));
        for (String key : keys) {
            String changedValue = valueMerger.apply(ours.get(key), theirs.get(key));
            if (changedValue != null && !changedValue.isEmpty()) {
                newDict.put(key, changedValue);
            }
        }
        return newDict;
    }

    private static @Nullable String takeChangedValue(@Nullable String ourValue, @Nullable String theirValue) {
        if (Js.strictEquals(ourValue, theirValue) || theirValue == null) {
            return ourValue;
        }
        return theirValue;
    }

    private static @Nullable String mergeVersions(@Nullable String ourValue, @Nullable String theirValue) {
        if (Js.strictEquals(ourValue, theirValue) || theirValue == null || theirValue.isEmpty()) {
            return ourValue;
        }
        if (ourValue == null || ourValue.isEmpty()) {
            return theirValue;
        }
        String ourVersion = ourValue.split("\\(", -1)[0];
        String theirVersion = theirValue.split("\\(", -1)[0];
        String validOurVersion = NodeSemver.valid(ourVersion, NodeSemver.Options.DEFAULT);
        String validTheirVersion = NodeSemver.valid(theirVersion, NodeSemver.Options.DEFAULT);
        if (validOurVersion != null && validTheirVersion != null) {
            return gt(ourVersion, theirVersion) ? ourValue : theirValue;
        }
        return theirValue;
    }

    private static boolean gt(String a, String b) {
        return NodeSemver.compare(a, b, NodeSemver.Options.DEFAULT) > 0;
    }

    // lockfileVersion.toString(): undefined throws, as JavaScript does reading a property of it
    private static String versionString(@Nullable Object version) {
        if (version == null || version == Js.UNDEFINED) {
            throw new LockfileFormatConverters.TypeError("Cannot read properties of " + Js.string(version) +
                                                         " (reading 'toString')");
        }
        return Js.string(version);
    }

    private static List<@Nullable Object> iterable(@Nullable Object value) {
        List<@Nullable Object> out = new ArrayList<>();
        if (value == null || value == Js.UNDEFINED) {
            return out;
        }
        if (value instanceof List) {
            out.addAll((List<?>) value);
            return out;
        }
        if (value instanceof String) {
            for (char c : ((String) value).toCharArray()) {
                out.add(String.valueOf(c));
            }
            return out;
        }
        throw new LockfileFormatConverters.TypeError(Js.string(value) + " is not iterable");
    }
}
