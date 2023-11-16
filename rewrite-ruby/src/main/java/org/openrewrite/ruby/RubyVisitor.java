/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.ruby;

import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.ruby.tree.*;

@SuppressWarnings("unused")
public class RubyVisitor<P> extends JavaVisitor<P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Ruby.CompilationUnit;
    }

    @Override
    public String getLanguage() {
        return "ruby";
    }

    public Space visitSpace(Space space, RubySpace.Location loc, P p) {
        return visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }

    public <J2 extends J> JContainer<J2> visitContainer(JContainer<J2> container,
                                                        RubyContainer.Location loc, P p) {
        return super.visitContainer(container, JContainer.Location.LANGUAGE_EXTENSION, p);
    }

    public <T> JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, RubyLeftPadded.Location loc, P p) {
        return super.visitLeftPadded(left, JLeftPadded.Location.LANGUAGE_EXTENSION, p);
    }

    public <T> JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, RubyRightPadded.Location loc, P p) {
        return super.visitRightPadded(right, JRightPadded.Location.LANGUAGE_EXTENSION, p);
    }

    public Ruby visitCompilationUnit(Ruby.CompilationUnit compilationUnit, P p) {
        Ruby.CompilationUnit c = compilationUnit;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withBody(visit(c.getBody(), p));
        c = c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
        return c;
    }

    public J visitArray(Ruby.Array array, P p) {
        Ruby.Array l = array;
        l = l.withPrefix(visitSpace(l.getPrefix(), RubySpace.Location.LIST_LITERAL, p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        Expression temp = (Expression) visitExpression(l, p);
        if (!(temp instanceof Ruby.Array)) {
            return temp;
        } else {
            l = (Ruby.Array) temp;
        }
        l = l.getPadding().withElements(visitContainer(l.getPadding().getElements(), RubyContainer.Location.LIST_LITERAL_ELEMENTS, p));
        l = l.withType(visitType(l.getType(), p));
        return l;
    }

    public J visitBinary(Ruby.Binary binary, P p) {
        Ruby.Binary b = binary;
        b = b.withPrefix(visitSpace(b.getPrefix(), RubySpace.Location.BINARY_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof Ruby.Binary)) {
            return temp;
        } else {
            b = (Ruby.Binary) temp;
        }
        b = b.withLeft(visitAndCast(b.getLeft(), p));
        b = b.getPadding().withOperator(visitLeftPadded(b.getPadding().getOperator(), RubyLeftPadded.Location.BINARY_OPERATOR, p));
        b = b.withRight(visitAndCast(b.getRight(), p));
        b = b.withType(visitType(b.getType(), p));
        return b;
    }

    public J visitDelimitedString(Ruby.DelimitedString delimitedString, P p) {
        Ruby.DelimitedString ds = delimitedString;
        ds = ds.withPrefix(visitSpace(ds.getPrefix(), RubySpace.Location.DELIMITED_STRING_PREFIX, p));
        ds = ds.withMarkers(visitMarkers(ds.getMarkers(), p));
        Expression temp = (Expression) visitExpression(ds, p);
        if (!(temp instanceof Ruby.DelimitedString)) {
            return temp;
        } else {
            ds = (Ruby.DelimitedString) temp;
        }
        ds = ds.withStrings(ListUtils.map(ds.getStrings(), s -> visit(s, p)));
        ds = ds.withType(visitType(ds.getType(), p));
        return ds;
    }

    public J visitDelimitedStringValue(Ruby.DelimitedString.Value value, P p) {
        Ruby.DelimitedString.Value v = value;
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        v = v.withTree(visit(v.getTree(), p));
        v = v.withAfter(visitSpace(v.getAfter(), RubySpace.Location.DELIMITED_STRING_VALUE_SUFFIX, p));
        return v;
    }

    public J visitExpansion(Ruby.Expansion expansion, P p) {
        Ruby.Expansion e = expansion;
        e = e.withPrefix(visitSpace(e.getPrefix(), RubySpace.Location.EXPANSION_PREFIX, p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withTree((TypedTree) visit(e.getTree(), p));
        return e;
    }

    public J visitKeyValue(Ruby.KeyValue keyValue, P p) {
        Ruby.KeyValue k = keyValue;
        k = k.withPrefix(visitSpace(k.getPrefix(), RubySpace.Location.KEY_VALUE_PREFIX, p));
        k = k.withMarkers(visitMarkers(k.getMarkers(), p));
        Expression temp = (Expression) visitExpression(k, p);
        if (!(temp instanceof Ruby.KeyValue)) {
            return temp;
        } else {
            k = (Ruby.KeyValue) temp;
        }
        k = k.getPadding().withKey(visitRightPadded(k.getPadding().getKey(), RubyRightPadded.Location.KEY_VALUE_SUFFIX, p));
        k = k.withValue((Expression) visit(k.getValue(), p));
        k = k.withType(visitType(k.getType(), p));
        return k;
    }

    public J visitHash(Ruby.Hash hash, P p) {
        Ruby.Hash h = hash;
        h = h.withPrefix(visitSpace(h.getPrefix(), RubySpace.Location.HASH_PREFIX, p));
        h = h.withMarkers(visitMarkers(h.getMarkers(), p));
        Expression temp = (Expression) visitExpression(h, p);
        if (!(temp instanceof Ruby.Hash)) {
            return temp;
        } else {
            h = (Ruby.Hash) temp;
        }
        h = h.getPadding().withElements(visitContainer(h.getPadding().getElements(),
                RubyContainer.Location.HASH_ELEMENTS, p));
        h = h.withType(visitType(h.getType(), p));
        return h;
    }

    public J visitRedo(Ruby.Redo redo, P p) {
        Ruby.Redo r = redo;
        r = r.withPrefix(visitSpace(r.getPrefix(), RubySpace.Location.REDO_PREFIX, p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        Statement temp = (Statement) visitStatement(r, p);
        if (!(temp instanceof Ruby.Redo)) {
            return temp;
        } else {
            r = (Ruby.Redo) temp;
        }
        return r;
    }

    public J visitYield(Ruby.Yield yield, P p) {
        Ruby.Yield y = yield;
        y = y.withPrefix(visitSpace(y.getPrefix(), RubySpace.Location.YIELD, p));
        y = y.withMarkers(visitMarkers(y.getMarkers(), p));
        Statement temp = (Statement) visitStatement(y, p);
        if (!(temp instanceof Ruby.Yield)) {
            return temp;
        } else {
            y = (Ruby.Yield) temp;
        }
        y = y.getPadding().withData(visitContainer(y.getPadding().getData(),
                RubyContainer.Location.YIELD_DATA, p));
        return y;
    }

    public J visitMultipleAssignment(Ruby.MultipleAssignment multipleAssignment, P p) {
        Ruby.MultipleAssignment m = multipleAssignment;
        m = m.withPrefix(visitSpace(m.getPrefix(), RubySpace.Location.MULTIPLE_ASSIGNMENT, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Statement temp = (Statement) visitStatement(m, p);
        if (!(temp instanceof Ruby.MultipleAssignment)) {
            return temp;
        } else {
            m = (Ruby.MultipleAssignment) temp;
        }
        m = m.getPadding().withAssignments(visitContainer(m.getPadding().getAssignments(),
                RubyContainer.Location.MULTIPLE_ASSIGNMENT_ASSIGNMENTS, p));
        m = m.getPadding().withInitializers(visitContainer(m.getPadding().getAssignments(),
                RubyContainer.Location.MULTIPLE_ASSIGNMENT_INITIALIZERS, p));
        return m;
    }
}
