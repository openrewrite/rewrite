/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle.gradle8;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class JacocoReportDeprecations extends Recipe {

    private static final String JACOCO_SETTINGS_INDEX = "JACOCO_SETTINGS_INDEX";

    @Override
    public String getDisplayName() {
        return "Replace Gradle 8 introduced deprecations in JaCoCo report task";
    }

    @Override
    public String getDescription() {
        return "Set the `enabled` to `required` and the `destination` to `outputLocation` for Reports deprecations that were removed in gradle 8. " +
                "See [the gradle docs on this topic](https://docs.gradle.org/current/userguide/upgrading_version_7.html#report_and_testreport_api_cleanup).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                Integer index = getCursor().getNearestMessage(JACOCO_SETTINGS_INDEX);
                if (index == null) {
                    index = 0;
                } else {
                    index++;
                }
                if (assignment.getVariable() instanceof J.FieldAccess) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) assignment.getVariable();
                    String fieldName = getFieldName(fieldAccess);
                    return replaceDeprecations(assignment, index, fieldName);
                } else if (assignment.getVariable() instanceof J.Identifier) {
                    J.Identifier identifier = (J.Identifier) assignment.getVariable();
                    return replaceDeprecations(assignment, index, identifier.getSimpleName());
                }
                return assignment;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                Integer parent = getCursor().getNearestMessage(JACOCO_SETTINGS_INDEX);
                if (parent == null) {
                    parent = 0;
                } else {
                    parent++;
                }

                // Handle method invocation syntax at various nesting levels
                if (!method.getArguments().isEmpty()) {
                    boolean shouldReplace = false;

                    // xml.enabled(false) or csv.enabled(false) - inside reports closure
                    if (parent == 2 && method.getSelect() instanceof J.Identifier) {
                        J.Identifier select = (J.Identifier) method.getSelect();
                        shouldReplace = isReportType(select.getSimpleName());
                    }
                    // reports.xml.enabled(false) - inside jacocoTestReport closure
                    else if (parent == 1 && method.getSelect() instanceof J.FieldAccess) {
                        J.FieldAccess selectField = (J.FieldAccess) method.getSelect();
                        if (selectField.getTarget() instanceof J.Identifier) {
                            J.Identifier target = (J.Identifier) selectField.getTarget();
                            shouldReplace = "reports".equalsIgnoreCase(target.getSimpleName()) &&
                                          isReportType(selectField.getSimpleName());
                        }
                    }
                    // enabled(false) - inside xml/csv/html closure
                    else if (parent == 3 && method.getSelect() == null) {
                        shouldReplace = true;
                    }

                    if (shouldReplace) {
                        J.MethodInvocation replacement = replaceDeprecatedMethodName(method);
                        if (replacement != method) {
                            return replacement;
                        }
                    }
                }

                if (isPartOfDeprecatedPath(method.getSimpleName(), parent)) {
                    getCursor().putMessage(JACOCO_SETTINGS_INDEX, parent);

                    return super.visitMethodInvocation(method, ctx);
                }
                return method;
            }

            private boolean isReportType(String name) {
                return "xml".equalsIgnoreCase(name) ||
                       "csv".equalsIgnoreCase(name) ||
                       "html".equalsIgnoreCase(name);
            }

            private J.MethodInvocation replaceDeprecatedMethodName(J.MethodInvocation method) {
                String methodName = method.getSimpleName();
                if ("enabled".equalsIgnoreCase(methodName) || "isEnabled".equalsIgnoreCase(methodName) || "setEnabled".equalsIgnoreCase(methodName)) {
                    return method.withName(method.getName().withSimpleName("required"));
                } else if ("destination".equalsIgnoreCase(methodName) || "setDestination".equalsIgnoreCase(methodName)) {
                    return method.withName(method.getName().withSimpleName("outputLocation"));
                }
                return method;
            }

            private J.Assignment replaceDeprecations(J.Assignment assignment, int index, String path) {
                if (isDeprecatedPath(path, index)) {
                    String field = path.substring(path.lastIndexOf(".") + 1);
                    if (assignment.getVariable() instanceof J.FieldAccess) {
                        J.FieldAccess fieldAccess = (J.FieldAccess) assignment.getVariable();
                        if ("enabled".equalsIgnoreCase(field) || "isEnabled".equalsIgnoreCase(field)) {
                            return assignment.withVariable(fieldAccess.withName(fieldAccess.getName().withSimpleName("required")));
                        } else if ("destination".equalsIgnoreCase(field)) {
                            return assignment.withVariable(fieldAccess.withName(fieldAccess.getName().withSimpleName("outputLocation")));
                        }
                    } else if (assignment.getVariable() instanceof J.Identifier) {
                        J.Identifier identifier = (J.Identifier) assignment.getVariable();
                        if ("enabled".equalsIgnoreCase(field) || "isEnabled".equalsIgnoreCase(field)) {
                            return assignment.withVariable(identifier.withSimpleName("required"));
                        } else if ("destination".equalsIgnoreCase(field)) {
                            return assignment.withVariable(identifier.withSimpleName("outputLocation"));
                        }
                    }
                }
                return assignment;
            }

            private boolean isPartOfDeprecatedPath(String path, int index) {
                if (StringUtils.isNullOrEmpty(path)) {
                    return false;
                }
                switch (index) {
                    case 0:
                        return "jacocoTestReport".equalsIgnoreCase(path);
                    case 1:
                        return "reports".equalsIgnoreCase(path);
                    case 2:
                        return "xml".equalsIgnoreCase(path) || "csv".equalsIgnoreCase(path) || "html".equalsIgnoreCase(path);
                    case 3:
                        return "enabled".equalsIgnoreCase(path) || "isEnabled".equalsIgnoreCase(path) || "destination".equalsIgnoreCase(path);
                    default:
                        return false;
                }
            }

            private boolean isDeprecatedPath(String path, int index) {
                if (StringUtils.isNullOrEmpty(path)) {
                    return false;
                }
                String[] parts = path.split("\\.");
                for (int i = 0; i < parts.length; i++) {
                    if (!isPartOfDeprecatedPath(parts[i], index + i)) {
                        return false;
                    }
                }
                return true;
            }

            private String getFieldName(J.FieldAccess fieldAccess) {
                String fieldName = fieldAccess.getSimpleName();
                Expression target = fieldAccess;
                while (target instanceof J.FieldAccess) {
                    target = ((J.FieldAccess) target).getTarget();
                    if (target instanceof J.Identifier) {
                        fieldName = ((J.Identifier) target).getSimpleName() + "." + fieldName;
                    } else if (target instanceof J.FieldAccess) {
                        fieldName = ((J.FieldAccess) target).getSimpleName() + "." + fieldName;
                    }
                }
                return fieldName;
            }
        });
    }
}
