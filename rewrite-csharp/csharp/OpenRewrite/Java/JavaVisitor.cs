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

        NameTree newAnnotationType = node.AnnotationType;
        var visitedType = Visit(node.AnnotationType, p);
        if (visitedType is NameTree tt && !ReferenceEquals(tt, node.AnnotationType))
        {
            newAnnotationType = tt;
            changed = true;
        }

        JContainer<Expression>? newArguments = node.Arguments;
        if (node.Arguments != null)
        {
            var newArgs = new List<JRightPadded<Expression>>();
            bool argsChanged = false;
            foreach (var paddedArg in node.Arguments.Elements)
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
                newArguments = node.Arguments.WithElements(newArgs);
                changed = true;
            }
        }

        return changed
            ? node.WithAnnotationType(newAnnotationType).WithArguments(newArguments)
            : node;
    }

    // -----------------------------------------------------------------------
    // Block : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitBlock(Block block, P p)
    {
        var stmtResult = VisitStatement(block, p);
        if (stmtResult is not Block node) return stmtResult;

        var statements = new List<JRightPadded<Statement>>();
        bool changed = false;
        foreach (var stmt in node.Statements)
        {
            var visited = Visit(stmt.Element, p);
            if (visited is Statement s)
            {
                if (!ReferenceEquals(s, stmt.Element)) changed = true;
                statements.Add(stmt.WithElement(s));
            }
            else
            {
                // Statement was removed (visitor returned null)
                changed = true;
            }
        }
        return changed ? node.WithStatements(statements) : node;
    }

    // -----------------------------------------------------------------------
    // ClassDeclaration : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitClassDeclaration(ClassDeclaration classDecl, P p)
    {
        var stmtResult = VisitStatement(classDecl, p);
        if (stmtResult is not ClassDeclaration node) return stmtResult;

        var changed = false;

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
        JContainer<TypeParameter>? newTypeParameters = node.TypeParameters;
        if (node.TypeParameters != null)
        {
            var tpElements = new List<JRightPadded<TypeParameter>>();
            bool tpChanged = false;
            foreach (var paddedTp in node.TypeParameters.Elements)
            {
                var visited = Visit(paddedTp.Element, p);
                if (visited is TypeParameter tp)
                {
                    if (!ReferenceEquals(tp, paddedTp.Element)) tpChanged = true;
                    tpElements.Add(paddedTp.WithElement(tp));
                }
                else
                {
                    tpElements.Add(paddedTp);
                }
            }
            if (tpChanged)
            {
                newTypeParameters = node.TypeParameters.WithElements(tpElements);
                changed = true;
            }
        }

        // PrimaryConstructor
        JContainer<TypeTree>? newPrimaryConstructor = node.PrimaryConstructor;
        if (node.PrimaryConstructor != null)
        {
            var pcElements = new List<JRightPadded<TypeTree>>();
            bool pcChanged = false;
            foreach (var paddedPc in node.PrimaryConstructor.Elements)
            {
                var visited = Visit(paddedPc.Element, p);
                if (visited is TypeTree tt)
                {
                    if (!ReferenceEquals(tt, paddedPc.Element)) pcChanged = true;
                    pcElements.Add(paddedPc.WithElement(tt));
                }
                else
                {
                    pcElements.Add(paddedPc);
                }
            }
            if (pcChanged)
            {
                newPrimaryConstructor = node.PrimaryConstructor.WithElements(pcElements);
                changed = true;
            }
        }

        // Extends
        JLeftPadded<TypeTree>? newExtends = node.Extends;
        if (node.Extends != null)
        {
            var visitedExtends = Visit(node.Extends.Element, p);
            if (visitedExtends is TypeTree et && !ReferenceEquals(et, node.Extends.Element))
            {
                newExtends = node.Extends.WithElement(et);
                changed = true;
            }
        }

        // Implements
        JContainer<TypeTree>? newImplements = node.Implements;
        if (node.Implements != null)
        {
            var implElements = new List<JRightPadded<TypeTree>>();
            bool implChanged = false;
            foreach (var paddedImpl in node.Implements.Elements)
            {
                var visited = Visit(paddedImpl.Element, p);
                if (visited is TypeTree tt)
                {
                    if (!ReferenceEquals(tt, paddedImpl.Element)) implChanged = true;
                    implElements.Add(paddedImpl.WithElement(tt));
                }
                else
                {
                    implElements.Add(paddedImpl);
                }
            }
            if (implChanged)
            {
                newImplements = node.Implements.WithElements(implElements);
                changed = true;
            }
        }

        // Permits
        JContainer<TypeTree>? newPermits = node.Permits;
        if (node.Permits != null)
        {
            var permElements = new List<JRightPadded<TypeTree>>();
            bool permChanged = false;
            foreach (var paddedPerm in node.Permits.Elements)
            {
                var visited = Visit(paddedPerm.Element, p);
                if (visited is TypeTree tt)
                {
                    if (!ReferenceEquals(tt, paddedPerm.Element)) permChanged = true;
                    permElements.Add(paddedPerm.WithElement(tt));
                }
                else
                {
                    permElements.Add(paddedPerm);
                }
            }
            if (permChanged)
            {
                newPermits = node.Permits.WithElements(permElements);
                changed = true;
            }
        }

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
        var newEnums = new List<JRightPadded<EnumValue>>();
        bool enumsChanged = false;
        foreach (var paddedValue in node.Enums)
        {
            var visited = Visit(paddedValue.Element, p);
            if (visited is EnumValue ev)
            {
                if (!ReferenceEquals(ev, paddedValue.Element)) enumsChanged = true;
                newEnums.Add(paddedValue.WithElement(ev));
            }
            else
            {
                newEnums.Add(paddedValue);
            }
        }
        if (enumsChanged) changed = true;

        return changed ? node.WithEnums(enumsChanged ? newEnums : node.Enums) : node;
    }

    // -----------------------------------------------------------------------
    // EnumValue : (neither Statement nor Expression)
    // -----------------------------------------------------------------------
    public virtual J VisitEnumValue(EnumValue enumValue, P p)
    {
        var changed = false;

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

        // Initializer (JLeftPadded<Expression>? — NewClass)
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
            ? enumValue.WithAnnotations(newAnnotations).WithName(newName).WithInitializer(newInitializer)
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
        JContainer<TypeParameter>? newTypeParameters = node.TypeParameters;
        if (node.TypeParameters != null)
        {
            var tpElements = new List<JRightPadded<TypeParameter>>();
            bool tpChanged = false;
            foreach (var paddedTp in node.TypeParameters.Elements)
            {
                var visited = Visit(paddedTp.Element, p);
                if (visited is TypeParameter tp)
                {
                    if (!ReferenceEquals(tp, paddedTp.Element)) tpChanged = true;
                    tpElements.Add(paddedTp.WithElement(tp));
                }
                else
                {
                    tpElements.Add(paddedTp);
                }
            }
            if (tpChanged)
            {
                newTypeParameters = node.TypeParameters.WithElements(tpElements);
                changed = true;
            }
        }

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
        JContainer<Statement> newParams = node.Parameters;
        var paramElements = new List<JRightPadded<Statement>>();
        bool paramsChanged = false;
        foreach (var paddedParam in node.Parameters.Elements)
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
            newParams = node.Parameters.WithElements(paramElements);
            changed = true;
        }

        // Throws
        JContainer<NameTree>? newThrows = node.Throws;
        if (node.Throws != null)
        {
            var throwElements = new List<JRightPadded<NameTree>>();
            bool throwsChanged = false;
            foreach (var paddedThrow in node.Throws.Elements)
            {
                var visited = Visit(paddedThrow.Element, p);
                if (visited is NameTree nt)
                {
                    if (!ReferenceEquals(nt, paddedThrow.Element)) throwsChanged = true;
                    throwElements.Add(paddedThrow.WithElement(nt));
                }
                else
                {
                    throwElements.Add(paddedThrow);
                }
            }
            if (throwsChanged)
            {
                newThrows = node.Throws.WithElements(throwElements);
                changed = true;
            }
        }

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
        JLeftPadded<Expression>? newDefaultValue = node.DefaultValue;
        if (node.DefaultValue != null)
        {
            var visitedDefault = Visit(node.DefaultValue.Element, p);
            if (visitedDefault is Expression e && !ReferenceEquals(e, node.DefaultValue.Element))
            {
                newDefaultValue = node.DefaultValue.WithElement(e);
                changed = true;
            }
        }

        // MethodType
        JavaType.Method? newMethodType = (JavaType.Method?)VisitType(node.MethodType, p);
        if (!ReferenceEquals(newMethodType, node.MethodType))
        {
            changed = true;
        }

        return changed
            ? node
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

        if (node.Expression != null)
        {
            var visited = Visit(node.Expression, p);
            if (visited is Expression expr && !ReferenceEquals(expr, node.Expression))
            {
                return node.WithExpression(expr);
            }
        }
        return node;
    }

    // -----------------------------------------------------------------------
    // If : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitIf(If iff, P p)
    {
        var stmtResult = VisitStatement(iff, p);
        if (stmtResult is not If node) return stmtResult;

        var changed = false;

        // Condition (ControlParentheses<Expression>)
        ControlParentheses<Expression> newCondition = node.Condition;
        var visitedCondition = Visit(node.Condition, p);
        if (visitedCondition is ControlParentheses<Expression> vc && !ReferenceEquals(vc, node.Condition))
        {
            newCondition = vc;
            changed = true;
        }

        // ThenPart (JRightPadded<Statement>)
        JRightPadded<Statement> newThenPart = node.ThenPart;
        var visitedThen = Visit(node.ThenPart.Element, p);
        if (visitedThen is Statement ts && !ReferenceEquals(ts, node.ThenPart.Element))
        {
            newThenPart = node.ThenPart.WithElement(ts);
            changed = true;
        }

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
            ? node.WithCondition(newCondition).WithThenPart(newThenPart).WithElsePart(newElsePart)
            : node;
    }

    // -----------------------------------------------------------------------
    // If.Else : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitElse(If.Else @else, P p)
    {
        var changed = false;

        JRightPadded<Statement> newBody = @else.Body;
        var visitedBody = Visit(@else.Body.Element, p);
        if (visitedBody is Statement bs && !ReferenceEquals(bs, @else.Body.Element))
        {
            newBody = @else.Body.WithElement(bs);
            changed = true;
        }

        return changed ? @else.WithBody(newBody) : @else;
    }

    // -----------------------------------------------------------------------
    // WhileLoop : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitWhileLoop(WhileLoop whl, P p)
    {
        var stmtResult = VisitStatement(whl, p);
        if (stmtResult is not WhileLoop node) return stmtResult;

        var changed = false;

        ControlParentheses<Expression> newCondition = node.Condition;
        var visitedCondition = Visit(node.Condition, p);
        if (visitedCondition is ControlParentheses<Expression> vc && !ReferenceEquals(vc, node.Condition))
        {
            newCondition = vc;
            changed = true;
        }

        JRightPadded<Statement> newBody = node.Body;
        var visitedBody = Visit(node.Body.Element, p);
        if (visitedBody is Statement bs && !ReferenceEquals(bs, node.Body.Element))
        {
            newBody = node.Body.WithElement(bs);
            changed = true;
        }

        return changed ? node.WithCondition(newCondition).WithBody(newBody) : node;
    }

    // -----------------------------------------------------------------------
    // DoWhileLoop : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitDoWhileLoop(DoWhileLoop dwl, P p)
    {
        var stmtResult = VisitStatement(dwl, p);
        if (stmtResult is not DoWhileLoop node) return stmtResult;

        var changed = false;

        // Body
        JRightPadded<Statement> newBody = node.Body;
        var visitedBody = Visit(node.Body.Element, p);
        if (visitedBody is Statement bs && !ReferenceEquals(bs, node.Body.Element))
        {
            newBody = node.Body.WithElement(bs);
            changed = true;
        }

        // Condition (JLeftPadded<ControlParentheses<Expression>>)
        JLeftPadded<ControlParentheses<Expression>> newCondition = node.Condition;
        var visitedCondition = Visit(node.Condition.Element, p);
        if (visitedCondition is ControlParentheses<Expression> vc && !ReferenceEquals(vc, node.Condition.Element))
        {
            newCondition = node.Condition.WithElement(vc);
            changed = true;
        }

        return changed ? node.WithBody(newBody).WithCondition(newCondition) : node;
    }

    // -----------------------------------------------------------------------
    // ForLoop : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitForLoop(ForLoop fl, P p)
    {
        var stmtResult = VisitStatement(fl, p);
        if (stmtResult is not ForLoop node) return stmtResult;

        var changed = false;

        // Control
        ForLoop.Control newControl = node.LoopControl;
        var visitedControl = Visit(node.LoopControl, p);
        if (visitedControl is ForLoop.Control vc && !ReferenceEquals(vc, node.LoopControl))
        {
            newControl = vc;
            changed = true;
        }

        // Body
        JRightPadded<Statement> newBody = node.Body;
        var visitedBody = Visit(node.Body.Element, p);
        if (visitedBody is Statement bs && !ReferenceEquals(bs, node.Body.Element))
        {
            newBody = node.Body.WithElement(bs);
            changed = true;
        }

        return changed ? node.WithLoopControl(newControl).WithBody(newBody) : node;
    }

    // -----------------------------------------------------------------------
    // ForLoop.Control : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitForControl(ForLoop.Control control, P p)
    {
        var changed = false;

        // Init
        var newInit = new List<JRightPadded<Statement>>();
        bool initChanged = false;
        foreach (var init in control.Init)
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

        // Condition
        JRightPadded<Expression> newControlCondition = control.Condition;
        var visitedCondition = Visit(control.Condition.Element, p);
        if (visitedCondition is Expression ce && !ReferenceEquals(ce, control.Condition.Element))
        {
            newControlCondition = control.Condition.WithElement(ce);
            changed = true;
        }

        // Update
        var newUpdate = new List<JRightPadded<Statement>>();
        bool updateChanged = false;
        foreach (var update in control.Update)
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

        return changed
            ? control
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

        // Control
        ForEachLoop.Control newControl = node.LoopControl;
        var visitedControl = Visit(node.LoopControl, p);
        if (visitedControl is ForEachLoop.Control vc && !ReferenceEquals(vc, node.LoopControl))
        {
            newControl = vc;
            changed = true;
        }

        // Body
        JRightPadded<Statement> newBody = node.Body;
        var visitedBody = Visit(node.Body.Element, p);
        if (visitedBody is Statement bs && !ReferenceEquals(bs, node.Body.Element))
        {
            newBody = node.Body.WithElement(bs);
            changed = true;
        }

        return changed ? node.WithLoopControl(newControl).WithBody(newBody) : node;
    }

    // -----------------------------------------------------------------------
    // ForEachLoop.Control : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitForEachControl(ForEachLoop.Control control, P p)
    {
        var changed = false;

        // Variable
        JRightPadded<VariableDeclarations> newVariable = control.Variable;
        var visitedVar = Visit(control.Variable.Element, p);
        if (visitedVar is VariableDeclarations vd && !ReferenceEquals(vd, control.Variable.Element))
        {
            newVariable = control.Variable.WithElement(vd);
            changed = true;
        }

        // Iterable
        JRightPadded<Expression> newIterable = control.Iterable;
        var visitedIterable = Visit(control.Iterable.Element, p);
        if (visitedIterable is Expression ie && !ReferenceEquals(ie, control.Iterable.Element))
        {
            newIterable = control.Iterable.WithElement(ie);
            changed = true;
        }

        return changed ? control.WithVariable(newVariable).WithIterable(newIterable) : control;
    }

    // -----------------------------------------------------------------------
    // Try : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitTry(Try tr, P p)
    {
        var stmtResult = VisitStatement(tr, p);
        if (stmtResult is not Try node) return stmtResult;

        var changed = false;

        // Resources
        JContainer<NameTree>? newResources = node.Resources;
        if (node.Resources != null)
        {
            var resElements = new List<JRightPadded<NameTree>>();
            bool resChanged = false;
            foreach (var paddedRes in node.Resources.Elements)
            {
                var visited = Visit(paddedRes.Element, p);
                if (visited is NameTree nt)
                {
                    if (!ReferenceEquals(nt, paddedRes.Element)) resChanged = true;
                    resElements.Add(paddedRes.WithElement(nt));
                }
                else
                {
                    resElements.Add(paddedRes);
                }
            }
            if (resChanged)
            {
                newResources = node.Resources.WithElements(resElements);
                changed = true;
            }
        }

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
        JLeftPadded<Block>? newFinally = node.Finally;
        if (node.Finally != null)
        {
            var visitedFinally = Visit(node.Finally.Element, p);
            if (visitedFinally is Block fb && !ReferenceEquals(fb, node.Finally.Element))
            {
                newFinally = node.Finally.WithElement(fb);
                changed = true;
            }
        }

        return changed
            ? node.WithResources(newResources).WithBody(newBody).WithCatches(catchesChanged ? newCatches : node.Catches).WithFinally(newFinally)
            : node;
    }

    // -----------------------------------------------------------------------
    // Try.Catch : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitCatch(Try.Catch cat, P p)
    {
        var changed = false;

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

        return changed ? cat.WithParameter(newParam).WithBody(newBody) : cat;
    }

    // -----------------------------------------------------------------------
    // Throw : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitThrow(Throw thr, P p)
    {
        var stmtResult = VisitStatement(thr, p);
        if (stmtResult is not Throw node) return stmtResult;

        var visited = Visit(node.Exception, p);
        if (visited is Expression e && !ReferenceEquals(e, node.Exception))
        {
            return node.WithException(e);
        }
        return node;
    }

    // -----------------------------------------------------------------------
    // Break : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitBreak(Break brk, P p)
    {
        var stmtResult = VisitStatement(brk, p);
        if (stmtResult is not Break node) return stmtResult;

        if (node.Label != null)
        {
            var visited = Visit(node.Label, p);
            if (visited is Identifier id && !ReferenceEquals(id, node.Label))
            {
                return node.WithLabel(id);
            }
        }
        return node;
    }

    // -----------------------------------------------------------------------
    // Continue : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitContinue(Continue cont, P p)
    {
        var stmtResult = VisitStatement(cont, p);
        if (stmtResult is not Continue node) return stmtResult;

        if (node.Label != null)
        {
            var visited = Visit(node.Label, p);
            if (visited is Identifier id && !ReferenceEquals(id, node.Label))
            {
                return node.WithLabel(id);
            }
        }
        return node;
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

        return node2;
    }

    // -----------------------------------------------------------------------
    // ControlParentheses<Expression> : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitControlParentheses(ControlParentheses<Expression> cp, P p)
    {
        var exprResult = VisitExpression(cp, p);
        if (exprResult is not ControlParentheses<Expression> node) return exprResult;

        var visited = Visit(node.Tree.Element, p);
        if (visited is Expression e && !ReferenceEquals(e, node.Tree.Element))
        {
            return node.WithTree(node.Tree.WithElement(e));
        }
        return node;
    }

    // -----------------------------------------------------------------------
    // ControlParentheses<TypeTree> : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitControlParentheses(ControlParentheses<TypeTree> cp, P p)
    {
        var exprResult = VisitExpression(cp, p);
        if (exprResult is not ControlParentheses<TypeTree> node) return exprResult;

        var visited = Visit(node.Tree.Element, p);
        if (visited is TypeTree tt && !ReferenceEquals(tt, node.Tree.Element))
        {
            return node.WithTree(node.Tree.WithElement(tt));
        }
        return node;
    }

    // -----------------------------------------------------------------------
    // ControlParentheses<VariableDeclarations> : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitControlParentheses(ControlParentheses<VariableDeclarations> cp, P p)
    {
        var exprResult = VisitExpression(cp, p);
        if (exprResult is not ControlParentheses<VariableDeclarations> node) return exprResult;

        var visited = Visit(node.Tree.Element, p);
        if (visited is VariableDeclarations vd && !ReferenceEquals(vd, node.Tree.Element))
        {
            return node.WithTree(node.Tree.WithElement(vd));
        }
        return node;
    }

    // -----------------------------------------------------------------------
    // Literal : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitLiteral(Literal literal, P p)
    {
        var exprResult = VisitExpression(literal, p);
        if (exprResult is not Literal node) return exprResult;

        return node;
    }

    // -----------------------------------------------------------------------
    // Identifier : Expression, TypeTree
    // -----------------------------------------------------------------------
    public virtual J VisitIdentifier(Identifier identifier, P p)
    {
        var exprResult = VisitExpression(identifier, p);
        if (exprResult is not Identifier node) return exprResult;

        var changed = false;

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
            ? node.WithAnnotations(newAnnotations).WithType(newType).WithFieldType(newFieldType)
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

        // Target
        Expression newTarget = node.Target;
        var visitedTarget = Visit(node.Target, p);
        if (visitedTarget is Expression t && !ReferenceEquals(t, node.Target))
        {
            newTarget = t;
            changed = true;
        }

        // Name (JLeftPadded<Identifier>)
        JLeftPadded<Identifier> newName = node.Name;
        var visitedName = Visit(node.Name.Element, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, node.Name.Element))
        {
            newName = node.Name.WithElement(id);
            changed = true;
        }

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type))
        {
            changed = true;
        }

        return changed
            ? node.WithTarget(newTarget).WithName(newName).WithType(newType)
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

        // Containing (JRightPadded<Expression>)
        JRightPadded<Expression> newContaining = node.Containing;
        var visitedContaining = Visit(node.Containing.Element, p);
        if (visitedContaining is Expression ce && !ReferenceEquals(ce, node.Containing.Element))
        {
            newContaining = node.Containing.WithElement(ce);
            changed = true;
        }

        // TypeParameters (JContainer<Expression>?)
        JContainer<Expression>? newTypeParams = node.TypeParameters;
        if (node.TypeParameters != null)
        {
            var tpElements = new List<JRightPadded<Expression>>();
            bool tpChanged = false;
            foreach (var paddedTp in node.TypeParameters.Elements)
            {
                var visited = Visit(paddedTp.Element, p);
                if (visited is Expression e)
                {
                    if (!ReferenceEquals(e, paddedTp.Element)) tpChanged = true;
                    tpElements.Add(paddedTp.WithElement(e));
                }
                else
                {
                    tpElements.Add(paddedTp);
                }
            }
            if (tpChanged)
            {
                newTypeParams = node.TypeParameters.WithElements(tpElements);
                changed = true;
            }
        }

        // Reference (JLeftPadded<Identifier>)
        JLeftPadded<Identifier> newReference = node.Reference;
        var visitedRef = Visit(node.Reference.Element, p);
        if (visitedRef is Identifier id && !ReferenceEquals(id, node.Reference.Element))
        {
            newReference = node.Reference.WithElement(id);
            changed = true;
        }

        // Types
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        JavaType.Method? newMethodType = (JavaType.Method?)VisitType(node.MethodType, p);
        if (!ReferenceEquals(newMethodType, node.MethodType)) changed = true;

        JavaType.Variable? newVariableType = (JavaType.Variable?)VisitType(node.VariableType, p);
        if (!ReferenceEquals(newVariableType, node.VariableType)) changed = true;

        return changed
            ? node.WithContaining(newContaining).WithTypeParameters(newTypeParams).WithReference(newReference)
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

        Expression newLeft = node.Left;
        var visitedLeft = Visit(node.Left, p);
        if (visitedLeft is Expression vl && !ReferenceEquals(vl, node.Left))
        {
            newLeft = vl;
            changed = true;
        }

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
            ? node.WithLeft(newLeft).WithRight(newRight).WithType(newType)
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

        // Condition
        Expression newCondition = node.Condition;
        var visitedCondition = Visit(node.Condition, p);
        if (visitedCondition is Expression vc && !ReferenceEquals(vc, node.Condition))
        {
            newCondition = vc;
            changed = true;
        }

        // TruePart (JLeftPadded<Expression>)
        JLeftPadded<Expression> newTruePart = node.TruePart;
        var visitedTrue = Visit(node.TruePart.Element, p);
        if (visitedTrue is Expression vt && !ReferenceEquals(vt, node.TruePart.Element))
        {
            newTruePart = node.TruePart.WithElement(vt);
            changed = true;
        }

        // FalsePart (JLeftPadded<Expression>)
        JLeftPadded<Expression> newFalsePart = node.FalsePart;
        var visitedFalse = Visit(node.FalsePart.Element, p);
        if (visitedFalse is Expression vf && !ReferenceEquals(vf, node.FalsePart.Element))
        {
            newFalsePart = node.FalsePart.WithElement(vf);
            changed = true;
        }

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithCondition(newCondition).WithTruePart(newTruePart).WithFalsePart(newFalsePart).WithType(newType)
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

        // Variable
        Expression newVariable = node.Variable;
        var visitedVariable = Visit(node.Variable, p);
        if (visitedVariable is Expression vv && !ReferenceEquals(vv, node.Variable))
        {
            newVariable = vv;
            changed = true;
        }

        // AssignmentValue (JLeftPadded<Expression>)
        JLeftPadded<Expression> newAssignmentValue = node.AssignmentValue;
        var visitedValue = Visit(node.AssignmentValue.Element, p);
        if (visitedValue is Expression val && !ReferenceEquals(val, node.AssignmentValue.Element))
        {
            newAssignmentValue = node.AssignmentValue.WithElement(val);
            changed = true;
        }

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithVariable(newVariable).WithAssignmentValue(newAssignmentValue).WithType(newType)
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
            ? node.WithVariable(newVariable).WithAssignmentValue(newAssignmentValue).WithType(newType)
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
            ? node.WithExpression(newExpression).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // Parentheses<Expression> : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitParentheses(Parentheses<Expression> parens, P p)
    {
        var exprResult = VisitExpression(parens, p);
        if (exprResult is not Parentheses<Expression> node) return exprResult;

        var visited = Visit(node.Tree.Element, p);
        if (visited is Expression e && !ReferenceEquals(e, node.Tree.Element))
        {
            return node.WithTree(node.Tree.WithElement(e));
        }
        return node;
    }

    // -----------------------------------------------------------------------
    // ExpressionStatement : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitExpressionStatement(ExpressionStatement expressionStatement, P p)
    {
        var stmtResult = VisitStatement(expressionStatement, p);
        if (stmtResult is not ExpressionStatement node) return stmtResult;

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

        // Variables (IList<JRightPadded<NamedVariable>>)
        var newVars = new List<JRightPadded<NamedVariable>>();
        bool varsChanged = false;
        foreach (var paddedVar in node.Variables)
        {
            var visited = Visit(paddedVar.Element, p);
            if (visited is NamedVariable nv)
            {
                if (!ReferenceEquals(nv, paddedVar.Element)) varsChanged = true;
                newVars.Add(paddedVar.WithElement(nv));
            }
            else
            {
                newVars.Add(paddedVar);
            }
        }
        if (varsChanged) changed = true;

        return changed
            ? node.WithLeadingAnnotations(newLeadingAnnotations).WithTypeExpression(newTypeExpr).WithVariables(varsChanged ? newVars : node.Variables)
            : node;
    }

    // -----------------------------------------------------------------------
    // NamedVariable : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitNamedVariable(NamedVariable namedVariable, P p)
    {
        var changed = false;

        // Name
        Identifier newName = namedVariable.Name;
        var visitedName = Visit(namedVariable.Name, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, namedVariable.Name))
        {
            newName = id;
            changed = true;
        }

        // Initializer (JLeftPadded<Expression>?)
        JLeftPadded<Expression>? newInitializer = namedVariable.Initializer;
        if (namedVariable.Initializer != null)
        {
            var visited = Visit(namedVariable.Initializer.Element, p);
            if (visited is Expression expr && !ReferenceEquals(expr, namedVariable.Initializer.Element))
            {
                newInitializer = namedVariable.Initializer.WithElement(expr);
                changed = true;
            }
        }

        // Type
        JavaType? newType = VisitType(namedVariable.Type, p);
        if (!ReferenceEquals(newType, namedVariable.Type)) changed = true;

        return changed
            ? namedVariable.WithName(newName).WithInitializer(newInitializer).WithType(newType)
            : namedVariable;
    }

    // -----------------------------------------------------------------------
    // Primitive : Expression, TypeTree
    // -----------------------------------------------------------------------
    public virtual J VisitPrimitive(Primitive primitive, P p)
    {
        var exprResult = VisitExpression(primitive, p);
        if (exprResult is not Primitive node) return exprResult;

        return node;
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

        // Select (JRightPadded<Expression>?)
        JRightPadded<Expression>? newSelect = node.Select;
        if (node.Select != null)
        {
            var visited = Visit(node.Select.Element, p);
            if (visited is Expression sel && !ReferenceEquals(sel, node.Select.Element))
            {
                newSelect = node.Select.WithElement(sel);
                changed = true;
            }
        }

        // TypeParameters (JContainer<Expression>?)
        JContainer<Expression>? newTypeParams = node.TypeParameters;
        if (node.TypeParameters != null)
        {
            var tpElements = new List<JRightPadded<Expression>>();
            bool tpChanged = false;
            foreach (var paddedTypeArg in node.TypeParameters.Elements)
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
                newTypeParams = node.TypeParameters.WithElements(tpElements);
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

        // Arguments (JContainer<Expression>)
        JContainer<Expression> newArgs = node.Arguments;
        var argElements = new List<JRightPadded<Expression>>();
        bool argsChanged = false;
        foreach (var paddedArg in node.Arguments.Elements)
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
            newArgs = node.Arguments.WithElements(argElements);
            changed = true;
        }

        // MethodType
        JavaType.Method? newMethodType = (JavaType.Method?)VisitType(node.MethodType, p);
        if (!ReferenceEquals(newMethodType, node.MethodType)) changed = true;

        return changed
            ? node.WithSelect(newSelect).WithTypeParameters(newTypeParams).WithName(newName).WithArguments(newArgs).WithMethodType(newMethodType)
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

        // Enclosing (JRightPadded<Expression>?)
        JRightPadded<Expression>? newEnclosing = node.Enclosing;
        if (node.Enclosing != null)
        {
            var visited = Visit(node.Enclosing.Element, p);
            if (visited is Expression e && !ReferenceEquals(e, node.Enclosing.Element))
            {
                newEnclosing = node.Enclosing.WithElement(e);
                changed = true;
            }
        }

        // Clazz (TypeTree?)
        TypeTree? newClazz = node.Clazz;
        if (node.Clazz != null)
        {
            var visited = Visit(node.Clazz, p);
            if (visited is TypeTree tt && !ReferenceEquals(tt, node.Clazz))
            {
                newClazz = tt;
                changed = true;
            }
        }

        // Arguments (JContainer<Expression>)
        JContainer<Expression> newArgs = node.Arguments;
        var argElements = new List<JRightPadded<Expression>>();
        bool argsChanged = false;
        foreach (var paddedArg in node.Arguments.Elements)
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
            newArgs = node.Arguments.WithElements(argElements);
            changed = true;
        }

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
            ? node.WithEnclosing(newEnclosing).WithClazz(newClazz).WithArguments(newArgs).WithBody(newBody).WithConstructorType(newConstructorType)
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
        JContainer<Expression>? newInitializer = node.Initializer;
        if (node.Initializer != null)
        {
            var initElements = new List<JRightPadded<Expression>>();
            bool initChanged = false;
            foreach (var paddedElem in node.Initializer.Elements)
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
                newInitializer = node.Initializer.WithElements(initElements);
                changed = true;
            }
        }

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithTypeExpression(newTypeExpr).WithDimensions(dimsChanged ? newDims : node.Dimensions).WithInitializer(newInitializer).WithType(newType)
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

        // Expression (JRightPadded<Expression>)
        JRightPadded<Expression> newExpr = node.Expression;
        var visitedExpr = Visit(node.Expression.Element, p);
        if (visitedExpr is Expression e && !ReferenceEquals(e, node.Expression.Element))
        {
            newExpr = node.Expression.WithElement(e);
            changed = true;
        }

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
            ? node.WithExpression(newExpr).WithClazz(newClazz).WithPattern(newPattern).WithType(newType)
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
        JRightPadded<TypeTree> newTypeTreePadded = node.TypeTreePadded;
        var visitedTypeTree = Visit(node.TypeTree, p);
        if (visitedTypeTree is TypeTree tt && !ReferenceEquals(tt, node.TypeTree))
        {
            newTypeTreePadded = node.TypeTreePadded.WithElement(tt);
            changed = true;
        }

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithAnnotations(newAnnotations).WithTypeTreePadded(newTypeTreePadded).WithType(newType)
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

        // Clazz
        NameTree newClazz = node.Clazz;
        var visitedClazz = Visit(node.Clazz, p);
        if (visitedClazz is NameTree nt && !ReferenceEquals(nt, node.Clazz))
        {
            newClazz = nt;
            changed = true;
        }

        // TypeParameters (JContainer<Expression>?)
        JContainer<Expression>? newTypeParams = node.TypeParameters;
        if (node.TypeParameters != null)
        {
            var tpElements = new List<JRightPadded<Expression>>();
            bool tpChanged = false;
            foreach (var paddedParam in node.TypeParameters.Elements)
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
                newTypeParams = node.TypeParameters.WithElements(tpElements);
                changed = true;
            }
        }

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithClazz(newClazz).WithTypeParameters(newTypeParams).WithType(newType)
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
            ? node.WithElementType(newElementType).WithAnnotations(newAnnotations).WithType(newType)
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
            ? node.WithIndexed(newIndexed).WithDimension(newDimension).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // ArrayDimension : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitArrayDimension(ArrayDimension dimension, P p)
    {
        var visited = Visit(dimension.Index.Element, p);
        if (visited is Expression i && !ReferenceEquals(i, dimension.Index.Element))
        {
            return dimension.WithIndex(dimension.Index.WithElement(i));
        }
        return dimension;
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

        // Params (Lambda.Parameters)
        Lambda.Parameters newParams = node.Params;
        var visitedParams = Visit(node.Params, p);
        if (visitedParams is Lambda.Parameters vp && !ReferenceEquals(vp, node.Params))
        {
            newParams = vp;
            changed = true;
        }

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
            ? node.WithParams(newParams).WithBody(newBody).WithType(newType)
            : node;
    }

    // -----------------------------------------------------------------------
    // Lambda.Parameters : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitLambdaParameters(Lambda.Parameters parameters, P p)
    {
        var elements = new List<JRightPadded<J>>();
        bool changed = false;

        foreach (var paddedParam in parameters.Elements)
        {
            var visitedParam = Visit(paddedParam.Element, p);
            if (visitedParam is J vp)
            {
                if (!ReferenceEquals(vp, paddedParam.Element)) changed = true;
                elements.Add(paddedParam.WithElement(vp));
            }
            else
            {
                elements.Add(paddedParam);
            }
        }

        return changed ? parameters.WithElements(elements) : parameters;
    }

    // -----------------------------------------------------------------------
    // Switch : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitSwitch(Switch @switch, P p)
    {
        var stmtResult = VisitStatement(@switch, p);
        if (stmtResult is not Switch node) return stmtResult;

        var changed = false;

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

        return changed ? node.WithSelector(newSelector).WithCases(newCases) : node;
    }

    // -----------------------------------------------------------------------
    // SwitchExpression : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitSwitchExpression(SwitchExpression se, P p)
    {
        var exprResult = VisitExpression(se, p);
        if (exprResult is not SwitchExpression node) return exprResult;

        var changed = false;

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

        return changed ? node.WithSelector(newSelector).WithCases(newCases) : node;
    }

    // -----------------------------------------------------------------------
    // Case : Statement
    // -----------------------------------------------------------------------
    public virtual J VisitCase(Case @case, P p)
    {
        var stmtResult = VisitStatement(@case, p);
        if (stmtResult is not Case node) return stmtResult;

        var changed = false;

        // CaseLabels (JContainer<J>)
        JContainer<J> newCaseLabels = node.CaseLabels;
        var newLabels = new List<JRightPadded<J>>();
        bool labelsChanged = false;
        foreach (var labelPadded in node.CaseLabels.Elements)
        {
            var visited = Visit(labelPadded.Element, p);
            if (visited is J v)
            {
                if (!ReferenceEquals(v, labelPadded.Element)) labelsChanged = true;
                newLabels.Add(labelPadded.WithElement(v));
            }
            else
            {
                newLabels.Add(labelPadded);
            }
        }
        if (labelsChanged)
        {
            newCaseLabels = node.CaseLabels.WithElements(newLabels);
            changed = true;
        }

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
        JRightPadded<J>? newBody = node.Body;
        if (node.Body != null)
        {
            var visitedBody = Visit(node.Body.Element, p);
            if (visitedBody is J vb && !ReferenceEquals(vb, node.Body.Element))
            {
                newBody = node.Body.WithElement(vb);
                changed = true;
            }
        }

        // Statements (JContainer<Statement>)
        JContainer<Statement> newStatements = node.Statements;
        var stmtElements = new List<JRightPadded<Statement>>();
        bool statementsChanged = false;
        foreach (var stmtPadded in node.Statements.Elements)
        {
            var visited = Visit(stmtPadded.Element, p);
            if (visited is Statement s)
            {
                if (!ReferenceEquals(s, stmtPadded.Element)) statementsChanged = true;
                stmtElements.Add(stmtPadded.WithElement(s));
            }
            else
            {
                stmtElements.Add(stmtPadded);
            }
        }
        if (statementsChanged)
        {
            newStatements = node.Statements.WithElements(stmtElements);
            changed = true;
        }

        return changed
            ? node.WithCaseLabels(newCaseLabels).WithGuard(newGuard).WithBody(newBody).WithStatements(newStatements)
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

        // Deconstructor
        Expression newDeconstructor = node.Deconstructor;
        var visitedDeconstructor = Visit(node.Deconstructor, p);
        if (visitedDeconstructor is Expression vd && !ReferenceEquals(vd, node.Deconstructor))
        {
            newDeconstructor = vd;
            changed = true;
        }

        // Nested (JContainer<J>)
        JContainer<J> newNested = node.Nested;
        var nestedElements = new List<JRightPadded<J>>();
        bool nestedChanged = false;
        foreach (var nested in node.Nested.Elements)
        {
            var visited = Visit(nested.Element, p);
            if (visited is J v)
            {
                if (!ReferenceEquals(v, nested.Element)) nestedChanged = true;
                nestedElements.Add(nested.WithElement(v));
            }
            else
            {
                nestedElements.Add(nested);
            }
        }
        if (nestedChanged)
        {
            newNested = node.Nested.WithElements(nestedElements);
            changed = true;
        }

        // Type
        JavaType? newType = VisitType(node.Type, p);
        if (!ReferenceEquals(newType, node.Type)) changed = true;

        return changed
            ? node.WithDeconstructor(newDeconstructor).WithNested(newNested).WithType(newType)
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

        // LabelName (JRightPadded<Identifier>)
        JRightPadded<Identifier> newLabelName = node.LabelName;
        var visitedLabel = Visit(node.LabelName.Element, p);
        if (visitedLabel is Identifier id && !ReferenceEquals(id, node.LabelName.Element))
        {
            newLabelName = node.LabelName.WithElement(id);
            changed = true;
        }

        // Statement
        Statement newStatement = node.Statement;
        var visitedStmt = Visit(node.Statement, p);
        if (visitedStmt is Statement s && !ReferenceEquals(s, node.Statement))
        {
            newStatement = s;
            changed = true;
        }

        return changed
            ? node.WithLabelName(newLabelName).WithStatement(newStatement)
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

        return changed ? node.WithLock(newLock).WithBody(newBody) : node;
    }

    // -----------------------------------------------------------------------
    // TypeCast : Expression
    // -----------------------------------------------------------------------
    public virtual J VisitTypeCast(TypeCast cast, P p)
    {
        var exprResult = VisitExpression(cast, p);
        if (exprResult is not TypeCast node) return exprResult;

        var changed = false;

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

        return changed ? node.WithClazz(newClazz).WithExpression(newExpr) : node;
    }

    // -----------------------------------------------------------------------
    // TypeParameter : (neither)
    // -----------------------------------------------------------------------
    public virtual J VisitTypeParameter(TypeParameter typeParameter, P p)
    {
        var changed = false;

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
            ? typeParameter.WithAnnotations(newAnnotations).WithName(newName).WithBounds(newBounds)
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

        // Expression (JRightPadded<Expression>)
        JRightPadded<Expression> newExpression = node.Expression;
        var visitedExpr = Visit(node.Expression.Element, p);
        if (visitedExpr is Expression e && !ReferenceEquals(e, node.Expression.Element))
        {
            newExpression = node.Expression.WithElement(e);
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
            ? node.WithExpression(newExpression).WithAnnotations(newAnnotations)
            : node;
    }
}
