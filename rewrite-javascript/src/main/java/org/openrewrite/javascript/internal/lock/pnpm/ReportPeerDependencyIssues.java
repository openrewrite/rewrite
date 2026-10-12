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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.pnpm.ResolvePeers.BadPeerIssue;
import org.openrewrite.javascript.internal.lock.pnpm.ResolvePeers.MissingPeerIssue;
import org.openrewrite.javascript.internal.lock.pnpm.ResolvePeers.ParentPackage;
import org.openrewrite.javascript.internal.lock.pnpm.ResolvePeers.PeerDependencyIssues;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Port of @pnpm/installing.deps-installer src/install/reportPeerDependencyIssues.ts (pnpm 11.2.2).
 */
final class ReportPeerDependencyIssues {

    private ReportPeerDependencyIssues() {
    }

    /**
     * {@code PeerDependencyRules} from the {@code peerDependencyRules} setting.
     */
    @Value
    static class PeerDependencyRules {
        @Nullable
        List<String> ignoreMissing;

        @Nullable
        List<String> allowAny;

        @Nullable
        Map<String, String> allowedVersions;
    }

    /**
     * {@code ERR_PNPM_PEER_DEP_ISSUES}, carrying the issues it was raised over.
     */
    static final class PeerDependencyIssuesError extends PnpmError {
        private final Map<String, PeerDependencyIssues> issuesByProjects;

        PeerDependencyIssuesError(Map<String, PeerDependencyIssues> issuesByProjects) {
            super("PEER_DEP_ISSUES", "Unmet peer dependencies");
            this.issuesByProjects = issuesByProjects;
        }

        /**
         * The first peer named among the issues: a bad one, else a missing one.
         */
        @Nullable
        String firstPeerName() {
            for (PeerDependencyIssues issues : issuesByProjects.values()) {
                if (!issues.getBad().isEmpty()) {
                    return issues.getBad().keySet().iterator().next();
                }
                if (!issues.getMissing().isEmpty()) {
                    return issues.getMissing().keySet().iterator().next();
                }
            }
            return null;
        }
    }

    static void reportPeerDependencyIssues(Map<String, PeerDependencyIssues> peerDependencyIssuesByProjects,
                                           @Nullable PeerDependencyRules rules, boolean strictPeerDependencies) {
        reportPeerDependencyIssues(peerDependencyIssuesByProjects, rules, strictPeerDependencies, PnpmRelease.PORTED);
    }

    static void reportPeerDependencyIssues(Map<String, PeerDependencyIssues> peerDependencyIssuesByProjects,
                                           @Nullable PeerDependencyRules rules, boolean strictPeerDependencies,
                                           PnpmRelease release) {
        Map<String, PeerDependencyIssues> newPeerDependencyIssuesByProjects =
                filterPeerDependencyIssues(peerDependencyIssuesByProjects, rules, release);
        boolean nothingToReport = true;
        for (PeerDependencyIssues peerIssuesOfProject : newPeerDependencyIssuesByProjects.values()) {
            nothingToReport &= peerIssuesOfProject.getBad().isEmpty() &&
                               (peerIssuesOfProject.getMissing().isEmpty() ||
                                peerIssuesOfProject.getConflicts().isEmpty() &&
                                peerIssuesOfProject.getIntersections().isEmpty());
        }
        if (nothingToReport) {
            return;
        }
        if (strictPeerDependencies) {
            throw new PeerDependencyIssuesError(newPeerDependencyIssuesByProjects);
        }
    }

    static Map<String, PeerDependencyIssues> filterPeerDependencyIssues(
            Map<String, PeerDependencyIssues> peerDependencyIssuesByProjects, @Nullable PeerDependencyRules rules) {
        return filterPeerDependencyIssues(peerDependencyIssuesByProjects, rules, PnpmRelease.PORTED);
    }

    static Map<String, PeerDependencyIssues> filterPeerDependencyIssues(
            Map<String, PeerDependencyIssues> peerDependencyIssuesByProjects, @Nullable PeerDependencyRules rules,
            PnpmRelease release) {
        if (rules == null) {
            return peerDependencyIssuesByProjects;
        }
        Predicate<String> ignoreMissingMatcher = Matcher.createMatcher(new ArrayList<>(new LinkedHashSet<>(
                rules.getIgnoreMissing() == null ? Collections.emptyList() : rules.getIgnoreMissing())));
        Predicate<String> allowAnyMatcher = Matcher.createMatcher(new ArrayList<>(new LinkedHashSet<>(
                rules.getAllowAny() == null ? Collections.emptyList() : rules.getAllowAny())));
        Map<String, List<String>> allowedVersionsMatchAll = new LinkedHashMap<>();
        Map<String, Map<String, List<AllowedVersionsByParent>>> allowedVersionsByParentPkgName = new LinkedHashMap<>();
        parseAllowedVersions(rules.getAllowedVersions() == null ? Collections.emptyMap() :
                rules.getAllowedVersions(), allowedVersionsMatchAll, allowedVersionsByParentPkgName, release);
        Map<String, PeerDependencyIssues> newPeerDependencyIssuesByProjects = new LinkedHashMap<>();
        for (Map.Entry<String, PeerDependencyIssues> project : peerDependencyIssuesByProjects.entrySet()) {
            PeerDependencyIssues issuesOfProject = project.getValue();
            PeerDependencyIssues filtered = new PeerDependencyIssues(new LinkedHashMap<>(), new LinkedHashMap<>(),
                    issuesOfProject.getConflicts(), issuesOfProject.getIntersections());
            newPeerDependencyIssuesByProjects.put(project.getKey(), filtered);
            for (Map.Entry<String, List<MissingPeerIssue>> missing : issuesOfProject.getMissing().entrySet()) {
                boolean allOptional = true;
                for (MissingPeerIssue issue : missing.getValue()) {
                    allOptional &= issue.isOptional();
                }
                if (ignoreMissingMatcher.test(missing.getKey()) || allOptional) {
                    continue;
                }
                filtered.getMissing().put(missing.getKey(), missing.getValue());
            }
            for (Map.Entry<String, List<BadPeerIssue>> bad : issuesOfProject.getBad().entrySet()) {
                String peerName = bad.getKey();
                if (allowAnyMatcher.test(peerName)) {
                    continue;
                }
                List<BadPeerIssue> filteredIssues = new ArrayList<>();
                for (BadPeerIssue issue : bad.getValue()) {
                    if (anySatisfies(issue.getFoundVersion(), allowedVersionsMatchAll.get(peerName), release)) {
                        continue;
                    }
                    ParentPackage currentParentPkg = issue.getParents().isEmpty() ? null :
                            issue.getParents().get(issue.getParents().size() - 1);
                    Map<String, List<AllowedVersionsByParent>> byParent = allowedVersionsByParentPkgName.get(peerName);
                    if (currentParentPkg != null && byParent != null &&
                        byParent.get(currentParentPkg.getName()) != null) {
                        Map<String, List<String>> allowedVersionsByParent = new LinkedHashMap<>();
                        for (AllowedVersionsByParent allowed : byParent.get(currentParentPkg.getName())) {
                            String parentRange = allowed.getParentPkg().getBareSpecifier();
                            if (!Js.truthy(parentRange) || Js.truthy(currentParentPkg.getVersion()) &&
                                                           (isSubRange(parentRange, currentParentPkg.getVersion(), release) ||
                                                            satisfies(currentParentPkg.getVersion(), parentRange, release))) {
                                allowedVersionsByParent.put(allowed.getTargetPkg().getName(), allowed.getRanges());
                            }
                        }
                        if (anySatisfies(issue.getFoundVersion(), allowedVersionsByParent.get(peerName), release)) {
                            continue;
                        }
                    }
                    filteredIssues.add(issue);
                }
                if (!filteredIssues.isEmpty()) {
                    filtered.getBad().put(peerName, filteredIssues);
                }
            }
        }
        return newPeerDependencyIssuesByProjects;
    }

    @Value
    private static class AllowedVersionsByParent {
        ParseOverrides.PackageSelector parentPkg;
        ParseOverrides.PackageSelector targetPkg;
        List<String> ranges;
    }

    private static boolean anySatisfies(String version, @Nullable List<String> ranges, PnpmRelease release) {
        if (ranges == null) {
            return false;
        }
        for (String range : ranges) {
            if (satisfies(version, range, release)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSubRange(@Nullable String superRange, String subRange, PnpmRelease release) {
        return !Js.truthy(superRange) || subRange.equals(superRange) ||
               ResolveDependencies.validRange(subRange, release) != null &&
               ResolveDependencies.validRange(superRange, release) != null &&
               release.underSemver(semver -> NodeSemver.subset(subRange, superRange,
                       NodeSemver.Options.DEFAULT.release(semver)));
    }

    // from 11.13.0 a selector of an empty range is a convergence override's, whose rules parseOverrides checks
    // (PnpmRelease#convergesAnEmptyRangeOverride)
    private static void parseAllowedVersions(Map<String, String> allowedVersions,
                                             Map<String, List<String>> allowedVersionsMatchAll,
                                             Map<String, Map<String, List<AllowedVersionsByParent>>> byParentPkgName,
                                             PnpmRelease release) {
        List<ParseOverrides.VersionOverride> overrides;
        try {
            overrides = ParseOverrides.parseOverrides(allowedVersions, Collections.emptyMap(), release);
        } catch (PnpmError e) {
            throw new PnpmError("INVALID_ALLOWED_VERSION_SELECTOR", e.getMessage() +
                                                                    " in pnpm.peerDependencyRules.allowedVersions");
        }
        for (ParseOverrides.VersionOverride override : overrides) {
            List<String> ranges = new ArrayList<>();
            for (String range : override.getNewBareSpecifier().split("\\|\\|", -1)) {
                ranges.add(range.trim());
            }
            if (override.getParentPkg() == null) {
                allowedVersionsMatchAll.put(override.getTargetPkg().getName(), ranges);
                continue;
            }
            byParentPkgName.computeIfAbsent(override.getTargetPkg().getName(), n -> new LinkedHashMap<>())
                    .computeIfAbsent(override.getParentPkg().getName(), n -> new ArrayList<>())
                    .add(new AllowedVersionsByParent(override.getParentPkg(), override.getTargetPkg(), ranges));
        }
    }

    private static boolean satisfies(String version, String range, PnpmRelease release) {
        try {
            return release.underSemver(semver -> NodeSemver.satisfies(version, range,
                    NodeSemver.Options.DEFAULT.release(semver)));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
