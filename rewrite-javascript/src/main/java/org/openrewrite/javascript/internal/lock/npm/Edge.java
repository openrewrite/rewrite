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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.Spec;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/edge.js (npm 11.17.0).
 */
public final class Edge {

    public enum Type {
        PROD("prod"), DEV("dev"), OPTIONAL("optional"), PEER("peer"), PEER_OPTIONAL("peerOptional"),
        WORKSPACE("workspace");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static final String DETACHED = "DETACHED";
    public static final String MISSING = "MISSING";
    public static final String PEER_LOCAL = "PEER LOCAL";
    public static final String INVALID = "INVALID";
    private static final String OK = "OK";

    private @Nullable String accept;
    private @Nullable String error;
    private @Nullable Map<String, Object> explanation;
    private @Nullable Node from;
    private final String name;
    private final String spec;
    private @Nullable Node to;
    private final Type type;

    // the release of the tree the edge was made in, which a detached edge can no longer ask its dependent for
    @Getter(AccessLevel.PACKAGE)
    private final NpmRelease release;

    // reload() after detach() sees JavaScript's undefined resolution, unlike the null of a missing dependency
    private boolean resolvedDetached;

    @Getter
    @Setter
    private @Nullable OverrideSet overrides;

    @Getter
    @Setter
    private boolean peerConflicted;

    public Edge(@Nullable Node from, Type type, @Nullable String name, @Nullable String spec) {
        this(from, type, name, spec, null, null);
    }

    public Edge(@Nullable Node from, Type type, @Nullable String name, @Nullable String spec,
                @Nullable String accept, @Nullable OverrideSet overrides) {
        if (spec == null) {
            throw new IllegalArgumentException("must provide string spec");
        }
        if (type == Type.WORKSPACE && Spec.npa(spec, null,
                (from != null ? from.getRelease() : NpmRelease.PORTED).npmPackageArg()).getType() !=
                                      Spec.Type.DIRECTORY) {
            throw new IllegalArgumentException("workspace edges must be a symlink");
        }
        if (name == null) {
            throw new IllegalArgumentException("must provide dependency name");
        }
        if (from == null) {
            throw new IllegalArgumentException("must provide \"from\" node");
        }
        if (accept != null) {
            this.accept = accept.isEmpty() ? "*" : accept;
        }
        this.overrides = overrides;

        this.name = name;
        this.type = type;
        this.spec = spec;
        this.explanation = null;
        this.from = from;
        this.release = from.getRelease();

        Edge current = from.getEdgesOut().get(name);
        if (current != null) {
            current.detach();
        }
        from.addEdgeOut(this);

        reload(true);
        if (!release.computesEdgeErrorWhenRead()) {
            // this[_setFrom](from), then this[_error] = this[_loadError]() once more
            error = loadError();
        }
        this.peerConflicted = false;
    }

    public boolean satisfiedBy(Node node) {
        // from npm 7.21.0, whose children resolve a name of another case
        if (!name.equals(node.getName()) && release.matchesNamesCaseInsensitively()) {
            return false;
        }
        if (from == null) {
            if (release.propagatesOverrideSets()) {
                return false;
            }
            // dep-valid.js reads the dependent a detached edge no longer has
            throw new IllegalArgumentException("Cannot read properties of null (reading 'errors')");
        }

        // from npm 12.0.0 no node has a shrinkwrap, and what the root bundles is overridden as any other
        // (node.inDepBundle alone); before npm 8.12.0 every node is read through the override
        if (release.readsRawSpecOfBundled() && (truthy(node.getHasShrinkwrap()) || node.isInShrinkwrap() ||
            node.isInBundle() && (node.isInDepBundle() || !release.overridesWhatTheRootBundles()))) {
            return DepValid.depValid(node, getRawSpec(), accept, from);
        }

        if (!release.propagatesOverrideSets() || overrides == null || overrides.getKeySpec() == null ||
            overrides.getKeySpec().isEmpty()) {
            return DepValid.depValid(node, getSpec(), accept, from);
        }
        if (DepValid.depValid(node, getSpec(), accept, from)) {
            return true;
        }
        if (!DepValid.depValid(node, getRawSpec(), accept, from)) {
            return false;
        }
        return !DepValid.depValid(node, overrides.getKeySpec(), accept, from);
    }

    public Map<String, Object> explain() {
        return explain(Collections.emptyList());
    }

    public Map<String, Object> explain(List<Node> seen) {
        if (explanation == null) {
            Map<String, Object> explanation = new LinkedHashMap<>();
            explanation.put("type", type.toString());
            explanation.put("name", name);
            explanation.put("spec", getSpec());
            if (!getRawSpec().equals(getSpec())) {
                explanation.put("rawSpec", getRawSpec());
                explanation.put("overridden", true);
            }
            if (isBundled()) {
                explanation.put("bundled", true);
            }
            if (getError() != null) {
                explanation.put("error", getError());
            }
            if (from != null) {
                explanation.put("from", from.explain(null, seen));
                // npm 12.0.0 notes an edge that a root packageExtensions rule, or a root .npm-extension, made or
                // changed on the from node
                String field = extendedField(from.getPackageExtensionsApplied(), "applied");
                if (field != null) {
                    Map<String, Object> packageExtensions = new LinkedHashMap<>();
                    packageExtensions.put("selector", Js.get(from.getPackageExtensionsApplied(), "selector"));
                    packageExtensions.put("field", field);
                    explanation.put("packageExtensions", packageExtensions);
                }
                field = extendedField(from.getNpmExtensionApplied(), "transformed");
                if (field != null) {
                    Map<String, Object> npmExtension = new LinkedHashMap<>();
                    npmExtension.put("extensionPoint", Js.get(from.getNpmExtensionApplied(), "extensionPoint"));
                    npmExtension.put("field", field);
                    explanation.put("npmExtension", npmExtension);
                }
            }
            this.explanation = explanation;
        }
        return explanation;
    }

    // the first manifest field whose list of names an extension's record holds this edge's under:
    // applied[field]?.includes(name), the record being arborist's variable of that name
    private @Nullable String extendedField(@Nullable Object applied, String variable) {
        if (!truthy(applied)) {
            return null;
        }
        for (String field : PackageExtensions.EXTENSION_FIELDS) {
            Object names = Js.get(applied, field);
            if (names == null || names == Js.UNDEFINED) {
                continue;
            }
            if (!(names instanceof List) && !(names instanceof String)) {
                throw new IllegalArgumentException(variable + "[field]?.includes is not a function");
            }
            if (names instanceof List ? ((List<?>) names).contains(name) : ((String) names).contains(name)) {
                return field;
            }
        }
        return null;
    }

    public boolean isBundled() {
        if (from == null) {
            return false;
        }
        Object bd = from.getPackage().get("bundleDependencies");
        if (bd == null) {
            return false;
        }
        if (bd instanceof List) {
            return ((List<?>) bd).contains(name);
        }
        if (bd instanceof String) {
            return ((String) bd).contains(name);
        }
        throw new IllegalArgumentException("this[#from]?.package?.bundleDependencies?.includes is not a function");
    }

    public boolean isWorkspace() {
        return type == Type.WORKSPACE;
    }

    public boolean isProd() {
        return type == Type.PROD;
    }

    public boolean isDev() {
        return type == Type.DEV;
    }

    public boolean isOptional() {
        return type == Type.OPTIONAL || type == Type.PEER_OPTIONAL;
    }

    public boolean isPeer() {
        return type == Type.PEER || type == Type.PEER_OPTIONAL;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getRawSpec() {
        return spec;
    }

    /**
     * @throws MalformedManifest when a {@code $name} override references a dependency neither the root nor this edge's
     * package declares; IllegalArgumentException, arborist's TypeError, when the override is not a string.
     */
    public String getSpec() {
        if (overrides != null && truthy(overrides.getValue()) &&
            (!"*".equals(overrides.getValue()) || !release.ignoresStarOverride()) &&
            name.equals(overrides.getName())) {
            if (!(overrides.getValue() instanceof String)) {
                throw new IllegalArgumentException("this.overrides.value.startsWith is not a function");
            }
            String value = (String) overrides.getValue();
            if (value.startsWith("$")) {
                String ref = value.substring(1);
                Map<String, Object> pkg = from == null ? null :
                        from.getSourceReference() != null && release.readsReferenceFromRealRoot() ?
                                from.getSourceReference().getRoot().getPackage() : from.getRoot().getPackage();
                String specValue = calculateReferentialOverrideSpec(ref, pkg);
                if (specValue == null && from != null && from.getRelease().resolvesOverrideReferenceInOwnPackage()) {
                    pkg = from.getPackage();
                    specValue = calculateReferentialOverrideSpec(ref, pkg);
                }
                if (specValue != null) {
                    return specValue;
                }
                throw new MalformedManifest("Unable to resolve reference " + value);
            }
            return value;
        }
        return spec;
    }

    private static @Nullable String calculateReferentialOverrideSpec(String ref, @Nullable Map<String, Object> pkg) {
        if (pkg == null) {
            return null;
        }
        String[] depTypes = {"devDependencies", "optionalDependencies", "dependencies", "peerDependencies"};
        for (String depType : depTypes) {
            Object deps = pkg.get(depType);
            Object spec = deps == null ? null : Js.get(deps, ref);
            if (truthy(spec)) {
                if (!(spec instanceof String)) {
                    throw new IllegalArgumentException("Override reference $" + ref + " does not name a spec");
                }
                return (String) spec;
            }
        }
        return null;
    }

    public @Nullable String getAccept() {
        return accept;
    }

    public boolean isValid() {
        return getError() == null;
    }

    public boolean isMissing() {
        return MISSING.equals(getError());
    }

    public boolean isInvalid() {
        return INVALID.equals(getError());
    }

    public boolean isPeerLocal() {
        return PEER_LOCAL.equals(getError());
    }

    public @Nullable String getError() {
        if (error == null) {
            error = loadError();
        }
        return OK.equals(error) ? null : error;
    }

    private @Nullable String loadError() {
        if (to == null) {
            return isOptional() ? null : MISSING;
        } else if (isPeer() && from == to.getParent() && (from == null || !from.isTop())) {
            return PEER_LOCAL;
        } else if (!satisfiedBy(to)) {
            return INVALID;
        } else if (release.propagatesOverrideSets() && overrides != null &&
                   (to.getOverrides() != null || !release.comparesOverridesOfOverriddenTargets()) &&
                   to.getEdgesOut().size() > 0 &&
                   OverrideSet.doOverrideSetsConflict(overrides, to.getOverrides())) {
            return INVALID;
        }
        return OK;
    }

    public void reload() {
        reload(false);
    }

    public void reload(boolean hard) {
        explanation = null;
        if (!release.propagatesOverrideSets()) {
            reloadWithDependentsRule(hard);
            return;
        }

        boolean needToUpdateOverrideSet = false;
        OverrideSet newOverrideSet = null;
        OverrideSet oldOverrideSet = null;
        if (from != null && from.getOverrides() != null) {
            newOverrideSet = from.getOverrides().getEdgeRule(this);
            if (!newOverrideSet.isEqual(overrides)) {
                needToUpdateOverrideSet = true;
                oldOverrideSet = overrides;
                overrides = newOverrideSet;
            }
        } else {
            overrides = null;
        }

        Node newTo = from == null ? null : from.resolve(name);
        boolean changed = from == null ? !resolvedDetached : newTo != to;
        if (changed) {
            if (to != null) {
                to.deleteEdgeIn(this);
            }
            to = newTo;
            resolvedDetached = from == null;
            error = null;
            if (to != null) {
                to.addEdgeIn(this);
            }
        } else if (hard) {
            error = null;
        } else if (needToUpdateOverrideSet && to != null) {
            to.updateOverridesEdgeInRemoved(oldOverrideSet);
            to.updateOverridesEdgeInAdded(newOverrideSet);
        }
    }

    // reload before npm 11.2.0: the edge takes the rule of its dependent's overrides, from npm 8.7.0 (before, it keeps
    // the one it was made with), and tells its target nothing; before npm 9.6.6 its error is computed here, before the
    // target hears of the edge
    private void reloadWithDependentsRule(boolean hard) {
        if (release.recomputesOverridesOnReload()) {
            if (from == null) {
                throw new IllegalArgumentException("Cannot read properties of null (reading 'overrides')");
            }
            if (from.getOverrides() != null) {
                overrides = from.getOverrides().getEdgeRule(this);
            } else {
                overrides = null;
            }
        }
        if (from == null) {
            throw new IllegalArgumentException("Cannot read properties of null (reading 'resolve')");
        }
        Node newTo = from.resolve(name);
        if (newTo != to) {
            if (to != null) {
                to.getEdgesIn().remove(this);
            }
            to = newTo;
            error = release.computesEdgeErrorWhenRead() ? null : loadError();
            if (to != null) {
                to.addEdgeIn(this);
            }
        } else if (hard) {
            error = release.computesEdgeErrorWhenRead() ? null : loadError();
        }
    }

    public void detach() {
        explanation = null;
        if (to != null) {
            if (release.propagatesOverrideSets()) {
                to.deleteEdgeIn(this);
            } else {
                to.getEdgesIn().remove(this);
            }
        }
        if (from != null) {
            from.getEdgesOut().delete(name);
        }
        to = null;
        error = DETACHED;
        from = null;
    }

    public @Nullable Node getFrom() {
        return from;
    }

    public @Nullable Node getTo() {
        return to;
    }

    /**
     * Arborist's {@code ArboristEdge}, the printable form of an edge.
     */
    public Map<String, Object> toJSON() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("name", name);
        json.put("spec", getSpec());
        json.put("type", type.toString());
        if (from != null && from.getLocation() != null) {
            json.put("from", from.getLocation());
        }
        if (to != null && to.getLocation() != null && !to.getLocation().isEmpty()) {
            json.put("to", to.getLocation());
        }
        if (getError() != null) {
            json.put("error", getError());
        }
        if (peerConflicted) {
            json.put("peerConflicted", true);
        }
        if (overrides != null && truthy(overrides.getValue())) {
            json.put("overridden", overrides.getValue());
        }
        return json;
    }

    @Override
    public String toString() {
        return "Edge" + toJSON();
    }
}
