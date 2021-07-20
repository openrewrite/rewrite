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

import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.internal.grammar.JsonPath;

import java.util.List;
import java.util.stream.Collectors;

public interface Y {

    static TreeVisitor<Yaml, Tree> recursiveDescentVisitor(List<Tree> cursorPath, JsonPath.ExpressionContext exprCtx) {
        return new TreeVisitor<Yaml, Tree>() {
            @Override
            public @Nullable Yaml visit(@Nullable Tree tree, Tree context) {
                return cursorPath.stream()
                        .filter(o -> o instanceof Yaml.Mapping.Entry)
                        .map(Yaml.Mapping.Entry.class::cast)
                        .filter(e -> e.getKey().getValue().equals(exprCtx.getText()))
                        .findFirst()
                        .orElse(null);
            }
        };
    }

    static <T extends Yaml> Yaml.Sequence sequenceOf(List<T> entries) {
        return new Yaml.Sequence(Tree.randomId(), Markers.EMPTY, null, entries.stream()
                .map(t -> {
                    if (t instanceof Yaml.Sequence.Entry) {
                        return (Yaml.Sequence.Entry) t;
                    }
                    return new Yaml.Sequence.Entry(Tree.randomId(), "", Markers.EMPTY, (Yaml.Block) t, true, null);
                })
                .collect(Collectors.toList()), null);
    }

}
