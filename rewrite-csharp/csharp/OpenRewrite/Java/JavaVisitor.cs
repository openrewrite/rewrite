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
        var exprResult = VisitExpression(annotation, p);
        if (exprResult is not Annotation node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        NameTree newAnnotationType = node.AnnotationType;
        var visitedType = Visit(node.AnnotationType, p);
        if (visitedType is NameTree tt && !ReferenceEquals(tt, node.AnnotationType))
        {
            newAnnotationType = tt;
            changed = true;
        }

        var newArguments = VisitContainer(node.Arguments, p);
        if (!ReferenceEquals(newArguments, node.Arguments)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithAnnotationType(newAnnotationType).WithArguments(newArguments)
            : node;
    }

    // -----------------------------------------------------------------------
    // Block : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitBlock(Block block, P p)
    {
        var stmtResult = VisitStatement(block, p);
        if (stmtResult is not Block node) return stmtResult;

        bool changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        var newEnd = VisitSpace(node.End, p);
        if (!ReferenceEquals(newEnd, node.End)) changed = true;

        var statements = new List<JRightPadded<Statement>>();
        foreach (var stmt in node.Statements)
        {
            var visited = VisitRightPadded(stmt, p);
            if (visited != null)
            {
                if (!ReferenceEquals(visited, stmt)) changed = true;
                statements.Add(visited);
            }
            else changed = true;
        }
        return changed ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithEnd(newEnd).WithStatements(statements) : node;
    }

    // -----------------------------------------------------------------------
    // ClassDeclaration : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitClassDeclaration(ClassDeclaration classDecl, P p)
    {
        var stmtResult = VisitStatement(classDecl, p);
        if (stmtResult is not ClassDeclaration node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // LeadingAnnotations
        IList<Annotation> newLeadingAnnotations = node.LeadingAnnotations;
        var annList = new List<Annotation>();
        bool annsChanged = false;
        foreach (var ann in node.LeadingAnnotations)
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
            newLeadingAnnotations = annList;
            changed = true;
        }

        // Kind annotations
        ClassDeclaration.Kind newKind = node.ClassKind;
        var kindAnnList = new List<Annotation>();
        bool kindAnnsChanged = false;
        foreach (var ann in node.ClassKind.Annotations)
        {
            var visited = Visit(ann, p);
            if (visited is Annotation a)
            {
                if (!ReferenceEquals(a, ann)) kindAnnsChanged = true;
                kindAnnList.Add(a);
            }
            else
            {
                kindAnnList.Add(ann);
            }
        }
        if (kindAnnsChanged)
        {
            newKind = node.ClassKind.WithAnnotations(kindAnnList);
            changed = true;
        }

        // Name
        Identifier newName = node.Name;
        var visitedName = Visit(node.Name, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, node.Name))
        {
            newName = id;
            changed = true;
        }

        // TypeParameters
        var newTypeParameters = VisitContainer(node.TypeParameters, p);
        if (!ReferenceEquals(newTypeParameters, node.TypeParameters)) changed = true;

        // PrimaryConstructor
        var newPrimaryConstructor = VisitContainer(node.PrimaryConstructor, p);
        if (!ReferenceEquals(newPrimaryConstructor, node.PrimaryConstructor)) changed = true;

        // Extends
        var newExtends = VisitLeftPadded(node.Extends, p);
        if (!ReferenceEquals(newExtends, node.Extends)) changed = true;

        // Implements
        var newImplements = VisitContainer(node.Implements, p);
        if (!ReferenceEquals(newImplements, node.Implements)) changed = true;

        // Permits
        var newPermits = VisitContainer(node.Permits, p);
        if (!ReferenceEquals(newPermits, node.Permits)) changed = true;

        // Body
        Block newBody = node.Body;
        var visitedBody = Visit(node.Body, p);
        if (visitedBody is Block b && !ReferenceEquals(b, node.Body))
        {
            newBody = b;
            changed = true;
        }

        // Type
        JavaType.FullyQualified? newType = (JavaType.FullyQualified?)VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type))
        {
            changed = true;
        }

        return changed
            ? node
                .WithPrefix(newPrefix)
                .WithMarkers(newMarkers)
                .WithLeadingAnnotations(newLeadingAnnotations)
                .WithClassKind(newKind)
                .WithName(newName)
                .WithTypeParameters(newTypeParameters)
                .WithPrimaryConstructor(newPrimaryConstructor)
                .WithExtends(newExtends)
                .WithImplements(newImplements)
                .WithPermits(newPermits)
                .WithBody(newBody)
                .WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // EnumValueSet : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitEnumValueSet(EnumValueSet enumValueSet, P p)
    {
        var stmtResult = VisitStatement(enumValueSet, p);
        if (stmtResult is not EnumValueSet node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        var newEnums = new List<JRightPadded<EnumValue>>();
        bool enumsChanged = false;
        foreach (var paddedValue in node.Enums)
        {
            var visited = VisitRightPadded(paddedValue, p);
            if (visited != null)
            {
                if (!ReferenceEquals(visited, paddedValue)) enumsChanged = true;
                newEnums.Add(visited);
            }
            else enumsChanged = true;
        }
        if (enumsChanged) changed = true;

        return changed ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithEnums(enumsChanged ? newEnums : node.Enums) : node;
    }

    // -----------------------------------------------------------------------
    // EnumValue : (neither Statement nor Expression)
    // -----------------------------------------------------------------------
    public virtual J VisitEnumValue(EnumValue enumValue, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(enumValue.Prefix, p);
        if (!ReferenceEquals(newPrefix, enumValue.Prefix)) changed = true;

        var newMarkers = VisitMarkers(enumValue.Markers, p);
        if (!ReferenceEquals(newMarkers, enumValue.Markers)) changed = true;

        // Annotations
        IList<Annotation> newAnnotations = enumValue.Annotations;
        var annList = new List<Annotation>();
        bool annsChanged = false;
        foreach (var ann in enumValue.Annotations)
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

        // Name
        Identifier newName = enumValue.Name;
        var visitedName = Visit(enumValue.Name, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, enumValue.Name))
        {
            newName = id;
            changed = true;
        }

        // Initializer (NewClass?)
        NewClass? newInitializer = enumValue.Initializer;
        if (enumValue.Initializer != null)
        {
            var visitedInit = Visit(enumValue.Initializer, p);
            if (visitedInit is NewClass nc && !ReferenceEquals(nc, enumValue.Initializer))
            {
                newInitializer = nc;
                changed = true;
            }
        }

        return changed
            ? enumValue.WithPrefix(newPrefix).WithMarkers(newMarkers).WithAnnotations(newAnnotations).WithName(newName).WithInitializer(newInitializer)
            : enumValue;
    }

    // -----------------------------------------------------------------------
    // MethodDeclaration : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitMethodDeclaration(MethodDeclaration method, P p)
    {
        var stmtResult = VisitStatement(method, p);
        if (stmtResult is not MethodDeclaration node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // LeadingAnnotations
        IList<Annotation> newLeadingAnnotations = node.LeadingAnnotations;
        var annList = new List<Annotation>();
        bool annsChanged = false;
        foreach (var ann in node.LeadingAnnotations)
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
            newLeadingAnnotations = annList;
            changed = true;
        }

        // TypeParameters
        var newTypeParameters = VisitContainer(node.TypeParameters, p);
        if (!ReferenceEquals(newTypeParameters, node.TypeParameters)) changed = true;

        // ReturnTypeExpression
        TypeTree? newReturnType = node.ReturnTypeExpression;
        if (node.ReturnTypeExpression != null)
        {
            var visited = Visit(node.ReturnTypeExpression, p);
            if (visited is TypeTree tt && !ReferenceEquals(tt, node.ReturnTypeExpression))
            {
                newReturnType = tt;
                changed = true;
            }
        }

        // Name
        Identifier newName = node.Name;
        var visitedName = Visit(node.Name, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, node.Name))
        {
            newName = id;
            changed = true;
        }

        // Parameters
        var newParams = VisitContainer(node.Parameters, p)!;
        if (!ReferenceEquals(newParams, node.Parameters)) changed = true;

        // Throws
        var newThrows = VisitContainer(node.Throws, p);
        if (!ReferenceEquals(newThrows, node.Throws)) changed = true;

        // Body
        Block? newBody = node.Body;
        if (node.Body != null)
        {
            var visited = Visit(node.Body, p);
            if (visited is Block b && !ReferenceEquals(b, node.Body))
            {
                newBody = b;
                changed = true;
            }
        }

        // DefaultValue
        var newDefaultValue = VisitLeftPadded(node.DefaultValue, p);
        if (!ReferenceEquals(newDefaultValue, node.DefaultValue)) changed = true;

        // MethodType
        JavaType.Method? newMethodType = (JavaType.Method?)VisitType(node.MethodType, p);
        if (!ReferenceEquals(newMethodType, node.MethodType))
        {
            changed = true;
        }

        return changed
            ? node
                .WithPrefix(newPrefix)
                .WithMarkers(newMarkers)
                .WithLeadingAnnotations(newLeadingAnnotations)
                .WithTypeParameters(newTypeParameters)
                .WithReturnTypeExpression(newReturnType)
                .WithName(newName)
                .WithParameters(newParams)
                .WithThrows(newThrows)
                .WithBody(newBody)
                .WithDefaultValue(newDefaultValue)
                .WithMethodType(newMethodType)
            : node;
    }

    // -----------------------------------------------------------------------
    // Return : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitReturn(Return ret, P p)
    {
        var stmtResult = VisitStatement(ret, p);
        if (stmtResult is not Return node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        Expression? newExpression = node.Expression;
        if (node.Expression != null)
        {
            var visited = Visit(node.Expression, p);
            if (visited is Expression expr && !ReferenceEquals(expr, node.Expression))
            {
                newExpression = expr;
                changed = true;
            }
        }

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpression(newExpression)
            : node;
    }

    // -----------------------------------------------------------------------
    // If : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitIf(If iff, P p)
    {
        var stmtResult = VisitStatement(iff, p);
        if (stmtResult is not If node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Condition (ControlParentheses<Expression>)
        ControlParentheses<Expression> newCondition = node.Condition;
        var visitedCondition = Visit(node.Condition, p);
        if (visitedCondition is ControlParentheses<Expression> vc && !ReferenceEquals(vc, node.Condition))
        {
            newCondition = vc;
            changed = true;
        }

        // ThenPart (JRightPadded<Statement>)
        var newThenPart = VisitRightPadded(node.ThenPart, p) ?? node.ThenPart;
        if (!ReferenceEquals(newThenPart, node.ThenPart)) changed = true;

        // ElsePart
        If.Else? newElsePart = node.ElsePart;
        if (node.ElsePart != null)
        {
            var visitedElse = Visit(node.ElsePart, p);
            if (visitedElse is If.Else ve && !ReferenceEquals(ve, node.ElsePart))
            {
                newElsePart = ve;
                changed = true;
            }
        }

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithCondition(newCondition).WithThenPart(newThenPart).WithElsePart(newElsePart)
            : node;
    }

    // -----------------------------------------------------------------------
    // If.Else : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitElse(If.Else @else, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(@else.Prefix, p);
        if (!ReferenceEquals(newPrefix, @else.Prefix)) changed = true;

        var newMarkers = VisitMarkers(@else.Markers, p);
        if (!ReferenceEquals(newMarkers, @else.Markers)) changed = true;

        var newBody = VisitRightPadded(@else.Body, p) ?? @else.Body;
        if (!ReferenceEquals(newBody, @else.Body)) changed = true;

        return changed ? @else.WithPrefix(newPrefix).WithMarkers(newMarkers).WithBody(newBody) : @else;
    }

    // -----------------------------------------------------------------------
    // WhileLoop : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitWhileLoop(WhileLoop whl, P p)
    {
        var stmtResult = VisitStatement(whl, p);
        if (stmtResult is not WhileLoop node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        ControlParentheses<Expression> newCondition = node.Condition;
        var visitedCondition = Visit(node.Condition, p);
        if (visitedCondition is ControlParentheses<Expression> vc && !ReferenceEquals(vc, node.Condition))
        {
            newCondition = vc;
            changed = true;
        }

        var newBody = VisitRightPadded(node.Body, p) ?? node.Body;
        if (!ReferenceEquals(newBody, node.Body)) changed = true;

        return changed ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithCondition(newCondition).WithBody(newBody) : node;
    }

    // -----------------------------------------------------------------------
    // DoWhileLoop : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitDoWhileLoop(DoWhileLoop dwl, P p)
    {
        var stmtResult = VisitStatement(dwl, p);
        if (stmtResult is not DoWhileLoop node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Body
        var newBody = VisitRightPadded(node.Body, p) ?? node.Body;
        if (!ReferenceEquals(newBody, node.Body)) changed = true;

        // Condition (JLeftPadded<ControlParentheses<Expression>>)
        var newCondition = VisitLeftPadded(node.Condition, p) ?? node.Condition;
        if (!ReferenceEquals(newCondition, node.Condition)) changed = true;

        return changed ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithBody(newBody).WithCondition(newCondition) : node;
    }

    // -----------------------------------------------------------------------
    // ForLoop : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitForLoop(ForLoop fl, P p)
    {
        var stmtResult = VisitStatement(fl, p);
        if (stmtResult is not ForLoop node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Control
        ForLoop.Control newControl = node.LoopControl;
        var visitedControl = Visit(node.LoopControl, p);
        if (visitedControl is ForLoop.Control vc && !ReferenceEquals(vc, node.LoopControl))
        {
            newControl = vc;
            changed = true;
        }

        // Body
        var newBody = VisitRightPadded(node.Body, p) ?? node.Body;
        if (!ReferenceEquals(newBody, node.Body)) changed = true;

        return changed ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithLoopControl(newControl).WithBody(newBody) : node;
    }

    // -----------------------------------------------------------------------
    // ForLoop.Control : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitForControl(ForLoop.Control control, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(control.Prefix, p);
        if (!ReferenceEquals(newPrefix, control.Prefix)) changed = true;

        var newMarkers = VisitMarkers(control.Markers, p);
        if (!ReferenceEquals(newMarkers, control.Markers)) changed = true;

        // Init
        var newInit = new List<JRightPadded<Statement>>();
        bool initChanged = false;
        foreach (var init in control.Init)
        {
            var visited = VisitRightPadded(init, p);
            if (visited != null)
            {
                if (!ReferenceEquals(visited, init)) initChanged = true;
                newInit.Add(visited);
            }
            else initChanged = true;
        }
        if (initChanged) changed = true;

        // Condition
        var newControlCondition = VisitRightPadded(control.Condition, p) ?? control.Condition;
        if (!ReferenceEquals(newControlCondition, control.Condition)) changed = true;

        // Update
        var newUpdate = new List<JRightPadded<Statement>>();
        bool updateChanged = false;
        foreach (var update in control.Update)
        {
            var visited = VisitRightPadded(update, p);
            if (visited != null)
            {
                if (!ReferenceEquals(visited, update)) updateChanged = true;
                newUpdate.Add(visited);
            }
            else updateChanged = true;
        }
        if (updateChanged) changed = true;

        return changed
            ? control
                .WithPrefix(newPrefix)
                .WithMarkers(newMarkers)
                .WithInit(initChanged ? newInit : control.Init)
                .WithCondition(newControlCondition)
                .WithUpdate(updateChanged ? newUpdate : control.Update)
            : control;
    }

    // -----------------------------------------------------------------------
    // ForEachLoop : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitForEachLoop(ForEachLoop fel, P p)
    {
        var stmtResult = VisitStatement(fel, p);
        if (stmtResult is not ForEachLoop node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Control
        ForEachLoop.Control newControl = node.LoopControl;
        var visitedControl = Visit(node.LoopControl, p);
        if (visitedControl is ForEachLoop.Control vc && !ReferenceEquals(vc, node.LoopControl))
        {
            newControl = vc;
            changed = true;
        }

        // Body
        var newBody = VisitRightPadded(node.Body, p) ?? node.Body;
        if (!ReferenceEquals(newBody, node.Body)) changed = true;

        return changed ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithLoopControl(newControl).WithBody(newBody) : node;
    }

    // -----------------------------------------------------------------------
    // ForEachLoop.Control : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitForEachControl(ForEachLoop.Control control, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(control.Prefix, p);
        if (!ReferenceEquals(newPrefix, control.Prefix)) changed = true;

        var newMarkers = VisitMarkers(control.Markers, p);
        if (!ReferenceEquals(newMarkers, control.Markers)) changed = true;

        // Variable
        var newVariable = VisitRightPadded(control.Variable, p) ?? control.Variable;
        if (!ReferenceEquals(newVariable, control.Variable)) changed = true;

        // Iterable
        var newIterable = VisitRightPadded(control.Iterable, p) ?? control.Iterable;
        if (!ReferenceEquals(newIterable, control.Iterable)) changed = true;

        return changed ? control.WithPrefix(newPrefix).WithMarkers(newMarkers).WithVariable(newVariable).WithIterable(newIterable) : control;
    }

    // -----------------------------------------------------------------------
    // Try : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitTry(Try tr, P p)
    {
        var stmtResult = VisitStatement(tr, p);
        if (stmtResult is not Try node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Resources
        var newResources = VisitContainer(node.Resources, p);
        if (!ReferenceEquals(newResources, node.Resources)) changed = true;

        // Body
        Block newBody = node.Body;
        var visitedBody = Visit(node.Body, p);
        if (visitedBody is Block bb && !ReferenceEquals(bb, node.Body))
        {
            newBody = bb;
            changed = true;
        }

        // Catches
        var newCatches = new List<Try.Catch>();
        bool catchesChanged = false;
        foreach (var c in node.Catches)
        {
            var visited = Visit(c, p);
            if (visited is Try.Catch vc)
            {
                if (!ReferenceEquals(vc, c)) catchesChanged = true;
                newCatches.Add(vc);
            }
        }
        if (catchesChanged) changed = true;

        // Finally
        var newFinally = VisitLeftPadded(node.Finally, p);
        if (!ReferenceEquals(newFinally, node.Finally)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithResources(newResources).WithBody(newBody).WithCatches(catchesChanged ? newCatches : node.Catches).WithFinally(newFinally)
            : node;
    }

    // -----------------------------------------------------------------------
    // Try.Catch : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitCatch(Try.Catch cat, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(cat.Prefix, p);
        if (!ReferenceEquals(newPrefix, cat.Prefix)) changed = true;

        var newMarkers = VisitMarkers(cat.Markers, p);
        if (!ReferenceEquals(newMarkers, cat.Markers)) changed = true;

        // Parameter (ControlParentheses<VariableDeclarations>)
        ControlParentheses<VariableDeclarations> newParam = cat.Parameter;
        var visitedParam = Visit(cat.Parameter, p);
        if (visitedParam is ControlParentheses<VariableDeclarations> vp && !ReferenceEquals(vp, cat.Parameter))
        {
            newParam = vp;
            changed = true;
        }

        // Body
        Block newBody = cat.Body;
        var visitedBody = Visit(cat.Body, p);
        if (visitedBody is Block cb && !ReferenceEquals(cb, cat.Body))
        {
            newBody = cb;
            changed = true;
        }

        return changed ? cat.WithPrefix(newPrefix).WithMarkers(newMarkers).WithParameter(newParam).WithBody(newBody) : cat;
    }

    // -----------------------------------------------------------------------
    // Throw : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitThrow(Throw thr, P p)
    {
        var stmtResult = VisitStatement(thr, p);
        if (stmtResult is not Throw node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        Expression newException = node.Exception;
        var visited = Visit(node.Exception, p);
        if (visited is Expression e && !ReferenceEquals(e, node.Exception))
        {
            newException = e;
            changed = true;
        }

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithException(newException)
            : node;
    }

    // -----------------------------------------------------------------------
    // Break : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitBreak(Break brk, P p)
    {
        var stmtResult = VisitStatement(brk, p);
        if (stmtResult is not Break node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        Identifier? newLabel = node.Label;
        if (node.Label != null)
        {
            var visited = Visit(node.Label, p);
            if (visited is Identifier id && !ReferenceEquals(id, node.Label))
            {
                newLabel = id;
                changed = true;
            }
        }

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithLabel(newLabel)
            : node;
    }

    // -----------------------------------------------------------------------
    // Continue : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitContinue(Continue cont, P p)
    {
        var stmtResult = VisitStatement(cont, p);
        if (stmtResult is not Continue node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        Identifier? newLabel = node.Label;
        if (node.Label != null)
        {
            var visited = Visit(node.Label, p);
            if (visited is Identifier id && !ReferenceEquals(id, node.Label))
            {
                newLabel = id;
                changed = true;
            }
        }

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithLabel(newLabel)
            : node;
    }

    // -----------------------------------------------------------------------
    // Empty : Statement, Expression
    // -----------------------------------------------------------------------
    public virtual J VisitEmpty(Empty emp, P p)
    {
        var stmtResult = VisitStatement(emp, p);
        if (stmtResult is not Empty node) return stmtResult;

        var exprResult = VisitExpression(node, p);
        if (exprResult is not Empty node2) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node2.Prefix, p);
        if (!ReferenceEquals(newPrefix, node2.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node2.Markers, p);
        if (!ReferenceEquals(newMarkers, node2.Markers)) changed = true;

        return changed
            ? node2.WithPrefix(newPrefix).WithMarkers(newMarkers)
            : node2;
    }

    // -----------------------------------------------------------------------
    // ControlParentheses<Expression> : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitControlParentheses(ControlParentheses<Expression> cp, P p)
    {
        var exprResult = VisitExpression(cp, p);
        if (exprResult is not ControlParentheses<Expression> node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        var newTree = VisitRightPadded(node.Tree, p) ?? node.Tree;
        if (!ReferenceEquals(newTree, node.Tree)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithTree(newTree)
            : node;
    }

    // -----------------------------------------------------------------------
    // ControlParentheses<TypeTree> : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitControlParentheses(ControlParentheses<TypeTree> cp, P p)
    {
        var exprResult = VisitExpression(cp, p);
        if (exprResult is not ControlParentheses<TypeTree> node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        var newTree = VisitRightPadded(node.Tree, p) ?? node.Tree;
        if (!ReferenceEquals(newTree, node.Tree)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithTree(newTree)
            : node;
    }

    // -----------------------------------------------------------------------
    // ControlParentheses<VariableDeclarations> : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitControlParentheses(ControlParentheses<VariableDeclarations> cp, P p)
    {
        var exprResult = VisitExpression(cp, p);
        if (exprResult is not ControlParentheses<VariableDeclarations> node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        var newTree = VisitRightPadded(node.Tree, p) ?? node.Tree;
        if (!ReferenceEquals(newTree, node.Tree)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithTree(newTree)
            : node;
    }

    // -----------------------------------------------------------------------
    // Literal : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitLiteral(Literal literal, P p)
    {
        var exprResult = VisitExpression(literal, p);
        if (exprResult is not Literal node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers)
            : node;
    }

    // -----------------------------------------------------------------------
    // Identifier : Expression, TypeTree
    // -----------------------------------------------------------------------
    public virtual J VisitIdentifier(Identifier identifier, P p)
    {
        var exprResult = VisitExpression(identifier, p);
        if (exprResult is not Identifier node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Annotations
        IList<Annotation> newAnnotations = node.Annotations;
        var annList = new List<Annotation>();
        bool annsChanged = false;
        foreach (var ann in node.Annotations)
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

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type))
        {
            changed = true;
        }

        // FieldType
        JavaType? newFieldType = VisitType(node.FieldType, p);
        if (!ReferenceEquals(newFieldType, node.FieldType))
        {
            changed = true;
        }

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithAnnotations(newAnnotations).WithType(newType).WithFieldType(newFieldType)
            : node;
    }

    // -----------------------------------------------------------------------
    // FieldAccess : Expression, Statement, TypeTree
    // -----------------------------------------------------------------------
    public virtual J VisitFieldAccess(FieldAccess fieldAccess, P p)
    {
        var stmtResult = VisitStatement(fieldAccess, p);
        if (stmtResult is not FieldAccess s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not FieldAccess node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Target
        Expression newTarget = node.Target;
        var visitedTarget = Visit(node.Target, p);
        if (visitedTarget is Expression t && !ReferenceEquals(t, node.Target))
        {
            newTarget = t;
            changed = true;
        }

        // Name (JLeftPadded<Identifier>)
        var newName = VisitLeftPadded(node.Name, p) ?? node.Name;
        if (!ReferenceEquals(newName, node.Name)) changed = true;

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type))
        {
            changed = true;
        }

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithTarget(newTarget).WithName(newName).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // MemberReference : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitMemberReference(MemberReference memberRef, P p)
    {
        var exprResult = VisitExpression(memberRef, p);
        if (exprResult is not MemberReference node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Containing (JRightPadded<Expression>)
        var newContaining = VisitRightPadded(node.Containing, p) ?? node.Containing;
        if (!ReferenceEquals(newContaining, node.Containing)) changed = true;

        // TypeParameters (JContainer<Expression>?)
        var newTypeParams = VisitContainer(node.TypeParameters, p);
        if (!ReferenceEquals(newTypeParams, node.TypeParameters)) changed = true;

        // Reference (JLeftPadded<Identifier>)
        var newReference = VisitLeftPadded(node.Reference, p) ?? node.Reference;
        if (!ReferenceEquals(newReference, node.Reference)) changed = true;

        // Types
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        JavaType.Method? newMethodType = (JavaType.Method?)VisitType(node.MethodType, p);
        if (!ReferenceEquals(newMethodType, node.MethodType)) changed = true;

        JavaType.Variable? newVariableType = (JavaType.Variable?)VisitType(node.VariableType, p);
        if (!ReferenceEquals(newVariableType, node.VariableType)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithContaining(newContaining).WithTypeParameters(newTypeParams).WithReference(newReference)
                  .WithType(newType).WithMethodType(newMethodType).WithVariableType(newVariableType)
            : node;
    }

    // -----------------------------------------------------------------------
    // Binary : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitBinary(Binary binary, P p)
    {
        var exprResult = VisitExpression(binary, p);
        if (exprResult is not Binary node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        Expression newLeft = node.Left;
        var visitedLeft = Visit(node.Left, p);
        if (visitedLeft is Expression vl && !ReferenceEquals(vl, node.Left))
        {
            newLeft = vl;
            changed = true;
        }

        var newOperator = VisitLeftPadded(node.Operator, p);
        if (newOperator != null && !ReferenceEquals(newOperator, node.Operator)) changed = true;

        Expression newRight = node.Right;
        var visitedRight = Visit(node.Right, p);
        if (visitedRight is Expression vr && !ReferenceEquals(vr, node.Right))
        {
            newRight = vr;
            changed = true;
        }

        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithLeft(newLeft).WithOperator(newOperator ?? node.Operator).WithRight(newRight).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // Ternary : Expression, Statement
    // -----------------------------------------------------------------------
    public virtual J VisitTernary(Ternary ternary, P p)
    {
        var exprResult = VisitExpression(ternary, p);
        if (exprResult is not Ternary e1) return exprResult;

        var stmtResult = VisitStatement(e1, p);
        if (stmtResult is not Ternary node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Condition
        Expression newCondition = node.Condition;
        var visitedCondition = Visit(node.Condition, p);
        if (visitedCondition is Expression vc && !ReferenceEquals(vc, node.Condition))
        {
            newCondition = vc;
            changed = true;
        }

        // TruePart (JLeftPadded<Expression>)
        var newTruePart = VisitLeftPadded(node.TruePart, p) ?? node.TruePart;
        if (!ReferenceEquals(newTruePart, node.TruePart)) changed = true;

        // FalsePart (JLeftPadded<Expression>)
        var newFalsePart = VisitLeftPadded(node.FalsePart, p) ?? node.FalsePart;
        if (!ReferenceEquals(newFalsePart, node.FalsePart)) changed = true;

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithCondition(newCondition).WithTruePart(newTruePart).WithFalsePart(newFalsePart).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // Assignment : Statement, Expression
    // -----------------------------------------------------------------------
    public virtual J VisitAssignment(Assignment assignment, P p)
    {
        var stmtResult = VisitStatement(assignment, p);
        if (stmtResult is not Assignment s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not Assignment node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Variable
        Expression newVariable = node.Variable;
        var visitedVariable = Visit(node.Variable, p);
        if (visitedVariable is Expression vv && !ReferenceEquals(vv, node.Variable))
        {
            newVariable = vv;
            changed = true;
        }

        // AssignmentValue (JLeftPadded<Expression>)
        var newAssignmentValue = VisitLeftPadded(node.AssignmentValue, p) ?? node.AssignmentValue;
        if (!ReferenceEquals(newAssignmentValue, node.AssignmentValue)) changed = true;

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithVariable(newVariable).WithAssignmentValue(newAssignmentValue).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // AssignmentOperation : Statement, Expression
    // -----------------------------------------------------------------------
    public virtual J VisitAssignmentOperation(AssignmentOperation assignment, P p)
    {
        var stmtResult = VisitStatement(assignment, p);
        if (stmtResult is not AssignmentOperation s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not AssignmentOperation node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Variable
        Expression newVariable = node.Variable;
        var visitedVariable = Visit(node.Variable, p);
        if (visitedVariable is Expression vv && !ReferenceEquals(vv, node.Variable))
        {
            newVariable = vv;
            changed = true;
        }

        // AssignmentValue
        Expression newAssignmentValue = node.AssignmentValue;
        var visitedValue = Visit(node.AssignmentValue, p);
        if (visitedValue is Expression val && !ReferenceEquals(val, node.AssignmentValue))
        {
            newAssignmentValue = val;
            changed = true;
        }

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithVariable(newVariable).WithAssignmentValue(newAssignmentValue).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // Unary : Statement, Expression
    // -----------------------------------------------------------------------
    public virtual J VisitUnary(Unary unary, P p)
    {
        var stmtResult = VisitStatement(unary, p);
        if (stmtResult is not Unary s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not Unary node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        Expression newExpression = node.Expression;
        var visitedExpr = Visit(node.Expression, p);
        if (visitedExpr is Expression ve && !ReferenceEquals(ve, node.Expression))
        {
            newExpression = ve;
            changed = true;
        }

        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpression(newExpression).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // Parentheses<Expression> : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitParentheses(Parentheses<Expression> parens, P p)
    {
        var exprResult = VisitExpression(parens, p);
        if (exprResult is not Parentheses<Expression> node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        var newTree = VisitRightPadded(node.Tree, p) ?? node.Tree;
        if (!ReferenceEquals(newTree, node.Tree)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithTree(newTree)
            : node;
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
        var expr = Visit(node.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, node.Expression))
        {
            return node.WithExpression(e);
        }

        return node;
    }

    // -----------------------------------------------------------------------
    // VariableDeclarations : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitVariableDeclarations(VariableDeclarations varDecl, P p)
    {
        var stmtResult = VisitStatement(varDecl, p);
        if (stmtResult is not VariableDeclarations node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // LeadingAnnotations
        IList<Annotation> newLeadingAnnotations = node.LeadingAnnotations;
        var annList = new List<Annotation>();
        bool annsChanged = false;
        foreach (var ann in node.LeadingAnnotations)
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
            newLeadingAnnotations = annList;
            changed = true;
        }

        // TypeExpression
        TypeTree? newTypeExpr = node.TypeExpression;
        if (node.TypeExpression != null)
        {
            var visited = Visit(node.TypeExpression, p);
            if (visited is TypeTree tt && !ReferenceEquals(tt, node.TypeExpression))
            {
                newTypeExpr = tt;
                changed = true;
            }
        }

        // Varargs (Space?)
        Space? newVarargs = node.Varargs;
        if (node.Varargs != null)
        {
            newVarargs = VisitSpace(node.Varargs, p);
            if (!ReferenceEquals(newVarargs, node.Varargs)) changed = true;
        }

        // Variables (IList<JRightPadded<NamedVariable>>)
        var newVars = new List<JRightPadded<NamedVariable>>();
        bool varsChanged = false;
        foreach (var paddedVar in node.Variables)
        {
            var visited = VisitRightPadded(paddedVar, p);
            if (visited != null)
            {
                if (!ReferenceEquals(visited, paddedVar)) varsChanged = true;
                newVars.Add(visited);
            }
            else varsChanged = true;
        }
        if (varsChanged) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithLeadingAnnotations(newLeadingAnnotations).WithTypeExpression(newTypeExpr).WithVarargs(newVarargs).WithVariables(varsChanged ? newVars : node.Variables)
            : node;
    }

    // -----------------------------------------------------------------------
    // NamedVariable : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitNamedVariable(NamedVariable namedVariable, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(namedVariable.Prefix, p);
        if (!ReferenceEquals(newPrefix, namedVariable.Prefix)) changed = true;

        var newMarkers = VisitMarkers(namedVariable.Markers, p);
        if (!ReferenceEquals(newMarkers, namedVariable.Markers)) changed = true;

        // Name
        Identifier newName = namedVariable.Name;
        var visitedName = Visit(namedVariable.Name, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, namedVariable.Name))
        {
            newName = id;
            changed = true;
        }

        // Initializer (JLeftPadded<Expression>?)
        var newInitializer = VisitLeftPadded(namedVariable.Initializer, p);
        if (!ReferenceEquals(newInitializer, namedVariable.Initializer)) changed = true;

        // Type
        JavaType? newType = VisitType(namedVariable.Type, p);
        if (!ReferenceEquals(newType, namedVariable.Type)) changed = true;

        return changed
            ? namedVariable.WithPrefix(newPrefix).WithMarkers(newMarkers).WithName(newName).WithInitializer(newInitializer).WithType(newType)
            : namedVariable;
    }

    // -----------------------------------------------------------------------
    // Primitive : Expression, TypeTree
    // -----------------------------------------------------------------------
    public virtual J VisitPrimitive(Primitive primitive, P p)
    {
        var exprResult = VisitExpression(primitive, p);
        if (exprResult is not Primitive node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers)
            : node;
    }

    // -----------------------------------------------------------------------
    // MethodInvocation : Statement, Expression
    // -----------------------------------------------------------------------
    public virtual J VisitMethodInvocation(MethodInvocation mi, P p)
    {
        var stmtResult = VisitStatement(mi, p);
        if (stmtResult is not MethodInvocation s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not MethodInvocation node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Select (JRightPadded<Expression>?)
        var newSelect = VisitRightPadded(node.Select, p);
        if (!ReferenceEquals(newSelect, node.Select)) changed = true;

        // TypeParameters (JContainer<Expression>?)
        var newTypeParams = VisitContainer(node.TypeParameters, p);
        if (!ReferenceEquals(newTypeParams, node.TypeParameters)) changed = true;

        // Name
        Identifier newName = node.Name;
        var visitedName = Visit(node.Name, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, node.Name))
        {
            newName = id;
            changed = true;
        }

        // Arguments (JContainer<Expression>)
        var newArgs = VisitContainer(node.Arguments, p)!;
        if (!ReferenceEquals(newArgs, node.Arguments)) changed = true;

        // MethodType
        JavaType.Method? newMethodType = (JavaType.Method?)VisitType(node.MethodType, p);
        if (!ReferenceEquals(newMethodType, node.MethodType)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithSelect(newSelect).WithTypeParameters(newTypeParams).WithName(newName).WithArguments(newArgs).WithMethodType(newMethodType)
            : node;
    }

    // -----------------------------------------------------------------------
    // NewClass : Expression, Statement
    // -----------------------------------------------------------------------
    public virtual J VisitNewClass(NewClass nc, P p)
    {
        var stmtResult = VisitStatement(nc, p);
        if (stmtResult is not NewClass s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not NewClass node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // New (Space)
        var newNew = VisitSpace(node.New, p);
        if (!ReferenceEquals(newNew, node.New)) changed = true;

        // Enclosing (JRightPadded<Expression>?)
        var newEnclosing = VisitRightPadded(node.Enclosing, p);
        if (!ReferenceEquals(newEnclosing, node.Enclosing)) changed = true;

        // Clazz (J?)
        J? newClazz = node.Clazz;
        if (node.Clazz != null)
        {
            var visited = Visit(node.Clazz, p);
            if (visited is J j && !ReferenceEquals(j, node.Clazz))
            {
                newClazz = j;
                changed = true;
            }
        }

        // Arguments (JContainer<Expression>)
        var newArgs = VisitContainer(node.Arguments, p)!;
        if (!ReferenceEquals(newArgs, node.Arguments)) changed = true;

        // Body (Block?)
        Block? newBody = node.Body;
        if (node.Body != null)
        {
            var visited = Visit(node.Body, p);
            if (visited is Block b && !ReferenceEquals(b, node.Body))
            {
                newBody = b;
                changed = true;
            }
        }

        // ConstructorType
        JavaType.Method? newConstructorType = (JavaType.Method?)VisitType(node.ConstructorType, p);
        if (!ReferenceEquals(newConstructorType, node.ConstructorType)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithNew(newNew).WithEnclosing(newEnclosing).WithClazz(newClazz).WithArguments(newArgs).WithBody(newBody).WithConstructorType(newConstructorType)
            : node;
    }

    // -----------------------------------------------------------------------
    // NewArray : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitNewArray(NewArray na, P p)
    {
        var exprResult = VisitExpression(na, p);
        if (exprResult is not NewArray node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // TypeExpression
        TypeTree? newTypeExpr = node.TypeExpression;
        if (node.TypeExpression != null)
        {
            var visited = Visit(node.TypeExpression, p);
            if (visited is TypeTree tt && !ReferenceEquals(tt, node.TypeExpression))
            {
                newTypeExpr = tt;
                changed = true;
            }
        }

        // Dimensions (IList<ArrayDimension>)
        var newDims = new List<ArrayDimension>();
        bool dimsChanged = false;
        foreach (var dim in node.Dimensions)
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

        // Initializer (JContainer<Expression>?)
        var newInitializer = VisitContainer(node.Initializer, p);
        if (!ReferenceEquals(newInitializer, node.Initializer)) changed = true;

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithTypeExpression(newTypeExpr).WithDimensions(dimsChanged ? newDims : node.Dimensions).WithInitializer(newInitializer).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // InstanceOf : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitInstanceOf(InstanceOf instanceOf, P p)
    {
        var exprResult = VisitExpression(instanceOf, p);
        if (exprResult is not InstanceOf node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Expression (JRightPadded<Expression>)
        var newExpr = VisitRightPadded(node.Expression, p) ?? node.Expression;
        if (!ReferenceEquals(newExpr, node.Expression)) changed = true;

        // Clazz (J)
        J newClazz = node.Clazz;
        var visitedClazz = Visit(node.Clazz, p);
        if (visitedClazz is J vc && !ReferenceEquals(vc, node.Clazz))
        {
            newClazz = vc;
            changed = true;
        }

        // Pattern (J?)
        J? newPattern = node.Pattern;
        if (node.Pattern != null)
        {
            var visitedPattern = Visit(node.Pattern, p);
            if (visitedPattern is J vp && !ReferenceEquals(vp, node.Pattern))
            {
                newPattern = vp;
                changed = true;
            }
        }

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpression(newExpr).WithClazz(newClazz).WithPattern(newPattern).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // NullableType : Expression, TypeTree
    // -----------------------------------------------------------------------
    public virtual J VisitNullableType(NullableType nullableType, P p)
    {
        var exprResult = VisitExpression(nullableType, p);
        if (exprResult is not NullableType node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Annotations
        IList<Annotation> newAnnotations = node.Annotations;
        var annList = new List<Annotation>();
        bool annsChanged = false;
        foreach (var ann in node.Annotations)
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

        // TypeTreePadded (JRightPadded<TypeTree>)
        var newTypeTreePadded = VisitRightPadded(node.TypeTreePadded, p) ?? node.TypeTreePadded;
        if (!ReferenceEquals(newTypeTreePadded, node.TypeTreePadded)) changed = true;

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithAnnotations(newAnnotations).WithTypeTreePadded(newTypeTreePadded).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // ParameterizedType : Expression, TypeTree
    // -----------------------------------------------------------------------
    public virtual J VisitParameterizedType(ParameterizedType pt, P p)
    {
        var exprResult = VisitExpression(pt, p);
        if (exprResult is not ParameterizedType node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Clazz
        NameTree newClazz = node.Clazz;
        var visitedClazz = Visit(node.Clazz, p);
        if (visitedClazz is NameTree nt && !ReferenceEquals(nt, node.Clazz))
        {
            newClazz = nt;
            changed = true;
        }

        // TypeParameters (JContainer<Expression>?)
        var newTypeParams = VisitContainer(node.TypeParameters, p);
        if (!ReferenceEquals(newTypeParams, node.TypeParameters)) changed = true;

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithClazz(newClazz).WithTypeParameters(newTypeParams).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // ArrayType : Expression, TypeTree
    // -----------------------------------------------------------------------
    public virtual J VisitArrayType(ArrayType at, P p)
    {
        var exprResult = VisitExpression(at, p);
        if (exprResult is not ArrayType node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // ElementType
        TypeTree newElementType = node.ElementType;
        var visitedElem = Visit(node.ElementType, p);
        if (visitedElem is TypeTree tt && !ReferenceEquals(tt, node.ElementType))
        {
            newElementType = tt;
            changed = true;
        }

        // Annotations
        IList<Annotation>? newAnnotations = node.Annotations;
        if (node.Annotations != null)
        {
            var annList = new List<Annotation>();
            bool annsChanged = false;
            foreach (var ann in node.Annotations)
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

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithElementType(newElementType).WithAnnotations(newAnnotations).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // ArrayAccess : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitArrayAccess(ArrayAccess arrayAccess, P p)
    {
        var exprResult = VisitExpression(arrayAccess, p);
        if (exprResult is not ArrayAccess node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Indexed
        Expression newIndexed = node.Indexed;
        var visitedIndexed = Visit(node.Indexed, p);
        if (visitedIndexed is Expression vi && !ReferenceEquals(vi, node.Indexed))
        {
            newIndexed = vi;
            changed = true;
        }

        // Dimension
        ArrayDimension newDimension = node.Dimension;
        var visitedDim = Visit(node.Dimension, p);
        if (visitedDim is ArrayDimension vd && !ReferenceEquals(vd, node.Dimension))
        {
            newDimension = vd;
            changed = true;
        }

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithIndexed(newIndexed).WithDimension(newDimension).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // ArrayDimension : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitArrayDimension(ArrayDimension dimension, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(dimension.Prefix, p);
        if (!ReferenceEquals(newPrefix, dimension.Prefix)) changed = true;

        var newMarkers = VisitMarkers(dimension.Markers, p);
        if (!ReferenceEquals(newMarkers, dimension.Markers)) changed = true;

        var newIndex = VisitRightPadded(dimension.Index, p) ?? dimension.Index;
        if (!ReferenceEquals(newIndex, dimension.Index)) changed = true;

        return changed
            ? dimension.WithPrefix(newPrefix).WithMarkers(newMarkers).WithIndex(newIndex)
            : dimension;
    }

    // -----------------------------------------------------------------------
    // Lambda : Expression, Statement
    // -----------------------------------------------------------------------
    public virtual J VisitLambda(Lambda lambda, P p)
    {
        var exprResult = VisitExpression(lambda, p);
        if (exprResult is not Lambda e1) return exprResult;

        var stmtResult = VisitStatement(e1, p);
        if (stmtResult is not Lambda node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Params (Lambda.Parameters)
        Lambda.Parameters newParams = node.Params;
        var visitedParams = Visit(node.Params, p);
        if (visitedParams is Lambda.Parameters vp && !ReferenceEquals(vp, node.Params))
        {
            newParams = vp;
            changed = true;
        }

        // Arrow (Space)
        var newArrow = VisitSpace(node.Arrow, p);
        if (!ReferenceEquals(newArrow, node.Arrow)) changed = true;

        // Body (J)
        J newBody = node.Body;
        var visitedBody = Visit(node.Body, p);
        if (visitedBody is J vb && !ReferenceEquals(vb, node.Body))
        {
            newBody = vb;
            changed = true;
        }

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithParams(newParams).WithArrow(newArrow).WithBody(newBody).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // Lambda.Parameters : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitLambdaParameters(Lambda.Parameters parameters, P p)
    {
        bool changed = false;

        var newPrefix = VisitSpace(parameters.Prefix, p);
        if (!ReferenceEquals(newPrefix, parameters.Prefix)) changed = true;

        var newMarkers = VisitMarkers(parameters.Markers, p);
        if (!ReferenceEquals(newMarkers, parameters.Markers)) changed = true;

        var elements = new List<JRightPadded<J>>();

        foreach (var paddedParam in parameters.Elements)
        {
            var visited = VisitRightPadded(paddedParam, p);
            if (visited != null)
            {
                if (!ReferenceEquals(visited, paddedParam)) changed = true;
                elements.Add(visited);
            }
            else changed = true;
        }

        return changed ? parameters.WithPrefix(newPrefix).WithMarkers(newMarkers).WithElements(elements) : parameters;
    }

    // -----------------------------------------------------------------------
    // Switch : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitSwitch(Switch @switch, P p)
    {
        var stmtResult = VisitStatement(@switch, p);
        if (stmtResult is not Switch node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Selector (ControlParentheses<Expression>)
        ControlParentheses<Expression> newSelector = node.Selector;
        var visitedSelector = Visit(node.Selector, p);
        if (visitedSelector is ControlParentheses<Expression> vs && !ReferenceEquals(vs, node.Selector))
        {
            newSelector = vs;
            changed = true;
        }

        // Cases (Block)
        Block newCases = node.Cases;
        var visitedCases = Visit(node.Cases, p);
        if (visitedCases is Block vc && !ReferenceEquals(vc, node.Cases))
        {
            newCases = vc;
            changed = true;
        }

        return changed ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithSelector(newSelector).WithCases(newCases) : node;
    }

    // -----------------------------------------------------------------------
    // SwitchExpression : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitSwitchExpression(SwitchExpression se, P p)
    {
        var exprResult = VisitExpression(se, p);
        if (exprResult is not SwitchExpression node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Selector (ControlParentheses<Expression>)
        ControlParentheses<Expression> newSelector = node.Selector;
        var visitedSelector = Visit(node.Selector, p);
        if (visitedSelector is ControlParentheses<Expression> vs && !ReferenceEquals(vs, node.Selector))
        {
            newSelector = vs;
            changed = true;
        }

        // Cases (Block)
        Block newCases = node.Cases;
        var visitedCases = Visit(node.Cases, p);
        if (visitedCases is Block vc && !ReferenceEquals(vc, node.Cases))
        {
            newCases = vc;
            changed = true;
        }

        return changed ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithSelector(newSelector).WithCases(newCases) : node;
    }

    // -----------------------------------------------------------------------
    // Case : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitCase(Case @case, P p)
    {
        var stmtResult = VisitStatement(@case, p);
        if (stmtResult is not Case node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // CaseLabels (JContainer<J>)
        var newCaseLabels = VisitContainer(node.CaseLabels, p)!;
        if (!ReferenceEquals(newCaseLabels, node.CaseLabels)) changed = true;

        // Guard (Expression?)
        Expression? newGuard = node.Guard;
        if (node.Guard != null)
        {
            var visitedGuard = Visit(node.Guard, p);
            if (visitedGuard is Expression vg && !ReferenceEquals(vg, node.Guard))
            {
                newGuard = vg;
                changed = true;
            }
        }

        // Body (JRightPadded<J>?)
        var newBody = VisitRightPadded(node.Body, p);
        if (!ReferenceEquals(newBody, node.Body)) changed = true;

        // Statements (JContainer<Statement>)
        var newStatements = VisitContainer(node.Statements, p)!;
        if (!ReferenceEquals(newStatements, node.Statements)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithCaseLabels(newCaseLabels).WithGuard(newGuard).WithBody(newBody).WithStatements(newStatements)
            : node;
    }

    // -----------------------------------------------------------------------
    // DeconstructionPattern : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitDeconstructionPattern(DeconstructionPattern dp, P p)
    {
        var exprResult = VisitExpression(dp, p);
        if (exprResult is not DeconstructionPattern node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Deconstructor
        Expression newDeconstructor = node.Deconstructor;
        var visitedDeconstructor = Visit(node.Deconstructor, p);
        if (visitedDeconstructor is Expression vd && !ReferenceEquals(vd, node.Deconstructor))
        {
            newDeconstructor = vd;
            changed = true;
        }

        // Nested (JContainer<J>)
        var newNested = VisitContainer(node.Nested, p)!;
        if (!ReferenceEquals(newNested, node.Nested)) changed = true;

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithDeconstructor(newDeconstructor).WithNested(newNested).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // Label : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitLabel(Label label, P p)
    {
        var stmtResult = VisitStatement(label, p);
        if (stmtResult is not Label node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // LabelName (JRightPadded<Identifier>)
        var newLabelName = VisitRightPadded(node.LabelName, p) ?? node.LabelName;
        if (!ReferenceEquals(newLabelName, node.LabelName)) changed = true;

        // Statement
        Statement newStatement = node.Statement;
        var visitedStmt = Visit(node.Statement, p);
        if (visitedStmt is Statement s && !ReferenceEquals(s, node.Statement))
        {
            newStatement = s;
            changed = true;
        }

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithLabelName(newLabelName).WithStatement(newStatement)
            : node;
    }

    // -----------------------------------------------------------------------
    // Synchronized : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitSynchronized(Synchronized sync, P p)
    {
        var stmtResult = VisitStatement(sync, p);
        if (stmtResult is not Synchronized node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Lock (ControlParentheses<Expression>)
        ControlParentheses<Expression> newLock = node.Lock;
        var visitedLock = Visit(node.Lock, p);
        if (visitedLock is ControlParentheses<Expression> vl && !ReferenceEquals(vl, node.Lock))
        {
            newLock = vl;
            changed = true;
        }

        // Body (Block)
        Block newBody = node.Body;
        var visitedBody = Visit(node.Body, p);
        if (visitedBody is Block vb && !ReferenceEquals(vb, node.Body))
        {
            newBody = vb;
            changed = true;
        }

        return changed ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithLock(newLock).WithBody(newBody) : node;
    }

    // -----------------------------------------------------------------------
    // TypeCast : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitTypeCast(TypeCast cast, P p)
    {
        var exprResult = VisitExpression(cast, p);
        if (exprResult is not TypeCast node) return exprResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Clazz (ControlParentheses<TypeTree>)
        ControlParentheses<TypeTree> newClazz = node.Clazz;
        var visitedClazz = Visit(node.Clazz, p);
        if (visitedClazz is ControlParentheses<TypeTree> vc && !ReferenceEquals(vc, node.Clazz))
        {
            newClazz = vc;
            changed = true;
        }

        // Expression
        Expression newExpr = node.Expression;
        var visitedExpr = Visit(node.Expression, p);
        if (visitedExpr is Expression ve && !ReferenceEquals(ve, node.Expression))
        {
            newExpr = ve;
            changed = true;
        }

        return changed ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithClazz(newClazz).WithExpression(newExpr) : node;
    }

    // -----------------------------------------------------------------------
    // TypeParameter : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitTypeParameter(TypeParameter typeParameter, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(typeParameter.Prefix, p);
        if (!ReferenceEquals(newPrefix, typeParameter.Prefix)) changed = true;

        var newMarkers = VisitMarkers(typeParameter.Markers, p);
        if (!ReferenceEquals(newMarkers, typeParameter.Markers)) changed = true;

        // Annotations
        IList<Annotation> newAnnotations = typeParameter.Annotations;
        var annList = new List<Annotation>();
        bool annsChanged = false;
        foreach (var ann in typeParameter.Annotations)
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

        // Name (Expression)
        Expression newName = typeParameter.Name;
        var visitedName = Visit(typeParameter.Name, p);
        if (visitedName is Expression ne && !ReferenceEquals(ne, typeParameter.Name))
        {
            newName = ne;
            changed = true;
        }

        // Bounds (JContainer<TypeTree>?)
        var newBounds = VisitContainer(typeParameter.Bounds, p);
        if (!ReferenceEquals(newBounds, typeParameter.Bounds)) changed = true;

        return changed
            ? typeParameter.WithPrefix(newPrefix).WithMarkers(newMarkers).WithAnnotations(newAnnotations).WithName(newName).WithBounds(newBounds)
            : typeParameter;
    }

    // -----------------------------------------------------------------------
    // Package : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitPackage(Package pkg, P p)
    {
        var stmtResult = VisitStatement(pkg, p);
        if (stmtResult is not Package node) return stmtResult;

        var changed = false;

        var newPrefix = VisitSpace(node.Prefix, p);
        if (!ReferenceEquals(newPrefix, node.Prefix)) changed = true;

        var newMarkers = VisitMarkers(node.Markers, p);
        if (!ReferenceEquals(newMarkers, node.Markers)) changed = true;

        // Expression
        Expression newExpression = node.Expression;
        var visitedExpr = Visit(node.Expression, p);
        if (visitedExpr is Expression e && !ReferenceEquals(e, node.Expression))
        {
            newExpression = e;
            changed = true;
        }

        // Annotations
        IList<Annotation> newAnnotations = node.Annotations;
        var annList = new List<Annotation>();
        bool annsChanged = false;
        foreach (var ann in node.Annotations)
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

        return changed
            ? node.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpression(newExpression).WithAnnotations(newAnnotations)
            : node;
    }
}
