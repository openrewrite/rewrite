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
import org.openrewrite.template.SourceTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class HclTemplate implements SourceTemplate<Hcl, HclCoordinates> {
    private final String code;
    private final int parameterCount;
    private final Consumer<String> onAfterVariableSubstitution;
    private final HclTemplateParser templateParser;

    private HclTemplate(String code, Consumer<String> onAfterVariableSubstitution, Consumer<String> onBeforeParseTemplate) {
        this.code = code;
        this.onAfterVariableSubstitution = onAfterVariableSubstitution;
        this.parameterCount = StringUtils.countOccurrences(code, "#{");
        this.templateParser = new HclTemplateParser(onAfterVariableSubstitution, onBeforeParseTemplate);
    }

    public static <H extends Hcl> H apply(String template, Cursor scope, HclCoordinates coordinates, Object... parameters) {
        return builder(template).build().apply(scope, coordinates, parameters);
    }

    @Override
    public <H extends Hcl> H apply(Cursor scope, HclCoordinates coordinates, Object... parameters) {
        if (!(scope.getValue() instanceof Hcl)) {
            throw new IllegalArgumentException("`scope` must point to a Hcl instance.");
        }

        if (parameters.length != parameterCount) {
            throw new IllegalArgumentException("This template requires " + parameterCount + " parameters.");
        }

        Substitutions substitutions = new Substitutions(code, parameters);
        String substitutedTemplate = substitutions.substitute();
        onAfterVariableSubstitution.accept(substitutedTemplate);

        Tree insertionPoint = coordinates.getTree();
        Location loc = coordinates.getSpaceLocation();

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
                            for (BodyContent ignored : gen) {
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
        }.visit(scope.getValue(), 0, scope.getParentOrThrow());

        assert h != null;
        return h;
    }

    public static Builder builder(String code) {
        return new Builder(code);
    }

    public static class Builder {
        private final String code;

        private Consumer<String> onAfterVariableSubstitution = s -> {
        };
        private Consumer<String> onBeforeParseTemplate = s -> {
        };

        Builder(String code) {
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
            return new HclTemplate(code, onAfterVariableSubstitution, onBeforeParseTemplate);
        }
    }
}
