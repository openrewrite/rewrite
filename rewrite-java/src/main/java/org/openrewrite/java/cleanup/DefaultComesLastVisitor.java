/*
 * Copyright 2020 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.DefaultComesLastStyle;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = true)
public class DefaultComesLastVisitor<P> extends JavaIsoVisitor<P> {
    DefaultComesLastStyle style;

    @Override
    public J.Switch visitSwitch(J.Switch switzh, P p) {
        J.Switch s = visitAndCast(switzh, p, super::visitSwitch);

        if (!isDefaultCaseLastOrNotPresent(switzh)) {
            List<J.Case> cases = s.getCases().getStatements().stream().map(J.Case.class::cast).collect(Collectors.toList());
            List<J.Case> fixedCases = new ArrayList<>(cases.size());

            int defaultCaseIndex = -1;
            J.Case defaultCase = null;

            for (int i = 0; i < cases.size(); i++) {
                J.Case aCase = cases.get(i);
                if (isDefaultCase(aCase)) {
                    defaultCaseIndex = i;
                    defaultCase = aCase;
                }
            }

            List<J.Case> casesGroupedWithDefault = new ArrayList<>();
            boolean foundNonEmptyCase = false;
            for (int i = defaultCaseIndex - 1; i >= 0; i--) {
                J.Case aCase = cases.get(i);
                if (aCase.getStatements().isEmpty() && !foundNonEmptyCase) {
                    casesGroupedWithDefault.add(0, aCase);
                } else {
                    foundNonEmptyCase = true;
                    fixedCases.add(0, aCase);
                }
            }

            foundNonEmptyCase = false;
            for (int i = defaultCaseIndex + 1; i < cases.size(); i++) {
                J.Case aCase = cases.get(i);
                if (defaultCase != null && defaultCase.getStatements().isEmpty() &&
                    aCase.getStatements().isEmpty() && !foundNonEmptyCase) {
                    casesGroupedWithDefault.add(aCase);
                } else {
                    if (defaultCase != null && defaultCase.getStatements().isEmpty() && !foundNonEmptyCase) {
                        // the last case grouped with default can be non-empty. it will be flipped with
                        // the default case, including its statements
                        casesGroupedWithDefault.add(aCase);
                    }
                    foundNonEmptyCase = true;
                    fixedCases.add(aCase);
                }
            }

            if (defaultCase != null && !casesGroupedWithDefault.isEmpty()) {
                J.Case lastGroupedWithDefault = casesGroupedWithDefault.get(casesGroupedWithDefault.size() - 1);
                if (!lastGroupedWithDefault.getStatements().isEmpty()) {
                    casesGroupedWithDefault.set(casesGroupedWithDefault.size() - 1,
                            lastGroupedWithDefault.withStatements(Collections.emptyList()));
                    defaultCase = defaultCase.withStatements(lastGroupedWithDefault.getStatements());
                }
            }

            J.Case lastNotGroupedWithDefault = fixedCases.get(fixedCases.size() - 1);
            if (!lastNotGroupedWithDefault.getStatements().stream().reduce((s1, s2) -> s2)
                    .map(stat -> stat instanceof J.Break || stat instanceof J.Continue ||
                                 stat instanceof J.Return || stat instanceof J.Throw)
                    .orElse(false)) {

                // add a break statement since this case is now no longer last and would fall through
                List<Statement> statementsOfCaseBeingMoved = new ArrayList<>(lastNotGroupedWithDefault.getStatements());
                J.Break breakStatement = autoFormat(
                        new J.Break(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null),
                        p, getCursor().getParentOrThrow()
                );
                statementsOfCaseBeingMoved.add(breakStatement);

                lastNotGroupedWithDefault = lastNotGroupedWithDefault.withStatements(
                        ListUtils.map(statementsOfCaseBeingMoved, stmt -> autoFormat(stmt, p))
                );
                fixedCases.set(fixedCases.size() - 1, lastNotGroupedWithDefault);
            }

            fixedCases.addAll(casesGroupedWithDefault);
            if (defaultCase != null) {
                if (defaultCase.getStatements().stream().reduce((s1, s2) -> s2)
                        .map(stat -> stat instanceof J.Break || stat instanceof J.Continue || isVoidReturn(stat))
                        .orElse(false)) {
                    List<Statement> fixedDefaultStatements = new ArrayList<>(defaultCase.getStatements());
                    fixedDefaultStatements.remove(fixedDefaultStatements.size() - 1);
                    fixedCases.add(defaultCase.withStatements(fixedDefaultStatements));
                } else {
                    fixedCases.add(defaultCase);
                }
            }

            boolean changed = true;
            if (cases.size() == fixedCases.size()) {
                changed = false;
                for (int i = 0; i < cases.size(); i++) {
                    if (cases.get(i) != fixedCases.get(i)) {
                        changed = true;
                        break;
                    }
                }
            }

            if (changed) {
                s = s.withCases(s.getCases().withStatements(fixedCases.stream().map(Statement.class::cast).collect(Collectors.toList())));
            }
        }

        return s;
    }

    private boolean isVoidReturn(Statement stat) {
        return stat instanceof J.Return && ((J.Return) stat).getExpression() == null;
    }

    private boolean isDefaultCaseLastOrNotPresent(J.Switch switzh) {
        J.Case defaultCase = null;
        J.Case prior = null;
        for (Statement aCaseStmt : switzh.getCases().getStatements()) {
            if (!(aCaseStmt instanceof J.Case)) {
                continue;
            }

            J.Case aCase = (J.Case) aCaseStmt;

            if (defaultCase != null) {
                // default case was not last
                return false;
            }

            if (isDefaultCase(aCase)) {
                defaultCase = aCase;
            }

            if (defaultCase != null && prior != null && Boolean.TRUE.equals(style.getSkipIfLastAndSharedWithCase()) && prior.getStatements().isEmpty()) {
                return true;
            }

            prior = aCase;
        }

        // either default was not present or it was last
        return true;
    }

    private boolean isDefaultCase(J.Case caze) {
        Expression elem = caze.getPattern();
        return elem instanceof J.Identifier && ((J.Identifier) elem).getSimpleName().equals("default");
    }

}
