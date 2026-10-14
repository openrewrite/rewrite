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
package org.openrewrite.javascript.internal.lock.npm;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.GitHost;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;
import static org.openrewrite.javascript.semver.NodeSemver.Options.LOOSE;

/**
 * Port of @npmcli/arborist lib/dep-valid.js (npm 11.17.0): whether a node satisfies a spec.
 */
final class DepValid {

    private static final Pattern COMMIT = Pattern.compile("^[a-fA-F0-9]{40,64}$");
    private static final Pattern SHA1_COMMIT = Pattern.compile("^[a-fA-F0-9]{40}$");

    /**
     * An error dep-valid records on the requesting node, for a spec nothing could satisfy.
     */
    @Getter
    static final class DependencyError extends IllegalArgumentException {
        private final @Nullable String dependency;
        private final @Nullable Object requested;

        DependencyError(String message, @Nullable String dependency, @Nullable Object requested,
                        @Nullable Throwable cause) {
            super(message, cause);
            this.dependency = dependency;
            this.requested = requested;
        }
    }

    private DepValid() {
    }

    private static @Nullable String lockedGitCommittish(Node child, Node requestor) {
        Shrinkwrap meta = requestor.getRoot().getMeta();
        Map<String, Object> data = meta == null ? null : meta.getData();
        Object packages = data == null ? null : data.get("packages");
        String location = requestor.getLocation();
        String name = child.getName();
        Object lock = packages == null || location == null ? null : Js.get(packages, location);
        if (!truthy(lock) || name == null) {
            return null;
        }
        Object spec = null;
        String[] types = {"dependencies", "optionalDependencies", "devDependencies", "peerDependencies"};
        for (String type : types) {
            Object deps = Js.get(lock, type);
            spec = deps == null || deps == Js.UNDEFINED ? null : Js.get(deps, name);
            if (truthy(spec)) {
                break;
            }
        }
        if (!(spec instanceof String) || !truthy(spec)) {
            return null;
        }
        try {
            Spec parsed = Spec.resolve(child.getName(), (String) spec, requestor.getRealpath(),
                    requestor.getRelease().npmPackageArg());
            if (parsed.getType() != Spec.Type.GIT) {
                return null;
            }
            return truthy(parsed.getGitCommittish()) ? parsed.getGitCommittish() : "";
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static boolean depValid(Node child, @Nullable String requested, @Nullable String accept, Node requestor) {
        return depValid(child, requested, requestor) || (accept != null && depValid(child, accept, requestor));
    }

    static boolean depValid(Node child, @Nullable Spec requested, @Nullable String accept, Node requestor) {
        return depValid(child, requested, requestor) || (accept != null && depValid(child, accept, requestor));
    }

    private static boolean depValid(Node child, @Nullable String requested, Node requestor) {
        if (requested == null) {
            return depValid(child, (Spec) null, requestor);
        }
        Spec spec;
        try {
            spec = Spec.resolve(child.getName(), requested.isEmpty() ? "*" : requested,
                    FromPath.fromPath(requestor, requestor.getEdgesOut().get(child.getName())),
                    requestor.getRelease().npmPackageArg());
        } catch (IllegalArgumentException er) {
            requestor.getErrors().add(new DependencyError(er.getMessage(), child.getName(), requested, er));
            return false;
        }
        return depValid(child, spec, requestor);
    }

    private static boolean depValid(Node child, @Nullable Spec requested, Node requestor) {
        if (requested == null) {
            requestor.getErrors().add(new DependencyError("Invalid dependency specifier", child.getName(), null, null));
            return false;
        }

        NodeSemver.Options loose = LOOSE.release(requestor.getRelease().semver());
        Spec.Options npa = requestor.getRelease().npmPackageArg();
        switch (requested.getType()) {
            case RANGE:
                if ("*".equals(requested.getFetchSpec())) {
                    return true;
                }
                // fall through
            case VERSION:
                return NodeSemver.satisfies(child.getVersion(), requested.getFetchSpec(), loose);
            case DIRECTORY:
                return linkValid(child, requested, requestor);
            case FILE:
                return tarballValid(child, requested);
            case ALIAS:
                return depValid(child, requested.getSubSpec(), requestor);
            case TAG:
                return truthy(child.getResolved()) &&
                       Spec.npa(child.getResolved(), null, npa).getType() == Spec.Type.REMOTE;
            case REMOTE:
                return requested.getFetchSpec() != null && Js.strictEquals(child.getResolved(),
                        requested.getFetchSpec());
            case GIT: {
                Spec resRepo = Spec.npa(truthy(child.getResolved()) ? child.getResolved() : "", null, npa);
                GitHost resHost = resRepo.getHosted();
                GitHost reqHost = requested.getHosted();
                String reqCommittish = requested.getGitCommittish() == null ? "" : requested.getGitCommittish();
                boolean reqCommit = (requestor.getRelease().readsSha256Commits() ? COMMIT : SHA1_COMMIT)
                        .matcher(reqCommittish).matches();
                GitHost.Opts nc = GitHost.Opts.of(null, !reqCommit);
                if (resHost == null) {
                    if (!Objects.equals(resRepo.getFetchSpec(), requested.getFetchSpec())) {
                        return false;
                    }
                } else {
                    if (reqHost == null || !Objects.equals(reqHost.ssh(nc), resHost.ssh(nc))) {
                        return false;
                    }
                }
                if (!truthy(requested.getGitRange())) {
                    if (!reqCommit && requestor.getRelease().comparesLockedGitCommittish()) {
                        String locked = lockedGitCommittish(child, requestor);
                        if (locked != null && !locked.equals(reqCommittish)) {
                            return false;
                        }
                    }
                    return true;
                }
                return NodeSemver.satisfies(child.getPackage().get("version"), requested.getGitRange(), loose);
            }
            default:
                break;
        }

        requestor.getErrors().add(new DependencyError("Unsupported dependency type", child.getName(), requested, null));
        return false;
    }

    private static boolean linkValid(Node child, Spec requested, Node requestor) {
        boolean isLink = child.isLink();
        if (requestor.isInstallLinks() && (!child.isWorkspace() || !requestor.getRelease().keepsWorkspaceLinksValid())) {
            return !isLink;
        }
        return isLink && child.getRealpath() != null &&
               PosixPath.relative(child.getRealpath(), requested.getFetchSpec()).isEmpty();
    }

    private static boolean tarballValid(Node child, Spec requested) {
        if (child.isLink()) {
            return false;
        }
        if (truthy(child.getResolved())) {
            if (!child.getRelease().comparesTarballPathsWithSlashes()) {
                // before npm 7.0.13, as written
                return Js.strictEquals(child.getResolved(), "file:" + requested.getFetchSpec());
            }
            if (!(child.getResolved() instanceof String)) {
                throw new IllegalArgumentException("child.resolved.replace is not a function");
            }
            String resolved = (String) child.getResolved();
            return resolved.replace('\\', '/').equals("file:" + requested.getFetchSpec().replace('\\', '/'));
        }
        Object legacyRequested = child.getPackage().get("_requested");
        if (legacyRequested instanceof Map) {
            return Objects.equals(((Map<?, ?>) legacyRequested).get("saveSpec"), requested.getSaveSpec());
        }
        return false;
    }
}
