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
package org.openrewrite.hcl.format;

import org.openrewrite.hcl.HclIsoVisitor;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.hcl.tree.Space;
import org.openrewrite.internal.ListUtils;

/**
 * Ensures that whitespace is on the outermost AST element possible.
 */
public class NormalizeFormatVisitor<P> extends HclIsoVisitor<P> {

    @Override
    public Hcl.Attribute visitAttribute(Hcl.Attribute attribute, P p) {
        Hcl.Attribute a = super.visitAttribute(attribute, p);

        if (a.getName().getPrefix() != Space.EMPTY) {
            a = concatenateSpace(a, a.getName().getPrefix());
            a = a.withName(a.getName().withPrefix(Space.EMPTY));
        }

        return a;
    }

    @Override
    public Hcl.AttributeAccess visitAttributeAccess(Hcl.AttributeAccess attributeAccess, P p) {
        Hcl.AttributeAccess a = super.visitAttributeAccess(attributeAccess, p);

        if (a.getAttribute().getPrefix() != Space.EMPTY) {
            a = concatenateSpace(a, a.getAttribute().getPrefix());
            a = a.withAttribute(a.getAttribute().withPrefix(Space.EMPTY));
        }

        return a;
    }

    @Override
    public Hcl.Binary visitBinary(Hcl.Binary binary, P p) {
        Hcl.Binary b = super.visitBinary(binary, p);

        if (b.getLeft().getPrefix() != Space.EMPTY) {
            b = concatenateSpace(b, b.getLeft().getPrefix());
            b = b.withLeft(b.getLeft().withPrefix(Space.EMPTY));
        }

        return b;
    }

    @Override
    public Hcl.Block visitBlock(Hcl.Block block, P p) {
        Hcl.Block b = super.visitBlock(block, p);

        Hcl.Identifier type = b.getType();
        if (type != null) {
            if (type.getPrefix() != Space.EMPTY) {
                b = concatenateSpace(b, type.getPrefix());
                b = b.withType(type.withPrefix(Space.EMPTY));
            }
        }

        return b;
    }

    @Override
    public Hcl.Conditional visitConditional(Hcl.Conditional conditional, P p) {
        Hcl.Conditional c = super.visitConditional(conditional, p);

        if (c.getCondition().getPrefix() != Space.EMPTY) {
            c = concatenateSpace(c, c.getCondition().getPrefix());
            c = c.withCondition(c.getCondition().withPrefix(Space.EMPTY));
        }

        return c;
    }

    @Override
    public Hcl.ConfigFile visitConfigFile(Hcl.ConfigFile configFile, P p) {
        Hcl.ConfigFile c = super.visitConfigFile(configFile, p);

        if (Space.firstPrefix(c.getBody()) != Space.EMPTY) {
            c = concatenateSpace(c, Space.firstPrefix(c.getBody()));
            c = c.withBody(Space.formatFirstPrefix(c.getBody(), Space.EMPTY));
        }

        return c;
    }

    @Override
    public Hcl.FunctionCall visitFunctionCall(Hcl.FunctionCall functionCall, P p) {
        Hcl.FunctionCall f = super.visitFunctionCall(functionCall, p);

        if (f.getName().getPrefix() != Space.EMPTY) {
            f = concatenateSpace(f, f.getName().getPrefix());
            f = f.withName(f.getName().withPrefix(Space.EMPTY));
        }

        return f;
    }

    @Override
    public Hcl.Index visitIndex(Hcl.Index index, P p) {
        Hcl.Index i = super.visitIndex(index, p);

        if (i.getIndexed().getPrefix() != Space.EMPTY) {
            i = concatenateSpace(i, i.getIndexed().getPrefix());
            i = i.withIndexed(i.getIndexed().withPrefix(Space.EMPTY));
        }

        return i;
    }

    @Override
    public Hcl.Unary visitUnary(Hcl.Unary unary, P p) {
        Hcl.Unary u = super.visitUnary(unary, p);

        if (u.getExpression().getPrefix() != Space.EMPTY) {
            u = concatenateSpace(u, u.getExpression().getPrefix());
            u = u.withExpression(u.getExpression().withPrefix(Space.EMPTY));
        }

        return u;
    }

    @Override
    public Hcl.VariableExpression visitVariableExpression(Hcl.VariableExpression variableExpression, P p) {
        Hcl.VariableExpression v = super.visitVariableExpression(variableExpression, p);

        if (v.getName().getPrefix() != Space.EMPTY) {
            v = concatenateSpace(v, v.getName().getPrefix());
            v = v.withName(v.getName().withPrefix(Space.EMPTY));
        }

        return v;
    }

    private <H extends Hcl> H concatenateSpace(H h, Space prefix) {
        Hcl h2 = h;

        if (h2.getPrefix().getComments().isEmpty()) {
            h2 = h2.withPrefix(h2.getPrefix().withWhitespace(h2.getPrefix().getWhitespace() + prefix.getWhitespace()));
        } else {
            h2 = h2.withPrefix(h2.getPrefix().withComments(ListUtils.mapLast(h2.getPrefix().getComments(), c -> c
                    .withSuffix(c.getSuffix() + prefix.getWhitespace()))));
        }

        if (!prefix.getComments().isEmpty()) {
            h2 = h2.withPrefix(h2.getPrefix().withComments(ListUtils.concatAll(h2.getPrefix().getComments(), prefix.getComments())));
        }

        //noinspection unchecked
        return (H) h2;
    }
}
