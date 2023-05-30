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
package org.openrewrite.hcl;

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.hcl.format.AttributeSpaceVisitor;
import org.openrewrite.hcl.internal.template.HclTemplateParser;
import org.openrewrite.hcl.internal.template.Substitutions;
import org.openrewrite.hcl.style.SpacesStyle;
import org.openrewrite.hcl.tree.*;
import org.openrewrite.hcl.tree.Space.Location;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.template.SourceTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HclTemplate implements SourceTemplate<Hcl, HclCoordinates> {
    private final Supplier<Cursor> parentScopeGetter;
    private final String code;
    private final int parameterCount;
    private final Consumer<String> onAfterVariableSubstitution;
    private final HclTemplateParser templateParser;

    private HclTemplate(Supplier<Cursor> parentScopeGetter, String code,
                        Consumer<String> onAfterVariableSubstitution, Consumer<String> onBeforeParseTemplate) {
        this.parentScopeGetter = parentScopeGetter;
        this.code = code;
        this.onAfterVariableSubstitution = onAfterVariableSubstitution;
        this.parameterCount = StringUtils.countOccurrences(code, "#{");
        this.templateParser = new HclTemplateParser(onAfterVariableSubstitution, onBeforeParseTemplate);
    }

    @Override
    public <H extends Hcl> H withTemplate(Tree changing, Cursor parentScope, HclCoordinates coordinates, Object[] parameters) {
        if (parameters.length != parameterCount) {
            throw new IllegalArgumentException("This template requires " + parameterCount + " parameters.");
        }

        Substitutions substitutions = new Substitutions(code, parameters);
        String substitutedTemplate = substitutions.substitute();
        onAfterVariableSubstitution.accept(substitutedTemplate);

        Tree insertionPoint = coordinates.getTree();
        Location loc = coordinates.getSpaceLocation();

        AtomicReference<Cursor> parentCursorRef = new AtomicReference<>();

        // Find the parent cursor of the CHANGING element, which may not be the same as the cursor of
        // the method using the template.
        new HclVisitor<Integer>() {
            @Nullable
            @Override
            public Hcl visit(@Nullable Tree tree, Integer integer) {
                if (tree != null && tree.isScope(changing)) {
                    parentCursorRef.set(getCursor());
                    return (Hcl) tree;
                }
                return super.visit(tree, integer);
            }
        }.visit(parentScopeGetter.get().getValue(), 0, parentScopeGetter.get().getParentOrThrow());

        Cursor parentCursor = parentCursorRef.get();

        //noinspection unchecked
        H h = (H) new HclVisitor<Integer>() {
            @Override
            public Hcl visitConfigFile(Hcl.ConfigFile configFile, Integer p) {
                Hcl.ConfigFile c = (Hcl.ConfigFile) super.visitConfigFile(configFile, p);
                if (loc.equals(Location.CONFIG_FILE_EOF)) {
                    List<BodyContent> gen = substitutions.unsubstitute(templateParser.parseBodyContent(substitutedTemplate));

                    if (coordinates.getComparator() != null) {
                        for (int i = 0; i < gen.size(); i++) {
                            BodyContent g = gen.get(i);
                            BodyContent formatted;
                            if (i == 0) {
                                formatted = c.getBody().isEmpty() ?
                                        g :
                                        g.withPrefix(Space.format("\n"));
                            } else {
                                formatted = g;
                            }

                            c = c.withBody(
                                    ListUtils.insertInOrder(
                                            c.getBody(),
                                            autoFormat(formatted, p, getCursor()),
                                            coordinates.getComparator()
                                    )
                            );
                        }
                    } else {
                        c = c.withBody(
                                ListUtils.concatAll(
                                        c.getBody(),
                                        ListUtils.map(gen, (i, s) -> autoFormat(i == 0 ? s.withPrefix(Space.format("\n")) : s, p, getCursor()))
                                )
                        );
                    }
                } else if (loc.equals(Location.CONFIG_FILE)) {
                    List<BodyContent> gen = substitutions.unsubstitute(templateParser.parseBodyContent(substitutedTemplate));
                    c = c.withBody(
                            ListUtils.concatAll(
                                    ListUtils.map(gen, (i, s) -> autoFormat(s, p, getCursor())),
                                    Space.formatFirstPrefix(c.getBody(), c.getBody().isEmpty() ?
                                            Space.EMPTY :
                                            c.getBody().get(0).getPrefix().withWhitespace("\n")))
                    );
                }
                return c;
            }

            @Override
            public Hcl visitBlock(Hcl.Block block, Integer p) {
                Hcl.Block b = (Hcl.Block) super.visitBlock(block, p);
                if (loc.equals(Location.BLOCK_CLOSE)) {
                    if (b.isScope(insertionPoint)) {
                        List<BodyContent> gen = substitutions.unsubstitute(templateParser.parseBodyContent(substitutedTemplate));

                        if (coordinates.getComparator() != null) {
                            for (BodyContent g : gen) {
                                b = b.withBody(
                                        ListUtils.insertInOrder(
                                                b.getBody(),
                                                autoFormat(gen.get(0), p, getCursor()),
                                                coordinates.getComparator()
                                        )
                                );
                            }
                            return b;
                        }

                        b = b.withBody(
                                ListUtils.concatAll(
                                        block.getBody(),
                                        ListUtils.map(gen, (i, s) -> autoFormat(i == 0 ? s.withPrefix(Space.format("\n")) : s, p, getCursor()))
                                )
                        );

                        Hcl.ConfigFile cf = getCursor().firstEnclosingOrThrow(Hcl.ConfigFile.class);
                        b = (Hcl.Block) new AttributeSpaceVisitor<Integer>(Optional.ofNullable(cf.getStyle(SpacesStyle.class))
                                .orElse(SpacesStyle.DEFAULT)).visit(b, p, getCursor().getParentOrThrow());
                        assert b != null;
                    }
                } else if (loc.equals(Location.BLOCK)) {
                    if (b.isScope(insertionPoint)) {
                        b = (Hcl.Block) autoFormat(templateParser.parseBodyContent(substitutedTemplate).get(0), p,
                                getCursor().getParentOrThrow());
                    }
                }
                return b;
            }

            @Override
            public Hcl visitExpression(Expression expression, Integer p) {
                Hcl e = super.visitExpression(expression, p);

                if (loc.equals(Location.EXPRESSION_PREFIX)) {
                    if (e.isScope(insertionPoint)) {
                        e = templateParser.parseExpression(substitutedTemplate).withPrefix(expression.getPrefix());
                    }
                }

                return e;
            }
        }.visit(changing, 0, parentCursor);

        assert h != null;
        return h;
    }

    public static Builder builder(Supplier<Cursor> parentScope, String code) {
        return new Builder(parentScope, code);
    }

    public static class Builder {
        private final Supplier<Cursor> parentScope;
        private final String code;

        private Consumer<String> onAfterVariableSubstitution = s -> {
        };
        private Consumer<String> onBeforeParseTemplate = s -> {
        };

        Builder(Supplier<Cursor> parentScope, String code) {
            this.parentScope = parentScope;
            this.code = code.trim();
        }

        public Builder doAfterVariableSubstitution(Consumer<String> afterVariableSubstitution) {
            this.onAfterVariableSubstitution = afterVariableSubstitution;
            return this;
        }

        public Builder doBeforeParseTemplate(Consumer<String> beforeParseTemplate) {
            this.onBeforeParseTemplate = beforeParseTemplate;
            return this;
        }

        public HclTemplate build() {
            return new HclTemplate(parentScope, code, onAfterVariableSubstitution, onBeforeParseTemplate);
        }
    }
}
