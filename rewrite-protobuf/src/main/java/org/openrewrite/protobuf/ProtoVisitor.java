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
package org.openrewrite.protobuf;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.protobuf.tree.*;

import java.util.List;

public class ProtoVisitor<P> extends TreeVisitor<Proto, P> {

    public Proto visitBlock(Proto.Block block, P p) {
        Proto.Block b = block;
        b = b.withPrefix(visitSpace(b.getPrefix(), p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        return b.getPadding().withStatements(ListUtils.map(b.getPadding().getStatements(), s -> visitRightPadded(s, p)));
    }

    public Proto visitConstant(Proto.Constant constant, P p) {
        Proto.Constant c = constant;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        return c.withMarkers(visitMarkers(c.getMarkers(), p));
    }

    public Proto visitDocument(Proto.Document document, P p) {
        Proto.Document d = document;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withSyntax((Proto.Syntax) visitSyntax(d.getSyntax(), p));
        return d.getPadding().withBody(ListUtils.map(d.getPadding().getBody(), it -> visitRightPadded(it, p)));
    }

    public Proto visitEmpty(Proto.Empty empty, P p) {
        Proto.Empty e = empty;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        return e.withMarkers(visitMarkers(e.getMarkers(), p));
    }

    public Proto visitEnum(Proto.Enum anEnum, P p) {
        Proto.Enum e = anEnum;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withName((Proto.Identifier) visit(e.getName(), p));
        return e.withBody((Proto.Block) visit(e.getBody(), p));
    }

    public Proto visitEnumField(Proto.EnumField enumField, P p) {
        Proto.EnumField f = enumField;
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = f.getPadding().withName(visitRightPadded(f.getPadding().getName(), p));
        f = f.withNumber((Proto.Constant) visit(f.getNumber(), p));
        return f.getPadding().withOptions(visitContainer(f.getPadding().getOptions(), p));
    }

    public Proto visitExtend(Proto.Extend extend, P p) {
        Proto.Extend e = extend;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withName((Proto.FullIdentifier) visitFullIdentifier(e.getName(), p));
        return e.withBody((Proto.Block) visit(e.getBody(), p));
    }

    public Proto visitExtensionName(Proto.ExtensionName extensionName, P p) {
        Proto.ExtensionName e = extensionName;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        return e.getPadding().withExtension(visitRightPadded(e.getPadding().getExtension(), p));
    }

    public Proto visitField(Proto.Field field, P p) {
        Proto.Field f = field;
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = f.withLabel((Proto.Keyword) visit(f.getLabel(), p));
        f = f.withType((TypeTree) visit(f.getType(), p));
        f = f.getPadding().withName(visitRightPadded(f.getPadding().getName(), p));
        f = f.withNumber((Proto.Constant) visit(f.getNumber(), p));
        return f.getPadding().withOptions(visitContainer(f.getPadding().getOptions(), p));
    }

    public Proto visitFullIdentifier(Proto.FullIdentifier fullIdentifier, P p) {
        Proto.FullIdentifier i = fullIdentifier;
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.getPadding().withTarget(visitRightPadded(i.getPadding().getTarget(), p));
        return i.withName((Proto.Identifier) visit(i.getName(), p));
    }

    public Proto visitIdentifier(Proto.Identifier identifier, P p) {
        Proto.Identifier i = identifier;
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        return i.withMarkers(visitMarkers(i.getMarkers(), p));
    }

    public Proto visitImport(Proto.Import anImport, P p) {
        Proto.Import i = anImport;
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.withModifier((Proto.Keyword) visit(i.getModifier(), p));
        return i.getPadding().withName(visitRightPadded(i.getPadding().getName(), p));
    }

    public Proto visitKeyword(Proto.Keyword keyword, P p) {
        Proto.Keyword k = keyword;
        k = k.withPrefix(visitSpace(k.getPrefix(), p));
        return k.withMarkers(visitMarkers(k.getMarkers(), p));
    }

    public Proto visitMapField(Proto.MapField mapField, P p) {
        Proto.MapField m = mapField;
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        m = m.getPadding().withMap(visitRightPadded(m.getPadding().getMap(), p));
        m = m.getPadding().withKeyType(visitRightPadded(m.getPadding().getKeyType(), p));
        m = m.getPadding().withValueType(visitRightPadded(m.getPadding().getValueType(), p));
        m = m.getPadding().withName(visitRightPadded(m.getPadding().getName(), p));
        return m.getPadding().withOptions(visitContainer(m.getPadding().getOptions(), p));
    }

    public Proto visitMessage(Proto.Message message, P p) {
        Proto.Message m = message;
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        m = m.withName((Proto.Identifier) visit(m.getName(), p));
        return m.withBody((Proto.Block) visit(m.getBody(), p));
    }

    public Proto visitOneOf(Proto.OneOf oneOf, P p) {
        Proto.OneOf o = oneOf;
        o = o.withPrefix(visitSpace(o.getPrefix(), p));
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        o = o.withName((Proto.Identifier) visit(o.getName(), p));
        return o.withFields((Proto.Block) visit(o.getFields(), p));
    }

    public Proto visitOption(Proto.Option option, P p) {
        Proto.Option o = option;
        o = o.withPrefix(visitSpace(o.getPrefix(), p));
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        o = o.getPadding().withName(visitRightPadded(o.getPadding().getName(), p));
        return o.withAssignment((Proto.Constant) visit(o.getAssignment(), p));
    }

    public Proto visitOptionDeclaration(Proto.OptionDeclaration optionDeclaration, P p) {
        Proto.OptionDeclaration o = optionDeclaration;
        o = o.withPrefix(visitSpace(o.getPrefix(), p));
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        o = o.getPadding().withName(visitRightPadded(o.getPadding().getName(), p));
        return o.withAssignment((Proto.Constant) visit(o.getAssignment(), p));
    }

    public Proto visitPackage(Proto.Package aPackage, P p) {
        Proto.Package pkg = aPackage;
        pkg = pkg.withPrefix(visitSpace(pkg.getPrefix(), p));
        pkg = pkg.withMarkers(visitMarkers(pkg.getMarkers(), p));
        return pkg.withName((Proto.FullIdentifier) visit(pkg.getName(), p));
    }

    public Proto visitPrimitive(Proto.Primitive primitive, P p) {
        Proto.Primitive pr = primitive;
        pr = pr.withPrefix(visitSpace(pr.getPrefix(), p));
        return pr.withMarkers(visitMarkers(pr.getMarkers(), p));
    }

    public Proto visitRange(Proto.Range range, P p) {
        Proto.Range r = range;
        r = r.withPrefix(visitSpace(r.getPrefix(), p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        r = r.getPadding().withFrom(visitRightPadded(r.getPadding().getFrom(), p));
        return r.withTo((Proto.Constant) visit(r.getTo(), p));
    }

    public Proto visitReserved(Proto.Reserved reserved, P p) {
        Proto.Reserved r = reserved;
        r = r.withPrefix(visitSpace(r.getPrefix(), p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        return r.getPadding().withReservations(visitContainer(r.getPadding().getReservations(), p));
    }

    public Proto visitRpc(Proto.Rpc rpc, P p) {
        Proto.Rpc r = rpc;
        r = r.withPrefix(visitSpace(r.getPrefix(), p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        r = r.withName((Proto.Identifier) visit(r.getName(), p));
        r = r.withRequest((Proto.RpcInOut) visit(r.getRequest(), p));
        r = r.withReturns((Proto.Keyword) visit(r.getReturns(), p));
        r = r.withResponse((Proto.RpcInOut) visit(r.getResponse(), p));
        return r.withBody((Proto.Block) visit(r.getBody(), p));
    }

    public Proto visitRpcInOut(Proto.RpcInOut rpcInOut, P p) {
        Proto.RpcInOut r = rpcInOut;
        r = r.withPrefix(visitSpace(r.getPrefix(), p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        r = r.withStream((Proto.Keyword) visit(r.getStream(), p));
        return r.getPadding().withType(visitRightPadded(r.getPadding().getType(), p));
    }

    public Proto visitService(Proto.Service service, P p) {
        Proto.Service s = service;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withName((Proto.Identifier) visit(s.getName(), p));
        return s.withBody((Proto.Block) visit(s.getBody(), p));
    }

    public Proto visitStringLiteral(Proto.StringLiteral stringLiteral, P p) {
        Proto.StringLiteral s = stringLiteral;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        return s.withMarkers(visitMarkers(s.getMarkers(), p));
    }

    public Proto visitSyntax(Proto.Syntax syntax, P p) {
        Proto.Syntax s = syntax;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withKeywordSuffix(visitSpace(s.getKeywordSuffix(), p));
        return s.getPadding().withLevel(visitRightPadded(s.getPadding().getLevel(), p));
    }

    public Space visitSpace(Space space, P p) {
        return space;
    }

    public <P2 extends Proto> @Nullable ProtoContainer<P2> visitContainer(@Nullable ProtoContainer<P2> container, P p) {
        if(container == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), container));

        Space before = visitSpace(container.getBefore(), p);
        List<ProtoRightPadded<P2>> ps = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, p));

        setCursor(getCursor().getParent());

        return ps == container.getPadding().getElements() && before == container.getBefore() ?
                container :
                ProtoContainer.build(before, ps, container.getMarkers());
    }

    public <T> @Nullable ProtoLeftPadded<T> visitLeftPadded(ProtoLeftPadded<T> left, P p) {
        setCursor(new Cursor(getCursor(), left));

        Space before = visitSpace(left.getBefore(), p);
        T t = left.getElement();

        if (t instanceof Proto) {
            //noinspection unchecked
            t = visitAndCast((Proto) left.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }

        return (before == left.getBefore() && t == left.getElement()) ? left : new ProtoLeftPadded<>(before, t, left.getMarkers());
    }

    @SuppressWarnings("ConstantConditions")
    public <T> @Nullable ProtoRightPadded<T> visitRightPadded(@Nullable ProtoRightPadded<T> right, P p) {
        if (right == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), right));

        T t = right.getElement();
        if (t instanceof Proto) {
            //noinspection unchecked
            t = (T) visit((Proto) right.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }
        Space after = visitSpace(right.getAfter(), p);
        return (after == right.getAfter() && t == right.getElement()) ? right : new ProtoRightPadded<>(t, after, right.getMarkers());
    }
}
