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
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;
import static org.openrewrite.javascript.semver.NodeSemver.Options.DEFAULT;

/**
 * Port of @npmcli/arborist lib/override-set.js (npm 11.17.0).
 */
@Getter
public final class OverrideSet {

    @Setter
    private @Nullable OverrideSet parent;

    private final Map<String, OverrideSet> children = new LinkedHashMap<>();

    @Setter
    private @Nullable String name;

    private @Nullable String key;
    private @Nullable String keySpec;

    /**
     * A spec, or whatever else the package gave as the rule's {@code "."}: npm fails on reading an
     * edge's spec through a rule whose value is not a string.
     */
    @Setter
    private @Nullable Object value;

    /**
     * The npm release whose arborist reads the overrides.
     */
    private final NpmRelease release;

    /**
     * @param overrides a package's {@code overrides}: a spec string or a map of keys to nested overrides.
     * @throws IllegalArgumentException for an override that is {@code null}, as arborist throws.
     */
    public OverrideSet(@Nullable Object overrides) {
        this(overrides, NpmRelease.PORTED);
    }

    public OverrideSet(@Nullable Object overrides, NpmRelease release) {
        this(overrides, null, null, release);
    }

    OverrideSet(@Nullable Object overrides, @Nullable String key, @Nullable OverrideSet parent) {
        this(overrides, key, parent, parent != null ? parent.release : NpmRelease.PORTED);
    }

    @SuppressWarnings("unchecked")
    private OverrideSet(@Nullable Object overrides, @Nullable String key, @Nullable OverrideSet parent,
                        NpmRelease release) {
        this.parent = parent;
        this.release = release;

        if (overrides instanceof String) {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put(".", overrides);
            overrides = rule;
        }

        if ("".equals(Js.get(overrides, "."))) {
            ((Map<String, Object>) overrides).put(".", "*");
        }

        if (parent != null) {
            Spec spec = Spec.npa(key, null, release.npmPackageArg());
            if (spec.getName() == null || spec.getName().isEmpty()) {
                throw new IllegalArgumentException("Override without name: " + key);
            }
            this.name = spec.getName();
            this.key = key;
            // before npm 9.0.0, spec.rawSpec === '' ? '' : spec.toString()
            this.keySpec = release.keepsEmptyOverrideKeySpec() && spec.getRawSpec().isEmpty() ? "" :
                    spec.withoutName().toString();
            Object dot = Js.get(overrides, ".");
            this.value = truthy(dot) ? dot : this.keySpec;
        }

        for (Map.Entry<String, Object> entry : Js.entries(overrides)) {
            if (".".equals(entry.getKey())) {
                continue;
            }
            OverrideSet child = new OverrideSet(entry.getValue(), entry.getKey(), this);
            children.put(child.key, child);
        }
    }

    boolean childrenAreEqual(OverrideSet other) {
        if (children.size() != other.children.size()) {
            return false;
        }
        for (String key : children.keySet()) {
            if (!other.children.containsKey(key)) {
                return false;
            }
            if (!Js.strictEquals(children.get(key).value, other.children.get(key).value)) {
                return false;
            }
            if (!children.get(key).childrenAreEqual(other.children.get(key))) {
                return false;
            }
        }
        return true;
    }

    public boolean isEqual(@Nullable OverrideSet other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!Objects.equals(key, other.key) || !Js.strictEquals(value, other.value)) {
            return false;
        }
        if (!childrenAreEqual(other)) {
            return false;
        }
        if (parent == null) {
            return other.parent == null;
        }
        return parent.isEqual(other.parent);
    }

    public OverrideSet getEdgeRule(Edge edge) {
        if (!release.matchesEdgeRuleByRawSpec()) {
            return getEdgeRule(edge.getName(), edge::getSpec);
        }
        return getEdgeRule(edge.getName(), () -> edge.getRawSpec().isEmpty() ? edge.getSpec() : edge.getRawSpec());
    }

    public OverrideSet getEdgeRule(String edgeName, String edgeSpec) {
        return getEdgeRule(edgeName, () -> edgeSpec);
    }

    // the spec is read only for a rule that compares it: reading an edge's can throw on an unresolvable $reference
    private OverrideSet getEdgeRule(String edgeName, Supplier<String> edgeSpec) {
        if (!release.matchesEdgeRuleBySpecType()) {
            return getEdgeRuleBySemver(edgeName, edgeSpec);
        }
        for (OverrideSet rule : getRuleset().values()) {
            if (!Objects.equals(rule.name, edgeName)) {
                continue;
            }

            if ("*".equals(rule.keySpec)) {
                return rule;
            }

            Spec spec = Spec.npa(edgeName + "@" + edgeSpec.get(), null, release.npmPackageArg());
            if (spec.getType() == Spec.Type.ALIAS) {
                spec = spec.getSubSpec();
            }

            if (spec.getType() == Spec.Type.GIT) {
                if (spec.getGitRange() != null && !spec.getGitRange().isEmpty() &&
                    NodeSemver.intersects(spec.getGitRange(), rule.keySpec, DEFAULT.release(release.semver()))) {
                    return rule;
                }
                continue;
            }

            if (spec.getType() == Spec.Type.RANGE || spec.getType() == Spec.Type.VERSION) {
                if (NodeSemver.intersects(spec.getFetchSpec(), rule.keySpec, DEFAULT.release(release.semver()))) {
                    return rule;
                }
                continue;
            }

            return rule;
        }
        return this;
    }

    // getEdgeRule before npm 9.3.0: semver.intersects(edge.spec, rule.keySpec), which throws on a spec that is no
    // range; before npm 9.0.0 a rule whose key names no spec applies first
    private OverrideSet getEdgeRuleBySemver(String edgeName, Supplier<String> edgeSpec) {
        for (OverrideSet rule : getRuleset().values()) {
            if (!Objects.equals(rule.name, edgeName)) {
                continue;
            }
            if (release.keepsEmptyOverrideKeySpec() && "".equals(rule.keySpec) ||
                NodeSemver.intersects(edgeSpec.get(), Objects.requireNonNull(rule.keySpec),
                        DEFAULT.release(release.semver()))) {
                return rule;
            }
        }
        return this;
    }

    public OverrideSet getNodeRule(Node node) {
        return getNodeRule(node.getName(), node.getVersion());
    }

    public OverrideSet getNodeRule(@Nullable String nodeName, Object version) {
        OverrideSet rule = getMatchingRule(nodeName, version);
        return rule != null ? rule : this;
    }

    public @Nullable OverrideSet getMatchingRule(Node node) {
        return getMatchingRule(node.getName(), node.getVersion());
    }

    public @Nullable OverrideSet getMatchingRule(@Nullable String nodeName, Object version) {
        for (OverrideSet rule : getRuleset().values()) {
            if (!Objects.equals(rule.name, nodeName)) {
                continue;
            }
            NodeSemver.Options semver = DEFAULT.release(release.semver());
            if (release.keepsEmptyOverrideKeySpec() && "".equals(rule.keySpec) ||
                NodeSemver.satisfies(version, rule.keySpec, semver) ||
                rule.value instanceof String && NodeSemver.satisfies(version, (String) rule.value, semver)) {
                return rule;
            }
        }
        return null;
    }

    public List<OverrideSet> ancestry() {
        List<OverrideSet> ancestry = new ArrayList<>();
        for (OverrideSet ancestor = this; ancestor != null; ancestor = ancestor.parent) {
            ancestry.add(ancestor);
        }
        return ancestry;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public Map<String, OverrideSet> getRuleset() {
        Map<String, OverrideSet> ruleset = new LinkedHashMap<>();
        for (OverrideSet override : ancestry()) {
            for (OverrideSet kid : override.children.values()) {
                ruleset.putIfAbsent(kid.key, kid);
            }
            if (!override.isRoot()) {
                ruleset.putIfAbsent(override.key, override);
            }
        }
        return ruleset;
    }

    public static @Nullable OverrideSet findSpecificOverrideSet(@Nullable OverrideSet first,
                                                                @Nullable OverrideSet second) {
        if (contains(second, first)) {
            return second;
        }
        if (contains(first, second)) {
            return first;
        }
        if (!release(first, second).fallsBackToCommonOverrideAncestor()) {
            return null;
        }
        if (haveConflictingRules(first, second)) {
            return null;
        }
        return findCommonAncestor(first, second);
    }

    private static NpmRelease release(@Nullable OverrideSet first, @Nullable OverrideSet second) {
        return first != null ? first.release : second != null ? second.release : NpmRelease.PORTED;
    }

    private static boolean contains(@Nullable OverrideSet set, @Nullable OverrideSet ancestor) {
        for (OverrideSet overrideSet = set; overrideSet != null; overrideSet = overrideSet.parent) {
            if (overrideSet.isEqual(ancestor)) {
                return true;
            }
        }
        return false;
    }

    public static @Nullable OverrideSet findCommonAncestor(OverrideSet first, OverrideSet second) {
        List<OverrideSet> firstAncestors = first.ancestry();
        for (OverrideSet secondAnc : second.ancestry()) {
            for (OverrideSet firstAnc : firstAncestors) {
                if (firstAnc.isEqual(secondAnc)) {
                    return firstAnc;
                }
            }
        }
        return null;
    }

    // findSpecificOverrideSet's undefined (conflicting rules) and null (no common ancestor) are one value here.
    public static boolean doOverrideSetsConflict(OverrideSet first, @Nullable OverrideSet second) {
        if (contains(second, first) || contains(first, second)) {
            return false;
        }
        if (!first.release.comparesOverrideRulesForConflict()) {
            return true;
        }
        return haveConflictingRules(first, second);
    }

    public static boolean haveConflictingRules(OverrideSet first, @Nullable OverrideSet second) {
        if (second == null) {
            throw new IllegalArgumentException("Cannot read properties of undefined (reading 'ruleset')");
        }
        Map<String, OverrideSet> firstRules = first.getRuleset();
        Map<String, OverrideSet> secondRules = second.getRuleset();

        for (Map.Entry<String, OverrideSet> entry : firstRules.entrySet()) {
            OverrideSet firstRule = entry.getValue();
            OverrideSet secondRule = secondRules.get(entry.getKey());
            if (secondRule == null) {
                continue;
            }
            if (firstRule == secondRule || firstRule.isEqual(secondRule)) {
                continue;
            }

            if (isReference(firstRule.value, "firstValue") || isReference(secondRule.value, "secondValue")) {
                continue;
            }

            try {
                Spec.Options npa = first.release.npmPackageArg();
                Spec firstSpec = Spec.npa(firstRule.name + "@" + firstRule.value, null, npa);
                Spec secondSpec = Spec.npa(secondRule.name + "@" + secondRule.value, null, npa);
                if ((firstSpec.getType() == Spec.Type.RANGE || firstSpec.getType() == Spec.Type.VERSION) &&
                    (secondSpec.getType() == Spec.Type.RANGE || secondSpec.getType() == Spec.Type.VERSION)) {
                    if (!NodeSemver.intersects(firstSpec.getFetchSpec(), secondSpec.getFetchSpec(),
                            DEFAULT.release(first.release.semver()))) {
                        return true;
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // an unparseable rule cannot be shown to conflict
            }
        }
        return false;
    }

    private static boolean isReference(@Nullable Object value, String variable) {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException(variable + ".startsWith is not a function");
        }
        return ((String) value).startsWith("$");
    }
}
