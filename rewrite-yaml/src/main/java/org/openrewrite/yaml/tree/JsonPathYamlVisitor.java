/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.yaml.tree;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

public class JsonPathYamlVisitor extends AbstractJsonPathTreeVisitor<Yaml> {

    public JsonPathYamlVisitor(List<Tree> cursorPath) {
        super(cursorPath);
    }

    @Override
    protected TreeVisitor<? extends Tree, TerminalNode> rootNodeVisitor() {
        return new TreeVisitor<Tree, TerminalNode>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, TerminalNode terminalNode) {
                if (tree instanceof Yaml.Mapping) {
                    return tree;
                }
                return null;
            }
        };
    }

    @Override
    protected TreeVisitor<Yaml, List<Yaml>> arraySliceVisitor() {
        return new TreeVisitor<Yaml, List<Yaml>>() {
            @Override
            public @Nullable Yaml visit(@Nullable Tree tree, List<Yaml> results) {
                if (tree instanceof Yaml.Mapping.Entry) {
                    visit(((Yaml.Mapping.Entry) tree).getValue(), results);
                } else if (tree instanceof Yaml.Mapping) {
                    ((Yaml.Mapping) tree).getEntries().forEach(e -> results.add(e.getValue()));
                } else if (tree instanceof Yaml.Sequence) {
                    ((Yaml.Sequence) tree).getEntries().forEach(e -> results.add(e.getBlock()));
                } else {
                    results.add((Yaml) tree);
                }
                return null;
            }
        };
    }

    @Override
    protected TreeVisitor<Yaml, List<Yaml>> resolveExpressionVisitor() {
        return new TreeVisitor<Yaml, List<Yaml>>() {
            @Override
            public @Nullable Yaml visit(@Nullable Tree tree, List<Yaml> results) {
                if (tree instanceof Yaml.Mapping.Entry) {
                    visit(((Yaml.Mapping.Entry) tree).getValue(), results);
                } else if (tree instanceof Yaml.Sequence) {
                    Yaml.Sequence s = (Yaml.Sequence) tree;
                    for (Yaml.Sequence.Entry e : s.getEntries()) {
                        results.add(e.getBlock());
                    }
                }
                return null;
            }
        };
    }

    @Override
    protected TreeVisitor<Yaml, List<Yaml>> findByIdentifierVisitor(TerminalNode id) {
        return new TreeVisitor<Yaml, List<Yaml>>() {
            @Override
            public @Nullable Yaml visit(@Nullable Tree tree, List<Yaml> results) {
                if (tree instanceof Yaml.Mapping.Entry) {
                    return visit(((Yaml.Mapping.Entry) tree).getValue(), results);
                } else if (tree instanceof Yaml.Mapping) {
                    Yaml.Mapping m = (Yaml.Mapping) super.visit(tree, results);
                    if (m != null) {
                        return m.getEntries().stream()
                                .filter(e -> e.getKey().getValue().equals(id.getText()))
                                .findFirst()
                                .orElse(null);
                    }
                } else if (tree instanceof Yaml.Sequence) {
                    Yaml.Sequence s = (Yaml.Sequence) super.visit(tree, results);
                    if (s != null) {
                        for (Yaml.Sequence.Entry e : s.getEntries()) {
                            Yaml y = super.visit(e.getBlock(), results);
                            if (y != null) {
                                results.add(y);
                            }
                        }
                    }
                }
                return null;
            }
        };
    }

}
