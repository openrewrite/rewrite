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
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.service.SourcePositionService;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.style.LineWrapSetting;

import java.util.Objects;

@Value
@EqualsAndHashCode(callSuper = false)
public class WrapMethodDeclarationParameters<P> extends JavaIsoVisitor<P> {

    SpacesStyle spacesStyle;
    WrappingAndBracesStyle style;

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P ctx) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

        try {
            // styles are parent loaded, so the getters may or may not be present and they may or may not return null
            if (style != null && style.getMethodDeclarationParameters() != null && style.getMethodDeclarationParameters().getWrap() != LineWrapSetting.DoNotWrap) {
                if (m.getParameters().size() <= 1) {
                    return m;
                }
                JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                if (style.getMethodDeclarationParameters().getWrap() == LineWrapSetting.ChopIfTooLong) {
                    if (sourceFile == null) {
                        return m;
                    }
                    Cursor minimized = minimize(getCursor(), ctx);
                    if (sourceFile.service(SourcePositionService.class).positionOf(minimized, ((J.MethodDeclaration)minimized.getValue()).getPadding().getParameters()).getMaxColumn() <= style.getHardWrapAt()) {
                        return m;
                    }
                }

                if (style.getMethodDeclarationParameters().getCloseNewLine()) {
                    m = m.getPadding().withParameters(m.getPadding().getParameters().getPadding().withElements(ListUtils.mapLast(m.getPadding().getParameters().getPadding().getElements(), rightPaddedParam -> {
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

                m = m.withParameters(ListUtils.map(m.getParameters(), (paramIndex, param) -> {
                    if (param instanceof J.Empty) {
                        return param;
                    }
                    Space prefix = param.getPrefix();
                    if (prefix.getComments().isEmpty()) {
                        if ((paramIndex != 0 || style.getMethodDeclarationParameters().getOpenNewLine()) && !prefix.getWhitespace().contains("\n")) {
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
                        if (index == -1 && (paramIndex != 0 || style.getMethodDeclarationParameters().getOpenNewLine())) {
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

    public Cursor minimize(Cursor cursor, P ctx) {
        if (cursor.getValue() instanceof J) {
            J cursorValue = cursor.getValue();
            J j = new JavaIsoVisitor<P>() {
                @Override
                public @Nullable <T> JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, P ctx) {
                    switch (loc) {
                        case METHOD_DECLARATION_PARAMETER:
                        case RECORD_STATE_VECTOR: {
                            if (right != null && right.getElement() instanceof J) {
                                //noinspection unchecked
                                right = right
                                        .withAfter(minimized(right.getAfter()))
                                        .withElement(((J) right.getElement()).withPrefix(minimized(((J) right.getElement()).getPrefix())));
                            }
                            break;
                        }
                    }
                    return super.visitRightPadded(right, loc, ctx);
                }

                @Override
                public Space visitSpace(@Nullable Space space, Space.Location loc, P ctx) {
                    if (space == null) {
                        return super.visitSpace(space, loc, ctx);
                    }
                    if (space == cursorValue.getPrefix()) {
                        return space;
                    }
                    switch (loc) {
                        case BLOCK_PREFIX:
                        case MODIFIER_PREFIX:
                        case METHOD_DECLARATION_PARAMETER_SUFFIX:
                        case METHOD_DECLARATION_PARAMETERS:
                        case METHOD_SELECT_SUFFIX:
                        case METHOD_INVOCATION_ARGUMENTS:
                        case METHOD_INVOCATION_ARGUMENT_SUFFIX:
                        case METHOD_INVOCATION_NAME:
                        case RECORD_STATE_VECTOR_SUFFIX: {
                            space = minimized(space);
                            break;
                        }
                    }
                    return super.visitSpace(space, loc, ctx);
                }

                //IntelliJ does not format when comments are present.
                private Space minimized(Space space) {
                    if (space.getComments().isEmpty()) {
                        return space.getWhitespace().isEmpty() ? space : Space.EMPTY;
                    }
                    return space;
                }
            }.visit(cursorValue, ctx);
            if (j != cursor.getValue()) {
                j = new MinimumViableSpacingVisitor<>(null).visit(j, ctx);
                j = new SpacesVisitor<>(spacesStyle, null, null).visit(j, ctx);
            }
            return new Cursor(cursor.getParent(), Objects.requireNonNull(j));
        }
        throw new IllegalArgumentException("Can only minimize J elements.");
    }
}
