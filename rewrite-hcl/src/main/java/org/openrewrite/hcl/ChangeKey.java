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
package org.openrewrite.hcl;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.hcl.format.AttributeSpaceVisitor;
import org.openrewrite.hcl.style.SpacesStyle;
import org.openrewrite.hcl.tree.BodyContent;
import org.openrewrite.hcl.tree.Expression;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.hcl.tree.Space;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.Style;

import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeKey extends Recipe {
    private static final Pattern IDENTIFIER = Pattern.compile("[\\p{L}$_][\\p{L}\\p{N}$_-]*");
    private static final String RENAMED = "RENAMED_ATTRIBUTE";

    @Option(displayName = "Old key path",
            description = "A JSONPath expression locating the attribute to rename.",
            example = "$.resource.name")
    String oldKeyPath;

    @Option(displayName = "New key",
            description = "The new name of the attribute selected by `oldKeyPath`.",
            example = "db_name")
    String newKey;

    String displayName = "Change key";

    String description = "Rename an HCL attribute while leaving its value intact. Attributes with a computed key, " +
                         "or with a sibling attribute or block already named `newKey`, are left unchanged.";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", oldKeyPath, newKey);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(oldKeyPath);
        return new HclIsoVisitor<ExecutionContext>() {
            @Override
            public Hcl.Attribute visitAttribute(Hcl.Attribute attribute, ExecutionContext ctx) {
                Hcl.Attribute a = super.visitAttribute(attribute, ctx);
                if (newKey.equals(a.getSimpleName()) || !matcher.matches(getCursor())) {
                    return a;
                }
                Cursor parent = getCursor().getParentTreeCursor();
                if (parent.getMessage(RENAMED, false) || hasSiblingNamedNewKey(parent.getValue(), attribute)) {
                    return a;
                }
                Expression name = rename(a.getName(), parent.getValue() instanceof Hcl.ObjectValue);
                if (name == null) {
                    return a;
                }
                parent.putMessage(RENAMED, true);
                return a.withName(name);
            }

            @Override
            public Hcl.Block visitBlock(Hcl.Block block, ExecutionContext ctx) {
                Hcl.Block b = super.visitBlock(block, ctx);
                if (getCursor().getMessage(RENAMED, false)) {
                    b = b.withBody(realign(block, block.getBody(), b, b.getBody(), ctx));
                }
                return b;
            }

            @Override
            public Hcl.ObjectValue visitObjectValue(Hcl.ObjectValue objectValue, ExecutionContext ctx) {
                Hcl.ObjectValue o = super.visitObjectValue(objectValue, ctx);
                if (getCursor().getMessage(RENAMED, false)) {
                    o = o.withArguments(realign(objectValue, objectValue.getAttributes(), o, o.getAttributes(), ctx));
                }
                return o;
            }

            @SuppressWarnings("unchecked")
            private <T extends Hcl> List<T> realign(Hcl parentBefore, List<T> before, Hcl parentAfter, List<T> after,
                                                    ExecutionContext ctx) {
                SpacesStyle style = Style.from(SpacesStyle.class, getCursor().firstEnclosingOrThrow(Hcl.ConfigFile.class),
                        () -> SpacesStyle.DEFAULT);
                AttributeSpaceVisitor<ExecutionContext> realigner = new AttributeSpaceVisitor<ExecutionContext>(style) {
                    @Override
                    public Hcl.ObjectValue visitObjectValue(Hcl.ObjectValue objectValue, ExecutionContext ctx) {
                        return objectValue;
                    }
                };
                Cursor cursorBefore = new Cursor(getCursor().getParentOrThrow(), parentBefore);
                Cursor cursorAfter = new Cursor(getCursor().getParentOrThrow(), parentAfter);
                return ListUtils.map(after, (i, e) -> {
                    T original = before.get(i);
                    if (!(e instanceof Hcl.Attribute) || !(original instanceof Hcl.Attribute) ||
                        !typeSpace(realigner.visitNonNull(original, ctx, cursorBefore)).equals(typeSpace(original))) {
                        return e;
                    }
                    return (T) realigner.visitNonNull(e, ctx, cursorAfter);
                });
            }
        };
    }

    private static String typeSpace(Hcl attribute) {
        return ((Hcl.Attribute) attribute).getPadding().getType().getBefore().getWhitespace();
    }

    private boolean hasSiblingNamedNewKey(Object parent, Hcl.Attribute attribute) {
        for (Hcl sibling : siblings(parent)) {
            if (sibling == attribute) {
                continue;
            }
            if (sibling instanceof Hcl.Attribute && newKey.equals(((Hcl.Attribute) sibling).getSimpleName())) {
                return true;
            }
            if (sibling instanceof Hcl.Block) {
                Hcl.Identifier type = ((Hcl.Block) sibling).getType();
                if (type != null && newKey.equals(type.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<? extends Hcl> siblings(Object parent) {
        if (parent instanceof Hcl.Block) {
            return ((Hcl.Block) parent).getBody();
        } else if (parent instanceof Hcl.ObjectValue) {
            return ((Hcl.ObjectValue) parent).getAttributes();
        } else if (parent instanceof Hcl.ConfigFile) {
            return ((Hcl.ConfigFile) parent).getBody();
        }
        return emptyList();
    }

    private @Nullable Expression rename(Expression name, boolean isObjectKey) {
        if (name instanceof Hcl.Identifier) {
            if (IDENTIFIER.matcher(newKey).matches() && !(isObjectKey && "for".equals(newKey))) {
                return ((Hcl.Identifier) name).withName(newKey);
            }
            if (isObjectKey) {
                return new Hcl.QuotedTemplate(Tree.randomId(), name.getPrefix(), name.getMarkers(),
                        singletonList(quotedLiteral(Space.EMPTY)));
            }
        } else if (name instanceof Hcl.QuotedTemplate) {
            Hcl.QuotedTemplate q = (Hcl.QuotedTemplate) name;
            List<Expression> parts = q.getExpressions();
            if (parts.isEmpty()) {
                return q.withExpressions(singletonList(quotedLiteral(Space.EMPTY)));
            }
            if (parts.size() == 1 && parts.get(0) instanceof Hcl.Literal) {
                return q.withExpressions(singletonList(quotedLiteral(parts.get(0).getPrefix())));
            }
        }
        return null;
    }

    private Hcl.Literal quotedLiteral(Space prefix) {
        String source = newKey
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("${", "$${")
                .replace("%{", "%%{");
        return new Hcl.Literal(Tree.randomId(), prefix, Markers.EMPTY, source, source);
    }
}
