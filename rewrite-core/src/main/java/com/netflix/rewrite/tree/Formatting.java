/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.tree;

import com.fasterxml.jackson.annotation.*;
import com.koloboke.collect.map.hash.HashObjObjMaps;

import java.io.Serializable;
import java.util.Map;

/**
 * The stylistic surroundings of a tree element
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "@c")
public abstract class Formatting implements Serializable {
    public static Reified format(String prefix) {
        return format(prefix, "");
    }

    public static Reified format(String prefix, String suffix) {
        return Reified.build(prefix, suffix);
    }

    public abstract Reified withPrefix(String prefix);

    public abstract Reified withSuffix(String suffix);

    public static Reified EMPTY;

    /**
     * Formatting should be inferred and reified from surrounding context
     */
    public static final Formatting INFER = new Formatting() {
        @Override
        public Reified withPrefix(String prefix) {
            return format(prefix);
        }

        @Override
        public Reified withSuffix(String suffix) {
            return format("", suffix);
        }
    };

    public static final Formatting NONE = new Formatting() {
        @Override
        public Reified withPrefix(String prefix) {
            return format(prefix);
        }

        @Override
        public Reified withSuffix(String suffix) {
            return format("", suffix);
        }
    };

    public static class Reified extends Formatting {
        String prefix;
        String suffix;

        // suffixes are uncommon, so we'll treat them as a secondary index
        private static final Map<String, Map<String, Reified>> flyweights;

        static {
            flyweights = HashObjObjMaps.newMutableMap();
            EMPTY = format("");
        }

        public Reified(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @JsonCreator
        public static Reified build(@JsonProperty("prefix") String prefix,
                                    @JsonProperty("suffix") String suffix) {
            synchronized (flyweights) {
                return flyweights
                        .computeIfAbsent(prefix, p -> HashObjObjMaps.newMutableMap())
                        .computeIfAbsent(suffix, s -> new Reified(prefix, s));
            }
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        @Override
        public Reified withPrefix(String prefix) {
            return format(prefix, suffix);
        }

        @Override
        public Reified withSuffix(String suffix) {
            return format(prefix, suffix);
        }
    }
}