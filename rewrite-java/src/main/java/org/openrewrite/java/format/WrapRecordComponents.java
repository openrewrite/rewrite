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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.service.SourcePositionService;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.style.LineWrapSetting;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class WrapRecordComponents<P> extends JavaIsoVisitor<P> {

    WrappingAndBracesStyle style;

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P ctx) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);

        try {
            // styles are parent loaded, so the getters may or may not be present and they may or may not return null
            if (style != null && style.getRecordComponents() != null && style.getRecordComponents().getWrap() != LineWrapSetting.DoNotWrap) {
                JContainer<Statement> primaryConstructor = c.getPadding().getPrimaryConstructor();
                if (primaryConstructor == null || primaryConstructor.getElements().size() <= 1) {
                    return c;
                }
                List<Statement> components = primaryConstructor.getElements();
                JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                if (style.getRecordComponents().getWrap() == LineWrapSetting.ChopIfTooLong) {
                    if (sourceFile == null) {
                        return c;
                    }
                    if (sourceFile.service(SourcePositionService.class).positionOf(getCursor(), primaryConstructor).getMaxColumn() <= style.getHardWrapAt()) {
                        return c;
                    }
                }

                if (style.getRecordComponents().getCloseNewLine()) {
                    c = c.getPadding().withPrimaryConstructor(primaryConstructor.getPadding().withElements(ListUtils.mapLast(primaryConstructor.getPadding().getElements(), rightPaddedParam -> {
                        Space after = rightPaddedParam.getAfter();
                        if (after.getLastWhitespace().contains("\n")) {
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

                c = c.withPrimaryConstructor(ListUtils.map(components, (paramIndex, param) -> {
                    if (param instanceof J.Empty) {
                        return param;
                    }
                    Space prefix = param.getPrefix();
                    if (prefix.getComments().isEmpty()) {
                        if ((paramIndex != 0 || style.getRecordComponents().getOpenNewLine()) && !prefix.getWhitespace().contains("\n")) {
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
                        if (index == -1 && (paramIndex != 0 || style.getRecordComponents().getOpenNewLine())) {
                            if (prefix.getComments().isEmpty()) {
                                if (!prefix.getWhitespace().contains("\n")) {
                                    prefix = prefix.withWhitespace("\n");
                                }
                            } else {
                                prefix = prefix.withComments(ListUtils.mapLast(prefix.getComments(),
                                        comment -> comment.withSuffix("\n")));
                            }
                        }
                    }
                    return param.withPrefix(prefix);
                }));
            }
        } catch (NoSuchMethodError | NoSuchFieldError ignore) {
            // Styles are parent-first loaded and this can happen if the style is from a older version of the runtime. Can be removed in future releases.
        }

        return c;
    }
}
