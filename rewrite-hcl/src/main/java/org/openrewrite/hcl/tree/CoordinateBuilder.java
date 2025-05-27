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
package org.openrewrite.hcl.tree;

import java.util.Comparator;

public abstract class CoordinateBuilder {
    Hcl tree;

    CoordinateBuilder(Hcl tree) {
        this.tree = tree;
    }

    HclCoordinates before(Space.Location location) {
        return new HclCoordinates(tree, location, HclCoordinates.Mode.BEFORE, null);
    }

    HclCoordinates after(Space.Location location) {
        return new HclCoordinates(tree, location, HclCoordinates.Mode.AFTER, null);
    }

    HclCoordinates replace(Space.Location location) {
        return new HclCoordinates(tree, location, HclCoordinates.Mode.REPLACEMENT, null);
    }

    public static class Expression extends CoordinateBuilder {
        Expression(org.openrewrite.hcl.tree.Expression tree) {
            super(tree);
        }

        public HclCoordinates replace() {
            return replace(Space.Location.EXPRESSION_PREFIX);
        }
    }

    public static class Block extends Expression {
        Block(Hcl.Block tree) {
            super(tree);
        }

        public HclCoordinates last() {
            return before(Space.Location.BLOCK_CLOSE);
        }

        public HclCoordinates add(Comparator<BodyContent> idealOrdering) {
            return new HclCoordinates(tree, Space.Location.BLOCK_CLOSE, HclCoordinates.Mode.BEFORE, idealOrdering);
        }

        @Override
        public HclCoordinates replace() {
            return replace(Space.Location.BLOCK);
        }
    }

    public static class ConfigFile extends CoordinateBuilder {
        ConfigFile(Hcl.ConfigFile tree) {
            super(tree);
        }

        public HclCoordinates last() {
            return before(Space.Location.CONFIG_FILE_EOF);
        }

        public HclCoordinates add(Comparator<BodyContent> idealOrdering) {
            return new HclCoordinates(tree, Space.Location.CONFIG_FILE_EOF, HclCoordinates.Mode.BEFORE, idealOrdering);
        }

        public HclCoordinates first() {
            return before(Space.Location.CONFIG_FILE);
        }
    }
}
