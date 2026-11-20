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

import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.ManifestRules;

import java.util.Comparator;
import java.util.List;

/**
 * npm's reading of the files the dependency recipes edit: overrides in the root's {@code overrides}, nested by path,
 * and reify's save, which sorts every dependency field with {@code localeCompare(x, 'en')} and drops an empty one.
 */
public final class NpmManifestRules implements ManifestRules {

    public static final NpmManifestRules RULES = new NpmManifestRules();

    private NpmManifestRules() {
    }

    @Override
    public List<String> sortedOnAdd(String field) {
        return DEPENDENCY_FIELDS;
    }

    @Override
    public Comparator<String> addOrder() {
        return LocaleCompare::en;
    }

    @Override
    public boolean keepsEmptyFieldsOnAdd() {
        return false;
    }
}
