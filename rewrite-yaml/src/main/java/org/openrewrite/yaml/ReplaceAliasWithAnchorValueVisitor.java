/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.yaml;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.yaml.tree.Yaml;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReplaceAliasWithAnchorValueVisitor<P> extends YamlVisitor<P> {
    Map<String, Yaml> anchorValues = new HashMap<>();
    @Override
    public Yaml visitMapping(Yaml.Mapping mapping, P p) {
        if (mapping.getAnchor() != null) {
            anchorValues.put(mapping.getAnchor().getKey(), mapping.withAnchor(null));
        }
        return super.visitMapping(mapping, p);
    }

    @Override
    public Yaml visitScalar(Yaml.Scalar scalar, P p) {
        if (scalar.getAnchor() != null) {
            anchorValues.put(scalar.getAnchor().getKey(), scalar.withAnchor(null));
        }
        return super.visitScalar(scalar, p);
    }

    @Override
    public Yaml visitAlias(Yaml.Alias alias, P p) {
        Yaml.Alias al = (Yaml.Alias) super.visitAlias(alias, p);
        Yaml anchorVal = anchorValues.get(al.getAnchor().getKey());
        // formatting reads its style from the enclosing Documents; callers that visit without them,
        // like JsonPathMatcher, only compare values, so they need no re-indentation
        if (getCursor().firstEnclosing(Yaml.Documents.class) != null && anchorVal instanceof Yaml.Mapping &&
            ((Yaml.Mapping) anchorVal).getOpeningBracePrefix() == null) {
            // the anchor's entries are indented for where the anchor is, so they move to where the alias is
            return autoFormat(anchorVal, p, getCursor().getParentOrThrow());
        } else if (anchorVal != null) {
            return anchorVal;
        }
        return al;
    }

    /// Whether this visitor would replace any alias in `yaml` with its anchor's value, answered without
    /// building the replaced tree. Only aliases of anchored mappings and scalars count; an alias of an
    /// anchored sequence is left in place by this visitor, so it does not.
    ///
    /// @param yaml the tree to search, in document order so that each anchor precedes its aliases
    /// @return `true` if at least one alias would be replaced
    static boolean hasResolvableAlias(Yaml yaml) {
        return new ReplaceAliasWithAnchorValueVisitor<AtomicBoolean>() {
            @Override
            public @Nullable Yaml visit(@Nullable Tree tree, AtomicBoolean found) {
                return found.get() ? (Yaml) tree : super.visit(tree, found);
            }

            @Override
            public Yaml visitAlias(Yaml.Alias alias, AtomicBoolean found) {
                if (anchorValues.containsKey(alias.getAnchor().getKey())) {
                    found.set(true);
                }
                return alias;
            }
        }.reduce(yaml, new AtomicBoolean()).get();
    }
}
