using Rewrite.Core;

namespace Rewrite.Java;

/// <summary>
/// Base visitor for Java LST elements.
/// Maintains a cursor for tracking the path from root to current node.
/// </summary>
public class JavaVisitor<P>
{
    public Cursor Cursor { get; set; } = new();

    public virtual J? Visit(Tree? tree, P p)
    {
        if (tree == null) return null;

        Cursor = new Cursor(Cursor, tree);

        var result = tree switch
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
            Binary bin => VisitBinary(bin, p),
            Ternary ter => VisitTernary(ter, p),
            Assignment asn => VisitAssignment(asn, p),
            AssignmentOperation asnOp => VisitAssignmentOperation(asnOp, p),
            Unary un => VisitUnary(un, p),
            Parentheses<Expression> paren => VisitParentheses(paren, p),
            ControlParentheses<Expression> cp => VisitControlParentheses(cp, p),
            ExpressionStatement es => VisitExpressionStatement(es, p),
            VariableDeclarations vd => VisitVariableDeclarations(vd, p),
            Primitive prim => VisitPrimitive(prim, p),
            MethodInvocation mi => VisitMethodInvocation(mi, p),
            NewClass nc => VisitNewClass(nc, p),
            NewArray na => VisitNewArray(na, p),
            ArrayType at => VisitArrayType(at, p),
            ArrayAccess aa => VisitArrayAccess(aa, p),
            ArrayDimension ad => VisitArrayDimension(ad, p),
            Lambda lam => VisitLambda(lam, p),
            Switch sw => VisitSwitch(sw, p),
            SwitchExpression se => VisitSwitchExpression(se, p),
            Case cs => VisitCase(cs, p),
            DeconstructionPattern dp => VisitDeconstructionPattern(dp, p),
            Synchronized sync => VisitSynchronized(sync, p),
            TypeCast tc => VisitTypeCast(tc, p),
            Package pkg => VisitPackage(pkg, p),
            _ => throw new InvalidOperationException($"Unknown J tree type: {tree.GetType().Name}")
        };

        Cursor = Cursor.Parent!;

        return result;
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
        foreach (var stmt in block.Statements)
        {
            var visited = Visit(stmt.Element, p);
            if (visited is Statement s)
            {
                statements.Add(stmt with { Element = s });
            }
        }
        return block with { Statements = statements };
    }

    public virtual J VisitClassDeclaration(ClassDeclaration classDecl, P p)
    {
        var body = Visit(classDecl.Body, p);
        if (body is Block b && !ReferenceEquals(b, classDecl.Body))
        {
            return classDecl with { Body = b };
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
                newEnums.Add(paddedValue with { Element = ev });
            }
            else
            {
                newEnums.Add(paddedValue);
            }
        }
        return changed ? enumValueSet with { Enums = newEnums } : enumValueSet;
    }

    public virtual J VisitEnumValue(EnumValue enumValue, P p)
    {
        var name = Visit(enumValue.Name, p);
        if (enumValue.Initializer != null)
        {
            var init = Visit(enumValue.Initializer.Element, p);
            if (init is Expression e && !ReferenceEquals(e, enumValue.Initializer.Element))
            {
                return enumValue with { Initializer = enumValue.Initializer with { Element = e } };
            }
        }
        if (name is Identifier id && !ReferenceEquals(id, enumValue.Name))
        {
            return enumValue with { Name = id };
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
                return method with { Body = b };
            }
        }

        return method;
    }

    public virtual J VisitReturn(Return ret, P p)
    {
        if (ret.Expression != null)
        {
            Visit(ret.Expression.Element, p);
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
            return fieldAccess with { Target = t };
        }
        return fieldAccess;
    }

    public virtual J VisitBinary(Binary binary, P p)
    {
        var left = Visit(binary.Left, p);
        var right = Visit(binary.Right, p);

        if (left is Expression l && right is Expression r &&
            (!ReferenceEquals(l, binary.Left) || !ReferenceEquals(r, binary.Right)))
        {
            return binary with { Left = l, Right = r };
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
            return ternary with
            {
                Condition = c,
                TruePart = ternary.TruePart with { Element = t },
                FalsePart = ternary.FalsePart with { Element = f }
            };
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
            return assignment with
            {
                Variable = v,
                AssignmentValue = assignment.AssignmentValue with { Element = val }
            };
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
            return assignment with { Variable = v, AssignmentValue = val };
        }
        return assignment;
    }

    public virtual J VisitUnary(Unary unary, P p)
    {
        var expr = Visit(unary.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, unary.Expression))
        {
            return unary with { Expression = e };
        }
        return unary;
    }

    public virtual J VisitParentheses(Parentheses<Expression> parens, P p)
    {
        var inner = Visit(parens.Tree.Element, p);
        if (inner is Expression e && !ReferenceEquals(e, parens.Tree.Element))
        {
            return parens with { Tree = parens.Tree with { Element = e } };
        }
        return parens;
    }

    public virtual J VisitExpressionStatement(ExpressionStatement expressionStatement, P p)
    {
        var expr = Visit(expressionStatement.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, expressionStatement.Expression))
        {
            return expressionStatement with { Expression = e };
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
            return arrayAccess with { Indexed = i, Dimension = d };
        }
        return arrayAccess;
    }

    public virtual J VisitArrayDimension(ArrayDimension dimension, P p)
    {
        var index = Visit(dimension.Index.Element, p);
        if (index is Expression i && !ReferenceEquals(i, dimension.Index.Element))
        {
            return dimension with { Index = dimension.Index with { Element = i } };
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
                elements.Add(paddedParam with { Element = vp });
            }
        }

        var newParams = changed ? @params with { Elements = elements } : @params;

        var body = Visit(lambda.Body, p);

        if (!ReferenceEquals(newParams, @params) ||
            (body is J b && !ReferenceEquals(b, lambda.Body)))
        {
            return lambda with
            {
                Params = newParams,
                Body = body is J newBody ? newBody : lambda.Body
            };
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
            return @switch with
            {
                Selector = selectorExpr is Expression newSe
                    ? selector with { Tree = selector.Tree with { Element = newSe } }
                    : selector,
                Cases = casesBlock is Block newCb ? newCb : @switch.Cases
            };
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
                newLabels.Add(labelPadded with { Element = v });
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
                newStatements.Add(stmtPadded with { Element = s });
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
            return @case with
            {
                CaseLabels = labelsChanged ? @case.CaseLabels with { Elements = newLabels } : @case.CaseLabels,
                Statements = statementsChanged ? @case.Statements with { Elements = newStatements } : @case.Statements,
                Body = bodyChanged && newBody != null ? @case.Body with { Element = newBody } : @case.Body
            };
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
            return se with
            {
                Selector = selector is Expression sel
                    ? se.Selector with { Tree = se.Selector.Tree with { Element = sel } }
                    : se.Selector,
                Cases = cases is Block newCases ? newCases : se.Cases
            };
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
                newElements.Add(nested with { Element = v });
            }
            else
            {
                newElements.Add(nested);
            }
        }

        var nestedPatterns = changed
            ? dp.Nested with { Elements = newElements }
            : dp.Nested;

        if (!ReferenceEquals(deconstructor, dp.Deconstructor) ||
            !ReferenceEquals(nestedPatterns, dp.Nested))
        {
            return dp with
            {
                Deconstructor = deconstructor,
                Nested = nestedPatterns
            };
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
            return sync with
            {
                Lock = lockExpr as ControlParentheses<Expression> ?? sync.Lock,
                Body = body as Block ?? sync.Body
            };
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
            return cast with
            {
                Clazz = clazz as ControlParentheses<TypeTree> ?? cast.Clazz,
                Expression = expr as Expression ?? cast.Expression
            };
        }

        return cast;
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
            return pkg with
            {
                Annotations = annotationsChanged ? newAnnotations : pkg.Annotations,
                Expression = expr is Expression newExpr
                    ? pkg.Expression with { Element = newExpr }
                    : pkg.Expression
            };
        }

        return pkg;
    }
}
