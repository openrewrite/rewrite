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
            Try.Catch cat => VisitCatch(cat, p),
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
            NamedVariable nv => VisitNamedVariable(nv, p),
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
            Lambda.Parameters lp => VisitLambdaParameters(lp, p),
            Switch sw => VisitSwitch(sw, p),
            SwitchExpression se => VisitSwitchExpression(se, p),
            Case cs => VisitCase(cs, p),
            DeconstructionPattern dp => VisitDeconstructionPattern(dp, p),
            If.Else els => VisitElse(els, p),
            Label lbl => VisitLabel(lbl, p),
            Synchronized sync => VisitSynchronized(sync, p),
            TypeCast tc => VisitTypeCast(tc, p),
            TypeParameter tp => VisitTypeParameter(tp, p),
            Package pkg => VisitPackage(pkg, p),
            ForLoop.Control flc => VisitForControl(flc, p),
            ForEachLoop.Control felc => VisitForEachControl(felc, p),
            _ => throw new InvalidOperationException($"Unknown J tree type: {tree.GetType()}")
        };
    }

    public virtual Space VisitSpace(Space space, P p)
    {
        return space;
    }

    public virtual JLeftPadded<T>? VisitLeftPadded<T>(JLeftPadded<T>? left, P p)
    {
        if (left == null) return null;

        Cursor = new Cursor(Cursor, left);

        var before = VisitSpace(left.Before, p);
        var t = left.Element;
        bool elementChanged = false;
        if (t is J j)
        {
            var visited = Visit(j, p);
            if (visited is T vt) { t = vt; elementChanged = !ReferenceEquals(visited, j); }
            else { Cursor = Cursor.Parent!; return null; }
        }

        Cursor = Cursor.Parent!;

        if (ReferenceEquals(before, left.Before) && !elementChanged)
            return left;
        return new JLeftPadded<T>(before, t);
    }

    public virtual JRightPadded<T>? VisitRightPadded<T>(JRightPadded<T>? right, P p)
    {
        if (right == null) return null;

        Cursor = new Cursor(Cursor, right);

        var t = right.Element;
        bool elementChanged = false;
        if (t is J j)
        {
            var visited = Visit(j, p);
            if (visited is T vt) { t = vt; elementChanged = !ReferenceEquals(visited, j); }
            else { Cursor = Cursor.Parent!; return null; }
        }

        var after = VisitSpace(right.After, p);
        var markers = VisitMarkers(right.Markers, p);

        Cursor = Cursor.Parent!;

        if (ReferenceEquals(after, right.After) && !elementChanged && ReferenceEquals(markers, right.Markers))
            return right;
        return new JRightPadded<T>(t, after, markers);
    }

    public virtual JContainer<T>? VisitContainer<T>(JContainer<T>? container, P p) where T : J
    {
        if (container == null) return null;

        Cursor = new Cursor(Cursor, container);

        var before = VisitSpace(container.Before, p);
        var elements = container.Elements;
        var changed = false;
        var newElements = new List<JRightPadded<T>>(elements.Count);
        foreach (var elem in elements)
        {
            var visited = VisitRightPadded(elem, p);
            if (visited != null)
            {
                if (!ReferenceEquals(visited, elem)) changed = true;
                newElements.Add(visited);
            }
            else changed = true;
        }

        Cursor = Cursor.Parent!;

        if (!changed && ReferenceEquals(before, container.Before))
            return container;
        return new JContainer<T>(before, newElements, container.Markers);
    }

    public virtual Markers VisitMarkers(Markers markers, P p)
    {
        if (ReferenceEquals(markers, Markers.Empty) || markers.MarkerList.Count == 0)
        {
            return markers;
        }

        var list = markers.MarkerList;
        bool changed = false;
        var newList = new List<Marker>(list.Count);
        foreach (var marker in list)
        {
            var visited = VisitMarker(marker, p);
            newList.Add(visited);
            if (!ReferenceEquals(visited, marker)) changed = true;
        }

        return changed ? markers.WithMarkerList(newList) : markers;
    }

    public virtual Marker VisitMarker(Marker marker, P p)
    {
        return marker;
    }

    public virtual J VisitExpression(Expression expression, P p)
    {
        return (J)expression;
    }

    public virtual J VisitStatement(Statement statement, P p)
    {
        return (J)statement;
    }

    public virtual JavaType? VisitType(JavaType? javaType, P p)
    {
        return javaType;
    }

    // -----------------------------------------------------------------------
    // Annotation : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitAnnotation(Annotation annotation, P p)
    {
        annotation = annotation
            .WithPrefix(VisitSpace(annotation.Prefix, p))
            .WithMarkers(VisitMarkers(annotation.Markers, p));

        var exprResult = VisitExpression(annotation, p);
        if (exprResult is not Annotation node) return exprResult;

        return node
            .WithAnnotationType((NameTree)Visit(node.AnnotationType, p)!)
            .WithArguments(VisitContainer(node.Arguments, p));
    }

    // -----------------------------------------------------------------------
    // Block : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitBlock(Block block, P p)
    {
        block = block
            .WithPrefix(VisitSpace(block.Prefix, p))
            .WithMarkers(VisitMarkers(block.Markers, p));

        var stmtResult = VisitStatement(block, p);
        if (stmtResult is not Block node) return stmtResult;

        return node
            .WithEnd(VisitSpace(node.End, p))
            .WithStatements(ListUtils.Map(node.Statements, stmt => VisitRightPadded(stmt, p)));
    }

    // -----------------------------------------------------------------------
    // ClassDeclaration : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitClassDeclaration(ClassDeclaration classDecl, P p)
    {
        classDecl = classDecl
            .WithPrefix(VisitSpace(classDecl.Prefix, p))
            .WithMarkers(VisitMarkers(classDecl.Markers, p));

        var stmtResult = VisitStatement(classDecl, p);
        if (stmtResult is not ClassDeclaration node) return stmtResult;

        var newKindAnnotations = ListUtils.Map(node.ClassKind.Annotations, ann => Visit(ann, p) as Annotation);

        return node
            .WithLeadingAnnotations(ListUtils.Map(node.LeadingAnnotations, ann => Visit(ann, p) as Annotation))
            .WithClassKind(node.ClassKind.WithAnnotations(newKindAnnotations))
            .WithName((Identifier)Visit(node.Name, p)!)
            .WithTypeParameters(VisitContainer(node.TypeParameters, p))
            .WithPrimaryConstructor(VisitContainer(node.PrimaryConstructor, p))
            .WithExtends(VisitLeftPadded(node.Extends, p))
            .WithImplements(VisitContainer(node.Implements, p))
            .WithPermits(VisitContainer(node.Permits, p))
            .WithBody((Block)Visit(node.Body, p)!)
            .WithType((JavaType.FullyQualified?)VisitType(node.Type, p));
    }

    // -----------------------------------------------------------------------
    // EnumValueSet : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitEnumValueSet(EnumValueSet enumValueSet, P p)
    {
        enumValueSet = enumValueSet
            .WithPrefix(VisitSpace(enumValueSet.Prefix, p))
            .WithMarkers(VisitMarkers(enumValueSet.Markers, p));

        var stmtResult = VisitStatement(enumValueSet, p);
        if (stmtResult is not EnumValueSet node) return stmtResult;

        return node
            .WithEnums(ListUtils.Map(node.Enums, e => VisitRightPadded(e, p)));
    }

    // -----------------------------------------------------------------------
    // EnumValue : (neither Statement nor Expression)
    // -----------------------------------------------------------------------
    public virtual J VisitEnumValue(EnumValue enumValue, P p)
    {
        return enumValue
            .WithPrefix(VisitSpace(enumValue.Prefix, p))
            .WithMarkers(VisitMarkers(enumValue.Markers, p))
            .WithAnnotations(ListUtils.Map(enumValue.Annotations, ann => Visit(ann, p) as Annotation))
            .WithName((Identifier)Visit(enumValue.Name, p)!)
            .WithInitializer((NewClass?)Visit(enumValue.Initializer, p));
    }

    // -----------------------------------------------------------------------
    // MethodDeclaration : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitMethodDeclaration(MethodDeclaration method, P p)
    {
        method = method
            .WithPrefix(VisitSpace(method.Prefix, p))
            .WithMarkers(VisitMarkers(method.Markers, p));

        var stmtResult = VisitStatement(method, p);
        if (stmtResult is not MethodDeclaration node) return stmtResult;

        return node
            .WithLeadingAnnotations(ListUtils.Map(node.LeadingAnnotations, ann => Visit(ann, p) as Annotation))
            .WithTypeParameters(VisitContainer(node.TypeParameters, p))
            .WithReturnTypeExpression((TypeTree?)Visit(node.ReturnTypeExpression, p))
            .WithName((Identifier)Visit(node.Name, p)!)
            .WithParameters(VisitContainer(node.Parameters, p)!)
            .WithThrows(VisitContainer(node.Throws, p))
            .WithBody((Block?)Visit(node.Body, p))
            .WithDefaultValue(VisitLeftPadded(node.DefaultValue, p))
            .WithMethodType((JavaType.Method?)VisitType(node.MethodType, p));
    }

    // -----------------------------------------------------------------------
    // Return : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitReturn(Return ret, P p)
    {
        ret = ret
            .WithPrefix(VisitSpace(ret.Prefix, p))
            .WithMarkers(VisitMarkers(ret.Markers, p));

        var stmtResult = VisitStatement(ret, p);
        if (stmtResult is not Return node) return stmtResult;

        return node
            .WithExpression((Expression?)Visit(node.Expression, p));
    }

    // -----------------------------------------------------------------------
    // If : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitIf(If iff, P p)
    {
        iff = iff
            .WithPrefix(VisitSpace(iff.Prefix, p))
            .WithMarkers(VisitMarkers(iff.Markers, p));

        var stmtResult = VisitStatement(iff, p);
        if (stmtResult is not If node) return stmtResult;

        return node
            .WithCondition((ControlParentheses<Expression>)Visit(node.Condition, p)!)
            .WithThenPart(VisitRightPadded(node.ThenPart, p)!)
            .WithElsePart((If.Else?)Visit(node.ElsePart, p));
    }

    // -----------------------------------------------------------------------
    // If.Else : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitElse(If.Else @else, P p)
    {
        return @else
            .WithPrefix(VisitSpace(@else.Prefix, p))
            .WithMarkers(VisitMarkers(@else.Markers, p))
            .WithBody(VisitRightPadded(@else.Body, p)!);
    }

    // -----------------------------------------------------------------------
    // WhileLoop : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitWhileLoop(WhileLoop whl, P p)
    {
        whl = whl
            .WithPrefix(VisitSpace(whl.Prefix, p))
            .WithMarkers(VisitMarkers(whl.Markers, p));

        var stmtResult = VisitStatement(whl, p);
        if (stmtResult is not WhileLoop node) return stmtResult;

        return node
            .WithCondition((ControlParentheses<Expression>)Visit(node.Condition, p)!)
            .WithBody(VisitRightPadded(node.Body, p)!);
    }

    // -----------------------------------------------------------------------
    // DoWhileLoop : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitDoWhileLoop(DoWhileLoop dwl, P p)
    {
        dwl = dwl
            .WithPrefix(VisitSpace(dwl.Prefix, p))
            .WithMarkers(VisitMarkers(dwl.Markers, p));

        var stmtResult = VisitStatement(dwl, p);
        if (stmtResult is not DoWhileLoop node) return stmtResult;

        return node
            .WithBody(VisitRightPadded(node.Body, p)!)
            .WithCondition(VisitLeftPadded(node.Condition, p)!);
    }

    // -----------------------------------------------------------------------
    // ForLoop : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitForLoop(ForLoop fl, P p)
    {
        fl = fl
            .WithPrefix(VisitSpace(fl.Prefix, p))
            .WithMarkers(VisitMarkers(fl.Markers, p));

        var stmtResult = VisitStatement(fl, p);
        if (stmtResult is not ForLoop node) return stmtResult;

        return node
            .WithLoopControl((ForLoop.Control)Visit(node.LoopControl, p)!)
            .WithBody(VisitRightPadded(node.Body, p)!);
    }

    // -----------------------------------------------------------------------
    // ForLoop.Control : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitForControl(ForLoop.Control control, P p)
    {
        return control
            .WithPrefix(VisitSpace(control.Prefix, p))
            .WithMarkers(VisitMarkers(control.Markers, p))
            .WithInit(ListUtils.Map(control.Init, init => VisitRightPadded(init, p)))
            .WithCondition(VisitRightPadded(control.Condition, p)!)
            .WithUpdate(ListUtils.Map(control.Update, update => VisitRightPadded(update, p)));
    }

    // -----------------------------------------------------------------------
    // ForEachLoop : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitForEachLoop(ForEachLoop fel, P p)
    {
        fel = fel
            .WithPrefix(VisitSpace(fel.Prefix, p))
            .WithMarkers(VisitMarkers(fel.Markers, p));

        var stmtResult = VisitStatement(fel, p);
        if (stmtResult is not ForEachLoop node) return stmtResult;

        return node
            .WithLoopControl((ForEachLoop.Control)Visit(node.LoopControl, p)!)
            .WithBody(VisitRightPadded(node.Body, p)!);
    }

    // -----------------------------------------------------------------------
    // ForEachLoop.Control : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitForEachControl(ForEachLoop.Control control, P p)
    {
        return control
            .WithPrefix(VisitSpace(control.Prefix, p))
            .WithMarkers(VisitMarkers(control.Markers, p))
            .WithVariable(VisitRightPadded(control.Variable, p)!)
            .WithIterable(VisitRightPadded(control.Iterable, p)!);
    }

    // -----------------------------------------------------------------------
    // Try : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitTry(Try tr, P p)
    {
        tr = tr
            .WithPrefix(VisitSpace(tr.Prefix, p))
            .WithMarkers(VisitMarkers(tr.Markers, p));

        var stmtResult = VisitStatement(tr, p);
        if (stmtResult is not Try node) return stmtResult;

        return node
            .WithResources(VisitContainer(node.Resources, p))
            .WithBody((Block)Visit(node.Body, p)!)
            .WithCatches(ListUtils.Map(node.Catches, c => Visit(c, p) as Try.Catch))
            .WithFinally(VisitLeftPadded(node.Finally, p));
    }

    // -----------------------------------------------------------------------
    // Try.Catch : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitCatch(Try.Catch cat, P p)
    {
        return cat
            .WithPrefix(VisitSpace(cat.Prefix, p))
            .WithMarkers(VisitMarkers(cat.Markers, p))
            .WithParameter((ControlParentheses<VariableDeclarations>)Visit(cat.Parameter, p)!)
            .WithBody((Block)Visit(cat.Body, p)!);
    }

    // -----------------------------------------------------------------------
    // Throw : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitThrow(Throw thr, P p)
    {
        thr = thr
            .WithPrefix(VisitSpace(thr.Prefix, p))
            .WithMarkers(VisitMarkers(thr.Markers, p));

        var stmtResult = VisitStatement(thr, p);
        if (stmtResult is not Throw node) return stmtResult;

        return node
            .WithException((Expression)Visit(node.Exception, p)!);
    }

    // -----------------------------------------------------------------------
    // Break : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitBreak(Break brk, P p)
    {
        brk = brk
            .WithPrefix(VisitSpace(brk.Prefix, p))
            .WithMarkers(VisitMarkers(brk.Markers, p));

        var stmtResult = VisitStatement(brk, p);
        if (stmtResult is not Break node) return stmtResult;

        return node
            .WithLabel((Identifier?)Visit(node.Label, p));
    }

    // -----------------------------------------------------------------------
    // Continue : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitContinue(Continue cont, P p)
    {
        cont = cont
            .WithPrefix(VisitSpace(cont.Prefix, p))
            .WithMarkers(VisitMarkers(cont.Markers, p));

        var stmtResult = VisitStatement(cont, p);
        if (stmtResult is not Continue node) return stmtResult;

        return node
            .WithLabel((Identifier?)Visit(node.Label, p));
    }

    // -----------------------------------------------------------------------
    // Empty : Statement, Expression
    // -----------------------------------------------------------------------
    public virtual J VisitEmpty(Empty emp, P p)
    {
        emp = emp
            .WithPrefix(VisitSpace(emp.Prefix, p))
            .WithMarkers(VisitMarkers(emp.Markers, p));

        var stmtResult = VisitStatement(emp, p);
        if (stmtResult is not Empty s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not Empty node) return exprResult;

        return node;
    }

    // -----------------------------------------------------------------------
    // ControlParentheses<Expression> : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitControlParentheses(ControlParentheses<Expression> cp, P p)
    {
        cp = cp
            .WithPrefix(VisitSpace(cp.Prefix, p))
            .WithMarkers(VisitMarkers(cp.Markers, p));

        var exprResult = VisitExpression(cp, p);
        if (exprResult is not ControlParentheses<Expression> node) return exprResult;

        return node
            .WithTree(VisitRightPadded(node.Tree, p)!);
    }

    // -----------------------------------------------------------------------
    // ControlParentheses<TypeTree> : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitControlParentheses(ControlParentheses<TypeTree> cp, P p)
    {
        cp = cp
            .WithPrefix(VisitSpace(cp.Prefix, p))
            .WithMarkers(VisitMarkers(cp.Markers, p));

        var exprResult = VisitExpression(cp, p);
        if (exprResult is not ControlParentheses<TypeTree> node) return exprResult;

        return node
            .WithTree(VisitRightPadded(node.Tree, p)!);
    }

    // -----------------------------------------------------------------------
    // ControlParentheses<VariableDeclarations> : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitControlParentheses(ControlParentheses<VariableDeclarations> cp, P p)
    {
        cp = cp
            .WithPrefix(VisitSpace(cp.Prefix, p))
            .WithMarkers(VisitMarkers(cp.Markers, p));

        var exprResult = VisitExpression(cp, p);
        if (exprResult is not ControlParentheses<VariableDeclarations> node) return exprResult;

        return node
            .WithTree(VisitRightPadded(node.Tree, p)!);
    }

    // -----------------------------------------------------------------------
    // Literal : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitLiteral(Literal literal, P p)
    {
        literal = literal
            .WithPrefix(VisitSpace(literal.Prefix, p))
            .WithMarkers(VisitMarkers(literal.Markers, p));

        var exprResult = VisitExpression(literal, p);
        if (exprResult is not Literal node) return exprResult;

        return node;
    }

    // -----------------------------------------------------------------------
    // Identifier : Expression, TypeTree
    // -----------------------------------------------------------------------
    public virtual J VisitIdentifier(Identifier identifier, P p)
    {
        identifier = identifier
            .WithPrefix(VisitSpace(identifier.Prefix, p))
            .WithMarkers(VisitMarkers(identifier.Markers, p));

        var exprResult = VisitExpression(identifier, p);
        if (exprResult is not Identifier node) return exprResult;

        return node
            .WithAnnotations(ListUtils.Map(node.Annotations, ann => Visit(ann, p) as Annotation))
            .WithType(VisitType(node.Type, p))
            .WithFieldType(VisitType(node.FieldType, p));
    }

    // -----------------------------------------------------------------------
    // FieldAccess : Expression, Statement, TypeTree
    // -----------------------------------------------------------------------
    public virtual J VisitFieldAccess(FieldAccess fieldAccess, P p)
    {
        fieldAccess = fieldAccess
            .WithPrefix(VisitSpace(fieldAccess.Prefix, p))
            .WithMarkers(VisitMarkers(fieldAccess.Markers, p));

        var exprResult = VisitExpression(fieldAccess, p);
        if (exprResult is not FieldAccess e1) return exprResult;

        var stmtResult = VisitStatement(e1, p);
        if (stmtResult is not FieldAccess node) return stmtResult;

        return node
            .WithTarget((Expression)Visit(node.Target, p)!)
            .WithName(VisitLeftPadded(node.Name, p)!)
            .WithType(VisitType(node.Type, p));
    }

    // -----------------------------------------------------------------------
    // MemberReference : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitMemberReference(MemberReference memberRef, P p)
    {
        memberRef = memberRef
            .WithPrefix(VisitSpace(memberRef.Prefix, p))
            .WithMarkers(VisitMarkers(memberRef.Markers, p));

        var exprResult = VisitExpression(memberRef, p);
        if (exprResult is not MemberReference node) return exprResult;

        return node
            .WithContaining(VisitRightPadded(node.Containing, p)!)
            .WithTypeParameters(VisitContainer(node.TypeParameters, p))
            .WithReference(VisitLeftPadded(node.Reference, p)!)
            .WithType(VisitType(node.Type, p))
            .WithMethodType((JavaType.Method?)VisitType(node.MethodType, p))
            .WithVariableType((JavaType.Variable?)VisitType(node.VariableType, p));
    }

    // -----------------------------------------------------------------------
    // Binary : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitBinary(Binary binary, P p)
    {
        binary = binary
            .WithPrefix(VisitSpace(binary.Prefix, p))
            .WithMarkers(VisitMarkers(binary.Markers, p));

        var exprResult = VisitExpression(binary, p);
        if (exprResult is not Binary node) return exprResult;

        return node
            .WithLeft((Expression)Visit(node.Left, p)!)
            .WithOperator(VisitLeftPadded(node.Operator, p)!)
            .WithRight((Expression)Visit(node.Right, p)!)
            .WithType(VisitType(node.Type, p));
    }

    // -----------------------------------------------------------------------
    // Ternary : Expression, Statement
    // -----------------------------------------------------------------------
    public virtual J VisitTernary(Ternary ternary, P p)
    {
        ternary = ternary
            .WithPrefix(VisitSpace(ternary.Prefix, p))
            .WithMarkers(VisitMarkers(ternary.Markers, p));

        var exprResult = VisitExpression(ternary, p);
        if (exprResult is not Ternary e1) return exprResult;

        var stmtResult = VisitStatement(e1, p);
        if (stmtResult is not Ternary node) return stmtResult;

        return node
            .WithCondition((Expression)Visit(node.Condition, p)!)
            .WithTruePart(VisitLeftPadded(node.TruePart, p)!)
            .WithFalsePart(VisitLeftPadded(node.FalsePart, p)!)
            .WithType(VisitType(node.Type, p));
    }

    // -----------------------------------------------------------------------
    // Assignment : Statement, Expression
    // -----------------------------------------------------------------------
    public virtual J VisitAssignment(Assignment assignment, P p)
    {
        assignment = assignment
            .WithPrefix(VisitSpace(assignment.Prefix, p))
            .WithMarkers(VisitMarkers(assignment.Markers, p));

        var stmtResult = VisitStatement(assignment, p);
        if (stmtResult is not Assignment s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not Assignment node) return exprResult;

        return node
            .WithVariable((Expression)Visit(node.Variable, p)!)
            .WithAssignmentValue(VisitLeftPadded(node.AssignmentValue, p)!)
            .WithType(VisitType(node.Type, p));
    }

    // -----------------------------------------------------------------------
    // AssignmentOperation : Statement, Expression
    // -----------------------------------------------------------------------
    public virtual J VisitAssignmentOperation(AssignmentOperation assignment, P p)
    {
        assignment = assignment
            .WithPrefix(VisitSpace(assignment.Prefix, p))
            .WithMarkers(VisitMarkers(assignment.Markers, p));

        var stmtResult = VisitStatement(assignment, p);
        if (stmtResult is not AssignmentOperation s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not AssignmentOperation node) return exprResult;

        return node
            .WithVariable((Expression)Visit(node.Variable, p)!)
            .WithAssignmentValue((Expression)Visit(node.AssignmentValue, p)!)
            .WithType(VisitType(node.Type, p));
    }

    // -----------------------------------------------------------------------
    // Unary : Statement, Expression
    // -----------------------------------------------------------------------
    public virtual J VisitUnary(Unary unary, P p)
    {
        unary = unary
            .WithPrefix(VisitSpace(unary.Prefix, p))
            .WithMarkers(VisitMarkers(unary.Markers, p));

        var stmtResult = VisitStatement(unary, p);
        if (stmtResult is not Unary s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not Unary node) return exprResult;

        return node
            .WithExpression((Expression)Visit(node.Expression, p)!)
            .WithType(VisitType(node.Type, p));
    }

    // -----------------------------------------------------------------------
    // Parentheses<Expression> : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitParentheses(Parentheses<Expression> parens, P p)
    {
        parens = parens
            .WithPrefix(VisitSpace(parens.Prefix, p))
            .WithMarkers(VisitMarkers(parens.Markers, p));

        var exprResult = VisitExpression(parens, p);
        if (exprResult is not Parentheses<Expression> node) return exprResult;

        return node
            .WithTree(VisitRightPadded(node.Tree, p)!);
    }

    // -----------------------------------------------------------------------
    // ExpressionStatement : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitExpressionStatement(ExpressionStatement expressionStatement, P p)
    {
        var stmtResult = VisitStatement(expressionStatement, p);
        if (stmtResult is not ExpressionStatement node) return stmtResult;

        // ExpressionStatement delegates Prefix/Markers to its inner Expression,
        // so they are visited when the Expression itself is visited.
        return node
            .WithExpression((Expression)Visit(node.Expression, p)!);
    }

    // -----------------------------------------------------------------------
    // VariableDeclarations : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitVariableDeclarations(VariableDeclarations varDecl, P p)
    {
        varDecl = varDecl
            .WithPrefix(VisitSpace(varDecl.Prefix, p))
            .WithMarkers(VisitMarkers(varDecl.Markers, p));

        var stmtResult = VisitStatement(varDecl, p);
        if (stmtResult is not VariableDeclarations node) return stmtResult;

        return node
            .WithLeadingAnnotations(ListUtils.Map(node.LeadingAnnotations, ann => Visit(ann, p) as Annotation))
            .WithTypeExpression((TypeTree?)Visit(node.TypeExpression, p))
            .WithVarargs(node.Varargs != null ? VisitSpace(node.Varargs, p) : null)
            .WithVariables(ListUtils.Map(node.Variables, v => VisitRightPadded(v, p)));
    }

    // -----------------------------------------------------------------------
    // NamedVariable : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitNamedVariable(NamedVariable namedVariable, P p)
    {
        return namedVariable
            .WithPrefix(VisitSpace(namedVariable.Prefix, p))
            .WithMarkers(VisitMarkers(namedVariable.Markers, p))
            .WithName((Identifier)Visit(namedVariable.Name, p)!)
            .WithInitializer(VisitLeftPadded(namedVariable.Initializer, p))
            .WithType(VisitType(namedVariable.Type, p));
    }

    // -----------------------------------------------------------------------
    // Primitive : Expression, TypeTree
    // -----------------------------------------------------------------------
    public virtual J VisitPrimitive(Primitive primitive, P p)
    {
        primitive = primitive
            .WithPrefix(VisitSpace(primitive.Prefix, p))
            .WithMarkers(VisitMarkers(primitive.Markers, p));

        var exprResult = VisitExpression(primitive, p);
        if (exprResult is not Primitive node) return exprResult;

        return node;
    }

    // -----------------------------------------------------------------------
    // MethodInvocation : Statement, Expression
    // -----------------------------------------------------------------------
    public virtual J VisitMethodInvocation(MethodInvocation mi, P p)
    {
        mi = mi
            .WithPrefix(VisitSpace(mi.Prefix, p))
            .WithMarkers(VisitMarkers(mi.Markers, p));

        var stmtResult = VisitStatement(mi, p);
        if (stmtResult is not MethodInvocation s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not MethodInvocation node) return exprResult;

        return node
            .WithSelect(VisitRightPadded(node.Select, p))
            .WithTypeParameters(VisitContainer(node.TypeParameters, p))
            .WithName((Identifier)Visit(node.Name, p)!)
            .WithArguments(VisitContainer(node.Arguments, p)!)
            .WithMethodType((JavaType.Method?)VisitType(node.MethodType, p));
    }

    // -----------------------------------------------------------------------
    // NewClass : Expression, Statement
    // -----------------------------------------------------------------------
    public virtual J VisitNewClass(NewClass nc, P p)
    {
        nc = nc
            .WithPrefix(VisitSpace(nc.Prefix, p))
            .WithMarkers(VisitMarkers(nc.Markers, p));

        var stmtResult = VisitStatement(nc, p);
        if (stmtResult is not NewClass s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not NewClass node) return exprResult;

        return node
            .WithNew(VisitSpace(node.New, p))
            .WithEnclosing(VisitRightPadded(node.Enclosing, p))
            .WithClazz((J?)Visit(node.Clazz, p))
            .WithArguments(VisitContainer(node.Arguments, p)!)
            .WithBody((Block?)Visit(node.Body, p))
            .WithConstructorType((JavaType.Method?)VisitType(node.ConstructorType, p));
    }

    // -----------------------------------------------------------------------
    // NewArray : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitNewArray(NewArray na, P p)
    {
        na = na
            .WithPrefix(VisitSpace(na.Prefix, p))
            .WithMarkers(VisitMarkers(na.Markers, p));

        var exprResult = VisitExpression(na, p);
        if (exprResult is not NewArray node) return exprResult;

        return node
            .WithTypeExpression((TypeTree?)Visit(node.TypeExpression, p))
            .WithDimensions(ListUtils.Map(node.Dimensions, dim => Visit(dim, p) as ArrayDimension))
            .WithInitializer(VisitContainer(node.Initializer, p))
            .WithType(VisitType(node.Type, p));
    }

    // -----------------------------------------------------------------------
    // InstanceOf : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitInstanceOf(InstanceOf instanceOf, P p)
    {
        instanceOf = instanceOf
            .WithPrefix(VisitSpace(instanceOf.Prefix, p))
            .WithMarkers(VisitMarkers(instanceOf.Markers, p));

        var exprResult = VisitExpression(instanceOf, p);
        if (exprResult is not InstanceOf node) return exprResult;

        return node
            .WithExpression(VisitRightPadded(node.Expression, p)!)
            .WithClazz((J)Visit(node.Clazz, p)!)
            .WithPattern((J?)Visit(node.Pattern, p))
            .WithType(VisitType(node.Type, p));
    }

    // -----------------------------------------------------------------------
    // NullableType : Expression, TypeTree
    // -----------------------------------------------------------------------
    public virtual J VisitNullableType(NullableType nullableType, P p)
    {
        nullableType = nullableType
            .WithPrefix(VisitSpace(nullableType.Prefix, p))
            .WithMarkers(VisitMarkers(nullableType.Markers, p));

        var exprResult = VisitExpression(nullableType, p);
        if (exprResult is not NullableType node) return exprResult;

        return node
            .WithAnnotations(ListUtils.Map(node.Annotations, ann => Visit(ann, p) as Annotation))
            .WithTypeTreePadded(VisitRightPadded(node.TypeTreePadded, p)!)
            .WithType(VisitType(node.Type, p));
    }

    // -----------------------------------------------------------------------
    // ParameterizedType : Expression, TypeTree
    // -----------------------------------------------------------------------
    public virtual J VisitParameterizedType(ParameterizedType pt, P p)
    {
        pt = pt
            .WithPrefix(VisitSpace(pt.Prefix, p))
            .WithMarkers(VisitMarkers(pt.Markers, p));

        var exprResult = VisitExpression(pt, p);
        if (exprResult is not ParameterizedType node) return exprResult;

        return node
            .WithClazz((NameTree)Visit(node.Clazz, p)!)
            .WithTypeParameters(VisitContainer(node.TypeParameters, p))
            .WithType(VisitType(node.Type, p));
    }

    // -----------------------------------------------------------------------
    // ArrayType : Expression, TypeTree
    // -----------------------------------------------------------------------
    public virtual J VisitArrayType(ArrayType at, P p)
    {
        at = at
            .WithPrefix(VisitSpace(at.Prefix, p))
            .WithMarkers(VisitMarkers(at.Markers, p));

        var exprResult = VisitExpression(at, p);
        if (exprResult is not ArrayType node) return exprResult;

        return node
            .WithElementType((TypeTree)Visit(node.ElementType, p)!)
            .WithAnnotations(node.Annotations != null ? ListUtils.Map(node.Annotations, ann => Visit(ann, p) as Annotation) : null)
            .WithType(VisitType(node.Type, p));
    }

    // -----------------------------------------------------------------------
    // ArrayAccess : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitArrayAccess(ArrayAccess arrayAccess, P p)
    {
        arrayAccess = arrayAccess
            .WithPrefix(VisitSpace(arrayAccess.Prefix, p))
            .WithMarkers(VisitMarkers(arrayAccess.Markers, p));

        var exprResult = VisitExpression(arrayAccess, p);
        if (exprResult is not ArrayAccess node) return exprResult;

        return node
            .WithIndexed((Expression)Visit(node.Indexed, p)!)
            .WithDimension((ArrayDimension)Visit(node.Dimension, p)!)
            .WithType(VisitType(node.Type, p));
    }

    // -----------------------------------------------------------------------
    // ArrayDimension : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitArrayDimension(ArrayDimension dimension, P p)
    {
        return dimension
            .WithPrefix(VisitSpace(dimension.Prefix, p))
            .WithMarkers(VisitMarkers(dimension.Markers, p))
            .WithIndex(VisitRightPadded(dimension.Index, p)!);
    }

    // -----------------------------------------------------------------------
    // Lambda : Expression (also implements Statement for JavaTemplate reasons)
    // -----------------------------------------------------------------------
    public virtual J VisitLambda(Lambda lambda, P p)
    {
        lambda = lambda
            .WithPrefix(VisitSpace(lambda.Prefix, p))
            .WithMarkers(VisitMarkers(lambda.Markers, p));

        var exprResult = VisitExpression(lambda, p);
        if (exprResult is not Lambda node) return exprResult;

        return node
            .WithParams((Lambda.Parameters)Visit(node.Params, p)!)
            .WithArrow(VisitSpace(node.Arrow, p))
            .WithBody((J)Visit(node.Body, p)!)
            .WithType(VisitType(node.Type, p));
    }

    // -----------------------------------------------------------------------
    // Lambda.Parameters : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitLambdaParameters(Lambda.Parameters parameters, P p)
    {
        return parameters
            .WithPrefix(VisitSpace(parameters.Prefix, p))
            .WithMarkers(VisitMarkers(parameters.Markers, p))
            .WithElements(ListUtils.Map(parameters.Elements, elem => VisitRightPadded(elem, p)));
    }

    // -----------------------------------------------------------------------
    // Switch : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitSwitch(Switch @switch, P p)
    {
        @switch = @switch
            .WithPrefix(VisitSpace(@switch.Prefix, p))
            .WithMarkers(VisitMarkers(@switch.Markers, p));

        var stmtResult = VisitStatement(@switch, p);
        if (stmtResult is not Switch node) return stmtResult;

        return node
            .WithSelector((ControlParentheses<Expression>)Visit(node.Selector, p)!)
            .WithCases((Block)Visit(node.Cases, p)!);
    }

    // -----------------------------------------------------------------------
    // SwitchExpression : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitSwitchExpression(SwitchExpression se, P p)
    {
        se = se
            .WithPrefix(VisitSpace(se.Prefix, p))
            .WithMarkers(VisitMarkers(se.Markers, p));

        var exprResult = VisitExpression(se, p);
        if (exprResult is not SwitchExpression node) return exprResult;

        return node
            .WithSelector((ControlParentheses<Expression>)Visit(node.Selector, p)!)
            .WithCases((Block)Visit(node.Cases, p)!);
    }

    // -----------------------------------------------------------------------
    // Case : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitCase(Case @case, P p)
    {
        @case = @case
            .WithPrefix(VisitSpace(@case.Prefix, p))
            .WithMarkers(VisitMarkers(@case.Markers, p));

        var stmtResult = VisitStatement(@case, p);
        if (stmtResult is not Case node) return stmtResult;

        return node
            .WithCaseLabels(VisitContainer(node.CaseLabels, p)!)
            .WithGuard((Expression?)Visit(node.Guard, p))
            .WithBody(VisitRightPadded(node.Body, p))
            .WithStatements(VisitContainer(node.Statements, p)!);
    }

    // -----------------------------------------------------------------------
    // DeconstructionPattern : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitDeconstructionPattern(DeconstructionPattern dp, P p)
    {
        dp = dp
            .WithPrefix(VisitSpace(dp.Prefix, p))
            .WithMarkers(VisitMarkers(dp.Markers, p));

        var exprResult = VisitExpression(dp, p);
        if (exprResult is not DeconstructionPattern node) return exprResult;

        return node
            .WithDeconstructor((Expression)Visit(node.Deconstructor, p)!)
            .WithNested(VisitContainer(node.Nested, p)!)
            .WithType(VisitType(node.Type, p));
    }

    // -----------------------------------------------------------------------
    // Label : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitLabel(Label label, P p)
    {
        label = label
            .WithPrefix(VisitSpace(label.Prefix, p))
            .WithMarkers(VisitMarkers(label.Markers, p));

        var stmtResult = VisitStatement(label, p);
        if (stmtResult is not Label node) return stmtResult;

        return node
            .WithLabelName(VisitRightPadded(node.LabelName, p)!)
            .WithStatement((Statement)Visit(node.Statement, p)!);
    }

    // -----------------------------------------------------------------------
    // Synchronized : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitSynchronized(Synchronized sync, P p)
    {
        sync = sync
            .WithPrefix(VisitSpace(sync.Prefix, p))
            .WithMarkers(VisitMarkers(sync.Markers, p));

        var stmtResult = VisitStatement(sync, p);
        if (stmtResult is not Synchronized node) return stmtResult;

        return node
            .WithLock((ControlParentheses<Expression>)Visit(node.Lock, p)!)
            .WithBody((Block)Visit(node.Body, p)!);
    }

    // -----------------------------------------------------------------------
    // TypeCast : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitTypeCast(TypeCast cast, P p)
    {
        cast = cast
            .WithPrefix(VisitSpace(cast.Prefix, p))
            .WithMarkers(VisitMarkers(cast.Markers, p));

        var exprResult = VisitExpression(cast, p);
        if (exprResult is not TypeCast node) return exprResult;

        return node
            .WithClazz((ControlParentheses<TypeTree>)Visit(node.Clazz, p)!)
            .WithExpression((Expression)Visit(node.Expression, p)!);
    }

    // -----------------------------------------------------------------------
    // TypeParameter : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitTypeParameter(TypeParameter typeParameter, P p)
    {
        return typeParameter
            .WithPrefix(VisitSpace(typeParameter.Prefix, p))
            .WithMarkers(VisitMarkers(typeParameter.Markers, p))
            .WithAnnotations(ListUtils.Map(typeParameter.Annotations, ann => Visit(ann, p) as Annotation))
            .WithName((Expression)Visit(typeParameter.Name, p)!)
            .WithBounds(VisitContainer(typeParameter.Bounds, p));
    }

    // -----------------------------------------------------------------------
    // Package : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitPackage(Package pkg, P p)
    {
        pkg = pkg
            .WithPrefix(VisitSpace(pkg.Prefix, p))
            .WithMarkers(VisitMarkers(pkg.Markers, p));

        var stmtResult = VisitStatement(pkg, p);
        if (stmtResult is not Package node) return stmtResult;

        return node
            .WithExpression((Expression)Visit(node.Expression, p)!)
            .WithAnnotations(ListUtils.Map(node.Annotations, ann => Visit(ann, p) as Annotation));
    }
}
