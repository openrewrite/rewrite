/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.format;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.service.SourcePositionService;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.style.LineWrapSetting;

import static org.openrewrite.java.format.MinimizationVisitor.minimized;

@Value
@EqualsAndHashCode(callSuper = false)
public class WrapMethodInvocationArguments<P> extends JavaIsoVisitor<P> {

    WrappingAndBracesStyle style;

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P ctx) {
        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

        try {
            // styles are parent loaded, so the getters may or may not be present and they may or may not return null
            if (style != null && style.getMethodCallArguments() != null && style.getMethodCallArguments().getWrap() != LineWrapSetting.DoNotWrap) {
                if (m.getArguments().size() <= 1) {
                    return m;
                }
                JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                if (style.getMethodCallArguments().getWrap() == LineWrapSetting.ChopIfTooLong) {
                    if (sourceFile == null) {
                        return m;
                    }
                    Cursor minimized = minimized(getCursor(), sourceFile);
                    if (sourceFile.service(SourcePositionService.class).positionOf(minimized, ((J.MethodInvocation) minimized.getValue())).getMaxColumn() <= style.getHardWrapAt()) {
                        return m;
                    }
                }

                if (style.getMethodCallArguments().getCloseNewLine()) {
                    m = m.getPadding().withArguments(m.getPadding().getArguments().getPadding().withElements(ListUtils.mapLast(m.getPadding().getArguments().getPadding().getElements(), rightPaddedParam -> {
                        Space after = rightPaddedParam.getAfter();
                        if (after.getWhitespace().contains("\n")) {
                            return rightPaddedParam;
                        }
                        if (after.getComments().isEmpty()) {
                            after = after.withWhitespace("\n");
                        } else {
                            after = after.withComments(ListUtils.mapLast(after.getComments(), comment -> comment.withSuffix("\n")));
                        }
                        return rightPaddedParam.withAfter(after);
                    })));
                }

                m = m.withArguments(ListUtils.map(m.getArguments(), (paramIndex, param) -> {
                    if (param instanceof J.Empty) {
                        return param;
                    }
                    Space prefix = param.getPrefix();
                    if (prefix.getComments().isEmpty()) {
                        if ((paramIndex != 0 || style.getMethodCallArguments().getOpenNewLine()) && !prefix.getWhitespace().contains("\n")) {
                            prefix = prefix.withWhitespace("\n");
                        }
                    } else {
                        int index = -1;
                        for (int i = prefix.getComments().size() - 1; i >= 0; i--) {
                            if (prefix.getComments().get(i).getSuffix().contains("\n")) {
                                index = i;
                                break;
                            }
                        }
                        if (index == -1 && (paramIndex != 0 || style.getMethodCallArguments().getOpenNewLine())) {
                            if (!prefix.getWhitespace().contains("\n")) {
                                prefix = prefix.withWhitespace("\n");
                            }
                        }
                    }
                    return param.withPrefix(prefix);
                }));
            }
        } catch (NoSuchMethodError | NoSuchFieldError ignore) {
            // Styles are parent-first loaded and this can happen if the style is from a older version of the runtime. Can be removed in future releases.
        }

        return m;
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, P ctx) {
        J.NewClass n = super.visitNewClass(newClass, ctx);

        try {
            // styles are parent loaded, so the getters may or may not be present and they may or may not return null
            if (style != null && style.getMethodCallArguments() != null && style.getMethodCallArguments().getWrap() != LineWrapSetting.DoNotWrap) {
                if (n.getArguments() == null || n.getArguments().size() <= 1) {
                    return n;
                }
                JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                if (style.getMethodCallArguments().getWrap() == LineWrapSetting.ChopIfTooLong) {
                    if (sourceFile == null) {
                        return n;
                    }
                    Cursor minimized = minimized(getCursor(), sourceFile);
                    if (sourceFile.service(SourcePositionService.class).positionOf(minimized, ((J.NewClass) minimized.getValue()).getPadding().getArguments()).getMaxColumn() <= style.getHardWrapAt()) {
                        return n;
                    }
                }

                if (style.getMethodCallArguments().getCloseNewLine()) {
                    n = n.getPadding().withArguments(n.getPadding().getArguments().getPadding().withElements(ListUtils.mapLast(n.getPadding().getArguments().getPadding().getElements(), rightPaddedParam -> {
                        Space after = rightPaddedParam.getAfter();
                        if (after.getWhitespace().contains("\n")) {
                            return rightPaddedParam;
                        }
                        if (after.getComments().isEmpty()) {
                            after = after.withWhitespace("\n");
                        } else {
                            after = after.withComments(ListUtils.mapLast(after.getComments(), comment -> comment.withSuffix("\n")));
                        }
                        return rightPaddedParam.withAfter(after);
                    })));
                }

                n = n.withArguments(ListUtils.map(n.getArguments(), (paramIndex, param) -> {
                    if (param instanceof J.Empty) {
                        return param;
                    }
                    Space prefix = param.getPrefix();
                    if (prefix.getComments().isEmpty()) {
                        if ((paramIndex != 0 || style.getMethodCallArguments().getOpenNewLine()) && !prefix.getWhitespace().contains("\n")) {
                            prefix = prefix.withWhitespace("\n");
                        }
                    } else {
                        int index = -1;
                        for (int i = prefix.getComments().size() - 1; i >= 0; i--) {
                            if (prefix.getComments().get(i).getSuffix().contains("\n")) {
                                index = i;
                                break;
                            }
                        }
                        if (index == -1 && (paramIndex != 0 || style.getMethodCallArguments().getOpenNewLine())) {
                            if (!prefix.getWhitespace().contains("\n")) {
                                prefix = prefix.withWhitespace("\n");
                            }
                        }
                    }
                    return param.withPrefix(prefix);
                }));
            }
        } catch (NoSuchMethodError | NoSuchFieldError ignore) {
            // Styles are parent-first loaded and this can happen if the style is from a older version of the runtime. Can be removed in future releases.
        }

        return n;
    }
}
