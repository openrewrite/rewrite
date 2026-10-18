/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.toml.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.TreeShapeValidationService;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.marker.ArrayTable;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlKey;

import java.util.*;

public class TomlTreeShapeValidationService implements TreeShapeValidationService {

    @Override
    public @Nullable Object value(Tree tree) {
        if (tree instanceof Toml.Literal) {
            Toml.Literal literal = (Toml.Literal) tree;
            return "the " + literal.getType() + " value `" + literal.getValue() + "`";
        } else if (tree instanceof Toml.Identifier) {
            return "the name `" + ((Toml.Identifier) tree).getName() + "`";
        }
        return null;
    }

    @Override
    public boolean skip(Tree tree) {
        // the parser adds an empty element to an empty array or table, where recipes often leave none
        return tree instanceof Toml.Empty;
    }

    @Override
    public List<String> invalid(SourceFile sourceFile) {
        List<String> invalid = new ArrayList<>();
        if (!(sourceFile instanceof Toml.Document)) {
            return invalid;
        }
        Set<String> tables = new HashSet<>();
        List<Toml> root = new ArrayList<>();
        for (Toml value : ((Toml.Document) sourceFile).getValues()) {
            if (value instanceof Toml.Table && ((Toml.Table) value).getName() != null &&
                !tables.add(((Toml.Table) value).getName().getName()) &&
                !isArrayOfTables((Toml.Table) value)) {
                invalid.add("the table `" + ((Toml.Table) value).getName().getName() +
                            "` appears twice, which TOML doesn't allow");
            } else if (!(value instanceof Toml.Table)) {
                root.add(value);
            }
        }
        duplicateKeys(root, invalid);
        new TomlIsoVisitor<List<String>>() {
            @Override
            public Toml.Table visitTable(Toml.Table table, List<String> found) {
                duplicateKeys(table.getValues(), found);
                return super.visitTable(table, found);
            }
        }.visit(sourceFile, invalid);
        return invalid;
    }

    private static boolean isArrayOfTables(Toml.Table table) {
        return table.getMarkers().findFirst(ArrayTable.class).isPresent();
    }

    private static void duplicateKeys(List<? extends Toml> values, List<String> found) {
        Set<String> keys = new HashSet<>();
        for (Toml value : values) {
            if (value instanceof Toml.KeyValue) {
                String key = key(((Toml.KeyValue) value).getKey());
                if (!keys.add(key)) {
                    found.add("the key `" + key + "` appears twice in one table, which TOML doesn't allow");
                }
            }
        }
    }

    private static String key(TomlKey key) {
        return key instanceof Toml.Identifier ? ((Toml.Identifier) key).getName() : key.toString();
    }
}
