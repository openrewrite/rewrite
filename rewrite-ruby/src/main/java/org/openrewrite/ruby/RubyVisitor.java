/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.ruby;

import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.ruby.tree.Rb;
import org.openrewrite.ruby.tree.RubyContainer;
import org.openrewrite.ruby.tree.RubyRightPadded;
import org.openrewrite.ruby.tree.RubySpace;

@SuppressWarnings("unused")
public class RubyVisitor<P> extends JavaVisitor<P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Rb.CompilationUnit;
    }

    @Override
    public String getLanguage() {
        return "ruby";
    }

    public Space visitSpace(Space space, RubySpace.Location loc, P p) {
        return visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }

    public <J2 extends J> JContainer<J2> visitContainer(@Nullable JContainer<J2> container,
                                                        RubyContainer.Location loc, P p) {
        return super.visitContainer(container, JContainer.Location.LANGUAGE_EXTENSION, p);
    }

    public <T> JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, RubyRightPadded.Location loc, P p) {
        return super.visitRightPadded(right, JRightPadded.Location.LANGUAGE_EXTENSION, p);
    }

    public J visitBreak(Rb.Break aBreak, P p) {
        Rb.Break b = aBreak;
        b = b.withPrefix(visitSpace(b.getPrefix(), RubySpace.Location.BREAK_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Statement temp = (Statement) visitStatement(b, p);
        if (!(temp instanceof Rb.Break)) {
            return temp;
        } else {
            b = (Rb.Break) temp;
        }
        J jBreak = visit(b.getBreak(), p);
        if (jBreak == null) {
            //noinspection DataFlowIssue
            return null;
        }
        b = b.withBreak((J.Break) jBreak);
        b = b.withValue((Expression) visit(b.getValue(), p));
        return b;
    }

    public Rb visitCompilationUnit(Rb.CompilationUnit compilationUnit, P p) {
        Rb.CompilationUnit c = compilationUnit;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.getPadding().withStatements(ListUtils.map(c.getPadding().getStatements(), statement ->
                visitRightPadded(statement, RubyRightPadded.Location.COMPILATION_UNIT_STATEMENT_SUFFIX, p)));
        c = c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
        return c;
    }

    public J visitAlias(Rb.Alias alias, P p) {
        Rb.Alias a = alias;
        a = a.withPrefix(visitSpace(a.getPrefix(), RubySpace.Location.ALIAS_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Statement temp = (Statement) visitStatement(a, p);
        if (!(temp instanceof Rb.Alias)) {
            return temp;
        } else {
            a = (Rb.Alias) temp;
        }
        a = a.withNewName(visitAndCast(a.getNewName(), p));
        a = a.withExistingName(visitAndCast(a.getExistingName(), p));
        return a;
    }

    public J visitArray(Rb.Array array, P p) {
        Rb.Array l = array;
        l = l.withPrefix(visitSpace(l.getPrefix(), RubySpace.Location.ARRAY_PREFIX, p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        Expression temp = (Expression) visitExpression(l, p);
        if (!(temp instanceof Rb.Array)) {
            return temp;
        } else {
            l = (Rb.Array) temp;
        }
        l = l.getPadding().withElements(visitContainer(l.getPadding().getElements(), RubyContainer.Location.ARRAY_ELEMENTS, p));
        l = l.withType(visitType(l.getType(), p));
        return l;
    }

    public J visitAssignmentOperation(Rb.AssignmentOperation assignOp, P p) {
        Rb.AssignmentOperation a = assignOp;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ASSIGNMENT_OPERATION_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Statement temp = (Statement) visitStatement(a, p);
        if (!(temp instanceof Rb.AssignmentOperation)) {
            return temp;
        } else {
            a = (Rb.AssignmentOperation) temp;
        }
        Expression temp2 = (Expression) visitExpression(a, p);
        if (!(temp2 instanceof Rb.AssignmentOperation)) {
            return temp2;
        } else {
            a = (Rb.AssignmentOperation) temp2;
        }
        a = a.withVariable(visitAndCast(a.getVariable(), p));
        a = a.getPadding().withOperator(visitLeftPadded(a.getPadding().getOperator(), JLeftPadded.Location.ASSIGNMENT_OPERATION_OPERATOR, p));
        a = a.withAssignment(visitAndCast(a.getAssignment(), p));
        a = a.withType(visitType(a.getType(), p));
        return a;
    }

    public J visitPreExecution(Rb.PreExecution begin, P p) {
        Rb.PreExecution b = begin;
        b = b.withPrefix(visitSpace(b.getPrefix(), RubySpace.Location.PRE_EXECUTION_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        b = b.withBlock((J.Block) visit(b.getBlock(), p));
        return b;
    }

    public J visitBegin(Rb.Begin begin, P p) {
        Rb.Begin b = begin;
        b = b.withPrefix(visitSpace(b.getPrefix(), RubySpace.Location.BEGIN_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof Rb.Begin)) {
            return temp;
        } else {
            b = (Rb.Begin) temp;
        }
        b = b.withBody((J.Block) visit(b.getBody(), p));
        b = b.withType(visitType(b.getType(), p));
        return b;
    }

    public J visitBinary(Rb.Binary binary, P p) {
        Rb.Binary b = binary;
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BINARY_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof Rb.Binary)) {
            return temp;
        } else {
            b = (Rb.Binary) temp;
        }
        b = b.withLeft(visitAndCast(b.getLeft(), p));
        b = b.getPadding().withOperator(visitLeftPadded(b.getPadding().getOperator(), JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        b = b.withRight(visitAndCast(b.getRight(), p));
        b = b.withType(visitType(b.getType(), p));
        return b;
    }

    public J visitBlock(Rb.Block block, P p) {
        Rb.Block b = block;
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BLOCK_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof Rb.Block)) {
            return temp;
        } else {
            b = (Rb.Block) temp;
        }
        b = b.getPadding().withParameters(visitContainer(b.getPadding().getParameters(),
                RubyContainer.Location.BLOCK_PARAMETERS, p));
        b = b.withBody((J.Block) visit(b.getBody(), p));
        return b;
    }

    public J visitBlockArgument(Rb.BlockArgument blockArgument, P p) {
        Rb.BlockArgument b = blockArgument;
        b = b.withPrefix(visitSpace(b.getPrefix(), RubySpace.Location.BLOCK_ARGUMENT_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof Rb.BlockArgument)) {
            return temp;
        } else {
            b = (Rb.BlockArgument) temp;
        }
        b = b.withArgument((Expression) visit(b.getArgument(), p));
        b = b.withType(visitType(b.getType(), p));
        return b;
    }

    public J visitBooleanCheck(Rb.BooleanCheck booleanCheck, P p) {
        Rb.BooleanCheck b = booleanCheck;
        b = b.withPrefix(visitSpace(b.getPrefix(), RubySpace.Location.BOOLEAN_CHECK_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof Rb.BooleanCheck)) {
            return temp;
        } else {
            b = (Rb.BooleanCheck) temp;
        }
        b = b.withLeft((Expression) visit(b.getLeft(), p));
        b = b.withPattern((J.Case) visit(b.getPattern(), p));
        b = b.withType(visitType(b.getType(), p));
        return b;
    }

    public J visitClassMethod(Rb.ClassMethod classMethod, P p) {
        Rb.ClassMethod c = classMethod;
        c = c.withPrefix(visitSpace(c.getPrefix(), RubySpace.Location.CLASS_METHOD_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        Statement temp = (Statement) visitStatement(c, p);
        if (!(temp instanceof Rb.ClassMethod)) {
            return temp;
        } else {
            c = (Rb.ClassMethod) temp;
        }
        c = c.withReceiver((Expression) visit(c.getReceiver(), p));
        c = c.getPadding().withMethod(visitLeftPadded(c.getPadding().getMethod(),
                JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        return c;
    }

    public J visitDelimitedArray(Rb.DelimitedArray delimitedArray, P p) {
        Rb.DelimitedArray da = delimitedArray;
        da = da.withPrefix(visitSpace(da.getPrefix(), RubySpace.Location.DELIMITED_ARRAY_PREFIX, p));
        da = da.withMarkers(visitMarkers(da.getMarkers(), p));
        Expression temp = (Expression) visitExpression(da, p);
        if (!(temp instanceof Rb.DelimitedArray)) {
            return temp;
        } else {
            da = (Rb.DelimitedArray) temp;
        }
        da = da.getPadding().withElements(visitContainer(da.getPadding().getElements(),
                RubyContainer.Location.DELIMITED_ARRAY_ELEMENTS, p));
        da = da.withType(visitType(da.getType(), p));
        return da;
    }

    public J visitComplexString(Rb.ComplexString complexString, P p) {
        Rb.ComplexString ds = complexString;
        ds = ds.withPrefix(visitSpace(ds.getPrefix(), RubySpace.Location.COMPLEX_STRING_PREFIX, p));
        ds = ds.withMarkers(visitMarkers(ds.getMarkers(), p));
        Expression temp = (Expression) visitExpression(ds, p);
        if (!(temp instanceof Rb.ComplexString)) {
            return temp;
        } else {
            ds = (Rb.ComplexString) temp;
        }
        ds = ds.withStrings(ListUtils.map(ds.getStrings(), s -> visit(s, p)));
        ds = ds.withType(visitType(ds.getType(), p));
        return ds;
    }

    public J visitComplexStringValue(Rb.ComplexString.Value value, P p) {
        Rb.ComplexString.Value v = value;
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        v = v.withTree(visit(v.getTree(), p));
        v = v.withAfter(visitSpace(v.getAfter(), RubySpace.Location.COMPLEX_STRING_VALUE_SUFFIX, p));
        return v;
    }

    public J visitPostExecution(Rb.PostExecution end, P p) {
        Rb.PostExecution e = end;
        e = e.withPrefix(visitSpace(e.getPrefix(), RubySpace.Location.POST_EXECUTION_PREFIX, p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withBlock((J.Block) visit(e.getBlock(), p));
        return e;
    }

    public J visitExpressionTypeTree(Rb.ExpressionTypeTree expressionTypeTree, P p) {
        Rb.ExpressionTypeTree s = expressionTypeTree;
        s = s.withPrefix(visitSpace(s.getPrefix(), RubySpace.Location.EXPRESSION_TYPE_TREE_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withReference(visit(s.getReference(), p));
        return s;
    }

    public J visitHash(Rb.Hash hash, P p) {
        Rb.Hash h = hash;
        h = h.withPrefix(visitSpace(h.getPrefix(), RubySpace.Location.HASH_PREFIX, p));
        h = h.withMarkers(visitMarkers(h.getMarkers(), p));
        Expression temp = (Expression) visitExpression(h, p);
        if (!(temp instanceof Rb.Hash)) {
            return temp;
        } else {
            h = (Rb.Hash) temp;
        }
        h = h.getPadding().withPairs(visitContainer(h.getPadding().getPairs(),
                RubyContainer.Location.HASH_ELEMENTS, p));
        h = h.withType(visitType(h.getType(), p));
        return h;
    }

    public J visitHeredoc(Rb.Heredoc heredoc, P p) {
        Rb.Heredoc h = heredoc;
        h = h.withPrefix(visitSpace(h.getPrefix(), RubySpace.Location.HEREDOC_PREFIX, p));
        h = h.withMarkers(visitMarkers(h.getMarkers(), p));
        Expression temp = (Expression) visitExpression(h, p);
        if (!(temp instanceof Rb.Heredoc)) {
            return temp;
        } else {
            h = (Rb.Heredoc) temp;
        }
        h = h.withValue((J.Literal) visit(h.getValue(), p));
        h = h.withType(visitType(h.getType(), p));
        return h;
    }

    public J visitKeyValue(Rb.Hash.KeyValue keyValue, P p) {
        Rb.Hash.KeyValue k = keyValue;
        k = k.withPrefix(visitSpace(k.getPrefix(), RubySpace.Location.KEY_VALUE_PREFIX, p));
        k = k.withMarkers(visitMarkers(k.getMarkers(), p));
        Expression temp = (Expression) visitExpression(k, p);
        if (!(temp instanceof Rb.Hash.KeyValue)) {
            return temp;
        } else {
            k = (Rb.Hash.KeyValue) temp;
        }
        k = k.withKey((Expression) visit(k.getKey(), p));
        k = k.getPadding().withSeparator(visitLeftPadded(k.getPadding().getSeparator(),
                JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        k = k.withValue((Expression) visit(k.getValue(), p));
        k = k.withType(visitType(k.getType(), p));
        return k;
    }

    public J visitModule(Rb.Module module, P p) {
        Rb.Module m = module;
        m = m.withPrefix(visitSpace(m.getPrefix(), RubySpace.Location.MODULE_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Statement temp = (Statement) visitStatement(m, p);
        if (!(temp instanceof Rb.Module)) {
            return temp;
        } else {
            m = (Rb.Module) temp;
        }
        m = m.withName(visitAndCast(m.getName(), p));
        m = m.withBlock((J.Block) visit(m.getBlock(), p));
        return m;
    }

    public J visitMultipleAssignment(Rb.MultipleAssignment multipleAssignment, P p) {
        Rb.MultipleAssignment m = multipleAssignment;
        m = m.withPrefix(visitSpace(m.getPrefix(), RubySpace.Location.MULTIPLE_ASSIGNMENT_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Statement temp = (Statement) visitStatement(m, p);
        if (!(temp instanceof Rb.MultipleAssignment)) {
            return temp;
        } else {
            m = (Rb.MultipleAssignment) temp;
        }
        m = m.getPadding().withAssignments(visitContainer(m.getPadding().getAssignments(),
                RubyContainer.Location.MULTIPLE_ASSIGNMENT_ASSIGNMENTS, p));
        m = m.getPadding().withInitializers(visitContainer(m.getPadding().getInitializers(),
                RubyContainer.Location.MULTIPLE_ASSIGNMENT_INITIALIZERS, p));
        return m;
    }

    public J visitNext(Rb.Next next, P p) {
        Rb.Next n = next;
        n = n.withPrefix(visitSpace(n.getPrefix(), RubySpace.Location.NEXT_PREFIX, p));
        n = n.withMarkers(visitMarkers(n.getMarkers(), p));
        Statement temp = (Statement) visitStatement(n, p);
        if (!(temp instanceof Rb.Next)) {
            return temp;
        } else {
            n = (Rb.Next) temp;
        }
        J jNext = visit(n.getNext(), p);
        if(jNext == null) {
            //noinspection DataFlowIssue
            return null;
        }
        n = n.withNext((J.Continue) jNext);
        n = n.withValue((Expression) visit(n.getValue(), p));
        return n;
    }

    public J visitNumericDomain(Rb.NumericDomain numericDomain, P p) {
        Rb.NumericDomain n = numericDomain;
        n = n.withPrefix(visitSpace(n.getPrefix(), RubySpace.Location.NUMERIC_DOMAIN_PREFIX, p));
        n = n.withMarkers(visitMarkers(n.getMarkers(), p));
        Expression temp = (Expression) visitExpression(n, p);
        if (!(temp instanceof Rb.NumericDomain)) {
            return temp;
        } else {
            n = (Rb.NumericDomain) temp;
        }
        n = n.withValue((Expression) visitNonNull(n.getValue(), p));
        n = n.withType(visitType(n.getType(), p));
        return n;
    }

    public J visitOpenEigenclass(Rb.OpenEigenclass openEigenclass, P p) {
        Rb.OpenEigenclass o = openEigenclass;
        o = o.withPrefix(visitSpace(o.getPrefix(), RubySpace.Location.OPEN_EIGENCLASS_PREFIX, p));
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        Statement temp = (Statement) visitStatement(o, p);
        if (!(temp instanceof Rb.OpenEigenclass)) {
            return temp;
        } else {
            o = (Rb.OpenEigenclass) temp;
        }
        o = o.getPadding().withEigenclass(visitLeftPadded(o.getPadding().getEigenclass(),
                JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        o = o.withBody((J.Block) visit(o.getBody(), p));
        return o;
    }

    public J visitUndef(Rb.Undef undef, P p) {
        Rb.Undef u = undef;
        u = u.withPrefix(visitSpace(u.getPrefix(), RubySpace.Location.UNDEF_PREFIX, p));
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        Statement temp = (Statement) visitStatement(u, p);
        if (!(temp instanceof Rb.Undef)) {
            return temp;
        } else {
            u = (Rb.Undef) temp;
        }
        u = u.getPadding().withNames(visitContainer(u.getPadding().getNames(),
                RubyContainer.Location.UNDEF_NAMES, p));
        return u;
    }

    public J visitPatternBinding(Rb.PatternBinding patternBinding, P p) {
        Rb.PatternBinding b = patternBinding;
        b = b.withPrefix(visitSpace(b.getPrefix(), RubySpace.Location.PATTERN_BINDING_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof Rb.PatternBinding)) {
            return temp;
        } else {
            b = (Rb.PatternBinding) temp;
        }
        b = b.withPattern(visitAndCast(b.getPattern(), p));
        b = b.getPadding().withName(visitLeftPadded(b.getPadding().getName(),
                JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        return b;
    }

    public J visitPatternGuard(Rb.PatternGuard patternGuard, P p) {
        Rb.PatternGuard g = patternGuard;
        g = g.withPrefix(visitSpace(g.getPrefix(), RubySpace.Location.PATTERN_GUARD_PREFIX, p));
        g = g.withMarkers(visitMarkers(g.getMarkers(), p));
        Expression temp = (Expression) visitExpression(g, p);
        if (!(temp instanceof Rb.PatternGuard)) {
            return temp;
        } else {
            g = (Rb.PatternGuard) temp;
        }
        g = g.withPattern(visitAndCast(g.getPattern(), p));
        g = g.getPadding().withCondition(visitLeftPadded(g.getPadding().getCondition(),
                JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        return g;
    }

    public J visitRedo(Rb.Redo redo, P p) {
        Rb.Redo r = redo;
        r = r.withPrefix(visitSpace(r.getPrefix(), RubySpace.Location.REDO_PREFIX, p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        Statement temp = (Statement) visitStatement(r, p);
        if (!(temp instanceof Rb.Redo)) {
            return temp;
        } else {
            r = (Rb.Redo) temp;
        }
        return r;
    }

    public J visitRescue(Rb.Rescue rescue, P p) {
        Rb.Rescue r = rescue;
        r = r.withPrefix(visitSpace(r.getPrefix(), RubySpace.Location.RESCUE_PREFIX, p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        Statement temp = (Statement) visitStatement(r, p);
        if (!(temp instanceof Rb.Rescue)) {
            return temp;
        } else {
            r = (Rb.Rescue) temp;
        }
        r = r.withTry((J.Try) visitNonNull(r.getTry(), p));
        r = r.withElse((J.Block) visit(r.getElse(), p));
        return r;
    }

    public J visitRetry(Rb.Retry retry, P p) {
        Rb.Retry r = retry;
        r = r.withPrefix(visitSpace(r.getPrefix(), RubySpace.Location.RETRY_PREFIX, p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        Statement temp = (Statement) visitStatement(r, p);
        if (!(temp instanceof Rb.Retry)) {
            return temp;
        } else {
            r = (Rb.Retry) temp;
        }
        return r;
    }

    public J visitRightwardAssignment(Rb.RightwardAssignment rightwardAssignment, P p) {
        Rb.RightwardAssignment r = rightwardAssignment;
        r = r.withPrefix(visitSpace(r.getPrefix(), RubySpace.Location.RIGHTWARD_ASSIGNMENT_PREFIX, p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        Expression temp = (Expression) visitExpression(r, p);
        if (!(temp instanceof Rb.RightwardAssignment)) {
            return temp;
        } else {
            r = (Rb.RightwardAssignment) temp;
        }
        r = r.withLeft((Expression) visit(r.getLeft(), p));
        r = r.withPattern((J.Case) visit(r.getPattern(), p));
        r = r.withType(visitType(r.getType(), p));
        return r;
    }

    public J visitSplat(Rb.Splat splat, P p) {
        Rb.Splat s = splat;
        s = s.withPrefix(visitSpace(s.getPrefix(), RubySpace.Location.SPLAT_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        Expression temp = (Expression) visitExpression(s, p);
        if (!(temp instanceof Rb.Splat)) {
            return temp;
        } else {
            s = (Rb.Splat) temp;
        }
        s = s.withValue((Expression) visitNonNull(s.getValue(), p));
        s = s.withType(visitType(s.getType(), p));
        return s;
    }

    public J visitStructPattern(Rb.StructPattern structPattern, P p) {
        Rb.StructPattern s = structPattern;
        s = s.withPrefix(visitSpace(s.getPrefix(), RubySpace.Location.STRUCT_PATTERN_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        Expression temp = (Expression) visitExpression(s, p);
        if (!(temp instanceof Rb.StructPattern)) {
            return temp;
        } else {
            s = (Rb.StructPattern) temp;
        }
        s = s.withConstant(visitAndCast(s.getConstant(), p));
        s = s.getPadding().withPattern(visitContainer(s.getPadding().getPattern(),
                RubyContainer.Location.STRUCT_PATTERN_ELEMENT, p));
        return s;
    }

    public J visitSubArrayIndex(Rb.SubArrayIndex subArrayIndex, P p) {
        Rb.SubArrayIndex s = subArrayIndex;
        s = s.withPrefix(visitSpace(s.getPrefix(), RubySpace.Location.SUB_ARRAY_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        Expression temp = (Expression) visitExpression(s, p);
        if (!(temp instanceof Rb.SubArrayIndex)) {
            return temp;
        } else {
            s = (Rb.SubArrayIndex) temp;
        }
        s = s.withStartIndex((Expression) visitNonNull(s.getStartIndex(), p));
        s = s.withLength((Expression) visitNonNull(s.getLength(), p));
        return s;
    }

    public J visitSymbol(Rb.Symbol symbol, P p) {
        Rb.Symbol s = symbol;
        s = s.withPrefix(visitSpace(s.getPrefix(), RubySpace.Location.SYMBOL_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        Expression temp = (Expression) visitExpression(s, p);
        if (!(temp instanceof Rb.Symbol)) {
            return temp;
        } else {
            s = (Rb.Symbol) temp;
        }
        s = s.withName((Expression) visit(s.getName(), p));
        s = s.withType(visitType(s.getType(), p));
        return s;
    }

    public J visitUnary(Rb.Unary binary, P p) {
        Rb.Unary u = binary;
        u = u.withPrefix(visitSpace(u.getPrefix(), Space.Location.UNARY_PREFIX, p));
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        Expression temp = (Expression) visitExpression(u, p);
        if (!(temp instanceof Rb.Unary)) {
            return temp;
        } else {
            u = (Rb.Unary) temp;
        }
        u = u.withExpression((Expression) visit(u.getExpression(), p));
        u = u.withType(visitType(u.getType(), p));
        return u;
    }

    public J visitYield(Rb.Yield yield, P p) {
        Rb.Yield y = yield;
        y = y.withPrefix(visitSpace(y.getPrefix(), RubySpace.Location.YIELD_DATA, p));
        y = y.withMarkers(visitMarkers(y.getMarkers(), p));
        Statement temp = (Statement) visitStatement(y, p);
        if (!(temp instanceof Rb.Yield)) {
            return temp;
        } else {
            y = (Rb.Yield) temp;
        }
        y = y.getPadding().withData(visitContainer(y.getPadding().getData(),
                RubyContainer.Location.YIELD_DATA, p));
        return y;
    }
}
