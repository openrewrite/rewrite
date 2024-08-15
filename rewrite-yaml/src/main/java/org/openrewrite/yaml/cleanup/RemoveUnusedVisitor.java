/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.yaml.cleanup;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

public class RemoveUnusedVisitor<P> extends YamlIsoVisitor<P> {

    @Nullable
    private final Cursor cursor;

    public RemoveUnusedVisitor(@Nullable Cursor cursor) {
        this.cursor = cursor;
    }

    @Override
    public Yaml.Sequence visitSequence(Yaml.Sequence sequence, P p) {
        Yaml.Sequence s = super.visitSequence(sequence, p);
        if (cursor == null || cursor.isScopeInPath(s)) {
            s = s.withEntries(ListUtils.map(s.getEntries(), e -> {
                if (e.getBlock() == null || isEmptyScalar(e.getBlock())) {
                    return null;
                }
                return e;
            }));
            if (s.getEntries().isEmpty()) {
                //noinspection ConstantConditions
                return null;
            }
        }
        return s;
    }

    @Override
    public Yaml.Mapping visitMapping(Yaml.Mapping mapping, P p) {
        Yaml.Mapping m = super.visitMapping(mapping, p);
        if (cursor == null || cursor.isScopeInPath(m)) {
            m = m.withEntries(ListUtils.map(m.getEntries(), e -> {
                if (e.getValue() == null || isEmptyScalar(e.getValue())) {
                    return null;
                }
                if (e.getValue() instanceof Yaml.Mapping &&
                        ((Yaml.Mapping) e.getValue()).getEntries().isEmpty()) {
                    return null;
                }
                return e;
            }));
            if (m.getEntries().isEmpty()) {
                //noinspection ConstantConditions
                return null;
            }
        }
        return m;
    }

    private boolean isEmptyScalar(Yaml.Block y) {
        if (y instanceof Yaml.Scalar) {
            Yaml.Scalar scalar = (Yaml.Scalar) y;
            return scalar.getValue().isEmpty() && Yaml.Scalar.Style.PLAIN.equals(scalar.getStyle());
        }
        return false;
    }
}
