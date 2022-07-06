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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MinimumSwitchCases extends Recipe {
    @Override
    public String getDisplayName() {
        return "`switch` statements should have at least 3 `case` clauses";
    }

    @Override
    public String getDescription() {
        return "`switch` statements are useful when many code paths branch depending on the value of a single expression. " +
                "For just one or two code paths, the code will be more readable with `if` statements.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1301");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            final JavaTemplate ifElseIfPrimitive = JavaTemplate.builder(this::getCursor, "" +
                    "if(#{any()} == #{any()}) {\n" +
                    "} else if(#{any()} == #{any()}) {\n" +
                    "}").build();

            final JavaTemplate ifElseIfString = JavaTemplate.builder(this::getCursor, "" +
                    "if(#{any(java.lang.String)}.equals(#{any(java.lang.String)})) {\n" +
                    "} else if(#{any(java.lang.String)}.equals(#{any(java.lang.String)})) {\n" +
                    "}").build();

            final JavaTemplate ifElseIfEnum = JavaTemplate.builder(this::getCursor, "" +
                    "if(#{any()} == #{}) {\n" +
                    "} else if(#{any()} == #{}) {\n" +
                    "}").build();

            final JavaTemplate ifElsePrimitive = JavaTemplate.builder(this::getCursor, "" +
                    "if(#{any()} == #{any()}) {\n" +
                    "} else {\n" +
                    "}").build();

            final JavaTemplate ifElseString = JavaTemplate.builder(this::getCursor, "" +
                    "if(#{any(java.lang.String)}.equals(#{any(java.lang.String)})) {\n" +
                    "} else {\n" +
                    "}").build();

            final JavaTemplate ifElseEnum = JavaTemplate.builder(this::getCursor, "" +
                    "if(#{any()} == #{}) {\n" +
                    "} else {\n" +
                    "}").build();

            final JavaTemplate ifPrimitive = JavaTemplate.builder(this::getCursor, "" +
                    "if(#{any()} == #{any()}) {\n" +
                    "}").build();

            final JavaTemplate ifString = JavaTemplate.builder(this::getCursor, "" +
                    "if(#{any(java.lang.String)}.equals(#{any(java.lang.String)})) {\n" +
                    "}").build();

            final JavaTemplate ifEnum = JavaTemplate.builder(this::getCursor, "" +
                    "if(#{any()} == #{}) {\n" +
                    "}").build();

            @Override
            public J visitSwitch(J.Switch switzh, ExecutionContext ctx) {
                if (switzh.getCases().getStatements().size() < 3) {
                    J.Switch sortedSwitch = (J.Switch) new DefaultComesLast().getVisitor().visit(switzh, ctx);
                    assert sortedSwitch != null;

                    J.Case[] cases = new J.Case[2];
                    int i = 0;
                    for (Statement statement : sortedSwitch.getCases().getStatements()) {
                        if (statement instanceof J.Case) {
                            cases[i++] = (J.Case) statement;
                        }
                    }

                    if (i == 0) {
                        return super.visitSwitch(switzh, ctx);
                    }

                    Expression tree = sortedSwitch.getSelector().getTree();
                    J.If generatedIf;
                    if (TypeUtils.isString(tree.getType())) {
                        if (cases[1] == null) {
                            generatedIf = switzh.withTemplate(ifString, switzh.getCoordinates().replace(),
                                    cases[0].getPattern(), tree);
                        } else if (isDefault(cases[1])) {
                            generatedIf = switzh.withTemplate(ifElseString, switzh.getCoordinates().replace(),
                                    cases[0].getPattern(), tree);
                        } else {
                            generatedIf = switzh.withTemplate(ifElseIfString, switzh.getCoordinates().replace(),
                                    cases[0].getPattern(), tree, cases[1].getPattern(), tree);
                        }
                    } else if(switchesOnEnum(switzh)) {
                        if (cases[1] == null) {
                            generatedIf = switzh.withTemplate(ifEnum, switzh.getCoordinates().replace(),
                                    tree, enumIdentToFieldAccessString(cases[0].getPattern()));
                        } else if (isDefault(cases[1])) {
                            generatedIf = switzh.withTemplate(ifElseEnum, switzh.getCoordinates().replace(),
                                    tree, enumIdentToFieldAccessString(cases[0].getPattern()));
                        } else {
                            generatedIf = switzh.withTemplate(ifElseIfEnum, switzh.getCoordinates().replace(),
                                    tree, enumIdentToFieldAccessString(cases[0].getPattern()), tree, enumIdentToFieldAccessString(cases[1].getPattern()));
                        }
                    } else {
                        if (cases[1] == null) {
                            generatedIf = switzh.withTemplate(ifPrimitive, switzh.getCoordinates().replace(),
                                    tree, cases[0].getPattern());
                        } else if (isDefault(cases[1])) {
                            generatedIf = switzh.withTemplate(ifElsePrimitive, switzh.getCoordinates().replace(),
                                    tree, cases[0].getPattern());
                        } else {
                            generatedIf = switzh.withTemplate(ifElseIfPrimitive, switzh.getCoordinates().replace(),
                                    tree, cases[0].getPattern(), tree, cases[1].getPattern());
                        }
                    }

                    // move first case to "if"
                    List<Statement> statements = new ArrayList<>();
                    for (Statement statement : cases[0].getStatements()) {
                        if (statement instanceof J.Block) {
                            statements.addAll(((J.Block) statement).getStatements());
                        } else {
                            statements.add(statement);
                        }
                    }

                    generatedIf = generatedIf.withThenPart(((J.Block) generatedIf.getThenPart()).withStatements(ListUtils.map(statements,
                            s -> s instanceof J.Break ? null : s)));

                    // move second case to "else"
                    if (cases[1] != null) {
                        assert generatedIf.getElsePart() != null;
                        if (isDefault(cases[1])) {
                            generatedIf = generatedIf.withElsePart(generatedIf.getElsePart().withBody(((J.Block) generatedIf.getElsePart().getBody()).withStatements(ListUtils.map(cases[1].getStatements(),
                                    s -> s instanceof J.Break ? null : s))));
                        } else {
                            J.If elseIf = (J.If) generatedIf.getElsePart().getBody();
                            generatedIf = generatedIf.withElsePart(generatedIf.getElsePart().withBody(elseIf.withThenPart(((J.Block) elseIf.getThenPart()).withStatements(ListUtils.map(cases[1].getStatements(),
                                    s -> s instanceof J.Break ? null : s)))));
                        }
                    }

                    return autoFormat(generatedIf, ctx, getCursor().getParentOrThrow());
                }

                return super.visitSwitch(switzh, ctx);
            }

            private boolean isDefault(J.Case caze) {
                return caze.getPattern() instanceof J.Identifier && ((J.Identifier) caze.getPattern()).getSimpleName().equals("default");
            }

            private boolean switchesOnEnum(J.Switch switzh) {
                JavaType selectorType = switzh.getSelector().getTree().getType();
                return selectorType instanceof JavaType.Class
                        && ((JavaType.Class)selectorType).getKind() == JavaType.Class.Kind.Enum;
            }

            private String enumIdentToFieldAccessString(Expression casePattern) {
                J.Identifier ident = (J.Identifier) casePattern;
                //noinspection ConstantConditions
                return ((JavaType.Class)ident.getType()).getClassName() + "." + ident.getSimpleName();
            }

        };
    }
}
