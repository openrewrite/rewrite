/*
 * Copyright 2026 the original author or authors.
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
using OpenRewrite.Core;

namespace OpenRewrite.Java;

/// <summary>
/// Base visitor for Java LST elements.
/// Extends TreeVisitor to provide dispatch for J tree types via switch pattern matching.
/// </summary>
public class JavaVisitor<P> : TreeVisitor<J, P>
{
    protected override J? Accept(J tree, P p)
    {
        return tree switch
        {
            Annotation ann => VisitAnnotation(ann, p),
            Block blk => VisitBlock(blk, p),
            ClassDeclaration cd => VisitClassDeclaration(cd, p),
            EnumValueSet evs => VisitEnumValueSet(evs, p),
            EnumValue ev => VisitEnumValue(ev, p),
            MethodDeclaration md => VisitMethodDeclaration(md, p),
            Return ret => VisitReturn(ret, p),
            If iff => VisitIf(iff, p),
            WhileLoop whl => VisitWhileLoop(whl, p),
            DoWhileLoop dwl => VisitDoWhileLoop(dwl, p),
            ForLoop fl => VisitForLoop(fl, p),
            ForEachLoop fel => VisitForEachLoop(fel, p),
            Try tr => VisitTry(tr, p),
            Throw thr => VisitThrow(thr, p),
            Break brk => VisitBreak(brk, p),
            Continue cont => VisitContinue(cont, p),
            Empty emp => VisitEmpty(emp, p),
            Literal lit => VisitLiteral(lit, p),
            Identifier id => VisitIdentifier(id, p),
            FieldAccess fa => VisitFieldAccess(fa, p),
            MemberReference mr => VisitMemberReference(mr, p),
            Binary bin => VisitBinary(bin, p),
            Ternary ter => VisitTernary(ter, p),
            Assignment asn => VisitAssignment(asn, p),
            AssignmentOperation asnOp => VisitAssignmentOperation(asnOp, p),
            Unary un => VisitUnary(un, p),
            Parentheses<Expression> paren => VisitParentheses(paren, p),
            ControlParentheses<Expression> cp => VisitControlParentheses(cp, p),
            ControlParentheses<TypeTree> cptt => VisitControlParentheses(cptt, p),
            ControlParentheses<VariableDeclarations> cpvd => VisitControlParentheses(cpvd, p),
            ExpressionStatement es => VisitExpressionStatement(es, p),
            VariableDeclarations vd => VisitVariableDeclarations(vd, p),
            Primitive prim => VisitPrimitive(prim, p),
            MethodInvocation mi => VisitMethodInvocation(mi, p),
            NewClass nc => VisitNewClass(nc, p),
            NewArray na => VisitNewArray(na, p),
            InstanceOf io => VisitInstanceOf(io, p),
            NullableType nt => VisitNullableType(nt, p),
            ParameterizedType pt => VisitParameterizedType(pt, p),
            ArrayType at => VisitArrayType(at, p),
            ArrayAccess aa => VisitArrayAccess(aa, p),
            ArrayDimension ad => VisitArrayDimension(ad, p),
            Lambda lam => VisitLambda(lam, p),
            Switch sw => VisitSwitch(sw, p),
            SwitchExpression se => VisitSwitchExpression(se, p),
            Case cs => VisitCase(cs, p),
            DeconstructionPattern dp => VisitDeconstructionPattern(dp, p),
            Label lbl => VisitLabel(lbl, p),
            Synchronized sync => VisitSynchronized(sync, p),
            TypeCast tc => VisitTypeCast(tc, p),
            TypeParameter tp => VisitTypeParameter(tp, p),
            Package pkg => VisitPackage(pkg, p),
            _ => throw new InvalidOperationException($"Unknown J tree type: {tree.GetType()}")
        };
    }

    public virtual J VisitAnnotation(Annotation annotation, P p)
    {
        var changed = false;

        NameTree newAnnotationType = annotation.AnnotationType;
        var visitedType = Visit(annotation.AnnotationType, p);
        if (visitedType is NameTree tt && !ReferenceEquals(tt, annotation.AnnotationType))
        {
            newAnnotationType = tt;
            changed = true;
        }

        JContainer<Expression>? newArguments = annotation.Arguments;
        if (annotation.Arguments != null)
        {
            var newArgs = new List<JRightPadded<Expression>>();
            bool argsChanged = false;
            foreach (var paddedArg in annotation.Arguments.Elements)
            {
                var visited = Visit(paddedArg.Element, p);
                if (visited is Expression e)
                {
                    if (!ReferenceEquals(e, paddedArg.Element)) argsChanged = true;
                    newArgs.Add(paddedArg.WithElement(e));
                }
                else
                {
                    newArgs.Add(paddedArg);
                }
            }
            if (argsChanged)
            {
                newArguments = annotation.Arguments.WithElements(newArgs);
                changed = true;
            }
        }

        return changed
            ? annotation.WithAnnotationType(newAnnotationType).WithArguments(newArguments)
            : annotation;
    }

    public virtual J VisitBlock(Block block, P p)
    {
        var statements = new List<JRightPadded<Statement>>();
        bool changed = false;
        foreach (var stmt in block.Statements)
        {
            var visited = Visit(stmt.Element, p);
            if (visited is Statement s)
            {
                if (!ReferenceEquals(s, stmt.Element)) changed = true;
                statements.Add(stmt.WithElement(s));
            }
        }
        return changed ? block.WithStatements(statements) : block;
    }

    public virtual J VisitClassDeclaration(ClassDeclaration classDecl, P p)
    {
        var body = Visit(classDecl.Body, p);
        if (body is Block b && !ReferenceEquals(b, classDecl.Body))
        {
            return classDecl.WithBody(b);
        }
        return classDecl;
    }

    public virtual J VisitEnumValueSet(EnumValueSet enumValueSet, P p)
    {
        var changed = false;
        var newEnums = new List<JRightPadded<EnumValue>>();
        foreach (var paddedValue in enumValueSet.Enums)
        {
            var visited = Visit(paddedValue.Element, p);
            if (visited is EnumValue ev)
            {
                if (!ReferenceEquals(ev, paddedValue.Element))
                {
                    changed = true;
                }
                newEnums.Add(paddedValue.WithElement(ev));
            }
            else
            {
                newEnums.Add(paddedValue);
            }
        }
        return changed ? enumValueSet.WithEnums(newEnums) : enumValueSet;
    }

    public virtual J VisitEnumValue(EnumValue enumValue, P p)
    {
        var changed = false;

        Identifier newName = enumValue.Name;
        var visitedName = Visit(enumValue.Name, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, enumValue.Name))
        {
            newName = id;
            changed = true;
        }

        JLeftPadded<Expression>? newInitializer = enumValue.Initializer;
        if (enumValue.Initializer != null)
        {
            var visitedInit = Visit(enumValue.Initializer.Element, p);
            if (visitedInit is Expression e && !ReferenceEquals(e, enumValue.Initializer.Element))
            {
                newInitializer = enumValue.Initializer.WithElement(e);
                changed = true;
            }
        }

        return changed
            ? enumValue.WithName(newName).WithInitializer(newInitializer)
            : enumValue;
    }

    public virtual J VisitMethodDeclaration(MethodDeclaration method, P p)
    {
        var changed = false;

        TypeTree? newReturnType = method.ReturnTypeExpression;
        if (method.ReturnTypeExpression != null)
        {
            var visited = Visit(method.ReturnTypeExpression, p);
            if (visited is TypeTree tt && !ReferenceEquals(tt, method.ReturnTypeExpression))
            {
                newReturnType = tt;
                changed = true;
            }
        }

        JContainer<Statement> newParams = method.Parameters;
        var paramElements = new List<JRightPadded<Statement>>();
        bool paramsChanged = false;
        foreach (var paddedParam in method.Parameters.Elements)
        {
            var visited = Visit(paddedParam.Element, p);
            if (visited is Statement s)
            {
                if (!ReferenceEquals(s, paddedParam.Element)) paramsChanged = true;
                paramElements.Add(paddedParam.WithElement(s));
            }
            else
            {
                paramElements.Add(paddedParam);
            }
        }
        if (paramsChanged)
        {
            newParams = method.Parameters.WithElements(paramElements);
            changed = true;
        }

        Block? newBody = method.Body;
        if (method.Body != null)
        {
            var visited = Visit(method.Body, p);
            if (visited is Block b && !ReferenceEquals(b, method.Body))
            {
                newBody = b;
                changed = true;
            }
        }

        return changed
            ? method.WithReturnTypeExpression(newReturnType).WithParameters(newParams).WithBody(newBody)
            : method;
    }

    public virtual J VisitReturn(Return ret, P p)
    {
        if (ret.Expression != null)
        {
            var visited = Visit(ret.Expression, p);
            if (visited is Expression expr && !ReferenceEquals(expr, ret.Expression))
            {
                return ret.WithExpression(expr);
            }
        }
        return ret;
    }

    public virtual J VisitIf(If iff, P p)
    {
        var changed = false;

        var visitedCondition = Visit(iff.Condition.Tree.Element, p);
        ControlParentheses<Expression> newCondition = iff.Condition;
        if (visitedCondition is Expression ce && !ReferenceEquals(ce, iff.Condition.Tree.Element))
        {
            newCondition = iff.Condition.WithTree(iff.Condition.Tree.WithElement(ce));
            changed = true;
        }

        JRightPadded<Statement> newThenPart = iff.ThenPart;
        var visitedThen = Visit(iff.ThenPart.Element, p);
        if (visitedThen is Statement ts && !ReferenceEquals(ts, iff.ThenPart.Element))
        {
            newThenPart = iff.ThenPart.WithElement(ts);
            changed = true;
        }

        If.Else? newElsePart = iff.ElsePart;
        if (iff.ElsePart != null)
        {
            var visitedElse = Visit(iff.ElsePart.Body.Element, p);
            if (visitedElse is Statement es && !ReferenceEquals(es, iff.ElsePart.Body.Element))
            {
                newElsePart = iff.ElsePart.WithBody(iff.ElsePart.Body.WithElement(es));
                changed = true;
            }
        }

        return changed
            ? iff.WithCondition(newCondition).WithThenPart(newThenPart).WithElsePart(newElsePart)
            : iff;
    }

    public virtual J VisitWhileLoop(WhileLoop whl, P p)
    {
        var changed = false;

        ControlParentheses<Expression> newCondition = whl.Condition;
        var visitedCondition = Visit(whl.Condition.Tree.Element, p);
        if (visitedCondition is Expression ce && !ReferenceEquals(ce, whl.Condition.Tree.Element))
        {
            newCondition = whl.Condition.WithTree(whl.Condition.Tree.WithElement(ce));
            changed = true;
        }

        JRightPadded<Statement> newBody = whl.Body;
        var visitedBody = Visit(whl.Body.Element, p);
        if (visitedBody is Statement bs && !ReferenceEquals(bs, whl.Body.Element))
        {
            newBody = whl.Body.WithElement(bs);
            changed = true;
        }

        return changed ? whl.WithCondition(newCondition).WithBody(newBody) : whl;
    }

    public virtual J VisitDoWhileLoop(DoWhileLoop dwl, P p)
    {
        var changed = false;

        JRightPadded<Statement> newBody = dwl.Body;
        var visitedBody = Visit(dwl.Body.Element, p);
        if (visitedBody is Statement bs && !ReferenceEquals(bs, dwl.Body.Element))
        {
            newBody = dwl.Body.WithElement(bs);
            changed = true;
        }

        JLeftPadded<ControlParentheses<Expression>> newCondition = dwl.Condition;
        var visitedCondition = Visit(dwl.Condition.Element.Tree.Element, p);
        if (visitedCondition is Expression ce && !ReferenceEquals(ce, dwl.Condition.Element.Tree.Element))
        {
            var newCtrlParen = dwl.Condition.Element.WithTree(dwl.Condition.Element.Tree.WithElement(ce));
            newCondition = dwl.Condition.WithElement(newCtrlParen);
            changed = true;
        }

        return changed ? dwl.WithBody(newBody).WithCondition(newCondition) : dwl;
    }

    public virtual J VisitForLoop(ForLoop fl, P p)
    {
        var changed = false;

        var newInit = new List<JRightPadded<Statement>>();
        bool initChanged = false;
        foreach (var init in fl.LoopControl.Init)
        {
            var visited = Visit(init.Element, p);
            if (visited is Statement s)
            {
                if (!ReferenceEquals(s, init.Element)) initChanged = true;
                newInit.Add(init.WithElement(s));
            }
            else
            {
                newInit.Add(init);
            }
        }
        if (initChanged) changed = true;

        JRightPadded<Expression> newControlCondition = fl.LoopControl.Condition;
        var visitedCondition = Visit(fl.LoopControl.Condition.Element, p);
        if (visitedCondition is Expression ce && !ReferenceEquals(ce, fl.LoopControl.Condition.Element))
        {
            newControlCondition = fl.LoopControl.Condition.WithElement(ce);
            changed = true;
        }

        var newUpdate = new List<JRightPadded<Statement>>();
        bool updateChanged = false;
        foreach (var update in fl.LoopControl.Update)
        {
            var visited = Visit(update.Element, p);
            if (visited is Statement s)
            {
                if (!ReferenceEquals(s, update.Element)) updateChanged = true;
                newUpdate.Add(update.WithElement(s));
            }
            else
            {
                newUpdate.Add(update);
            }
        }
        if (updateChanged) changed = true;

        JRightPadded<Statement> newBody = fl.Body;
        var visitedBody = Visit(fl.Body.Element, p);
        if (visitedBody is Statement bs && !ReferenceEquals(bs, fl.Body.Element))
        {
            newBody = fl.Body.WithElement(bs);
            changed = true;
        }

        if (changed)
        {
            var newControl = fl.LoopControl
                .WithInit(initChanged ? newInit : fl.LoopControl.Init)
                .WithCondition(newControlCondition)
                .WithUpdate(updateChanged ? newUpdate : fl.LoopControl.Update);
            return fl.WithLoopControl(newControl).WithBody(newBody);
        }
        return fl;
    }

    public virtual J VisitForEachLoop(ForEachLoop fel, P p)
    {
        var changed = false;

        JRightPadded<VariableDeclarations> newVariable = fel.LoopControl.Variable;
        var visitedVar = Visit(fel.LoopControl.Variable.Element, p);
        if (visitedVar is VariableDeclarations vd && !ReferenceEquals(vd, fel.LoopControl.Variable.Element))
        {
            newVariable = fel.LoopControl.Variable.WithElement(vd);
            changed = true;
        }

        JRightPadded<Expression> newIterable = fel.LoopControl.Iterable;
        var visitedIterable = Visit(fel.LoopControl.Iterable.Element, p);
        if (visitedIterable is Expression ie && !ReferenceEquals(ie, fel.LoopControl.Iterable.Element))
        {
            newIterable = fel.LoopControl.Iterable.WithElement(ie);
            changed = true;
        }

        JRightPadded<Statement> newBody = fel.Body;
        var visitedBody = Visit(fel.Body.Element, p);
        if (visitedBody is Statement bs && !ReferenceEquals(bs, fel.Body.Element))
        {
            newBody = fel.Body.WithElement(bs);
            changed = true;
        }

        if (changed)
        {
            var newControl = fel.LoopControl.WithVariable(newVariable).WithIterable(newIterable);
            return fel.WithLoopControl(newControl).WithBody(newBody);
        }
        return fel;
    }

    public virtual J VisitTry(Try tr, P p)
    {
        var changed = false;

        Block newBody = tr.Body;
        var visitedBody = Visit(tr.Body, p);
        if (visitedBody is Block bb && !ReferenceEquals(bb, tr.Body))
        {
            newBody = bb;
            changed = true;
        }

        var newCatches = new List<Try.Catch>();
        bool catchesChanged = false;
        foreach (var c in tr.Catches)
        {
            bool catchChanged = false;

            ControlParentheses<VariableDeclarations> newParam = c.Parameter;
            var visitedParam = Visit(c.Parameter.Tree.Element, p);
            if (visitedParam is VariableDeclarations pvd && !ReferenceEquals(pvd, c.Parameter.Tree.Element))
            {
                newParam = c.Parameter.WithTree(c.Parameter.Tree.WithElement(pvd));
                catchChanged = true;
            }

            Block newCatchBody = c.Body;
            var visitedCatchBody = Visit(c.Body, p);
            if (visitedCatchBody is Block cb && !ReferenceEquals(cb, c.Body))
            {
                newCatchBody = cb;
                catchChanged = true;
            }

            if (catchChanged)
            {
                catchesChanged = true;
                newCatches.Add(c.WithParameter(newParam).WithBody(newCatchBody));
            }
            else
            {
                newCatches.Add(c);
            }
        }
        if (catchesChanged) changed = true;

        JLeftPadded<Block>? newFinally = tr.Finally;
        if (tr.Finally != null)
        {
            var visitedFinally = Visit(tr.Finally.Element, p);
            if (visitedFinally is Block fb && !ReferenceEquals(fb, tr.Finally.Element))
            {
                newFinally = tr.Finally.WithElement(fb);
                changed = true;
            }
        }

        return changed
            ? tr.WithBody(newBody).WithCatches(catchesChanged ? newCatches : tr.Catches).WithFinally(newFinally)
            : tr;
    }

    public virtual J VisitThrow(Throw thr, P p)
    {
        var visited = Visit(thr.Exception, p);
        if (visited is Expression e && !ReferenceEquals(e, thr.Exception))
        {
            return thr.WithException(e);
        }
        return thr;
    }

    public virtual J VisitBreak(Break brk, P p)
    {
        return brk;
    }

    public virtual J VisitContinue(Continue cont, P p)
    {
        return cont;
    }

    public virtual J VisitEmpty(Empty emp, P p)
    {
        return emp;
    }

    public virtual J VisitControlParentheses(ControlParentheses<Expression> cp, P p)
    {
        var visited = Visit(cp.Tree.Element, p);
        if (visited is Expression e && !ReferenceEquals(e, cp.Tree.Element))
        {
            return cp.WithTree(cp.Tree.WithElement(e));
        }
        return cp;
    }

    public virtual J VisitControlParentheses(ControlParentheses<TypeTree> cp, P p)
    {
        var visited = Visit(cp.Tree.Element, p);
        if (visited is TypeTree tt && !ReferenceEquals(tt, cp.Tree.Element))
        {
            return cp.WithTree(cp.Tree.WithElement(tt));
        }
        return cp;
    }

    public virtual J VisitControlParentheses(ControlParentheses<VariableDeclarations> cp, P p)
    {
        var visited = Visit(cp.Tree.Element, p);
        if (visited is VariableDeclarations vd && !ReferenceEquals(vd, cp.Tree.Element))
        {
            return cp.WithTree(cp.Tree.WithElement(vd));
        }
        return cp;
    }

    public virtual J VisitLiteral(Literal literal, P p)
    {
        return literal;
    }

    public virtual J VisitIdentifier(Identifier identifier, P p)
    {
        return identifier;
    }

    public virtual J VisitFieldAccess(FieldAccess fieldAccess, P p)
    {
        var target = Visit(fieldAccess.Target, p);
        if (target is Expression t && !ReferenceEquals(t, fieldAccess.Target))
        {
            return fieldAccess.WithTarget(t);
        }
        return fieldAccess;
    }

    public virtual J VisitMemberReference(MemberReference memberRef, P p)
    {
        var changed = false;

        JRightPadded<Expression> newContaining = memberRef.Containing;
        var visitedContaining = Visit(memberRef.Containing.Element, p);
        if (visitedContaining is Expression ce && !ReferenceEquals(ce, memberRef.Containing.Element))
        {
            newContaining = memberRef.Containing.WithElement(ce);
            changed = true;
        }

        JLeftPadded<Identifier> newReference = memberRef.Reference;
        var visitedRef = Visit(memberRef.Reference.Element, p);
        if (visitedRef is Identifier id && !ReferenceEquals(id, memberRef.Reference.Element))
        {
            newReference = memberRef.Reference.WithElement(id);
            changed = true;
        }

        return changed
            ? memberRef.WithContaining(newContaining).WithReference(newReference)
            : memberRef;
    }

    public virtual J VisitBinary(Binary binary, P p)
    {
        var left = Visit(binary.Left, p);
        var right = Visit(binary.Right, p);

        if (left is Expression l && right is Expression r &&
            (!ReferenceEquals(l, binary.Left) || !ReferenceEquals(r, binary.Right)))
        {
            return binary.WithLeft(l).WithRight(r);
        }
        return binary;
    }

    public virtual J VisitTernary(Ternary ternary, P p)
    {
        var condition = Visit(ternary.Condition, p);
        var truePart = Visit(ternary.TruePart.Element, p);
        var falsePart = Visit(ternary.FalsePart.Element, p);

        if (condition is Expression c && truePart is Expression t && falsePart is Expression f &&
            (!ReferenceEquals(c, ternary.Condition) ||
             !ReferenceEquals(t, ternary.TruePart.Element) ||
             !ReferenceEquals(f, ternary.FalsePart.Element)))
        {
            return ternary.WithCondition(c).WithTruePart(ternary.TruePart.WithElement(t)).WithFalsePart(ternary.FalsePart.WithElement(f));
        }
        return ternary;
    }

    public virtual J VisitAssignment(Assignment assignment, P p)
    {
        var variable = Visit(assignment.Variable, p);
        var value = Visit(assignment.AssignmentValue.Element, p);

        if (variable is Expression v && value is Expression val &&
            (!ReferenceEquals(v, assignment.Variable) || !ReferenceEquals(val, assignment.AssignmentValue.Element)))
        {
            return assignment.WithVariable(v).WithAssignmentValue(assignment.AssignmentValue.WithElement(val));
        }
        return assignment;
    }

    public virtual J VisitAssignmentOperation(AssignmentOperation assignment, P p)
    {
        var variable = Visit(assignment.Variable, p);
        var value = Visit(assignment.AssignmentValue, p);

        if (variable is Expression v && value is Expression val &&
            (!ReferenceEquals(v, assignment.Variable) || !ReferenceEquals(val, assignment.AssignmentValue)))
        {
            return assignment.WithVariable(v).WithAssignmentValue(val);
        }
        return assignment;
    }

    public virtual J VisitUnary(Unary unary, P p)
    {
        var expr = Visit(unary.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, unary.Expression))
        {
            return unary.WithExpression(e);
        }
        return unary;
    }

    public virtual J VisitParentheses(Parentheses<Expression> parens, P p)
    {
        var inner = Visit(parens.Tree.Element, p);
        if (inner is Expression e && !ReferenceEquals(e, parens.Tree.Element))
        {
            return parens.WithTree(parens.Tree.WithElement(e));
        }
        return parens;
    }

    public virtual J VisitExpressionStatement(ExpressionStatement expressionStatement, P p)
    {
        var expr = Visit(expressionStatement.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, expressionStatement.Expression))
        {
            return expressionStatement.WithExpression(e);
        }
        return expressionStatement;
    }

    public virtual J VisitVariableDeclarations(VariableDeclarations varDecl, P p)
    {
        var changed = false;

        TypeTree? newTypeExpr = varDecl.TypeExpression;
        if (varDecl.TypeExpression != null)
        {
            var visited = Visit(varDecl.TypeExpression, p);
            if (visited is TypeTree tt && !ReferenceEquals(tt, varDecl.TypeExpression))
            {
                newTypeExpr = tt;
                changed = true;
            }
        }

        var newVars = new List<JRightPadded<NamedVariable>>();
        bool varsChanged = false;
        foreach (var paddedVar in varDecl.Variables)
        {
            var namedVar = paddedVar.Element;
            if (namedVar.Initializer != null)
            {
                var visited = Visit(namedVar.Initializer.Element, p);
                if (visited is Expression expr && !ReferenceEquals(expr, namedVar.Initializer.Element))
                {
                    var newInit = namedVar.Initializer.WithElement(expr);
                    var newNamedVar = namedVar.WithInitializer(newInit);
                    newVars.Add(paddedVar.WithElement(newNamedVar));
                    varsChanged = true;
                    continue;
                }
            }
            newVars.Add(paddedVar);
        }
        if (varsChanged) changed = true;

        return changed
            ? varDecl.WithTypeExpression(newTypeExpr).WithVariables(varsChanged ? newVars : varDecl.Variables)
            : varDecl;
    }

    public virtual J VisitPrimitive(Primitive primitive, P p)
    {
        return primitive;
    }

    public virtual J VisitMethodInvocation(MethodInvocation mi, P p)
    {
        var changed = false;

        JRightPadded<Expression>? newSelect = mi.Select;
        if (mi.Select != null)
        {
            var visited = Visit(mi.Select.Element, p);
            if (visited is Expression sel && !ReferenceEquals(sel, mi.Select.Element))
            {
                newSelect = mi.Select.WithElement(sel);
                changed = true;
            }
        }

        JContainer<Expression>? newTypeParams = mi.TypeParameters;
        if (mi.TypeParameters != null)
        {
            var tpElements = new List<JRightPadded<Expression>>();
            bool tpChanged = false;
            foreach (var paddedTypeArg in mi.TypeParameters.Elements)
            {
                var visited = Visit(paddedTypeArg.Element, p);
                if (visited is Expression e)
                {
                    if (!ReferenceEquals(e, paddedTypeArg.Element)) tpChanged = true;
                    tpElements.Add(paddedTypeArg.WithElement(e));
                }
                else
                {
                    tpElements.Add(paddedTypeArg);
                }
            }
            if (tpChanged)
            {
                newTypeParams = mi.TypeParameters.WithElements(tpElements);
                changed = true;
            }
        }

        JContainer<Expression> newArgs = mi.Arguments;
        var argElements = new List<JRightPadded<Expression>>();
        bool argsChanged = false;
        foreach (var paddedArg in mi.Arguments.Elements)
        {
            var visited = Visit(paddedArg.Element, p);
            if (visited is Expression e)
            {
                if (!ReferenceEquals(e, paddedArg.Element)) argsChanged = true;
                argElements.Add(paddedArg.WithElement(e));
            }
            else
            {
                argElements.Add(paddedArg);
            }
        }
        if (argsChanged)
        {
            newArgs = mi.Arguments.WithElements(argElements);
            changed = true;
        }

        return changed
            ? mi.WithSelect(newSelect).WithTypeParameters(newTypeParams).WithArguments(newArgs)
            : mi;
    }

    public virtual J VisitNewClass(NewClass nc, P p)
    {
        var changed = false;

        JRightPadded<Expression>? newEnclosing = nc.Enclosing;
        if (nc.Enclosing != null)
        {
            var visited = Visit(nc.Enclosing.Element, p);
            if (visited is Expression e && !ReferenceEquals(e, nc.Enclosing.Element))
            {
                newEnclosing = nc.Enclosing.WithElement(e);
                changed = true;
            }
        }

        TypeTree? newClazz = nc.Clazz;
        if (nc.Clazz != null)
        {
            var visited = Visit(nc.Clazz, p);
            if (visited is TypeTree tt && !ReferenceEquals(tt, nc.Clazz))
            {
                newClazz = tt;
                changed = true;
            }
        }

        JContainer<Expression> newArgs = nc.Arguments;
        var argElements = new List<JRightPadded<Expression>>();
        bool argsChanged = false;
        foreach (var paddedArg in nc.Arguments.Elements)
        {
            var visited = Visit(paddedArg.Element, p);
            if (visited is Expression e)
            {
                if (!ReferenceEquals(e, paddedArg.Element)) argsChanged = true;
                argElements.Add(paddedArg.WithElement(e));
            }
            else
            {
                argElements.Add(paddedArg);
            }
        }
        if (argsChanged)
        {
            newArgs = nc.Arguments.WithElements(argElements);
            changed = true;
        }

        Block? newBody = nc.Body;
        if (nc.Body != null)
        {
            var visited = Visit(nc.Body, p);
            if (visited is Block b && !ReferenceEquals(b, nc.Body))
            {
                newBody = b;
                changed = true;
            }
        }

        return changed
            ? nc.WithEnclosing(newEnclosing).WithClazz(newClazz).WithArguments(newArgs).WithBody(newBody)
            : nc;
    }

    public virtual J VisitNewArray(NewArray na, P p)
    {
        var changed = false;

        TypeTree? newTypeExpr = na.TypeExpression;
        if (na.TypeExpression != null)
        {
            var visited = Visit(na.TypeExpression, p);
            if (visited is TypeTree tt && !ReferenceEquals(tt, na.TypeExpression))
            {
                newTypeExpr = tt;
                changed = true;
            }
        }

        var newDims = new List<ArrayDimension>();
        bool dimsChanged = false;
        foreach (var dim in na.Dimensions)
        {
            var visited = Visit(dim, p);
            if (visited is ArrayDimension d)
            {
                if (!ReferenceEquals(d, dim)) dimsChanged = true;
                newDims.Add(d);
            }
            else
            {
                newDims.Add(dim);
            }
        }
        if (dimsChanged) changed = true;

        JContainer<Expression>? newInitializer = na.Initializer;
        if (na.Initializer != null)
        {
            var initElements = new List<JRightPadded<Expression>>();
            bool initChanged = false;
            foreach (var paddedElem in na.Initializer.Elements)
            {
                var visited = Visit(paddedElem.Element, p);
                if (visited is Expression e)
                {
                    if (!ReferenceEquals(e, paddedElem.Element)) initChanged = true;
                    initElements.Add(paddedElem.WithElement(e));
                }
                else
                {
                    initElements.Add(paddedElem);
                }
            }
            if (initChanged)
            {
                newInitializer = na.Initializer.WithElements(initElements);
                changed = true;
            }
        }

        return changed
            ? na.WithTypeExpression(newTypeExpr).WithDimensions(dimsChanged ? newDims : na.Dimensions).WithInitializer(newInitializer)
            : na;
    }

    public virtual J VisitLabel(Label label, P p)
    {
        var changed = false;

        JRightPadded<Identifier> newLabelName = label.LabelName;
        var visitedLabel = Visit(label.LabelName.Element, p);
        if (visitedLabel is Identifier id && !ReferenceEquals(id, label.LabelName.Element))
        {
            newLabelName = label.LabelName.WithElement(id);
            changed = true;
        }

        Statement newStatement = label.Statement;
        var visitedStmt = Visit(label.Statement, p);
        if (visitedStmt is Statement s && !ReferenceEquals(s, label.Statement))
        {
            newStatement = s;
            changed = true;
        }

        return changed
            ? label.WithLabelName(newLabelName).WithStatement(newStatement)
            : label;
    }

    public virtual J VisitInstanceOf(InstanceOf instanceOf, P p)
    {
        var changed = false;

        JRightPadded<Expression> newExpr = instanceOf.Expression;
        var visitedExpr = Visit(instanceOf.Expression.Element, p);
        if (visitedExpr is Expression e && !ReferenceEquals(e, instanceOf.Expression.Element))
        {
            newExpr = instanceOf.Expression.WithElement(e);
            changed = true;
        }

        J newClazz = instanceOf.Clazz;
        var visitedClazz = Visit(instanceOf.Clazz, p);
        if (visitedClazz is J vc && !ReferenceEquals(vc, instanceOf.Clazz))
        {
            newClazz = vc;
            changed = true;
        }

        J? newPattern = instanceOf.Pattern;
        if (instanceOf.Pattern != null)
        {
            var visitedPattern = Visit(instanceOf.Pattern, p);
            if (visitedPattern is J vp && !ReferenceEquals(vp, instanceOf.Pattern))
            {
                newPattern = vp;
                changed = true;
            }
        }

        Modifier? newModifier = instanceOf.InstanceOfModifier;
        if (instanceOf.InstanceOfModifier != null)
        {
            var visitedMod = Visit(instanceOf.InstanceOfModifier, p);
            if (visitedMod is Modifier vm && !ReferenceEquals(vm, instanceOf.InstanceOfModifier))
            {
                newModifier = vm;
                changed = true;
            }
        }

        return changed
            ? instanceOf.WithExpression(newExpr).WithClazz(newClazz).WithPattern(newPattern).WithInstanceOfModifier(newModifier)
            : instanceOf;
    }

    public virtual J VisitNullableType(NullableType nullableType, P p)
    {
        var visited = Visit(nullableType.TypeTree, p);
        if (visited is TypeTree tt && !ReferenceEquals(tt, nullableType.TypeTree))
        {
            return nullableType.WithTypeTreePadded(nullableType.TypeTreePadded.WithElement(tt));
        }
        return nullableType;
    }

    public virtual J VisitParameterizedType(ParameterizedType pt, P p)
    {
        var changed = false;

        NameTree newClazz = pt.Clazz;
        var visitedClazz = Visit(pt.Clazz, p);
        if (visitedClazz is NameTree nt && !ReferenceEquals(nt, pt.Clazz))
        {
            newClazz = nt;
            changed = true;
        }

        JContainer<Expression>? newTypeParams = pt.TypeParameters;
        if (pt.TypeParameters != null)
        {
            var tpElements = new List<JRightPadded<Expression>>();
            bool tpChanged = false;
            foreach (var paddedParam in pt.TypeParameters.Elements)
            {
                var visited = Visit(paddedParam.Element, p);
                if (visited is Expression e)
                {
                    if (!ReferenceEquals(e, paddedParam.Element)) tpChanged = true;
                    tpElements.Add(paddedParam.WithElement(e));
                }
                else
                {
                    tpElements.Add(paddedParam);
                }
            }
            if (tpChanged)
            {
                newTypeParams = pt.TypeParameters.WithElements(tpElements);
                changed = true;
            }
        }

        return changed
            ? pt.WithClazz(newClazz).WithTypeParameters(newTypeParams)
            : pt;
    }

    public virtual J VisitArrayType(ArrayType at, P p)
    {
        var changed = false;

        TypeTree newElementType = at.ElementType;
        var visitedElem = Visit(at.ElementType, p);
        if (visitedElem is TypeTree tt && !ReferenceEquals(tt, at.ElementType))
        {
            newElementType = tt;
            changed = true;
        }

        IList<Annotation>? newAnnotations = at.Annotations;
        if (at.Annotations != null)
        {
            var annList = new List<Annotation>();
            bool annsChanged = false;
            foreach (var ann in at.Annotations)
            {
                var visited = Visit(ann, p);
                if (visited is Annotation a)
                {
                    if (!ReferenceEquals(a, ann)) annsChanged = true;
                    annList.Add(a);
                }
                else
                {
                    annList.Add(ann);
                }
            }
            if (annsChanged)
            {
                newAnnotations = annList;
                changed = true;
            }
        }

        return changed
            ? at.WithElementType(newElementType).WithAnnotations(newAnnotations)
            : at;
    }

    public virtual J VisitArrayAccess(ArrayAccess arrayAccess, P p)
    {
        var indexed = Visit(arrayAccess.Indexed, p);
        var dimension = Visit(arrayAccess.Dimension, p);
        if (indexed is Expression i && dimension is ArrayDimension d &&
            (!ReferenceEquals(i, arrayAccess.Indexed) || !ReferenceEquals(d, arrayAccess.Dimension)))
        {
            return arrayAccess.WithIndexed(i).WithDimension(d);
        }
        return arrayAccess;
    }

    public virtual J VisitArrayDimension(ArrayDimension dimension, P p)
    {
        var index = Visit(dimension.Index.Element, p);
        if (index is Expression i && !ReferenceEquals(i, dimension.Index.Element))
        {
            return dimension.WithIndex(dimension.Index.WithElement(i));
        }
        return dimension;
    }

    public virtual J VisitLambda(Lambda lambda, P p)
    {
        var @params = lambda.Params;
        var elements = new List<JRightPadded<J>>();
        bool changed = false;

        foreach (var paddedParam in @params.Elements)
        {
            var visitedParam = Visit(paddedParam.Element, p);
            if (visitedParam is J vp)
            {
                if (!ReferenceEquals(vp, paddedParam.Element))
                {
                    changed = true;
                }
                elements.Add(paddedParam.WithElement(vp));
            }
        }

        var newParams = changed ? @params.WithElements(elements) : @params;

        var body = Visit(lambda.Body, p);

        if (!ReferenceEquals(newParams, @params) ||
            (body is J b && !ReferenceEquals(b, lambda.Body)))
        {
            return lambda.WithParams(newParams).WithBody(body is J newBody ? newBody : lambda.Body);
        }

        return lambda;
    }

    public virtual J VisitSwitch(Switch @switch, P p)
    {
        var selector = @switch.Selector;
        var selectorExpr = Visit(selector.Tree.Element, p);

        var casesBlock = VisitBlock(@switch.Cases, p);

        if (selectorExpr is Expression se && !ReferenceEquals(se, selector.Tree.Element) ||
            casesBlock is Block cb && !ReferenceEquals(cb, @switch.Cases))
        {
            return @switch.WithSelector(selectorExpr is Expression newSe ? selector.WithTree(selector.Tree.WithElement(newSe)) : selector).WithCases(casesBlock is Block newCb ? newCb : @switch.Cases);
        }

        return @switch;
    }

    public virtual J VisitCase(Case @case, P p)
    {
        var labels = @case.CaseLabels.Elements;
        var newLabels = new List<JRightPadded<J>>();
        bool labelsChanged = false;

        foreach (var labelPadded in labels)
        {
            var visited = Visit(labelPadded.Element, p);
            if (visited is J v)
            {
                if (!ReferenceEquals(v, labelPadded.Element))
                {
                    labelsChanged = true;
                }
                newLabels.Add(labelPadded.WithElement(v));
            }
        }

        var statements = @case.Statements.Elements;
        var newStatements = new List<JRightPadded<Statement>>();
        bool statementsChanged = false;

        foreach (var stmtPadded in statements)
        {
            var visited = Visit(stmtPadded.Element, p);
            if (visited is Statement s)
            {
                if (!ReferenceEquals(s, stmtPadded.Element))
                {
                    statementsChanged = true;
                }
                newStatements.Add(stmtPadded.WithElement(s));
            }
        }

        J? newBody = null;
        bool bodyChanged = false;
        if (@case.Body != null)
        {
            var visitedBody = Visit(@case.Body.Element, p);
            if (visitedBody is J vb && !ReferenceEquals(vb, @case.Body.Element))
            {
                newBody = vb;
                bodyChanged = true;
            }
        }

        if (labelsChanged || statementsChanged || bodyChanged)
        {
            return @case.WithCaseLabels(labelsChanged ? @case.CaseLabels.WithElements(newLabels) : @case.CaseLabels).WithStatements(statementsChanged ? @case.Statements.WithElements(newStatements) : @case.Statements).WithBody(bodyChanged && newBody != null ? @case.Body!.WithElement(newBody) : @case.Body);
        }

        return @case;
    }

    public virtual J VisitSwitchExpression(SwitchExpression se, P p)
    {
        var selector = Visit(se.Selector.Tree.Element, p);
        var cases = Visit(se.Cases, p);

        if ((selector is Expression s && !ReferenceEquals(s, se.Selector.Tree.Element)) ||
            (cases is Block b && !ReferenceEquals(b, se.Cases)))
        {
            return se.WithSelector(selector is Expression sel ? se.Selector.WithTree(se.Selector.Tree.WithElement(sel)) : se.Selector).WithCases(cases is Block newCases ? newCases : se.Cases);
        }

        return se;
    }

    public virtual J VisitDeconstructionPattern(DeconstructionPattern dp, P p)
    {
        Expression deconstructor = dp.Deconstructor;
        var visitedDeconstructor = Visit(deconstructor, p);
        if (visitedDeconstructor is Expression d && !ReferenceEquals(d, deconstructor))
        {
            deconstructor = d;
        }

        var newElements = new List<JRightPadded<J>>();
        bool changed = false;

        foreach (var nested in dp.Nested.Elements)
        {
            var visited = Visit(nested.Element, p);
            if (visited is J v && !ReferenceEquals(v, nested.Element))
            {
                changed = true;
                newElements.Add(nested.WithElement(v));
            }
            else
            {
                newElements.Add(nested);
            }
        }

        var nestedPatterns = changed
            ? dp.Nested.WithElements(newElements)
            : dp.Nested;

        if (!ReferenceEquals(deconstructor, dp.Deconstructor) ||
            !ReferenceEquals(nestedPatterns, dp.Nested))
        {
            return dp.WithDeconstructor(deconstructor).WithNested(nestedPatterns);
        }

        return dp;
    }

    public virtual J VisitSynchronized(Synchronized sync, P p)
    {
        var lockExpr = Visit(sync.Lock, p);
        var body = Visit(sync.Body, p);

        if ((lockExpr is ControlParentheses<Expression> cp && !ReferenceEquals(cp, sync.Lock)) ||
            (body is Block b && !ReferenceEquals(b, sync.Body)))
        {
            return sync.WithLock(lockExpr as ControlParentheses<Expression> ?? sync.Lock).WithBody(body as Block ?? sync.Body);
        }

        return sync;
    }

    public virtual J VisitTypeCast(TypeCast cast, P p)
    {
        var clazz = Visit(cast.Clazz, p);
        var expr = Visit(cast.Expression, p);

        if ((clazz is ControlParentheses<TypeTree> cp && !ReferenceEquals(cp, cast.Clazz)) ||
            (expr is Expression e && !ReferenceEquals(e, cast.Expression)))
        {
            return cast.WithClazz(clazz as ControlParentheses<TypeTree> ?? cast.Clazz).WithExpression(expr as Expression ?? cast.Expression);
        }

        return cast;
    }

    public virtual J VisitTypeParameter(TypeParameter typeParameter, P p)
    {
        var changed = false;

        Expression newName = typeParameter.Name;
        var visitedName = Visit(typeParameter.Name, p);
        if (visitedName is Expression ne && !ReferenceEquals(ne, typeParameter.Name))
        {
            newName = ne;
            changed = true;
        }

        JContainer<TypeTree>? newBounds = typeParameter.Bounds;
        if (typeParameter.Bounds != null)
        {
            var boundElements = new List<JRightPadded<TypeTree>>();
            bool boundsChanged = false;
            foreach (var paddedBound in typeParameter.Bounds.Elements)
            {
                var visited = Visit(paddedBound.Element, p);
                if (visited is TypeTree tt)
                {
                    if (!ReferenceEquals(tt, paddedBound.Element)) boundsChanged = true;
                    boundElements.Add(paddedBound.WithElement(tt));
                }
                else
                {
                    boundElements.Add(paddedBound);
                }
            }
            if (boundsChanged)
            {
                newBounds = typeParameter.Bounds.WithElements(boundElements);
                changed = true;
            }
        }

        return changed
            ? typeParameter.WithName(newName).WithBounds(newBounds)
            : typeParameter;
    }

    public virtual J VisitPackage(Package pkg, P p)
    {
        var annotations = pkg.Annotations;
        bool annotationsChanged = false;
        var newAnnotations = new List<Annotation>();
        foreach (var ann in annotations)
        {
            var visited = Visit(ann, p);
            if (visited is Annotation a)
            {
                if (!ReferenceEquals(a, ann))
                {
                    annotationsChanged = true;
                }
                newAnnotations.Add(a);
            }
        }

        var expr = Visit(pkg.Expression.Element, p);

        if (annotationsChanged ||
            (expr is Expression e && !ReferenceEquals(e, pkg.Expression.Element)))
        {
            return pkg.WithAnnotations(annotationsChanged ? newAnnotations : pkg.Annotations).WithExpression(expr is Expression newExpr ? pkg.Expression.WithElement(newExpr) : pkg.Expression);
        }

        return pkg;
    }
}
