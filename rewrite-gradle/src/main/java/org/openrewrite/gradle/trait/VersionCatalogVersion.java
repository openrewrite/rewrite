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
package org.openrewrite.gradle.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.trait.Trait;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.gradle.trait.GradleTraitMatcher.literalArgument;
import static org.openrewrite.internal.StringUtils.matchesGlob;

/**
 * Represents a single {@code version(alias, value)} declaration inside a Gradle version
 * catalog. Several {@code library(...)} entries commonly share one of these via
 * {@code versionRef(...)}, so bumping it satisfies every one of them at once.
 */
@Value
public class VersionCatalogVersion implements Trait<J.MethodInvocation> {
    Cursor cursor;

    public @Nullable String getAlias() {
        return literalArgument(getTree(), 0);
    }

    public @Nullable String getVersion() {
        return literalArgument(getTree(), 1);
    }

    /**
     * @return a copy of this version declaration with its value literal rewritten to
     * {@code newVersion}.
     */
    public VersionCatalogVersion withVersion(String newVersion) {
        J.MethodInvocation outer = getTree();
        Expression argument = outer.getArguments().get(1);
        if (!(argument instanceof J.Literal)) {
            return this;
        }
        J.Literal oldLiteral = (J.Literal) argument;
        String quote = oldLiteral.getValueSource() == null ? "'" : oldLiteral.getValueSource().substring(0, 1);
        J.Literal newLiteral = oldLiteral.withValue(newVersion).withValueSource(quote + newVersion + quote);
        List<Expression> newArguments = new ArrayList<>(outer.getArguments());
        newArguments.set(1, newLiteral);
        return new VersionCatalogVersion(new Cursor(cursor.getParent(), outer.withArguments(newArguments)));
    }

    public static class Matcher extends GradleTraitMatcher<VersionCatalogVersion> {
        @Nullable
        private String aliasPattern;

        public Matcher alias(@Nullable String aliasPattern) {
            this.aliasPattern = aliasPattern;
            return this;
        }

        @Override
        protected @Nullable VersionCatalogVersion test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof J.MethodInvocation) {
                J.MethodInvocation m = (J.MethodInvocation) value;
                if ("version".equals(m.getSimpleName()) && m.getArguments().size() == 2 && m.getSelect() == null
                    && isTopLevelStatement(cursor) && withinBlock(cursor, "versionCatalogs")) {
                    String alias = literalArgument(m, 0);
                    if (alias != null && matchesGlob(alias, aliasPattern)) {
                        return new VersionCatalogVersion(cursor);
                    }
                }
            }
            return null;
        }
    }
}
