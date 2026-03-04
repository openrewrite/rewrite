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
        Visit(annotation.AnnotationType, p);
        if (annotation.Arguments != null)
        {
            foreach (var paddedArg in annotation.Arguments.Elements)
            {
                Visit(paddedArg.Element, p);
            }
        }
        return annotation;
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
        var name = Visit(enumValue.Name, p);
        if (enumValue.Initializer != null)
        {
            var init = Visit(enumValue.Initializer.Element, p);
            if (init is Expression e && !ReferenceEquals(e, enumValue.Initializer.Element))
            {
                return enumValue.WithInitializer(enumValue.Initializer.WithElement(e));
            }
        }
        if (name is Identifier id && !ReferenceEquals(id, enumValue.Name))
        {
            return enumValue.WithName(id);
        }
        return enumValue;
    }

    public virtual J VisitMethodDeclaration(MethodDeclaration method, P p)
    {
        if (method.ReturnTypeExpression != null)
        {
            Visit(method.ReturnTypeExpression, p);
        }

        foreach (var paddedParam in method.Parameters.Elements)
        {
            Visit(paddedParam.Element, p);
        }

        if (method.Body != null)
        {
            var body = Visit(method.Body, p);
            if (body is Block b && !ReferenceEquals(b, method.Body))
            {
                return method.WithBody(b);
            }
        }

        return method;
    }

    public virtual J VisitReturn(Return ret, P p)
    {
        if (ret.Expression != null)
        {
            Visit(ret.Expression, p);
        }
        return ret;
    }

    public virtual J VisitIf(If iff, P p)
    {
        Visit(iff.Condition.Tree.Element, p);
        Visit(iff.ThenPart.Element, p);
        if (iff.ElsePart != null)
        {
            Visit(iff.ElsePart.Body.Element, p);
        }
        return iff;
    }

    public virtual J VisitWhileLoop(WhileLoop whl, P p)
    {
        Visit(whl.Condition.Tree.Element, p);
        Visit(whl.Body.Element, p);
        return whl;
    }

    public virtual J VisitDoWhileLoop(DoWhileLoop dwl, P p)
    {
        Visit(dwl.Body.Element, p);
        Visit(dwl.Condition.Element.Tree.Element, p);
        return dwl;
    }

    public virtual J VisitForLoop(ForLoop fl, P p)
    {
        foreach (var init in fl.LoopControl.Init)
        {
            Visit(init.Element, p);
        }
        Visit(fl.LoopControl.Condition.Element, p);
        foreach (var update in fl.LoopControl.Update)
        {
            Visit(update.Element, p);
        }
        Visit(fl.Body.Element, p);
        return fl;
    }

    public virtual J VisitForEachLoop(ForEachLoop fel, P p)
    {
        Visit(fel.LoopControl.Variable.Element, p);
        Visit(fel.LoopControl.Iterable.Element, p);
        Visit(fel.Body.Element, p);
        return fel;
    }

    public virtual J VisitTry(Try tr, P p)
    {
        Visit(tr.Body, p);
        foreach (var c in tr.Catches)
        {
            Visit(c.Parameter.Tree.Element, p);
            Visit(c.Body, p);
        }
        if (tr.Finally != null)
        {
            Visit(tr.Finally.Element, p);
        }
        return tr;
    }

    public virtual J VisitThrow(Throw thr, P p)
    {
        Visit(thr.Exception, p);
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
        Visit(cp.Tree.Element, p);
        return cp;
    }

    public virtual J VisitControlParentheses(ControlParentheses<TypeTree> cp, P p)
    {
        Visit(cp.Tree.Element, p);
        return cp;
    }

    public virtual J VisitControlParentheses(ControlParentheses<VariableDeclarations> cp, P p)
    {
        Visit(cp.Tree.Element, p);
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
        Visit(memberRef.Containing.Element, p);
        Visit(memberRef.Reference.Element, p);
        return memberRef;
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
        if (varDecl.TypeExpression != null)
        {
            Visit(varDecl.TypeExpression, p);
        }

        foreach (var paddedVar in varDecl.Variables)
        {
            var namedVar = paddedVar.Element;
            if (namedVar.Initializer != null)
            {
                Visit(namedVar.Initializer.Element, p);
            }
        }

        return varDecl;
    }

    public virtual J VisitPrimitive(Primitive primitive, P p)
    {
        return primitive;
    }

    public virtual J VisitMethodInvocation(MethodInvocation mi, P p)
    {
        if (mi.Select != null)
        {
            Visit(mi.Select.Element, p);
        }

        if (mi.TypeParameters != null)
        {
            foreach (var paddedTypeArg in mi.TypeParameters.Elements)
            {
                Visit(paddedTypeArg.Element, p);
            }
        }

        foreach (var paddedArg in mi.Arguments.Elements)
        {
            Visit(paddedArg.Element, p);
        }

        return mi;
    }

    public virtual J VisitNewClass(NewClass nc, P p)
    {
        if (nc.Enclosing != null)
        {
            Visit(nc.Enclosing.Element, p);
        }

        if (nc.Clazz != null)
        {
            Visit(nc.Clazz, p);
        }

        foreach (var paddedArg in nc.Arguments.Elements)
        {
            Visit(paddedArg.Element, p);
        }

        if (nc.Body != null)
        {
            Visit(nc.Body, p);
        }

        return nc;
    }

    public virtual J VisitNewArray(NewArray na, P p)
    {
        if (na.TypeExpression != null)
        {
            Visit(na.TypeExpression, p);
        }

        foreach (var dim in na.Dimensions)
        {
            Visit(dim, p);
        }

        if (na.Initializer != null)
        {
            foreach (var paddedElem in na.Initializer.Elements)
            {
                Visit(paddedElem.Element, p);
            }
        }

        return na;
    }

    public virtual J VisitLabel(Label label, P p)
    {
        Visit(label.LabelName.Element, p);
        Visit(label.Statement, p);
        return label;
    }

    public virtual J VisitInstanceOf(InstanceOf instanceOf, P p)
    {
        Visit(instanceOf.Expression.Element, p);
        Visit(instanceOf.Clazz, p);
        if (instanceOf.Pattern != null) Visit(instanceOf.Pattern, p);
        if (instanceOf.InstanceOfModifier != null) Visit(instanceOf.InstanceOfModifier, p);
        return instanceOf;
    }

    public virtual J VisitNullableType(NullableType nullableType, P p)
    {
        Visit(nullableType.TypeTree, p);
        return nullableType;
    }

    public virtual J VisitParameterizedType(ParameterizedType pt, P p)
    {
        Visit(pt.Clazz, p);
        if (pt.TypeParameters != null)
        {
            foreach (var paddedParam in pt.TypeParameters.Elements)
            {
                Visit(paddedParam.Element, p);
            }
        }
        return pt;
    }

    public virtual J VisitArrayType(ArrayType at, P p)
    {
        Visit(at.ElementType, p);

        if (at.Annotations != null)
        {
            foreach (var ann in at.Annotations)
            {
                Visit(ann, p);
            }
        }

        return at;
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
        Visit(typeParameter.Name, p);
        if (typeParameter.Bounds != null)
        {
            foreach (var paddedBound in typeParameter.Bounds.Elements)
            {
                Visit(paddedBound.Element, p);
            }
        }
        return typeParameter;
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
