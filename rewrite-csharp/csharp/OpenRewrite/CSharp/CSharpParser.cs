using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using Microsoft.CodeAnalysis.CSharp.Syntax;
using Rewrite.Core;
using Rewrite.Java;

namespace Rewrite.CSharp;

/// <summary>
/// Parses C# source code into an LST using Roslyn.
/// </summary>
public class CSharpParser
{
    public CompilationUnit Parse(string source, string sourcePath = "source.cs",
        SemanticModel? semanticModel = null)
    {
        var symbols = PreprocessorSourceTransformer.ExtractSymbols(source);
        if (symbols.Count == 0)
            return ParseSingle(source, sourcePath, semanticModel);

        return ParseMulti(source, sourcePath, semanticModel, symbols);
    }

    private CompilationUnit ParseSingle(string source, string sourcePath, SemanticModel? semanticModel)
    {
        if (semanticModel != null)
        {
            var root = semanticModel.SyntaxTree.GetCompilationUnitRoot();
            var visitor = new CSharpParserVisitor(source, semanticModel);
            return visitor.VisitCompilationUnit(root);
        }
        else
        {
            var syntaxTree = CSharpSyntaxTree.ParseText(source, path: sourcePath);
            var root = syntaxTree.GetCompilationUnitRoot();
            var visitor = new CSharpParserVisitor(source);
            return visitor.VisitCompilationUnit(root);
        }
    }

    private CompilationUnit ParseMulti(string source, string sourcePath,
        SemanticModel? semanticModel, HashSet<string> symbols)
    {
        var directiveLines = PreprocessorSourceTransformer.GetDirectivePositions(source);
        var permutations = PreprocessorSourceTransformer.GenerateUniquePermutations(source, symbols);
        PreprocessorSourceTransformer.ComputeActiveBranchIndices(directiveLines, permutations);

        var branches = new List<JRightPadded<CompilationUnit>>();
        for (int i = 0; i < permutations.Count; i++)
        {
            var (cleanSource, definedSymbols) = permutations[i];
            bool isPrimary = i == 0;

            CompilationUnit cu;
            if (isPrimary && semanticModel != null)
            {
                // Re-parse with the clean source but preserve semantic model capability
                var syntaxTree = CSharpSyntaxTree.ParseText(cleanSource, path: sourcePath);
                var compilation = semanticModel.Compilation.ReplaceSyntaxTree(
                    semanticModel.SyntaxTree, syntaxTree);
                var newSemanticModel = compilation.GetSemanticModel(syntaxTree);
                cu = ParseSingle(cleanSource, sourcePath, newSemanticModel);
            }
            else
            {
                cu = ParseSingle(cleanSource, sourcePath, null);
            }

            // Add ConditionalBranchMarker to identify this as a branch
            cu = cu.WithMarkers(cu.Markers.Add(new ConditionalBranchMarker( Guid.NewGuid(), definedSymbols.ToList())));
            branches.Add(new JRightPadded<CompilationUnit>(cu, Space.Empty, Markers.Empty));
        }

        var directive = new ConditionalDirective(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Empty,
            directiveLines,
            branches
        );

        // Outer shell CompilationUnit wrapping the ConditionalDirective.
        // EOF is Space.Empty because the ConditionalDirective printer handles
        // everything including the trailing content from branch outputs.
        var primaryCu = branches[0].Element;
        return new CompilationUnit(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Empty,
            primaryCu.SourcePath,
            new List<Statement> { directive },
            Space.Empty
        );
    }
}

/// <summary>
/// Converts Roslyn syntax trees to OpenRewrite LST.
/// </summary>
internal class CSharpParserVisitor : CSharpSyntaxVisitor<J>
{
    private readonly string _source;
    private int _cursor;
    private readonly SemanticModel? _semanticModel;
    private readonly CSharpTypeMapping? _typeMapping;

    public CSharpParserVisitor(string source, SemanticModel? semanticModel = null)
    {
        _source = source;
        _cursor = 0;
        _semanticModel = semanticModel;
        _typeMapping = semanticModel != null ? new CSharpTypeMapping(semanticModel) : null;
    }

    public new CompilationUnit VisitCompilationUnit(CompilationUnitSyntax node)
    {
        var members = new List<Statement>();

        // Process directives at the very start of the file (before any usings/members)
        // These would otherwise be absorbed into the CompilationUnit prefix
        var leadingDirectives = ProcessGapDirectives(node.SpanStart);
        members.AddRange(leadingDirectives);

        // If leading directives were found, don't extract prefix — the trailing
        // whitespace after the last directive naturally becomes the next member's prefix
        var prefix = leadingDirectives.Count > 0 ? Space.Empty : ExtractPrefix(node);

        // Handle using directives
        foreach (var usingDirective in node.Usings)
        {
            members.AddRange(ProcessGapDirectives(usingDirective.SpanStart));
            var visited = VisitUsingDirective(usingDirective);
            members.Add(visited);
        }

        // Handle top-level statements and type declarations
        foreach (var member in node.Members)
        {
            members.AddRange(ProcessGapDirectives(member.SpanStart));
            var visited = Visit(member);
            if (visited is Statement stmt)
            {
                members.Add(stmt);
            }
        }

        // Process trailing directives before EOF
        members.AddRange(ProcessGapDirectives(_source.Length));

        var eof = ExtractRemaining();

        return new CompilationUnit(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            node.SyntaxTree.FilePath ?? "source.cs",
            members,
            eof
        );
    }

    public override J VisitGlobalStatement(GlobalStatementSyntax node)
    {
        return Visit(node.Statement)!;
    }

    public new UsingDirective VisitUsingDirective(UsingDirectiveSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Handle 'global' keyword
        bool isGlobal = node.GlobalKeyword.IsKind(SyntaxKind.GlobalKeyword);
        Space globalAfter = Space.Empty;
        if (isGlobal)
        {
            _cursor = node.GlobalKeyword.Span.End;
            globalAfter = ExtractSpaceBefore(node.UsingKeyword);
        }

        // Skip 'using' keyword
        _cursor = node.UsingKeyword.Span.End;

        // Handle 'static' keyword
        bool isStatic = node.StaticKeyword.IsKind(SyntaxKind.StaticKeyword);
        Space staticBefore = Space.Empty;
        if (isStatic)
        {
            staticBefore = ExtractSpaceBefore(node.StaticKeyword);
            _cursor = node.StaticKeyword.Span.End;
        }

        // Handle alias
        JRightPadded<Identifier>? alias = null;
        if (node.Alias != null)
        {
            var aliasPrefix = ExtractPrefix(node.Alias.Name);
            _cursor = node.Alias.Name.Identifier.Span.End;
            var aliasName = new Identifier(
                Guid.NewGuid(),
                aliasPrefix,
                Markers.Empty,
                node.Alias.Name.Identifier.Text,
                null
            );
            var aliasAfter = ExtractSpaceBefore(node.Alias.EqualsToken);
            _cursor = node.Alias.EqualsToken.Span.End;
            alias = new JRightPadded<Identifier>(aliasName, aliasAfter, Markers.Empty);
        }

        // Parse namespace or type
        var namespaceOrType = VisitType(node.NamespaceOrType);

        // Consume the semicolon
        SkipTo(node.SemicolonToken.SpanStart);
        SkipToken(node.SemicolonToken);

        return new UsingDirective(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JRightPadded<bool>(isGlobal, globalAfter, Markers.Empty),
            new JLeftPadded<bool>(staticBefore, isStatic),
            alias,
            namespaceOrType!
        );
    }

    public override J VisitFileScopedNamespaceDeclaration(FileScopedNamespaceDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Skip 'namespace' keyword
        _cursor = node.NamespaceKeyword.Span.End;

        // Parse the namespace name (use VisitType to handle qualified names)
        var name = VisitType(node.Name);
        if (name is not Expression nameExpr)
        {
            throw new InvalidOperationException($"Expected Expression for namespace name but got {name?.GetType().Name}");
        }

        // Get space before semicolon
        var nameAfter = ExtractSpaceBefore(node.SemicolonToken);
        _cursor = node.SemicolonToken.Span.End;

        return new Package(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JRightPadded<Expression>(nameExpr, nameAfter, Markers.Empty),
            []  // No annotations for C# namespaces
        );
    }

    public override J VisitNamespaceDeclaration(NamespaceDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Skip 'namespace' keyword
        _cursor = node.NamespaceKeyword.Span.End;

        // Parse the namespace name (use VisitType to handle qualified names)
        var name = VisitType(node.Name);
        if (name is not Expression nameExpr)
        {
            throw new InvalidOperationException($"Expected Expression for namespace name but got {name?.GetType().Name}");
        }

        // Get space before open brace
        var nameAfter = ExtractSpaceBefore(node.OpenBraceToken);
        _cursor = node.OpenBraceToken.Span.End;

        // Parse members (using directives, types, nested namespaces)
        var members = new List<JRightPadded<Statement>>();

        // First, handle using directives within the namespace
        foreach (var usingDirective in node.Usings)
        {
            foreach (var d in ProcessGapDirectives(usingDirective.SpanStart))
                members.Add(new JRightPadded<Statement>(d, Space.Empty, Markers.Empty));
            var visited = VisitUsingDirective(usingDirective);
            members.Add(new JRightPadded<Statement>(visited, Space.Empty, Markers.Empty));
        }

        // Then handle member declarations (types, nested namespaces)
        foreach (var member in node.Members)
        {
            foreach (var d in ProcessGapDirectives(member.SpanStart))
                members.Add(new JRightPadded<Statement>(d, Space.Empty, Markers.Empty));
            var visited = Visit(member);
            if (visited is Statement stmt)
            {
                members.Add(new JRightPadded<Statement>(stmt, Space.Empty, Markers.Empty));
            }
        }

        // Process trailing directives before close brace
        foreach (var d in ProcessGapDirectives(node.CloseBraceToken.SpanStart))
            members.Add(new JRightPadded<Statement>(d, Space.Empty, Markers.Empty));

        // Get space before close brace
        var end = ExtractSpaceBefore(node.CloseBraceToken);
        _cursor = node.CloseBraceToken.Span.End;

        return new NamespaceDeclaration(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JRightPadded<Expression>(nameExpr, nameAfter, Markers.Empty),
            members,
            end
        );
    }

    public override J VisitBlock(BlockSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Skip past the open brace
        _cursor = node.OpenBraceToken.Span.End;

        var statements = new List<JRightPadded<Statement>>();
        foreach (var stmt in node.Statements)
        {
            foreach (var d in ProcessGapDirectives(stmt.SpanStart))
                statements.Add(new JRightPadded<Statement>(d, Space.Empty, Markers.Empty));
            var visited = Visit(stmt);
            if (visited is Statement s)
            {
                statements.Add(new JRightPadded<Statement>(s, Space.Empty, Markers.Empty));
            }
        }

        // Process trailing directives before close brace
        foreach (var d in ProcessGapDirectives(node.CloseBraceToken.SpanStart))
            statements.Add(new JRightPadded<Statement>(d, Space.Empty, Markers.Empty));

        // Extract space before close brace
        var end = ExtractSpaceBefore(node.CloseBraceToken);
        _cursor = node.CloseBraceToken.Span.End;

        return new Block(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JRightPadded<bool>(false, Space.Empty, Markers.Empty),
            statements,
            end
        );
    }

    public override J VisitClassDeclaration(ClassDeclarationSyntax node)
    {
        return VisitTypeDeclaration(node);
    }

    public override J VisitInterfaceDeclaration(InterfaceDeclarationSyntax node)
    {
        return VisitTypeDeclaration(node);
    }

    public override J VisitStructDeclaration(StructDeclarationSyntax node)
    {
        var classDecl = (ClassDeclaration)VisitTypeDeclaration(node);
        // Add Struct marker to distinguish from regular class
        return classDecl.WithMarkers(classDecl.Markers.Add(new Struct(Guid.NewGuid())));
    }

    public override J VisitRecordDeclaration(RecordDeclarationSyntax node)
    {
        var classDecl = (ClassDeclaration)VisitTypeDeclaration(node);

        // For record struct, add Struct marker (KindType is already Record)
        if (node.ClassOrStructKeyword.IsKind(SyntaxKind.StructKeyword))
        {
            return classDecl.WithMarkers(classDecl.Markers.Add(new Struct(Guid.NewGuid())));
        }

        // For "record class" (explicit class keyword), add RecordClass marker
        if (node.ClassOrStructKeyword.IsKind(SyntaxKind.ClassKeyword))
        {
            return classDecl.WithMarkers(classDecl.Markers.Add(new RecordClass(Guid.NewGuid())));
        }

        return classDecl;
    }

    public override J VisitEnumDeclaration(EnumDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse modifiers
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(),
                modPrefix,
                Markers.Empty,
                MapModifier(mod.Kind()),
                []
            ));
        }

        // 'enum' keyword prefix stored as left padding of name
        var enumPrefix = ExtractSpaceBefore(node.EnumKeyword);
        _cursor = node.EnumKeyword.Span.End;

        // Parse the name
        var namePrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var name = new Identifier(
            Guid.NewGuid(),
            namePrefix,
            Markers.Empty,
            node.Identifier.Text,
            null
        );

        // Parse base list (underlying type like : byte)
        JLeftPadded<TypeTree>? baseType = null;
        if (node.BaseList != null)
        {
            var colonPrefix = ExtractSpaceBefore(node.BaseList.ColonToken);
            _cursor = node.BaseList.ColonToken.Span.End;

            if (node.BaseList.Types.Count > 0)
            {
                var bt = (TypeTree)Visit(node.BaseList.Types[0].Type)!;
                baseType = new JLeftPadded<TypeTree>(colonPrefix, bt);
            }
        }

        // Parse members
        var membersPrefix = ExtractSpaceBefore(node.OpenBraceToken);
        _cursor = node.OpenBraceToken.Span.End;

        JContainer<Expression>? members = null;
        if (node.Members.Count > 0)
        {
            var memberList = new List<JRightPadded<Expression>>();
            for (int i = 0; i < node.Members.Count; i++)
            {
                var member = node.Members[i];
                var memberJ = (Expression)VisitEnumMemberDeclaration(member);

                Space afterSpace;
                if (i < node.Members.SeparatorCount)
                {
                    var separator = node.Members.GetSeparator(i);
                    afterSpace = ExtractSpaceBefore(separator);
                    _cursor = separator.Span.End;
                }
                else
                {
                    afterSpace = ExtractSpaceBefore(node.CloseBraceToken);
                }

                memberList.Add(new JRightPadded<Expression>(memberJ, afterSpace, Markers.Empty));
            }

            members = new JContainer<Expression>(membersPrefix, memberList, Markers.Empty);
        }
        else
        {
            members = new JContainer<Expression>(membersPrefix, [], Markers.Empty);
        }

        _cursor = node.CloseBraceToken.Span.End;

        return new EnumDeclaration(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            null,
            modifiers,
            new JLeftPadded<Identifier>(enumPrefix, name),
            baseType,
            members
        );
    }

    public override J VisitEnumMemberDeclaration(EnumMemberDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        _cursor = node.Identifier.Span.End;
        var name = new Identifier(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Empty,
            node.Identifier.Text,
            null
        );

        JLeftPadded<Expression>? initializer = null;
        if (node.EqualsValue != null)
        {
            var equalsPrefix = ExtractSpaceBefore(node.EqualsValue.EqualsToken);
            _cursor = node.EqualsValue.EqualsToken.Span.End;

            var valueExpr = Visit(node.EqualsValue.Value);
            if (valueExpr is Expression expr)
            {
                initializer = new JLeftPadded<Expression>(equalsPrefix, expr);
            }
        }

        return new EnumMemberDeclaration(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            [],
            name,
            initializer
        );
    }

    private J VisitTypeDeclaration(TypeDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse modifiers
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(),
                modPrefix,
                Markers.Empty,
                MapModifier(mod.Kind()),
                []
            ));
        }

        // Parse the 'class'/'interface'/'struct'/'record' keyword
        var kindPrefix = ExtractSpaceBefore(node.Keyword);
        _cursor = node.Keyword.Span.End;

        // For record declarations, also skip the optional 'class' or 'struct' keyword
        // (record, record class, record struct)
        if (node is RecordDeclarationSyntax recordDecl &&
            !recordDecl.ClassOrStructKeyword.IsKind(SyntaxKind.None))
        {
            _cursor = recordDecl.ClassOrStructKeyword.Span.End;
        }

        var kind = new ClassDeclaration.Kind(
            Guid.NewGuid(),
            kindPrefix,
            Markers.Empty,
            [],
            MapClassKind(node.Keyword.Kind())
        );

        // Parse the name
        var namePrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var name = new Identifier(
            Guid.NewGuid(),
            namePrefix,
            Markers.Empty,
            node.Identifier.Text,
            null
        );

        // Parse type parameters (generics)
        JContainer<TypeParameter>? typeParameters = null;
        if (node.TypeParameterList != null)
        {
            typeParameters = ParseTypeParameterList(node.TypeParameterList, node.ConstraintClauses);
        }

        // Parse primary constructor (C# 12)
        // Following Kotlin pattern: synthesize a MethodDeclaration and add it to the body
        MethodDeclaration? primaryConstructorMethod = null;
        if (node.ParameterList != null)
        {
            primaryConstructorMethod = ParsePrimaryConstructor(node.ParameterList);
        }

        // Parse base list (inheritance)
        // In C#, the base list is: `: Base, IFoo, IBar`
        // We map this to: Extends = first type, Implements = remaining types
        JLeftPadded<TypeTree>? extends = null;
        JContainer<TypeTree>? implements = null;
        if (node.BaseList != null)
        {
            var colonPrefix = ExtractSpaceBefore(node.BaseList.ColonToken);
            _cursor = node.BaseList.ColonToken.Span.End;

            var baseTypes = node.BaseList.Types;
            if (baseTypes.Count > 0)
            {
                // First base type goes into Extends
                // colonPrefix = space before `:`, the type's prefix = space after `:`
                var firstTypeTree = (TypeTree)Visit(baseTypes[0].Type)!;
                extends = new JLeftPadded<TypeTree>(colonPrefix, firstTypeTree);

                // Remaining types go into Implements
                if (baseTypes.Count > 1)
                {
                    // Get space before first comma for container's Before
                    var firstSeparator = baseTypes.GetSeparator(0);
                    var containerBefore = ExtractSpaceBefore(firstSeparator);
                    _cursor = firstSeparator.Span.End;

                    var implementsList = new List<JRightPadded<TypeTree>>();
                    for (var i = 1; i < baseTypes.Count; i++)
                    {
                        var typeTree = (TypeTree)Visit(baseTypes[i].Type)!;

                        // After space is before the next comma, or empty for the last element
                        Space afterSpace;
                        if (i < baseTypes.Count - 1)
                        {
                            var separator = baseTypes.GetSeparator(i);
                            afterSpace = ExtractSpaceBefore(separator);
                            _cursor = separator.Span.End;
                        }
                        else
                        {
                            afterSpace = Space.Empty;
                        }

                        implementsList.Add(new JRightPadded<TypeTree>(typeTree, afterSpace, Markers.Empty));
                    }

                    implements = new JContainer<TypeTree>(containerBefore, implementsList, Markers.Empty);
                }
            }
        }

        // Constraint clauses are now integrated into type parameters via ConstrainedTypeParameter
        // (handled in ParseTypeParameterList above). Parse any remaining constraint text for
        // type declarations without type parameters.
        if (node.TypeParameterList == null && node.ConstraintClauses.Count > 0)
        {
            // Skip constraint clause text if type has no type parameters
            // (shouldn't happen in valid C#, but handle gracefully)
            foreach (var clause in node.ConstraintClauses)
            {
                _cursor = clause.Span.End;
            }
        }

        // Parse the body (check for semicolon-terminated records first)
        Block body;

        // Positional records can end with semicolon instead of braces: record Person(string Name);
        // Check if this is a semicolon-terminated record (semicolon present AND no open brace)
        if (node is RecordDeclarationSyntax rec &&
            rec.SemicolonToken.Span.Length > 0 &&
            rec.OpenBraceToken.Span.Length == 0)
        {
            // Positional record with semicolon (no braces): record Person(string Name);
            var semicolonPrefix = ExtractSpaceBefore(rec.SemicolonToken);
            _cursor = rec.SemicolonToken.Span.End;

            body = new Block(
                Guid.NewGuid(),
                semicolonPrefix,
                Markers.Build([new Semicolon(Guid.NewGuid())]),
                new JRightPadded<bool>(false, Space.Empty, Markers.Empty),
                [],
                Space.Empty
            );
        }
        else
        {
            body = VisitClassBody(node);
        }

        var classMarkers = Markers.Empty;

        // If there's a primary constructor, prepend it as the first statement in the body.
        // The MethodDeclaration already carries a PrimaryConstructor marker to identify it.
        if (primaryConstructorMethod != null)
        {
            var bodyStatements = new List<JRightPadded<Statement>>();
            bodyStatements.Add(new JRightPadded<Statement>(primaryConstructorMethod, Space.Empty, Markers.Empty));
            bodyStatements.AddRange(body.Statements);
            body = body.WithStatements(bodyStatements);
        }

        return new ClassDeclaration(
            Guid.NewGuid(),
            prefix,
            classMarkers,
            [],
            modifiers,
            kind,
            name,
            typeParameters,
            null, // PrimaryConstructor (stored in body instead)
            extends,
            implements,
            null, // Permits (Java-only)
            body,
            _typeMapping?.ClassType(node)
        );
    }

    private Block VisitClassBody(TypeDeclarationSyntax node)
    {
        var prefix = ExtractSpaceBefore(node.OpenBraceToken);
        _cursor = node.OpenBraceToken.Span.End;

        var statements = new List<JRightPadded<Statement>>();
        foreach (var member in node.Members)
        {
            foreach (var d in ProcessGapDirectives(member.SpanStart))
                statements.Add(new JRightPadded<Statement>(d, Space.Empty, Markers.Empty));
            var visited = Visit(member);
            if (visited is Statement s)
            {
                statements.Add(new JRightPadded<Statement>(s, Space.Empty, Markers.Empty));
            }
        }

        // Process trailing directives before close brace
        foreach (var d in ProcessGapDirectives(node.CloseBraceToken.SpanStart))
            statements.Add(new JRightPadded<Statement>(d, Space.Empty, Markers.Empty));

        var end = ExtractSpaceBefore(node.CloseBraceToken);
        _cursor = node.CloseBraceToken.Span.End;

        return new Block(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JRightPadded<bool>(false, Space.Empty, Markers.Empty),
            statements,
            end
        );
    }

    private static ClassDeclaration.KindType MapClassKind(SyntaxKind kind)
    {
        return kind switch
        {
            SyntaxKind.ClassKeyword => ClassDeclaration.KindType.Class,
            SyntaxKind.StructKeyword => ClassDeclaration.KindType.Class, // Struct uses Class kind + Struct marker
            SyntaxKind.InterfaceKeyword => ClassDeclaration.KindType.Interface,
            SyntaxKind.RecordKeyword => ClassDeclaration.KindType.Record,
            _ => ClassDeclaration.KindType.Class
        };
    }

    public override J VisitAttributeList(AttributeListSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Skip open bracket
        _cursor = node.OpenBracketToken.Span.End;

        // Parse target (e.g., assembly:, return:)
        JRightPadded<Identifier>? target = null;
        if (node.Target != null)
        {
            var targetPrefix = ExtractPrefix(node.Target);
            _cursor = node.Target.Identifier.Span.End;
            var targetId = new Identifier(
                Guid.NewGuid(),
                targetPrefix,
                Markers.Empty,
                node.Target.Identifier.Text,
                null
            );
            var colonSpace = ExtractSpaceBefore(node.Target.ColonToken);
            _cursor = node.Target.ColonToken.Span.End;
            target = new JRightPadded<Identifier>(targetId, colonSpace, Markers.Empty);
        }

        // Parse attributes
        var attributes = new List<JRightPadded<Annotation>>();
        for (int i = 0; i < node.Attributes.Count; i++)
        {
            var attr = node.Attributes[i];
            var annotation = VisitAttribute(attr);

            Space afterSpace = Space.Empty;
            if (i < node.Attributes.Count - 1)
            {
                var sep = node.Attributes.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(sep);
                _cursor = sep.Span.End;
            }

            attributes.Add(new JRightPadded<Annotation>(annotation, afterSpace, Markers.Empty));
        }

        // Skip close bracket
        SkipTo(node.CloseBracketToken.SpanStart);
        _cursor = node.CloseBracketToken.Span.End;

        return new AttributeList(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            target,
            attributes
        );
    }

    private new Annotation VisitAttribute(AttributeSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse the attribute name (cast to NameTree since it's always a name reference)
        var annotationType = (NameTree)VisitType(node.Name)!;

        // Parse arguments if present
        JContainer<Expression>? arguments = null;
        if (node.ArgumentList != null)
        {
            var openParenSpace = ExtractSpaceBefore(node.ArgumentList.OpenParenToken);
            _cursor = node.ArgumentList.OpenParenToken.Span.End;

            var args = new List<JRightPadded<Expression>>();
            for (int i = 0; i < node.ArgumentList.Arguments.Count; i++)
            {
                var arg = node.ArgumentList.Arguments[i];
                var argExpr = Visit(arg.Expression);
                if (argExpr is Expression expr)
                {
                    Space afterSpace = Space.Empty;
                    if (i < node.ArgumentList.Arguments.Count - 1)
                    {
                        var sep = node.ArgumentList.Arguments.GetSeparator(i);
                        afterSpace = ExtractSpaceBefore(sep);
                        _cursor = sep.Span.End;
                    }
                    args.Add(new JRightPadded<Expression>(expr, afterSpace, Markers.Empty));
                }
            }

            var closeParenSpace = ExtractSpaceBefore(node.ArgumentList.CloseParenToken);
            _cursor = node.ArgumentList.CloseParenToken.Span.End;

            arguments = new JContainer<Expression>(openParenSpace, args, Markers.Empty);
        }

        return new Annotation(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            annotationType!,
            arguments
        );
    }

    public override J VisitMethodDeclaration(MethodDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse modifiers
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(),
                modPrefix,
                Markers.Empty,
                MapModifier(mod.Kind()),
                []
            ));
        }

        // Parse return type
        var returnType = VisitType(node.ReturnType);

        // Parse the method name
        var namePrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var name = new Identifier(
            Guid.NewGuid(),
            namePrefix,
            Markers.Empty,
            node.Identifier.Text,
            null
        );

        // Parse parameters
        var paramsPrefix = ExtractSpaceBefore(node.ParameterList.OpenParenToken);
        _cursor = node.ParameterList.OpenParenToken.Span.End;

        var parameters = new List<JRightPadded<Statement>>();
        for (int i = 0; i < node.ParameterList.Parameters.Count; i++)
        {
            var p = node.ParameterList.Parameters[i];
            var paramStatement = ConvertParameter(p);
            if (paramStatement is Statement stmt)
            {
                Space afterSpace = Space.Empty;
                if (i < node.ParameterList.Parameters.Count - 1)
                {
                    var separatorToken = node.ParameterList.Parameters.GetSeparator(i);
                    afterSpace = ExtractSpaceBefore(separatorToken);
                    _cursor = separatorToken.Span.End;
                }
                else
                {
                    afterSpace = ExtractSpaceBefore(node.ParameterList.CloseParenToken);
                }
                parameters.Add(new JRightPadded<Statement>(stmt, afterSpace, Markers.Empty));
            }
        }

        // If no parameters, capture space before close paren
        if (parameters.Count == 0)
        {
            var emptySpace = ExtractSpaceBefore(node.ParameterList.CloseParenToken);
            // Empty parameters - the space goes in the container's before
            paramsPrefix = new Space(paramsPrefix.Whitespace, paramsPrefix.Comments);
        }

        _cursor = node.ParameterList.CloseParenToken.Span.End;

        var paramsContainer = new JContainer<Statement>(
            paramsPrefix,
            parameters,
            Markers.Empty
        );

        // Parse body (if present)
        Block? body = null;
        if (node.Body != null)
        {
            body = (Block)VisitBlock(node.Body);
        }
        else if (node.ExpressionBody != null)
        {
            // Expression-bodied method: syntactic sugar for a single return statement
            var arrowPrefix = ExtractSpaceBefore(node.ExpressionBody.ArrowToken);
            _cursor = node.ExpressionBody.ArrowToken.Span.End;
            var expr = (Expression)Visit(node.ExpressionBody.Expression)!;

            var returnStmt = new Return(Guid.NewGuid(), Space.Empty, Markers.Empty, expr);
            body = new Block(Guid.NewGuid(), arrowPrefix, Markers.Empty,
                new JRightPadded<bool>(false, Space.Empty, Markers.Empty),
                [new JRightPadded<Statement>(returnStmt, Space.Empty, Markers.Empty)],
                Space.Empty);

            _cursor = node.SemicolonToken.Span.End;
        }
        else if (node.SemicolonToken != default)
        {
            // Abstract/interface method
            _cursor = node.SemicolonToken.Span.End;
        }

        return new MethodDeclaration(
            Guid.NewGuid(),
            prefix,
            node.ExpressionBody != null
                ? Markers.Build([new ExpressionBodied(Guid.NewGuid())])
                : Markers.Empty,
            [],
            modifiers,
            null, // TypeParameters
            returnType,
            name,
            paramsContainer,
            null, // Throws
            body,
            null, // DefaultValue
            _typeMapping?.MethodType(node)
        );
    }

    public override J VisitConstructorDeclaration(ConstructorDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse modifiers
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(), modPrefix, Markers.Empty, MapModifier(mod.Kind()), []
            ));
        }

        // Constructor has no return type

        // Parse name (class name)
        var namePrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var name = new Identifier(Guid.NewGuid(), namePrefix, Markers.Empty, node.Identifier.Text, null);

        // Parse parameters (same pattern as VisitMethodDeclaration)
        var paramsPrefix = ExtractSpaceBefore(node.ParameterList.OpenParenToken);
        _cursor = node.ParameterList.OpenParenToken.Span.End;

        var parameters = new List<JRightPadded<Statement>>();
        for (int i = 0; i < node.ParameterList.Parameters.Count; i++)
        {
            var p = node.ParameterList.Parameters[i];
            var paramStatement = ConvertParameter(p);
            if (paramStatement is Statement stmt)
            {
                Space afterSpace = Space.Empty;
                if (i < node.ParameterList.Parameters.Count - 1)
                {
                    var separatorToken = node.ParameterList.Parameters.GetSeparator(i);
                    afterSpace = ExtractSpaceBefore(separatorToken);
                    _cursor = separatorToken.Span.End;
                }
                else
                {
                    afterSpace = ExtractSpaceBefore(node.ParameterList.CloseParenToken);
                }
                parameters.Add(new JRightPadded<Statement>(stmt, afterSpace, Markers.Empty));
            }
        }

        if (parameters.Count == 0)
        {
            var emptySpace = ExtractSpaceBefore(node.ParameterList.CloseParenToken);
            paramsPrefix = new Space(paramsPrefix.Whitespace, paramsPrefix.Comments);
        }

        _cursor = node.ParameterList.CloseParenToken.Span.End;

        var paramsContainer = new JContainer<Statement>(
            paramsPrefix, parameters, Markers.Empty
        );

        // Parse optional initializer: `: base(args)` or `: this(args)`
        // Stored as JLeftPadded<MethodInvocation> in defaultValue
        JLeftPadded<Expression>? defaultValue = null;
        if (node.Initializer != null)
        {
            var colonPrefix = ExtractSpaceBefore(node.Initializer.ColonToken);
            _cursor = node.Initializer.ColonToken.Span.End;

            var kwPrefix = ExtractSpaceBefore(node.Initializer.ThisOrBaseKeyword);
            _cursor = node.Initializer.ThisOrBaseKeyword.Span.End;
            var kwName = node.Initializer.ThisOrBaseKeyword.Kind() == SyntaxKind.BaseKeyword
                ? "base" : "this";

            var arguments = ParseArgumentList(node.Initializer.ArgumentList);

            var initInvocation = new MethodInvocation(
                Guid.NewGuid(), kwPrefix, Markers.Empty,
                null, // select
                new Identifier(Guid.NewGuid(), Space.Empty, Markers.Empty, kwName, null),
                null, // typeParameters
                arguments,
                null // methodType
            );

            defaultValue = new JLeftPadded<Expression>(colonPrefix, initInvocation);
        }

        // Parse body or expression body
        Block? body = null;
        Markers methodMarkers = Markers.Empty;
        if (node.ExpressionBody != null)
        {
            var arrowPrefix = ExtractSpaceBefore(node.ExpressionBody.ArrowToken);
            _cursor = node.ExpressionBody.ArrowToken.Span.End;
            var expr = (Expression)Visit(node.ExpressionBody.Expression)!;

            var returnStmt = new Return(Guid.NewGuid(), Space.Empty, Markers.Empty, expr);
            body = new Block(Guid.NewGuid(), arrowPrefix, Markers.Empty,
                new JRightPadded<bool>(false, Space.Empty, Markers.Empty),
                [new JRightPadded<Statement>(returnStmt, Space.Empty, Markers.Empty)],
                Space.Empty);

            _cursor = node.SemicolonToken.Span.End;
            methodMarkers = Markers.Build([new ExpressionBodied(Guid.NewGuid())]);
        }
        else if (node.Body != null)
        {
            body = (Block)VisitBlock(node.Body);
        }
        else if (node.SemicolonToken != default)
        {
            _cursor = node.SemicolonToken.Span.End;
        }

        return new MethodDeclaration(
            Guid.NewGuid(),
            prefix,
            methodMarkers,
            [],
            modifiers,
            null, // TypeParameters
            null, // ReturnTypeExpression (constructors have none)
            name,
            paramsContainer,
            null, // Throws
            body,
            defaultValue,
            _typeMapping?.MethodType(node)
        );
    }

    public override J VisitDestructorDeclaration(DestructorDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse modifiers (destructors can have extern)
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(), modPrefix, Markers.Empty,
                MapModifier(mod.Kind()), []
            ));
        }

        // Parse ~Name: tilde is part of the name
        var tildePrefix = ExtractSpaceBefore(node.TildeToken);
        _cursor = node.TildeToken.Span.End;
        var namePrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var name = new Identifier(Guid.NewGuid(), namePrefix, Markers.Empty,
            "~" + node.Identifier.Text, null);

        // Parse empty parameter list
        var paramsPrefix = ExtractSpaceBefore(node.ParameterList.OpenParenToken);
        _cursor = node.ParameterList.OpenParenToken.Span.End;
        var emptySpace = ExtractSpaceBefore(node.ParameterList.CloseParenToken);
        _cursor = node.ParameterList.CloseParenToken.Span.End;
        var paramsContainer = new JContainer<Statement>(paramsPrefix, [], Markers.Empty);

        // Parse body
        Block? body = null;
        Markers methodMarkers = Markers.Empty;
        if (node.ExpressionBody != null)
        {
            var arrowPrefix = ExtractSpaceBefore(node.ExpressionBody.ArrowToken);
            _cursor = node.ExpressionBody.ArrowToken.Span.End;
            var expr = (Expression)Visit(node.ExpressionBody.Expression)!;
            var returnStmt = new Return(Guid.NewGuid(), Space.Empty, Markers.Empty, expr);
            body = new Block(Guid.NewGuid(), arrowPrefix, Markers.Empty,
                new JRightPadded<bool>(false, Space.Empty, Markers.Empty),
                [new JRightPadded<Statement>(returnStmt, Space.Empty, Markers.Empty)],
                Space.Empty);
            _cursor = node.SemicolonToken.Span.End;
            methodMarkers = Markers.Build([new ExpressionBodied(Guid.NewGuid())]);
        }
        else if (node.Body != null)
        {
            body = (Block)VisitBlock(node.Body);
        }
        else if (node.SemicolonToken != default)
        {
            _cursor = node.SemicolonToken.Span.End;
        }

        return new MethodDeclaration(
            Guid.NewGuid(), prefix, methodMarkers,
            [], modifiers,
            null, // TypeParameters
            null, // ReturnTypeExpression (destructors have none)
            name, paramsContainer,
            null, // Throws
            body,
            null, // DefaultValue
            _typeMapping?.MethodType(node)
        );
    }

    public override J VisitDelegateDeclaration(DelegateDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        var attributeLists = new List<AttributeList>();
        foreach (var attrList in node.AttributeLists)
        {
            attributeLists.Add((AttributeList)Visit(attrList)!);
        }

        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(Guid.NewGuid(), modPrefix, Markers.Empty, MapModifier(mod.Kind()), []));
        }

        // 'delegate' keyword — its before-space becomes the JLeftPadded.Before for return type
        var delegatePrefix = ExtractSpaceBefore(node.DelegateKeyword);
        _cursor = node.DelegateKeyword.Span.End;

        var returnType = VisitType(node.ReturnType)!;
        var returnTypePadded = new JLeftPadded<TypeTree>(delegatePrefix, returnType);

        var namePrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var identifier = new Identifier(Guid.NewGuid(), namePrefix, Markers.Empty, node.Identifier.Text, null);

        JContainer<TypeParameter>? typeParameters = null;
        if (node.TypeParameterList != null)
        {
            typeParameters = ParseTypeParameterList(node.TypeParameterList, node.ConstraintClauses);
        }

        // Parse parameters
        var paramsPrefix = ExtractSpaceBefore(node.ParameterList.OpenParenToken);
        _cursor = node.ParameterList.OpenParenToken.Span.End;

        var parameters = new List<JRightPadded<Statement>>();
        for (int i = 0; i < node.ParameterList.Parameters.Count; i++)
        {
            var p = node.ParameterList.Parameters[i];
            var paramStatement = ConvertParameter(p);

            Space afterSpace = Space.Empty;
            if (i < node.ParameterList.Parameters.Count - 1)
            {
                var separatorToken = node.ParameterList.Parameters.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(separatorToken);
                _cursor = separatorToken.Span.End;
            }
            else
            {
                afterSpace = ExtractSpaceBefore(node.ParameterList.CloseParenToken);
            }
            parameters.Add(new JRightPadded<Statement>(paramStatement, afterSpace, Markers.Empty));
        }

        if (parameters.Count == 0)
        {
            paramsPrefix = new Space(paramsPrefix.Whitespace, paramsPrefix.Comments);
        }
        _cursor = node.ParameterList.CloseParenToken.Span.End;

        var paramsContainer = new JContainer<Statement>(paramsPrefix, parameters, Markers.Empty);

        _cursor = node.SemicolonToken.Span.End;

        return new DelegateDeclaration(Guid.NewGuid(), prefix, Markers.Empty,
            attributeLists, modifiers, returnTypePadded, identifier, typeParameters, paramsContainer);
    }

    public override J VisitEventDeclaration(EventDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        var attributeLists = new List<AttributeList>();
        foreach (var attrList in node.AttributeLists)
        {
            attributeLists.Add((AttributeList)Visit(attrList)!);
        }

        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(Guid.NewGuid(), modPrefix, Markers.Empty, MapModifier(mod.Kind()), []));
        }

        // 'event' keyword — its before-space becomes the JLeftPadded.Before for type
        var eventPrefix = ExtractSpaceBefore(node.EventKeyword);
        _cursor = node.EventKeyword.Span.End;

        var typeExpr = VisitType(node.Type)!;
        var typePadded = new JLeftPadded<TypeTree>(eventPrefix, typeExpr);

        JRightPadded<TypeTree>? interfaceSpecifier = null;
        if (node.ExplicitInterfaceSpecifier != null)
        {
            var ifaceType = (TypeTree)VisitType(node.ExplicitInterfaceSpecifier.Name)!;
            var dotSpace = ExtractSpaceBefore(node.ExplicitInterfaceSpecifier.DotToken);
            _cursor = node.ExplicitInterfaceSpecifier.DotToken.Span.End;
            interfaceSpecifier = new JRightPadded<TypeTree>(ifaceType, dotSpace, Markers.Empty);
        }

        var namePrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var name = new Identifier(Guid.NewGuid(), namePrefix, Markers.Empty, node.Identifier.Text, null);

        JContainer<Statement>? accessors = null;
        if (node.AccessorList != null)
        {
            var accessorListPrefix = ExtractSpaceBefore(node.AccessorList.OpenBraceToken);
            _cursor = node.AccessorList.OpenBraceToken.Span.End;

            var accessorStatements = new List<JRightPadded<Statement>>();
            for (int i = 0; i < node.AccessorList.Accessors.Count; i++)
            {
                var accessor = node.AccessorList.Accessors[i];
                var accessorDecl = VisitAccessorDeclaration(accessor);

                // Last accessor gets close-brace space as its right padding
                var afterSpace = i == node.AccessorList.Accessors.Count - 1
                    ? ExtractSpaceBefore(node.AccessorList.CloseBraceToken)
                    : Space.Empty;

                accessorStatements.Add(new JRightPadded<Statement>(accessorDecl, afterSpace, Markers.Empty));
            }

            _cursor = node.AccessorList.CloseBraceToken.Span.End;

            accessors = new JContainer<Statement>(accessorListPrefix, accessorStatements, Markers.Empty);
        }

        if (node.SemicolonToken != default && node.SemicolonToken.Span.Length > 0)
        {
            _cursor = node.SemicolonToken.Span.End;
        }

        return new EventDeclaration(Guid.NewGuid(), prefix, Markers.Empty,
            attributeLists, modifiers, typePadded, interfaceSpecifier, name, accessors);
    }

    public override J VisitIndexerDeclaration(IndexerDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(Guid.NewGuid(), modPrefix, Markers.Empty, MapModifier(mod.Kind()), []));
        }

        var typeExpr = VisitType(node.Type)!;

        JRightPadded<TypeTree>? explicitInterfaceSpecifier = null;
        if (node.ExplicitInterfaceSpecifier != null)
        {
            var ifaceType = (TypeTree)VisitType(node.ExplicitInterfaceSpecifier.Name)!;
            var dotSpace = ExtractSpaceBefore(node.ExplicitInterfaceSpecifier.DotToken);
            _cursor = node.ExplicitInterfaceSpecifier.DotToken.Span.End;
            explicitInterfaceSpecifier = new JRightPadded<TypeTree>(ifaceType, dotSpace, Markers.Empty);
        }

        var thisPrefix = ExtractSpaceBefore(node.ThisKeyword);
        _cursor = node.ThisKeyword.Span.End;
        var indexer = (Expression)new Identifier(Guid.NewGuid(), thisPrefix, Markers.Empty, "this", null);

        var bracketPrefix = ExtractSpaceBefore(node.ParameterList.OpenBracketToken);
        _cursor = node.ParameterList.OpenBracketToken.Span.End;

        var parameters = new List<JRightPadded<Expression>>();
        for (int i = 0; i < node.ParameterList.Parameters.Count; i++)
        {
            var p = node.ParameterList.Parameters[i];
            var paramStatement = ConvertParameter(p);

            Space afterSpace = Space.Empty;
            if (i < node.ParameterList.Parameters.Count - 1)
            {
                var separatorToken = node.ParameterList.Parameters.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(separatorToken);
                _cursor = separatorToken.Span.End;
            }
            else
            {
                afterSpace = ExtractSpaceBefore(node.ParameterList.CloseBracketToken);
            }
            Expression paramExpr = new StatementExpression(Guid.NewGuid(), Space.Empty, Markers.Empty, paramStatement);
            parameters.Add(new JRightPadded<Expression>(paramExpr, afterSpace, Markers.Empty));
        }
        _cursor = node.ParameterList.CloseBracketToken.Span.End;

        JLeftPadded<Expression>? expressionBody = null;
        Block? accessors = null;

        if (node.ExpressionBody != null)
        {
            var arrowSpace = ExtractSpaceBefore(node.ExpressionBody.ArrowToken);
            _cursor = node.ExpressionBody.ArrowToken.Span.End;
            var expr = (Expression)Visit(node.ExpressionBody.Expression)!;
            expressionBody = new JLeftPadded<Expression>(arrowSpace, expr);
            SkipTo(node.SemicolonToken.SpanStart);
            SkipToken(node.SemicolonToken);
        }
        else if (node.AccessorList != null)
        {
            accessors = VisitAccessorList(node.AccessorList);
        }

        return new IndexerDeclaration(Guid.NewGuid(), prefix, Markers.Empty,
            modifiers, typeExpr, explicitInterfaceSpecifier, indexer,
            new JContainer<Expression>(bracketPrefix, parameters, Markers.Empty),
            expressionBody, accessors);
    }

    public override J VisitOperatorDeclaration(OperatorDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        var attributeLists = new List<AttributeList>();
        foreach (var attrList in node.AttributeLists)
        {
            attributeLists.Add((AttributeList)Visit(attrList)!);
        }

        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(Guid.NewGuid(), modPrefix, Markers.Empty, MapModifier(mod.Kind()), []));
        }

        var returnType = VisitType(node.ReturnType)!;

        JRightPadded<TypeTree>? explicitInterfaceSpecifier = null;
        if (node.ExplicitInterfaceSpecifier != null)
        {
            var ifaceType = (TypeTree)VisitType(node.ExplicitInterfaceSpecifier.Name)!;
            var dotSpace = ExtractSpaceBefore(node.ExplicitInterfaceSpecifier.DotToken);
            _cursor = node.ExplicitInterfaceSpecifier.DotToken.Span.End;
            explicitInterfaceSpecifier = new JRightPadded<TypeTree>(ifaceType, dotSpace, Markers.Empty);
        }

        var operatorKeywordPrefix = ExtractSpaceBefore(node.OperatorKeyword);
        _cursor = node.OperatorKeyword.Span.End;
        var operatorKeyword = new Keyword(Guid.NewGuid(), operatorKeywordPrefix, Markers.Empty, KeywordKind.Operator);

        Keyword? checkedKeyword = null;
        if (node.CheckedKeyword != default && node.CheckedKeyword.Span.Length > 0)
        {
            var checkedPrefix = ExtractSpaceBefore(node.CheckedKeyword);
            _cursor = node.CheckedKeyword.Span.End;
            checkedKeyword = new Keyword(Guid.NewGuid(), checkedPrefix, Markers.Empty, KeywordKind.Checked);
        }

        var opTokenPrefix = ExtractSpaceBefore(node.OperatorToken);
        _cursor = node.OperatorToken.Span.End;
        var opKind = node.OperatorToken.Kind() switch
        {
            SyntaxKind.PlusToken => OperatorDeclaration.OperatorKind.Plus,
            SyntaxKind.MinusToken => OperatorDeclaration.OperatorKind.Minus,
            SyntaxKind.AsteriskToken => OperatorDeclaration.OperatorKind.Star,
            SyntaxKind.SlashToken => OperatorDeclaration.OperatorKind.Division,
            SyntaxKind.PercentToken => OperatorDeclaration.OperatorKind.Percent,
            SyntaxKind.ExclamationToken => OperatorDeclaration.OperatorKind.Bang,
            SyntaxKind.TildeToken => OperatorDeclaration.OperatorKind.Tilde,
            SyntaxKind.PlusPlusToken => OperatorDeclaration.OperatorKind.PlusPlus,
            SyntaxKind.MinusMinusToken => OperatorDeclaration.OperatorKind.MinusMinus,
            SyntaxKind.LessThanToken => OperatorDeclaration.OperatorKind.LessThan,
            SyntaxKind.GreaterThanToken => OperatorDeclaration.OperatorKind.GreaterThan,
            SyntaxKind.LessThanEqualsToken => OperatorDeclaration.OperatorKind.LessThanEquals,
            SyntaxKind.GreaterThanEqualsToken => OperatorDeclaration.OperatorKind.GreaterThanEquals,
            SyntaxKind.EqualsEqualsToken => OperatorDeclaration.OperatorKind.Equals,
            SyntaxKind.ExclamationEqualsToken => OperatorDeclaration.OperatorKind.NotEquals,
            SyntaxKind.AmpersandToken => OperatorDeclaration.OperatorKind.Ampersand,
            SyntaxKind.BarToken => OperatorDeclaration.OperatorKind.Bar,
            SyntaxKind.CaretToken => OperatorDeclaration.OperatorKind.Caret,
            SyntaxKind.TrueKeyword => OperatorDeclaration.OperatorKind.True,
            SyntaxKind.FalseKeyword => OperatorDeclaration.OperatorKind.False,
            SyntaxKind.LessThanLessThanToken => OperatorDeclaration.OperatorKind.LeftShift,
            SyntaxKind.GreaterThanGreaterThanToken => OperatorDeclaration.OperatorKind.RightShift,
            _ => throw new InvalidOperationException($"Unsupported operator token: {node.OperatorToken.Kind()}")
        };
        var operatorToken = new JLeftPadded<OperatorDeclaration.OperatorKind>(opTokenPrefix, opKind);

        var paramsPrefix = ExtractSpaceBefore(node.ParameterList.OpenParenToken);
        _cursor = node.ParameterList.OpenParenToken.Span.End;

        var parameters = new List<JRightPadded<Expression>>();
        for (int i = 0; i < node.ParameterList.Parameters.Count; i++)
        {
            var p = node.ParameterList.Parameters[i];
            var paramStatement = ConvertParameter(p);

            Space afterSpace = Space.Empty;
            if (i < node.ParameterList.Parameters.Count - 1)
            {
                var separatorToken = node.ParameterList.Parameters.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(separatorToken);
                _cursor = separatorToken.Span.End;
            }
            else
            {
                afterSpace = ExtractSpaceBefore(node.ParameterList.CloseParenToken);
            }
            Expression paramExpr = new StatementExpression(Guid.NewGuid(), Space.Empty, Markers.Empty, paramStatement);
            parameters.Add(new JRightPadded<Expression>(paramExpr, afterSpace, Markers.Empty));
        }
        _cursor = node.ParameterList.CloseParenToken.Span.End;

        var body = (Block)VisitBlock(node.Body!);

        return new OperatorDeclaration(Guid.NewGuid(), prefix, Markers.Empty,
            attributeLists, modifiers, explicitInterfaceSpecifier, operatorKeyword, checkedKeyword,
            operatorToken, returnType, new JContainer<Expression>(paramsPrefix, parameters, Markers.Empty),
            body, _typeMapping?.MethodType(node));
    }

    public override J VisitConversionOperatorDeclaration(ConversionOperatorDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(Guid.NewGuid(), modPrefix, Markers.Empty, MapModifier(mod.Kind()), []));
        }

        var implicitExplicitPrefix = ExtractSpaceBefore(node.ImplicitOrExplicitKeyword);
        _cursor = node.ImplicitOrExplicitKeyword.Span.End;
        var conversionKind = node.ImplicitOrExplicitKeyword.Kind() == SyntaxKind.ImplicitKeyword
            ? ConversionOperatorDeclaration.ExplicitImplicit.Implicit
            : ConversionOperatorDeclaration.ExplicitImplicit.Explicit;
        var kindPadded = new JLeftPadded<ConversionOperatorDeclaration.ExplicitImplicit>(implicitExplicitPrefix, conversionKind);

        // 'operator' keyword — its before-space becomes the JLeftPadded.Before for return type
        var operatorPrefix = ExtractSpaceBefore(node.OperatorKeyword);
        _cursor = node.OperatorKeyword.Span.End;

        var returnType = VisitType(node.Type)!;
        var returnTypePadded = new JLeftPadded<TypeTree>(operatorPrefix, returnType);

        var paramsPrefix = ExtractSpaceBefore(node.ParameterList.OpenParenToken);
        _cursor = node.ParameterList.OpenParenToken.Span.End;

        var parameters = new List<JRightPadded<Statement>>();
        for (int i = 0; i < node.ParameterList.Parameters.Count; i++)
        {
            var p = node.ParameterList.Parameters[i];
            var paramStatement = ConvertParameter(p);

            Space afterSpace = Space.Empty;
            if (i < node.ParameterList.Parameters.Count - 1)
            {
                var separatorToken = node.ParameterList.Parameters.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(separatorToken);
                _cursor = separatorToken.Span.End;
            }
            else
            {
                afterSpace = ExtractSpaceBefore(node.ParameterList.CloseParenToken);
            }
            parameters.Add(new JRightPadded<Statement>(paramStatement, afterSpace, Markers.Empty));
        }
        _cursor = node.ParameterList.CloseParenToken.Span.End;

        JLeftPadded<Expression>? expressionBody = null;
        Block? body = null;

        if (node.ExpressionBody != null)
        {
            var arrowSpace = ExtractSpaceBefore(node.ExpressionBody.ArrowToken);
            _cursor = node.ExpressionBody.ArrowToken.Span.End;
            var expr = (Expression)Visit(node.ExpressionBody.Expression)!;
            expressionBody = new JLeftPadded<Expression>(arrowSpace, expr);
            SkipTo(node.SemicolonToken.SpanStart);
            SkipToken(node.SemicolonToken);
        }
        else if (node.Body != null)
        {
            body = (Block)VisitBlock(node.Body);
        }
        else if (node.SemicolonToken != default && node.SemicolonToken.Span.Length > 0)
        {
            _cursor = node.SemicolonToken.Span.End;
        }

        return new ConversionOperatorDeclaration(Guid.NewGuid(), prefix, Markers.Empty,
            modifiers, kindPadded, returnTypePadded,
            new JContainer<Statement>(paramsPrefix, parameters, Markers.Empty),
            expressionBody, body);
    }

    public override J VisitPropertyDeclaration(PropertyDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse modifiers
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(),
                modPrefix,
                Markers.Empty,
                MapModifier(mod.Kind()),
                []
            ));
        }

        // Parse the type
        var typeExpr = VisitType(node.Type);

        // Parse the property name
        var namePrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var name = new Identifier(
            Guid.NewGuid(),
            namePrefix,
            Markers.Empty,
            node.Identifier.Text,
            null
        );

        // Parse either expression body or accessor list
        Block? accessors = null;
        JLeftPadded<Expression>? expressionBody = null;

        if (node.ExpressionBody != null)
        {
            // Expression body: public int X => 42;
            var arrowSpace = ExtractSpaceBefore(node.ExpressionBody.ArrowToken);
            _cursor = node.ExpressionBody.ArrowToken.Span.End;
            var expr = Visit(node.ExpressionBody.Expression);
            expressionBody = new JLeftPadded<Expression>(arrowSpace, (Expression)expr!);

            // Consume semicolon
            SkipTo(node.SemicolonToken.SpanStart);
            SkipToken(node.SemicolonToken);
        }
        else if (node.AccessorList != null)
        {
            accessors = VisitAccessorList(node.AccessorList);
        }

        return new PropertyDeclaration(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            modifiers,
            typeExpr!,
            name,
            accessors,
            expressionBody
        );
    }

    private new Block VisitAccessorList(AccessorListSyntax node)
    {
        var prefix = ExtractSpaceBefore(node.OpenBraceToken);
        _cursor = node.OpenBraceToken.Span.End;

        var statements = new List<JRightPadded<Statement>>();
        foreach (var accessor in node.Accessors)
        {
            var accessorDecl = VisitAccessorDeclaration(accessor);
            statements.Add(new JRightPadded<Statement>(accessorDecl, Space.Empty, Markers.Empty));
        }

        var end = ExtractSpaceBefore(node.CloseBraceToken);
        _cursor = node.CloseBraceToken.Span.End;

        return new Block(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JRightPadded<bool>(false, Space.Empty, Markers.Empty),
            statements,
            end
        );
    }

    private new AccessorDeclaration VisitAccessorDeclaration(AccessorDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse modifiers (like 'private' for private set)
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(),
                modPrefix,
                Markers.Empty,
                MapModifier(mod.Kind()),
                []
            ));
        }

        // Parse the keyword (get/set/init)
        var keywordPrefix = ExtractSpaceBefore(node.Keyword);
        _cursor = node.Keyword.Span.End;
        var accessorKind = MapAccessorKind(node.Keyword.Kind());
        var kind = new JLeftPadded<AccessorKind>(keywordPrefix, accessorKind);

        // Parse body, expression body, or auto-implemented (semicolon only)
        Block? body = null;
        JLeftPadded<Expression>? expressionBody = null;

        if (node.ExpressionBody != null)
        {
            // Expression body: get => _x;
            var arrowSpace = ExtractSpaceBefore(node.ExpressionBody.ArrowToken);
            _cursor = node.ExpressionBody.ArrowToken.Span.End;
            var expr = Visit(node.ExpressionBody.Expression);
            expressionBody = new JLeftPadded<Expression>(arrowSpace, (Expression)expr!);

            // Consume semicolon
            SkipTo(node.SemicolonToken.SpanStart);
            SkipToken(node.SemicolonToken);
        }
        else if (node.Body != null)
        {
            body = (Block)VisitBlock(node.Body);
        }
        else if (node.SemicolonToken != default)
        {
            // Auto-implemented accessor - consume the semicolon
            SkipTo(node.SemicolonToken.SpanStart);
            SkipToken(node.SemicolonToken);
        }

        return new AccessorDeclaration(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            modifiers,
            kind,
            body,
            expressionBody
        );
    }

    private static AccessorKind MapAccessorKind(SyntaxKind kind)
    {
        return kind switch
        {
            SyntaxKind.GetKeyword => AccessorKind.Get,
            SyntaxKind.SetKeyword => AccessorKind.Set,
            SyntaxKind.InitKeyword => AccessorKind.Init,
            SyntaxKind.AddKeyword => AccessorKind.Add,
            SyntaxKind.RemoveKeyword => AccessorKind.Remove,
            _ => throw new InvalidOperationException($"Unknown accessor keyword: {kind}")
        };
    }

    private Statement ConvertParameter(ParameterSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse modifiers (ref, out, params, etc.)
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(),
                modPrefix,
                Markers.Empty,
                MapModifier(mod.Kind()),
                []
            ));
        }

        // Parse the type
        TypeTree? typeExpr = null;
        if (node.Type != null)
        {
            typeExpr = VisitType(node.Type);
        }

        // Parse the parameter name
        var namePrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var name = new Identifier(
            Guid.NewGuid(),
            namePrefix,
            Markers.Empty,
            node.Identifier.Text,
            null
        );

        // Parse default value if present
        JLeftPadded<Expression>? initializer = null;
        if (node.Default != null)
        {
            var equalsPrefix = ExtractSpaceBefore(node.Default.EqualsToken);
            _cursor = node.Default.EqualsToken.Span.End;

            var initExpr = Visit(node.Default.Value);
            if (initExpr is Expression expr)
            {
                initializer = new JLeftPadded<Expression>(equalsPrefix, expr);
            }
        }

        var namedVar = new NamedVariable(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Empty,
            name,
            [],
            initializer,
            _typeMapping?.VariableType(node)
        );

        return new VariableDeclarations(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            [],
            modifiers,
            typeExpr,
            null,
            [],
            [new JRightPadded<NamedVariable>(namedVar, Space.Empty, Markers.Empty)]
        );
    }

    public override J VisitReturnStatement(ReturnStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.ReturnKeyword.Span.End;

        Expression? expression = null;
        if (node.Expression != null)
        {
            var expr = Visit(node.Expression);
            if (expr is Expression e)
            {
                expression = e;
            }
        }

        // Consume the semicolon
        SkipTo(node.SemicolonToken.SpanStart);
        SkipToken(node.SemicolonToken);

        return new Return(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            expression
        );
    }

    public override J VisitIfStatement(IfStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.IfKeyword.Span.End;

        // Parse condition with parentheses
        var conditionPrefix = ExtractSpaceBefore(node.OpenParenToken);
        _cursor = node.OpenParenToken.Span.End;

        var condExpr = Visit(node.Condition);
        if (condExpr is not Expression condExpression)
        {
            throw new InvalidOperationException($"Expected Expression but got {condExpr?.GetType().Name}");
        }

        var conditionAfter = ExtractSpaceBefore(node.CloseParenToken);
        _cursor = node.CloseParenToken.Span.End;

        var condition = new ControlParentheses<Expression>(
            Guid.NewGuid(),
            conditionPrefix,
            Markers.Empty,
            new JRightPadded<Expression>(condExpression, conditionAfter, Markers.Empty)
        );

        // Parse then part
        var thenStmt = Visit(node.Statement);
        if (thenStmt is not Statement thenStatement)
        {
            throw new InvalidOperationException($"Expected Statement but got {thenStmt?.GetType().Name}");
        }
        var thenPart = new JRightPadded<Statement>(thenStatement, Space.Empty, Markers.Empty);

        // Parse else part if present
        If.Else? elsePart = null;
        if (node.Else != null)
        {
            var elsePrefix = ExtractSpaceBefore(node.Else.ElseKeyword);
            _cursor = node.Else.ElseKeyword.Span.End;

            var elseStmt = Visit(node.Else.Statement);
            if (elseStmt is Statement elseStatement)
            {
                elsePart = new If.Else(
                    Guid.NewGuid(),
                    elsePrefix,
                    Markers.Empty,
                    new JRightPadded<Statement>(elseStatement, Space.Empty, Markers.Empty)
                );
            }
        }

        return new If(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            condition,
            thenPart,
            elsePart
        );
    }

    public override J VisitSwitchStatement(SwitchStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.SwitchKeyword.Span.End;

        // Parse selector with parentheses
        var selectorPrefix = ExtractSpaceBefore(node.OpenParenToken);
        _cursor = node.OpenParenToken.Span.End;

        var selectorExpr = Visit(node.Expression);
        if (selectorExpr is not Expression selectorExpression)
        {
            throw new InvalidOperationException($"Expected Expression but got {selectorExpr?.GetType().Name}");
        }

        var selectorAfter = ExtractSpaceBefore(node.CloseParenToken);
        _cursor = node.CloseParenToken.Span.End;

        var selector = new ControlParentheses<Expression>(
            Guid.NewGuid(),
            selectorPrefix,
            Markers.Empty,
            new JRightPadded<Expression>(selectorExpression, selectorAfter, Markers.Empty)
        );

        // Parse the switch body as a block containing cases
        var blockPrefix = ExtractSpaceBefore(node.OpenBraceToken);
        _cursor = node.OpenBraceToken.Span.End;

        var cases = new List<JRightPadded<Statement>>();
        foreach (var section in node.Sections)
        {
            // Each section may produce multiple Case elements (one per label)
            foreach (var caseStmt in VisitSwitchSection(section))
            {
                cases.Add(new JRightPadded<Statement>(caseStmt, Space.Empty, Markers.Empty));
            }
        }

        var blockEndPrefix = ExtractSpaceBefore(node.CloseBraceToken);
        _cursor = node.CloseBraceToken.Span.End;

        var casesBlock = new Block(
            Guid.NewGuid(),
            blockPrefix,
            Markers.Empty,
            new JRightPadded<bool>(false, Space.Empty, Markers.Empty), // not static
            cases,
            blockEndPrefix
        );

        return new Switch(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            selector,
            casesBlock
        );
    }

    public override J VisitSwitchExpression(SwitchExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse the governing expression (selector)
        var selectorExpr = Visit(node.GoverningExpression);
        if (selectorExpr is not Expression selector)
        {
            throw new InvalidOperationException($"Expected Expression but got {selectorExpr?.GetType().Name}");
        }

        // Space before 'switch' keyword (stored as right padding of expression)
        var selectorAfter = ExtractSpaceBefore(node.SwitchKeyword);
        _cursor = node.SwitchKeyword.Span.End;

        // Space before '{' (stored as JContainer prefix for arms)
        var armsPrefix = ExtractSpaceBefore(node.OpenBraceToken);
        _cursor = node.OpenBraceToken.Span.End;

        // Parse each arm as SwitchExpressionArm
        var arms = new List<JRightPadded<SwitchExpressionArm>>();
        for (int i = 0; i < node.Arms.Count; i++)
        {
            var arm = node.Arms[i];
            var armPrefix = ExtractPrefix(arm);

            // Parse the pattern — may be a Pattern, VariableDeclarations (type pattern),
            // Binary (and/or), Unary (not), or Parentheses (grouping)
            var patternNode = Visit(arm.Pattern)!;

            // Parse when clause if present
            JLeftPadded<Expression>? whenExpression = null;
            if (arm.WhenClause != null)
            {
                var whenPrefix = ExtractSpaceBefore(arm.WhenClause.WhenKeyword);
                _cursor = arm.WhenClause.WhenKeyword.Span.End;

                var condition = Visit(arm.WhenClause.Condition);
                if (condition is Expression condExpr)
                {
                    whenExpression = new JLeftPadded<Expression>(whenPrefix, condExpr);
                }
            }

            // Space before '=>' (stored as left padding of expression)
            var beforeArrow = ExtractSpaceBefore(arm.EqualsGreaterThanToken);
            _cursor = arm.EqualsGreaterThanToken.Span.End;

            // Parse the result expression
            var resultExpr = Visit(arm.Expression);
            if (resultExpr is not Expression result)
            {
                throw new InvalidOperationException($"Expected Expression but got {resultExpr?.GetType().Name}");
            }

            var switchArm = new SwitchExpressionArm(
                Guid.NewGuid(),
                armPrefix,
                Markers.Empty,
                patternNode,
                whenExpression,
                new JLeftPadded<Expression>(beforeArrow, result)
            );

            // Handle comma separator
            Space armAfter;
            if (i < node.Arms.SeparatorCount)
            {
                var comma = node.Arms.GetSeparator(i);
                armAfter = ExtractSpaceBefore(comma);
                _cursor = comma.Span.End;
            }
            else
            {
                // Last arm - space before '}'
                armAfter = ExtractSpaceBefore(node.CloseBraceToken);
            }

            arms.Add(new JRightPadded<SwitchExpressionArm>(switchArm, armAfter, Markers.Empty));
        }

        _cursor = node.CloseBraceToken.Span.End;

        return new SwitchExpression(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JRightPadded<Expression>(selector, selectorAfter, Markers.Empty),
            new JContainer<SwitchExpressionArm>(armsPrefix, arms, Markers.Empty)
        );
    }

    /// <summary>
    /// Parses a switch section, creating one J.Case per label.
    /// In C# fallthrough like "case 1: case 2: stmt;", each case keyword becomes a separate J.Case.
    /// Only the last Case in the sequence gets the statements.
    /// </summary>
    private new IEnumerable<Case> VisitSwitchSection(SwitchSectionSyntax node)
    {
        var labels = node.Labels;
        var labelCount = labels.Count;

        // Parse all labels FIRST (in order, so cursor progresses correctly)
        var parsedLabels = new List<(Space Prefix, J Label, Space AfterLabel, Expression? Guard)>();
        for (int i = 0; i < labelCount; i++)
        {
            var label = labels[i];
            var casePrefix = ExtractPrefix(label);
            var (labelJ, afterLabel, guard) = VisitSwitchLabel(label);
            parsedLabels.Add((casePrefix, labelJ, afterLabel, guard));
        }

        // Parse statements AFTER labels (cursor is now at the right position)
        var statements = new List<JRightPadded<Statement>>();
        foreach (var stmt in node.Statements)
        {
            var stmtJ = Visit(stmt);
            if (stmtJ is Statement statement)
            {
                statements.Add(new JRightPadded<Statement>(statement, Space.Empty, Markers.Empty));
            }
        }
        var statementsContainer = new JContainer<Statement>(Space.Empty, statements, Markers.Empty);
        var emptyStatements = new JContainer<Statement>(Space.Empty, new List<JRightPadded<Statement>>(), Markers.Empty);

        // Create one Case per label
        for (int i = 0; i < parsedLabels.Count; i++)
        {
            var (casePrefix, labelJ, afterLabel, guard) = parsedLabels[i];

            // Only the LAST label gets the statements
            var caseStatements = (i == parsedLabels.Count - 1) ? statementsContainer : emptyStatements;

            yield return new Case(
                Guid.NewGuid(),
                casePrefix,
                Markers.Empty,
                CaseType.Statement,
                new JContainer<J>(Space.Empty, [new JRightPadded<J>(labelJ, afterLabel, Markers.Empty)], Markers.Empty),
                guard,
                caseStatements,
                null  // Body (for switch expressions)
            );
        }
    }

    /// <summary>
    /// Parses a switch label and returns the label J element, the space before the colon, and optional guard.
    /// The prefix before 'case'/'default' keyword is extracted separately in VisitSwitchSection.
    /// </summary>
    private (J Label, Space ColonPrefix, Expression? Guard) VisitSwitchLabel(SwitchLabelSyntax label)
    {
        if (label is CaseSwitchLabelSyntax caseLabel)
        {
            // case expr:
            _cursor = caseLabel.Keyword.Span.End;
            var valueExpr = Visit(caseLabel.Value);
            var colonPrefix = ExtractSpaceBefore(caseLabel.ColonToken);
            _cursor = caseLabel.ColonToken.Span.End;

            if (valueExpr is J valueJ)
            {
                return (valueJ, colonPrefix, null);
            }
            throw new InvalidOperationException($"Expected J but got {valueExpr?.GetType().Name}");
        }
        else if (label is DefaultSwitchLabelSyntax defaultLabel)
        {
            // default:
            _cursor = defaultLabel.Keyword.Span.End;
            var colonPrefix = ExtractSpaceBefore(defaultLabel.ColonToken);
            _cursor = defaultLabel.ColonToken.Span.End;

            // Return an Identifier for 'default' with empty prefix (prefix is on the Case)
            var defaultId = new Identifier(
                Guid.NewGuid(),
                Space.Empty,
                Markers.Empty,
                "default",
                null
            );
            return (defaultId, colonPrefix, null);
        }
        else if (label is CasePatternSwitchLabelSyntax patternLabel)
        {
            // case pattern when guard:
            _cursor = patternLabel.Keyword.Span.End;
            var pattern = Visit(patternLabel.Pattern);

            // Parse when clause if present
            Expression? guard = null;
            Space afterLabel;
            if (patternLabel.WhenClause != null)
            {
                // afterLabel = space before 'when' keyword (goes in label's After padding)
                afterLabel = ExtractPrefix(patternLabel.WhenClause);
                _cursor = patternLabel.WhenClause.WhenKeyword.Span.End;

                // Parse the guard condition - its prefix is space after 'when' keyword
                var condition = Visit(patternLabel.WhenClause.Condition);
                if (condition is Expression guardExpr)
                {
                    guard = guardExpr;
                }

                // Skip past colon (space before colon is discarded for simplicity)
                _cursor = patternLabel.ColonToken.Span.End;
            }
            else
            {
                // No guard: afterLabel = space before colon
                afterLabel = ExtractSpaceBefore(patternLabel.ColonToken);
                _cursor = patternLabel.ColonToken.Span.End;
            }

            if (pattern is J patternJ)
            {
                return (patternJ, afterLabel, guard);
            }
            throw new InvalidOperationException($"Expected J but got {pattern?.GetType().Name}");
        }

        throw new InvalidOperationException($"Unknown switch label type: {label.GetType().Name}");
    }

    /// <summary>
    /// Parses an is-pattern expression (e.g., obj is string s, obj is null, obj is int n and > 0).
    /// </summary>
    public override J VisitIsPatternExpression(IsPatternExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Visit the left-hand expression
        var expression = (Expression)Visit(node.Expression)!;

        // Space before "is" keyword
        var isPrefix = ExtractSpaceBefore(node.IsKeyword);
        _cursor = node.IsKeyword.Span.End;

        // Visit the right-hand pattern
        var pattern = Visit(node.Pattern)!;

        return new IsPattern(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            expression,
            new JLeftPadded<J>(isPrefix, (J)pattern)
        );
    }

    /// <summary>
    /// Parses a declaration pattern (e.g., case int i:) as J.VariableDeclarations
    /// following Java's approach for type patterns.
    /// </summary>
    public override J VisitDeclarationPattern(DeclarationPatternSyntax node)
    {
        // Parse the type (e.g., int, string, MyClass)
        var typeExpr = VisitType(node.Type);

        // Handle the designation (variable name or discard)
        NamedVariable? namedVar = null;
        if (node.Designation is SingleVariableDesignationSyntax varDesignation)
        {
            // Regular type pattern: case int i:
            var namePrefix = ExtractSpaceBefore(varDesignation.Identifier);
            _cursor = varDesignation.Identifier.Span.End;
            var name = new Identifier(Guid.NewGuid(), namePrefix, Markers.Empty, varDesignation.Identifier.Text, null);
            namedVar = new NamedVariable(Guid.NewGuid(), Space.Empty, Markers.Empty, name, [], null, null);
        }
        else if (node.Designation is DiscardDesignationSyntax discardDesignation)
        {
            // Type pattern with discard: case int _:
            var namePrefix = ExtractSpaceBefore(discardDesignation.UnderscoreToken);
            _cursor = discardDesignation.UnderscoreToken.Span.End;
            var name = new Identifier(Guid.NewGuid(), namePrefix, Markers.Empty, "_", null);
            namedVar = new NamedVariable(Guid.NewGuid(), Space.Empty, Markers.Empty, name, [], null, null);
        }

        var variables = namedVar != null
            ? new List<JRightPadded<NamedVariable>> { new JRightPadded<NamedVariable>(namedVar, Space.Empty, Markers.Empty) }
            : new List<JRightPadded<NamedVariable>>();

        return new VariableDeclarations(
            Guid.NewGuid(),
            Space.Empty,  // Prefix is handled by the case label itself
            Markers.Empty,
            [],           // Leading annotations
            [],           // Modifiers
            typeExpr,
            null,         // Varargs
            [],           // Dimensions before name
            variables
        );
    }

    /// <summary>
    /// Parses a var pattern (e.g., var x) as J.VariableDeclarations with 'var' as the type.
    /// </summary>
    public override J VisitVarPattern(VarPatternSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.VarKeyword.Span.End;

        // Create 'var' as the type
        var varType = new Identifier(Guid.NewGuid(), prefix, Markers.Empty, "var", null);

        // Handle the designation (variable name or discard)
        NamedVariable? namedVar = null;
        if (node.Designation is SingleVariableDesignationSyntax varDesignation)
        {
            var namePrefix = ExtractSpaceBefore(varDesignation.Identifier);
            _cursor = varDesignation.Identifier.Span.End;
            var name = new Identifier(Guid.NewGuid(), namePrefix, Markers.Empty, varDesignation.Identifier.Text, null);
            namedVar = new NamedVariable(Guid.NewGuid(), Space.Empty, Markers.Empty, name, [], null, null);
        }
        else if (node.Designation is DiscardDesignationSyntax discardDesignation)
        {
            var namePrefix = ExtractSpaceBefore(discardDesignation.UnderscoreToken);
            _cursor = discardDesignation.UnderscoreToken.Span.End;
            var name = new Identifier(Guid.NewGuid(), namePrefix, Markers.Empty, "_", null);
            namedVar = new NamedVariable(Guid.NewGuid(), Space.Empty, Markers.Empty, name, [], null, null);
        }

        var variables = namedVar != null
            ? new List<JRightPadded<NamedVariable>> { new JRightPadded<NamedVariable>(namedVar, Space.Empty, Markers.Empty) }
            : new List<JRightPadded<NamedVariable>>();

        return new VariableDeclarations(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Empty,
            [],
            [],
            varType,
            null,
            [],
            variables
        );
    }

    public override J VisitRelationalPattern(RelationalPatternSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse the operator
        var operatorPrefix = ExtractSpaceBefore(node.OperatorToken);
        var operatorType = node.OperatorToken.Kind() switch
        {
            SyntaxKind.LessThanToken => RelationalPattern.Type.LessThan,
            SyntaxKind.LessThanEqualsToken => RelationalPattern.Type.LessThanOrEqual,
            SyntaxKind.GreaterThanToken => RelationalPattern.Type.GreaterThan,
            SyntaxKind.GreaterThanEqualsToken => RelationalPattern.Type.GreaterThanOrEqual,
            _ => throw new InvalidOperationException($"Unsupported operator '{node.OperatorToken}' in RelationalPattern")
        };
        _cursor = node.OperatorToken.Span.End;

        // Parse the value expression
        var value = Visit(node.Expression);
        if (value is not Expression valueExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {value?.GetType().Name}");
        }

        return new RelationalPattern(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JLeftPadded<RelationalPattern.Type>(operatorPrefix, operatorType),
            valueExpr
        );
    }

    public override J VisitConstantPattern(ConstantPatternSyntax node)
    {
        var prefix = ExtractPrefix(node);
        var value = (Expression)Visit(node.Expression)!;
        return new ConstantPattern(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            value
        );
    }

    public override J VisitDiscardPattern(DiscardPatternSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.UnderscoreToken.Span.End;
        return new DiscardPattern(Guid.NewGuid(), prefix, Markers.Empty, null);
    }

    public override J VisitTypePattern(TypePatternSyntax node)
    {
        // Type pattern is just a type check without variable binding: case int:
        // Return the type as a type expression
        return VisitType(node.Type) ?? throw new InvalidOperationException($"Unable to parse type in TypePattern: {node.Type}");
    }

    public override J VisitPredefinedType(PredefinedTypeSyntax node)
    {
        // PredefinedType for primitive types like int, string, bool, etc.
        return VisitType(node) ?? throw new InvalidOperationException($"Unable to parse PredefinedType: {node}");
    }

    public override J VisitBinaryPattern(BinaryPatternSyntax node)
    {
        // Parse the left pattern, wrapping non-Expression results in StatementExpression
        var leftJ = Visit(node.Left)!;
        Expression left = leftJ is Expression leftExpr
            ? leftExpr
            : new StatementExpression(Guid.NewGuid(), Space.Empty, Markers.Empty, (Statement)leftJ);

        // Parse the operator — reuse J.Binary with And/Or; printer detects pattern context
        var operatorPrefix = ExtractSpaceBefore(node.OperatorToken);
        var operatorType = node.OperatorToken.Kind() switch
        {
            SyntaxKind.AndKeyword => Binary.OperatorType.And,
            SyntaxKind.OrKeyword => Binary.OperatorType.Or,
            _ => throw new InvalidOperationException($"Unsupported operator '{node.OperatorToken}' in BinaryPattern")
        };
        _cursor = node.OperatorToken.Span.End;

        // Parse the right pattern, wrapping non-Expression results in StatementExpression
        var rightJ = Visit(node.Right)!;
        Expression right = rightJ is Expression rightExpr
            ? rightExpr
            : new StatementExpression(Guid.NewGuid(), Space.Empty, Markers.Empty, (Statement)rightJ);

        return new Binary(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Empty,
            left,
            new JLeftPadded<Binary.OperatorType>(operatorPrefix, operatorType),
            right,
            null
        );
    }

    public override J VisitUnaryPattern(UnaryPatternSyntax node)
    {
        // Unary pattern is 'not' followed by a pattern
        var prefix = ExtractPrefix(node);

        // Skip the 'not' keyword, capturing space before
        var operatorPrefix = ExtractSpaceBefore(node.OperatorToken);
        _cursor = node.OperatorToken.Span.End;

        // Parse the nested pattern
        var pattern = Visit(node.Pattern);
        if (pattern is not Expression patternExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {pattern?.GetType().Name}");
        }

        // Use J.Unary with Not operator — printer detects pattern context for 'not' vs '!'
        return new Unary(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JLeftPadded<Unary.OperatorType>(operatorPrefix, Unary.OperatorType.Not),
            patternExpr,
            null  // type
        );
    }

    public override J VisitParenthesizedPattern(ParenthesizedPatternSyntax node)
    {
        // Parenthesized pattern wraps another pattern in parentheses for grouping
        var prefix = ExtractPrefix(node);

        // Skip the opening paren
        _cursor = node.OpenParenToken.Span.End;

        // Parse the inner pattern
        var innerPattern = Visit(node.Pattern);
        if (innerPattern is not Expression innerExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {innerPattern?.GetType().Name}");
        }

        // Capture space before closing paren
        var afterSpace = ExtractSpaceBefore(node.CloseParenToken);
        _cursor = node.CloseParenToken.Span.End;

        return new Parentheses<Expression>(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JRightPadded<Expression>(innerExpr, afterSpace, Markers.Empty)
        );
    }

    public override J VisitRecursivePattern(RecursivePatternSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse optional type qualifier (e.g., "string" in "string { Length: > 5 }" or "Point" in "Point(x, y)")
        TypeTree? typeQualifier = null;
        if (node.Type != null)
        {
            typeQualifier = VisitType(node.Type);
        }

        // Check for positional pattern clause (deconstruction pattern)
        if (node.PositionalPatternClause != null)
        {
            var clause = node.PositionalPatternClause;
            var containerPrefix = ExtractSpaceBefore(clause.OpenParenToken);
            _cursor = clause.OpenParenToken.Span.End;

            var elements = new List<JRightPadded<J>>();
            for (var i = 0; i < clause.Subpatterns.Count; i++)
            {
                var subpatternSyntax = clause.Subpatterns[i];
                var subpattern = VisitPositionalSubpattern(subpatternSyntax);

                // Space after: comma space for non-last, space before ) for last
                Space afterSpace;
                if (i < clause.Subpatterns.SeparatorCount)
                {
                    var separator = clause.Subpatterns.GetSeparator(i);
                    afterSpace = ExtractSpaceBefore(separator);
                    _cursor = separator.Span.End;
                }
                else
                {
                    // Last element's After holds space before close paren
                    afterSpace = ExtractSpaceBefore(clause.CloseParenToken);
                }

                elements.Add(new JRightPadded<J>(subpattern, afterSpace, Markers.Empty));
            }

            _cursor = clause.CloseParenToken.Span.End;
            var nested = new JContainer<J>(containerPrefix, elements, Markers.Empty);

            // Deconstructor is the type qualifier, or Empty for tuple patterns like (int x, int y)
            // For tuple patterns: Empty has Space.Empty prefix (DeconstructionPattern has the prefix)
            // For typed patterns: typeQualifier has its own prefix from VisitType
            Expression deconstructor = typeQualifier is Expression te
                ? te
                : new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty);

            return new DeconstructionPattern(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                deconstructor,
                nested,
                null
            );
        }

        // Property pattern clause (e.g., "{ Length: > 5 }")
        JContainer<NamedExpression> subpatterns;
        if (node.PropertyPatternClause != null)
        {
            var clause = node.PropertyPatternClause;
            var containerPrefix = ExtractSpaceBefore(clause.OpenBraceToken);
            _cursor = clause.OpenBraceToken.Span.End;

            var elements = new List<JRightPadded<NamedExpression>>();
            for (var i = 0; i < clause.Subpatterns.Count; i++)
            {
                var subpatternSyntax = clause.Subpatterns[i];
                var subpattern = VisitSubpattern(subpatternSyntax);

                // Space after: comma space for non-last, space before } for last
                Space afterSpace;
                if (i < clause.Subpatterns.SeparatorCount)
                {
                    var separator = clause.Subpatterns.GetSeparator(i);
                    afterSpace = ExtractSpaceBefore(separator);
                    _cursor = separator.Span.End;
                }
                else
                {
                    // Last element's After holds space before close brace
                    afterSpace = ExtractSpaceBefore(clause.CloseBraceToken);
                }

                elements.Add(new JRightPadded<NamedExpression>(subpattern, afterSpace, Markers.Empty));
            }

            _cursor = clause.CloseBraceToken.Span.End;
            subpatterns = new JContainer<NamedExpression>(containerPrefix, elements, Markers.Empty);
        }
        else
        {
            // Should not happen for property patterns, but handle gracefully
            subpatterns = new JContainer<NamedExpression>(Space.Empty, [], Markers.Empty);
        }

        return new PropertyPattern(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            typeQualifier,
            subpatterns
        );
    }

    private J VisitPositionalSubpattern(SubpatternSyntax node)
    {
        // For positional patterns, subpatterns are just patterns without names
        // e.g., in (int x, int y), each "int x" is a subpattern
        return Visit(node.Pattern)!;
    }

    private NamedExpression VisitSubpattern(SubpatternSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse the property name
        if (node.NameColon == null)
        {
            throw new InvalidOperationException("Subpattern without name not yet supported");
        }

        // Advance cursor past the identifier
        _cursor = node.NameColon.Name.Identifier.Span.End;

        var nameIdent = new Identifier(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Empty,
            node.NameColon.Name.Identifier.Text,
            null
        );

        // Space after name (before colon)
        var colonSpace = ExtractSpaceBefore(node.NameColon.ColonToken);
        _cursor = node.NameColon.ColonToken.Span.End;

        var name = new JRightPadded<Identifier>(nameIdent, colonSpace, Markers.Empty);

        // Parse the pattern
        var pattern = Visit(node.Pattern);
        if (pattern is not Expression patternExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {pattern?.GetType().Name}");
        }

        return new NamedExpression(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            name,
            patternExpr
        );
    }

    public override J VisitWhileStatement(WhileStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.WhileKeyword.Span.End;

        // Parse condition with parentheses
        var conditionPrefix = ExtractSpaceBefore(node.OpenParenToken);
        _cursor = node.OpenParenToken.Span.End;

        var condExpr = Visit(node.Condition);
        if (condExpr is not Expression condExpression)
        {
            throw new InvalidOperationException($"Expected Expression but got {condExpr?.GetType().Name}");
        }

        var conditionAfter = ExtractSpaceBefore(node.CloseParenToken);
        _cursor = node.CloseParenToken.Span.End;

        var condition = new ControlParentheses<Expression>(
            Guid.NewGuid(),
            conditionPrefix,
            Markers.Empty,
            new JRightPadded<Expression>(condExpression, conditionAfter, Markers.Empty)
        );

        // Parse body
        var bodyStmt = Visit(node.Statement);
        if (bodyStmt is not Statement bodyStatement)
        {
            throw new InvalidOperationException($"Expected Statement but got {bodyStmt?.GetType().Name}");
        }

        return new WhileLoop(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            condition,
            new JRightPadded<Statement>(bodyStatement, Space.Empty, Markers.Empty)
        );
    }

    public override J VisitDoStatement(DoStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.DoKeyword.Span.End;

        // Parse body
        var bodyStmt = Visit(node.Statement);
        if (bodyStmt is not Statement bodyStatement)
        {
            throw new InvalidOperationException($"Expected Statement but got {bodyStmt?.GetType().Name}");
        }

        // Parse 'while' keyword
        var whilePrefix = ExtractSpaceBefore(node.WhileKeyword);
        _cursor = node.WhileKeyword.Span.End;

        // Parse condition with parentheses
        var conditionPrefix = ExtractSpaceBefore(node.OpenParenToken);
        _cursor = node.OpenParenToken.Span.End;

        var condExpr = Visit(node.Condition);
        if (condExpr is not Expression condExpression)
        {
            throw new InvalidOperationException($"Expected Expression but got {condExpr?.GetType().Name}");
        }

        var conditionAfter = ExtractSpaceBefore(node.CloseParenToken);
        _cursor = node.CloseParenToken.Span.End;

        var condition = new ControlParentheses<Expression>(
            Guid.NewGuid(),
            conditionPrefix,
            Markers.Empty,
            new JRightPadded<Expression>(condExpression, conditionAfter, Markers.Empty)
        );

        // Consume the semicolon
        SkipTo(node.SemicolonToken.SpanStart);
        SkipToken(node.SemicolonToken);

        return new DoWhileLoop(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JRightPadded<Statement>(bodyStatement, Space.Empty, Markers.Empty),
            new JLeftPadded<ControlParentheses<Expression>>(whilePrefix, condition)
        );
    }

    public override J VisitLabeledStatement(LabeledStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse the label identifier
        var labelName = new Identifier(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Empty,
            node.Identifier.Text,
            null
        );
        _cursor = node.Identifier.Span.End;

        // Parse the colon — space before it becomes the right-padding
        var colonPrefix = ExtractSpaceBefore(node.ColonToken);
        _cursor = node.ColonToken.Span.End;

        var labelPadded = new JRightPadded<Identifier>(labelName, colonPrefix, Markers.Empty);

        // Parse the statement that follows the label
        var statement = (Statement)Visit(node.Statement)!;

        return new Label(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            labelPadded,
            statement
        );
    }

    public override J VisitUnsafeStatement(UnsafeStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.UnsafeKeyword.Span.End;
        var block = (Block)Visit(node.Block)!;
        return new UnsafeStatement(Guid.NewGuid(), prefix, Markers.Empty, block);
    }

    public override J VisitFixedStatement(FixedStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.FixedKeyword.Span.End;

        // Parse control parentheses with variable declaration
        var openParenPrefix = ExtractSpaceBefore(node.OpenParenToken);
        _cursor = node.OpenParenToken.Span.End;

        var declaration = (VariableDeclarations)Visit(node.Declaration)!;

        var closeParenAfter = ExtractSpaceBefore(node.CloseParenToken);
        _cursor = node.CloseParenToken.Span.End;

        var declarations = new ControlParentheses<VariableDeclarations>(
            Guid.NewGuid(),
            openParenPrefix,
            Markers.Empty,
            new JRightPadded<VariableDeclarations>(declaration, closeParenAfter, Markers.Empty)
        );

        var statement = (Statement)Visit(node.Statement)!;

        return new FixedStatement(Guid.NewGuid(), prefix, Markers.Empty, declarations, (Block)statement);
    }

    public override J VisitExternAliasDirective(ExternAliasDirectiveSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.ExternKeyword.Span.End;

        var aliasPrefix = ExtractSpaceBefore(node.AliasKeyword);
        _cursor = node.AliasKeyword.Span.End;

        var identifierPrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;

        var identifier = new Identifier(Guid.NewGuid(), identifierPrefix, Markers.Empty,
            node.Identifier.Text, null);

        // Consume semicolon
        _cursor = node.SemicolonToken.Span.End;

        return new ExternAlias(Guid.NewGuid(), prefix, Markers.Empty,
            new JLeftPadded<Identifier>(aliasPrefix, identifier));
    }

    public override J VisitInitializerExpression(InitializerExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.OpenBraceToken.Span.End;

        var elements = new List<JRightPadded<Expression>>();
        for (int i = 0; i < node.Expressions.Count; i++)
        {
            var expr = (Expression)Visit(node.Expressions[i])!;
            var afterSpace = Space.Empty;
            if (i < node.Expressions.Count - 1)
            {
                var sep = node.Expressions.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(sep);
                _cursor = sep.Span.End;
            }
            else
            {
                // Check for trailing comma
                if (node.Expressions.SeparatorCount > i)
                {
                    var sep = node.Expressions.GetSeparator(i);
                    afterSpace = ExtractSpaceBefore(sep);
                    _cursor = sep.Span.End;
                }
                else
                {
                    afterSpace = ExtractSpaceBefore(node.CloseBraceToken);
                }
            }

            elements.Add(new JRightPadded<Expression>(expr, afterSpace, Markers.Empty));
        }

        if (node.Expressions.Count == 0)
        {
            // Empty initializer: { }
            // Capture space before close brace
        }

        _cursor = node.CloseBraceToken.Span.End;

        var containerBefore = Space.Empty; // Space before { is in prefix
        return new InitializerExpression(Guid.NewGuid(), prefix, Markers.Empty,
            new JContainer<Expression>(containerBefore, elements, Markers.Empty));
    }

    public override J VisitDefaultExpression(DefaultExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.Keyword.Span.End;

        // Parse parentheses and type: (T)
        SkipTo(node.OpenParenToken.SpanStart);
        var beforeParen = ExtractSpaceBefore(node.OpenParenToken);
        _cursor = node.OpenParenToken.Span.End;

        var type = (TypeTree)VisitType(node.Type);

        var afterType = ExtractSpaceBefore(node.CloseParenToken);
        _cursor = node.CloseParenToken.Span.End;

        var typeContainer = new JContainer<TypeTree>(
            beforeParen,
            [new JRightPadded<TypeTree>(type, afterType, Markers.Empty)],
            Markers.Empty
        );

        return new DefaultExpression(Guid.NewGuid(), prefix, Markers.Empty, typeContainer);
    }

    // C# `lock` maps to J.Synchronized intentionally — do NOT replace with a Cs.LockStatement.
    // The Java-side Cs model has a Cs.LockStatement but it is unused; J.Synchronized is the
    // correct mapping because lock and synchronized have identical semantics and structure
    // (keyword + parenthesized expression + block), and using the J type means existing Java
    // recipes and visitors work on C# lock statements without any extra dispatch.
    public override J VisitLockStatement(LockStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.LockKeyword.Span.End;

        // Parse control parentheses: (expression)
        var openParenPrefix = ExtractSpaceBefore(node.OpenParenToken);
        _cursor = node.OpenParenToken.Span.End;

        var expression = Visit(node.Expression);
        if (expression is not Expression expr)
        {
            throw new InvalidOperationException($"Expected Expression but got {expression?.GetType().Name}");
        }

        var closeParenPrefix = ExtractSpaceBefore(node.CloseParenToken);
        _cursor = node.CloseParenToken.Span.End;

        var controlParens = new ControlParentheses<Expression>(
            Guid.NewGuid(),
            openParenPrefix,
            Markers.Empty,
            new JRightPadded<Expression>(expr, closeParenPrefix, Markers.Empty)
        );

        // Parse body block
        var body = Visit(node.Statement);
        if (body is not Block block)
        {
            throw new InvalidOperationException($"Expected Block but got {body?.GetType().Name}");
        }

        return new Synchronized(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            controlParens,
            block
        );
    }

    public override J VisitYieldStatement(YieldStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.YieldKeyword.Span.End;

        // Determine the kind and extract space before "return"/"break"
        var keywordPrefix = ExtractSpaceBefore(node.ReturnOrBreakKeyword);
        _cursor = node.ReturnOrBreakKeyword.Span.End;

        var kind = node.ReturnOrBreakKeyword.Kind() == SyntaxKind.ReturnKeyword
            ? KeywordKind.Return
            : KeywordKind.Break;

        var keyword = new Keyword(Guid.NewGuid(), keywordPrefix, Markers.Empty, kind);

        // Parse optional expression (only for yield return)
        Expression? expression = null;
        if (node.Expression != null)
        {
            var expr = Visit(node.Expression);
            if (expr is not Expression e)
            {
                throw new InvalidOperationException($"Expected Expression but got {expr?.GetType().Name}");
            }
            expression = e;
        }

        // Skip semicolon
        _cursor = node.SemicolonToken.Span.End;

        return new Yield(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            keyword,
            expression
        );
    }

    public override J VisitCheckedStatement(CheckedStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.Keyword.Span.End;

        var kind = node.Keyword.IsKind(SyntaxKind.CheckedKeyword) ? KeywordKind.Checked : KeywordKind.Unchecked;
        var keyword = new Keyword(Guid.NewGuid(), Space.Empty, Markers.Empty, kind);

        var block = (Block)Visit(node.Block)!;

        return new CheckedStatement(Guid.NewGuid(), prefix, Markers.Empty, keyword, block);
    }

    public override J VisitCheckedExpression(CheckedExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.Keyword.Span.End;

        var kind = node.Keyword.IsKind(SyntaxKind.CheckedKeyword) ? KeywordKind.Checked : KeywordKind.Unchecked;
        var keyword = new Keyword(Guid.NewGuid(), Space.Empty, Markers.Empty, kind);

        // Parse the parenthesized expression
        var openParenPrefix = ExtractSpaceBefore(node.OpenParenToken);
        _cursor = node.OpenParenToken.Span.End;

        var expr = (Expression)Visit(node.Expression)!;

        var closeParenPrefix = ExtractSpaceBefore(node.CloseParenToken);
        _cursor = node.CloseParenToken.Span.End;

        var controlParens = new ControlParentheses<Expression>(
            Guid.NewGuid(),
            openParenPrefix,
            Markers.Empty,
            new JRightPadded<Expression>(expr, closeParenPrefix, Markers.Empty)
        );

        return new CheckedExpression(Guid.NewGuid(), prefix, Markers.Empty, keyword, controlParens);
    }

    public override J VisitRangeExpression(RangeExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse optional start expression
        JRightPadded<Expression>? start = null;
        if (node.LeftOperand != null)
        {
            var startExpr = (Expression)Visit(node.LeftOperand)!;
            var afterStart = ExtractSpaceBefore(node.OperatorToken);
            start = new JRightPadded<Expression>(startExpr, afterStart, Markers.Empty);
        }

        _cursor = node.OperatorToken.Span.End;

        // Parse optional end expression
        Expression? end = null;
        if (node.RightOperand != null)
        {
            end = (Expression)Visit(node.RightOperand)!;
        }

        return new RangeExpression(Guid.NewGuid(), prefix, Markers.Empty, start, end);
    }

    public override J VisitStackAllocArrayCreationExpression(StackAllocArrayCreationExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.StackAllocKeyword.Span.End;

        // Parse the array type as a J.NewArray (reusing VisitArrayCreationExpression logic)
        // The type is an ArrayTypeSyntax like "int[3]"
        var arrayPrefix = ExtractPrefix(node.Type);

        TypeTree? typeExpression = null;
        var dimensions = new List<ArrayDimension>();

        if (node.Type is ArrayTypeSyntax arrayType)
        {
            typeExpression = VisitType(arrayType.ElementType);

            foreach (var rankSpec in arrayType.RankSpecifiers)
            {
                var bracketPrefix = ExtractSpaceBefore(rankSpec.OpenBracketToken);
                _cursor = rankSpec.OpenBracketToken.Span.End;

                for (int i = 0; i < rankSpec.Sizes.Count; i++)
                {
                    var size = rankSpec.Sizes[i];
                    var dimPrefix = i == 0 ? bracketPrefix : ExtractPrefix(size);

                    if (size is OmittedArraySizeExpressionSyntax)
                    {
                        var afterSpace = ExtractSpaceBefore(rankSpec.CloseBracketToken);
                        dimensions.Add(new ArrayDimension(
                            Guid.NewGuid(), dimPrefix, Markers.Empty,
                            new JRightPadded<Expression>(
                                new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty),
                                afterSpace, Markers.Empty)));
                    }
                    else
                    {
                        var sizeExpr = (Expression)Visit(size)!;
                        Space afterSpace;
                        if (i < rankSpec.Sizes.Count - 1)
                        {
                            afterSpace = ExtractSpaceBefore(rankSpec.Sizes.GetSeparator(i));
                            _cursor = rankSpec.Sizes.GetSeparator(i).Span.End;
                        }
                        else
                        {
                            afterSpace = ExtractSpaceBefore(rankSpec.CloseBracketToken);
                        }
                        dimensions.Add(new ArrayDimension(
                            Guid.NewGuid(), dimPrefix, Markers.Empty,
                            new JRightPadded<Expression>(sizeExpr, afterSpace, Markers.Empty)));
                    }
                }

                _cursor = rankSpec.CloseBracketToken.Span.End;
            }
        }

        // Parse optional initializer
        JContainer<Expression>? initializer = null;
        if (node.Initializer != null)
        {
            initializer = ParseArrayInitializer(node.Initializer);
        }

        var newArray = new NewArray(
            Guid.NewGuid(), arrayPrefix, Markers.Empty,
            typeExpression, dimensions, initializer, null);

        return new StackAllocExpression(Guid.NewGuid(), prefix, Markers.Empty, newArray);
    }

    public override J VisitCollectionExpression(CollectionExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.OpenBracketToken.Span.End;

        var elements = new List<JRightPadded<Expression>>();
        for (var i = 0; i < node.Elements.Count; i++)
        {
            var element = node.Elements[i];
            Expression expr;
            if (element is ExpressionElementSyntax exprElem)
            {
                expr = (Expression)Visit(exprElem.Expression)!;
            }
            else
            {
                expr = (Expression)Visit(element)!;
            }

            Space afterSpace;
            if (i < node.Elements.SeparatorCount)
            {
                var separator = node.Elements.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(separator);
                _cursor = separator.Span.End;
            }
            else
            {
                afterSpace = ExtractSpaceBefore(node.CloseBracketToken);
            }

            elements.Add(new JRightPadded<Expression>(expr, afterSpace, Markers.Empty));
        }

        _cursor = node.CloseBracketToken.Span.End;

        return new CollectionExpression(Guid.NewGuid(), prefix, Markers.Empty, elements, null);
    }

    public override J VisitForStatement(ForStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.ForKeyword.Span.End;

        // Parse open paren
        var controlPrefix = ExtractSpaceBefore(node.OpenParenToken);
        _cursor = node.OpenParenToken.Span.End;

        // Parse initializers
        var init = new List<JRightPadded<Statement>>();
        if (node.Declaration != null)
        {
            var declPrefix = ExtractPrefix(node.Declaration);
            var typeExpr = VisitType(node.Declaration.Type);
            var variables = new List<JRightPadded<NamedVariable>>();

            for (int i = 0; i < node.Declaration.Variables.Count; i++)
            {
                var v = node.Declaration.Variables[i];
                var varPrefix = ExtractPrefix(v);

                var namePrefix = ExtractSpaceBefore(v.Identifier);
                _cursor = v.Identifier.Span.End;
                var name = new Identifier(Guid.NewGuid(), namePrefix, Markers.Empty, v.Identifier.Text, null);

                JLeftPadded<Expression>? initializer = null;
                if (v.Initializer != null)
                {
                    var equalsPrefix = ExtractSpaceBefore(v.Initializer.EqualsToken);
                    _cursor = v.Initializer.EqualsToken.Span.End;
                    var initExpr = Visit(v.Initializer.Value);
                    if (initExpr is Expression expr)
                    {
                        initializer = new JLeftPadded<Expression>(equalsPrefix, expr);
                    }
                }

                var namedVar = new NamedVariable(Guid.NewGuid(), varPrefix, Markers.Empty, name, [], initializer, _typeMapping?.VariableType(v));

                Space afterSpace = Space.Empty;
                if (i < node.Declaration.Variables.Count - 1)
                {
                    var sep = node.Declaration.Variables.GetSeparator(i);
                    afterSpace = ExtractSpaceBefore(sep);
                    _cursor = sep.Span.End;
                }

                variables.Add(new JRightPadded<NamedVariable>(namedVar, afterSpace, Markers.Empty));
            }

            var varDecl = new VariableDeclarations(Guid.NewGuid(), declPrefix, Markers.Empty, [], [], typeExpr, null, [], variables);
            init.Add(new JRightPadded<Statement>(varDecl, Space.Empty, Markers.Empty));
        }
        else
        {
            // Parse initializer expressions
            for (int i = 0; i < node.Initializers.Count; i++)
            {
                var initNode = node.Initializers[i];
                var initExpr = Visit(initNode);
                if (initExpr is Expression expr)
                {
                    var exprStmt = new ExpressionStatement(Guid.NewGuid(), expr);
                    Space afterSpace = Space.Empty;
                    if (i < node.Initializers.Count - 1)
                    {
                        var sep = node.Initializers.GetSeparator(i);
                        afterSpace = ExtractSpaceBefore(sep);
                        _cursor = sep.Span.End;
                    }
                    init.Add(new JRightPadded<Statement>(exprStmt, afterSpace, Markers.Empty));
                }
            }
        }

        // First semicolon
        var firstSemiPrefix = ExtractSpaceBefore(node.FirstSemicolonToken);
        _cursor = node.FirstSemicolonToken.Span.End;

        // Parse condition
        Expression? condExpr = null;
        if (node.Condition != null)
        {
            var visited = Visit(node.Condition);
            if (visited is Expression e) condExpr = e;
        }
        condExpr ??= new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty);

        // Second semicolon
        var secondSemiPrefix = ExtractSpaceBefore(node.SecondSemicolonToken);
        _cursor = node.SecondSemicolonToken.Span.End;

        // Parse incrementors
        var update = new List<JRightPadded<Statement>>();
        if (node.Incrementors.Count == 0)
        {
            // No incrementors - create an Empty statement with any trailing space in its prefix
            var emptyPrefix = ExtractSpaceBefore(node.CloseParenToken);
            var empty = new Empty(Guid.NewGuid(), emptyPrefix, Markers.Empty);
            update.Add(new JRightPadded<Statement>(empty, Space.Empty, Markers.Empty));
        }
        else
        {
            for (int i = 0; i < node.Incrementors.Count; i++)
            {
                var incNode = node.Incrementors[i];
                var incExpr = Visit(incNode);
                if (incExpr is Expression expr)
                {
                    var exprStmt = new ExpressionStatement(Guid.NewGuid(), expr);
                    Space afterSpace = Space.Empty;
                    if (i < node.Incrementors.Count - 1)
                    {
                        var sep = node.Incrementors.GetSeparator(i);
                        afterSpace = ExtractSpaceBefore(sep);
                        _cursor = sep.Span.End;
                    }
                    update.Add(new JRightPadded<Statement>(exprStmt, afterSpace, Markers.Empty));
                }
            }
        }

        // Close paren
        _cursor = node.CloseParenToken.Span.End;

        var control = new ForLoop.Control(
            Guid.NewGuid(),
            controlPrefix,
            Markers.Empty,
            init,
            new JRightPadded<Expression>(condExpr, secondSemiPrefix, Markers.Empty),
            update
        );

        // Parse body
        var bodyStmt = Visit(node.Statement);
        if (bodyStmt is not Statement bodyStatement)
        {
            throw new InvalidOperationException($"Expected Statement but got {bodyStmt?.GetType().Name}");
        }

        return new ForLoop(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            control,
            new JRightPadded<Statement>(bodyStatement, Space.Empty, Markers.Empty)
        );
    }

    public override J VisitForEachStatement(ForEachStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.ForEachKeyword.Span.End;

        // Parse open paren
        var controlPrefix = ExtractSpaceBefore(node.OpenParenToken);
        _cursor = node.OpenParenToken.Span.End;

        // Parse variable declaration
        var typeExpr = VisitType(node.Type);
        var namePrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var name = new Identifier(Guid.NewGuid(), namePrefix, Markers.Empty, node.Identifier.Text, null);
        var namedVar = new NamedVariable(Guid.NewGuid(), Space.Empty, Markers.Empty, name, [], null, _typeMapping?.VariableType(node));
        var varDecl = new VariableDeclarations(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Empty,
            [], [], typeExpr, null, [],
            [new JRightPadded<NamedVariable>(namedVar, Space.Empty, Markers.Empty)]
        );

        // Parse 'in' keyword
        var inPrefix = ExtractSpaceBefore(node.InKeyword);
        _cursor = node.InKeyword.Span.End;

        // Parse iterable expression
        var iterableExpr = Visit(node.Expression);
        if (iterableExpr is not Expression iterable)
        {
            throw new InvalidOperationException($"Expected Expression but got {iterableExpr?.GetType().Name}");
        }

        // Close paren
        var closeParenPrefix = ExtractSpaceBefore(node.CloseParenToken);
        _cursor = node.CloseParenToken.Span.End;

        var control = new ForEachLoop.Control(
            Guid.NewGuid(),
            controlPrefix,
            Markers.Empty,
            new JRightPadded<VariableDeclarations>(varDecl, inPrefix, Markers.Empty),
            new JRightPadded<Expression>(iterable, closeParenPrefix, Markers.Empty)
        );

        // Parse body
        var bodyStmt = Visit(node.Statement);
        if (bodyStmt is not Statement bodyStatement)
        {
            throw new InvalidOperationException($"Expected Statement but got {bodyStmt?.GetType().Name}");
        }

        return new ForEachLoop(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            control,
            new JRightPadded<Statement>(bodyStatement, Space.Empty, Markers.Empty)
        );
    }

    public override J VisitTryStatement(TryStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.TryKeyword.Span.End;

        // Parse try body
        var body = (Block)VisitBlock(node.Block);

        // Parse catch clauses
        var catches = new List<Try.Catch>();
        foreach (var catchClause in node.Catches)
        {
            var catchPrefix = ExtractSpaceBefore(catchClause.CatchKeyword);
            _cursor = catchClause.CatchKeyword.Span.End;

            ControlParentheses<VariableDeclarations>? parameter = null;
            if (catchClause.Declaration != null)
            {
                var parenPrefix = ExtractSpaceBefore(catchClause.Declaration.OpenParenToken);
                _cursor = catchClause.Declaration.OpenParenToken.Span.End;

                var typeExpr = VisitType(catchClause.Declaration.Type);

                Identifier? exName = null;
                if (catchClause.Declaration.Identifier != default)
                {
                    var exNamePrefix = ExtractSpaceBefore(catchClause.Declaration.Identifier);
                    _cursor = catchClause.Declaration.Identifier.Span.End;
                    exName = new Identifier(Guid.NewGuid(), exNamePrefix, Markers.Empty, catchClause.Declaration.Identifier.Text, null);
                }

                var varDecl = new VariableDeclarations(
                    Guid.NewGuid(),
                    Space.Empty,
                    Markers.Empty,
                    [], [], typeExpr, null, [],
                    exName != null
                        ? [new JRightPadded<NamedVariable>(new NamedVariable(Guid.NewGuid(), Space.Empty, Markers.Empty, exName, [], null, _typeMapping?.VariableType(catchClause.Declaration!)), Space.Empty, Markers.Empty)]
                        : []
                );

                var closeParenPrefix = ExtractSpaceBefore(catchClause.Declaration.CloseParenToken);
                _cursor = catchClause.Declaration.CloseParenToken.Span.End;

                parameter = new ControlParentheses<VariableDeclarations>(
                    Guid.NewGuid(),
                    parenPrefix,
                    Markers.Empty,
                    new JRightPadded<VariableDeclarations>(varDecl, closeParenPrefix, Markers.Empty)
                );
            }
            else
            {
                // Catch-all without declaration
                var emptyVarDecl = new VariableDeclarations(
                    Guid.NewGuid(), Space.Empty, Markers.Empty, [], [], null, null, [], []
                );
                parameter = new ControlParentheses<VariableDeclarations>(
                    Guid.NewGuid(),
                    Space.Empty,
                    Markers.Empty,
                    new JRightPadded<VariableDeclarations>(emptyVarDecl, Space.Empty, Markers.Empty)
                );
            }

            var catchBody = (Block)VisitBlock(catchClause.Block);

            catches.Add(new Try.Catch(
                Guid.NewGuid(),
                catchPrefix,
                Markers.Empty,
                parameter,
                catchBody
            ));
        }

        // Parse finally clause if present
        JLeftPadded<Block>? finallyBlock = null;
        if (node.Finally != null)
        {
            var finallyPrefix = ExtractSpaceBefore(node.Finally.FinallyKeyword);
            _cursor = node.Finally.FinallyKeyword.Span.End;
            var finallyBody = (Block)VisitBlock(node.Finally.Block);
            finallyBlock = new JLeftPadded<Block>(finallyPrefix, finallyBody);
        }

        return new Try(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            null,
            body,
            catches,
            finallyBlock
        );
    }

    public override J VisitThrowStatement(ThrowStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.ThrowKeyword.Span.End;

        Expression exception;
        if (node.Expression != null)
        {
            var expr = Visit(node.Expression);
            if (expr is not Expression e)
            {
                throw new InvalidOperationException($"Expected Expression but got {expr?.GetType().Name}");
            }
            exception = e;
        }
        else
        {
            // Re-throw (just 'throw;')
            exception = new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty);
        }

        // Consume the semicolon
        SkipTo(node.SemicolonToken.SpanStart);
        SkipToken(node.SemicolonToken);

        return new Throw(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            exception
        );
    }

    public override J VisitRefExpression(RefExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.RefKeyword.Span.End;

        var expr = Visit(node.Expression);
        if (expr is not Expression e)
        {
            throw new InvalidOperationException($"Expected Expression but got {expr?.GetType().Name}");
        }

        return new RefExpression(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            RefKind.Ref,
            e
        );
    }

    public override J VisitThrowExpression(ThrowExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.ThrowKeyword.Span.End;

        var expr = Visit(node.Expression);
        if (expr is not Expression e)
        {
            throw new InvalidOperationException($"Expected Expression but got {expr?.GetType().Name}");
        }

        var throwStatement = new Throw(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            e
        );

        return new StatementExpression(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Empty,
            throwStatement
        );
    }

    public override J VisitTypeOfExpression(TypeOfExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.Keyword.Span.End;

        // Skip the open paren
        SkipTo(node.OpenParenToken.SpanStart);
        SkipToken(node.OpenParenToken);

        var type = (J)VisitType(node.Type);

        // Skip the close paren
        SkipTo(node.CloseParenToken.SpanStart);
        SkipToken(node.CloseParenToken);

        return new InstanceOf(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JRightPadded<Expression>(new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty), Space.Empty, Markers.Empty),
            type,
            null,
            _typeMapping?.Type(node),
            null
        );
    }

    public override J VisitSizeOfExpression(SizeOfExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.Keyword.Span.End;

        // Skip the open paren
        SkipTo(node.OpenParenToken.SpanStart);
        SkipToken(node.OpenParenToken);

        var type = (Expression)VisitType(node.Type);

        // Skip the close paren
        SkipTo(node.CloseParenToken.SpanStart);
        SkipToken(node.CloseParenToken);

        return new SizeOf(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            type,
            _typeMapping?.Type(node)
        );
    }

    public override J VisitBreakStatement(BreakStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.BreakKeyword.Span.End;

        // Consume the semicolon
        SkipTo(node.SemicolonToken.SpanStart);
        SkipToken(node.SemicolonToken);

        return new Break(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            null // C# doesn't have labeled breaks
        );
    }

    public override J VisitContinueStatement(ContinueStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.ContinueKeyword.Span.End;

        // Consume the semicolon
        SkipTo(node.SemicolonToken.SpanStart);
        SkipToken(node.SemicolonToken);

        return new Continue(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            null // C# doesn't have labeled continues
        );
    }

    public override J VisitEmptyStatement(EmptyStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.SemicolonToken.Span.End;

        return new Empty(
            Guid.NewGuid(),
            prefix,
            Markers.Empty
        );
    }

    public override J VisitExpressionStatement(ExpressionStatementSyntax node)
    {
        var expr = Visit(node.Expression);
        if (expr is not Expression expression)
        {
            throw new InvalidOperationException($"Expected Expression but got {expr?.GetType().Name}");
        }

        // Consume the semicolon
        SkipTo(node.SemicolonToken.SpanStart);
        SkipToken(node.SemicolonToken);

        return new ExpressionStatement(Guid.NewGuid(), expression);
    }

    public override J VisitLiteralExpression(LiteralExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Standalone 'default' literal (C# 7.1+)
        if (node.Kind() == SyntaxKind.DefaultLiteralExpression)
        {
            _cursor = node.Token.Span.End;
            return new DefaultExpression(Guid.NewGuid(), prefix, Markers.Empty, null);
        }

        var valueSource = node.Token.Text;
        var value = node.Token.Value;

        // Skip past the literal token
        _cursor = node.Token.Span.End;

        var type = GetPrimitiveType(node.Kind());

        return new Literal(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            value,
            valueSource,
            null,
            type
        );
    }

    public override J VisitThisExpression(ThisExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.Token.Span.End;

        return new Identifier(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            node.Token.Text,
            _typeMapping?.Type(node)
        );
    }

    public override J VisitBaseExpression(BaseExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.Token.Span.End;

        return new Identifier(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            node.Token.Text,
            _typeMapping?.Type(node)
        );
    }

    public override J VisitInterpolatedStringExpression(InterpolatedStringExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Extract the delimiter from the start token (e.g., $", $@", @$", $""")
        var delimiter = node.StringStartToken.Text;
        _cursor = node.StringStartToken.Span.End;

        // Parse contents - mix of text and interpolations
        var parts = new List<J>();
        foreach (var content in node.Contents)
        {
            var visited = Visit(content);
            if (visited is J j)
            {
                parts.Add(j);
            }
        }

        // Skip the closing token
        _cursor = node.StringEndToken.Span.End;

        return new InterpolatedString(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            delimiter,
            parts
        );
    }

    public override J VisitInterpolatedStringText(InterpolatedStringTextSyntax node)
    {
        // Text portions are represented as J.Literal
        var prefix = ExtractPrefix(node);
        _cursor = node.TextToken.Span.End;

        return new Literal(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            node.TextToken.ValueText,  // Unescaped value
            node.TextToken.Text,       // Source text (with {{ and }})
            null,
            new JavaType.Primitive(JavaType.PrimitiveKind.String)
        );
    }

    public override J VisitInterpolation(InterpolationSyntax node)
    {
        // Move past the opening brace
        _cursor = node.OpenBraceToken.Span.End;

        // Parse the expression - its prefix will contain space after '{'
        var expression = Visit(node.Expression);
        if (expression is not Expression expr)
        {
            throw new InvalidOperationException($"Expected Expression but got {expression?.GetType().Name}");
        }

        // Parse optional alignment (after comma)
        JLeftPadded<Expression>? alignment = null;
        if (node.AlignmentClause != null)
        {
            var before = ExtractSpaceBefore(node.AlignmentClause.CommaToken);
            _cursor = node.AlignmentClause.CommaToken.Span.End;
            var alignExpr = Visit(node.AlignmentClause.Value);
            if (alignExpr is Expression ae)
            {
                alignment = new JLeftPadded<Expression>(before, ae);
            }
        }

        // Parse optional format specifier (after colon)
        JLeftPadded<Identifier>? format = null;
        if (node.FormatClause != null)
        {
            var before = ExtractSpaceBefore(node.FormatClause.ColonToken);
            _cursor = node.FormatClause.ColonToken.Span.End;
            var formatText = node.FormatClause.FormatStringToken.Text;
            // Format token follows immediately after colon, extract any space before it
            var formatStart = node.FormatClause.FormatStringToken.SpanStart;
            var formatPrefix = _cursor < formatStart
                ? Space.Format(_source[_cursor..formatStart])
                : Space.Empty;
            _cursor = node.FormatClause.FormatStringToken.Span.End;
            format = new JLeftPadded<Identifier>(
                before,
                new Identifier(Guid.NewGuid(), formatPrefix, Markers.Empty, formatText, null)
            );
        }

        // Space before closing brace
        var after = ExtractSpaceBefore(node.CloseBraceToken);
        _cursor = node.CloseBraceToken.Span.End;

        return new Interpolation(
            Guid.NewGuid(),
            Space.Empty,  // Space after '{' is stored in expression's prefix
            Markers.Empty,
            expr,
            alignment,
            format,
            after
        );
    }

    public override J VisitIdentifierName(IdentifierNameSyntax node)
    {
        var prefix = ExtractPrefix(node);
        var name = node.Identifier.Text;

        // Skip past the identifier token
        _cursor = node.Identifier.Span.End;

        return new Identifier(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            name,
            _typeMapping?.Type(node)
        );
    }

    public override J VisitPrefixUnaryExpression(PrefixUnaryExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.OperatorToken.Span.End;

        var opType = MapPrefixUnaryOperator(node.Kind());

        var operand = Visit(node.Operand);
        if (operand is not Expression expr)
        {
            throw new InvalidOperationException($"Expected Expression but got {operand?.GetType().Name}");
        }

        return new Unary(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JLeftPadded<Unary.OperatorType>(Space.Empty, opType),
            expr,
            _typeMapping?.Type(node)
        );
    }

    public override J VisitPostfixUnaryExpression(PostfixUnaryExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Null-forgiving operator: expr!
        if (node.IsKind(SyntaxKind.SuppressNullableWarningExpression))
        {
            var operand = Visit(node.Operand);
            if (operand is not Expression expr)
            {
                throw new InvalidOperationException($"Expected Expression but got {operand?.GetType().Name}");
            }

            var bangPrefix = ExtractSpaceBefore(node.OperatorToken);
            _cursor = node.OperatorToken.Span.End;

            return new NullSafeExpression(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                new JRightPadded<Expression>(expr, bangPrefix, Markers.Empty)
            );
        }

        var operand2 = Visit(node.Operand);
        if (operand2 is not Expression expr2)
        {
            throw new InvalidOperationException($"Expected Expression but got {operand2?.GetType().Name}");
        }

        var operatorPrefix = ExtractSpaceBefore(node.OperatorToken);
        _cursor = node.OperatorToken.Span.End;

        var opType = MapPostfixUnaryOperator(node.Kind());

        return new Unary(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JLeftPadded<Unary.OperatorType>(operatorPrefix, opType),
            expr2,
            _typeMapping?.Type(node)
        );
    }

    public override J VisitAwaitExpression(AwaitExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.AwaitKeyword.Span.End;

        var expression = Visit(node.Expression);
        if (expression is not Expression expr)
        {
            throw new InvalidOperationException($"Expected Expression but got {expression?.GetType().Name}");
        }

        return new AwaitExpression(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            expr
        );
    }

    private static Unary.OperatorType MapPrefixUnaryOperator(SyntaxKind kind)
    {
        return kind switch
        {
            SyntaxKind.PreIncrementExpression => Unary.OperatorType.PreIncrement,
            SyntaxKind.PreDecrementExpression => Unary.OperatorType.PreDecrement,
            SyntaxKind.UnaryPlusExpression => Unary.OperatorType.Positive,
            SyntaxKind.UnaryMinusExpression => Unary.OperatorType.Negative,
            SyntaxKind.BitwiseNotExpression => Unary.OperatorType.Complement,
            SyntaxKind.LogicalNotExpression => Unary.OperatorType.Not,
            _ => throw new InvalidOperationException($"Unsupported prefix unary operator: {kind}")
        };
    }

    private static Unary.OperatorType MapPostfixUnaryOperator(SyntaxKind kind)
    {
        return kind switch
        {
            SyntaxKind.PostIncrementExpression => Unary.OperatorType.PostIncrement,
            SyntaxKind.PostDecrementExpression => Unary.OperatorType.PostDecrement,
            _ => throw new InvalidOperationException($"Unsupported postfix unary operator: {kind}")
        };
    }

    public override J VisitBinaryExpression(BinaryExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Visit left operand
        var left = Visit(node.Left);
        if (left is not Expression leftExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {left?.GetType().Name}");
        }

        // Extract space before the operator
        var operatorPrefix = ExtractSpaceBefore(node.OperatorToken);

        // Skip past the operator token
        _cursor = node.OperatorToken.Span.End;

        // Handle null-coalescing as Ternary with NullCoalescing marker
        if (node.Kind() == SyntaxKind.CoalesceExpression)
        {
            // Visit right operand (the fallback value)
            var right = Visit(node.Right);
            if (right is not Expression rightExpr)
            {
                throw new InvalidOperationException($"Expected Expression but got {right?.GetType().Name}");
            }

            // Model as Ternary with NullCoalescing marker
            // condition ?? falsePart (truePart is empty)
            return new Ternary(
                Guid.NewGuid(),
                prefix,
                Markers.Build([NullCoalescing.Instance]),
                leftExpr,
                new JLeftPadded<Expression>(Space.Empty, new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty)),
                new JLeftPadded<Expression>(operatorPrefix, rightExpr),
                _typeMapping?.Type(node)
            );
        }

        // Map the operator
        var opType = MapBinaryOperator(node.Kind());

        // Visit right operand
        var right2 = Visit(node.Right);
        if (right2 is not Expression rightExpr2)
        {
            throw new InvalidOperationException($"Expected Expression but got {right2?.GetType().Name}");
        }

        return new Binary(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            leftExpr,
            new JLeftPadded<Binary.OperatorType>(operatorPrefix, opType),
            rightExpr2,
            _typeMapping?.Type(node)
        );
    }

    public override J VisitConditionalExpression(ConditionalExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Visit condition
        var condition = Visit(node.Condition);
        if (condition is not Expression conditionExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {condition?.GetType().Name}");
        }

        // Extract space before ? and skip it
        var questionSpace = ExtractSpaceBefore(node.QuestionToken);
        _cursor = node.QuestionToken.Span.End;

        // Visit true part (when true)
        var truePart = Visit(node.WhenTrue);
        if (truePart is not Expression trueExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {truePart?.GetType().Name}");
        }

        // Extract space before : and skip it
        var colonSpace = ExtractSpaceBefore(node.ColonToken);
        _cursor = node.ColonToken.Span.End;

        // Visit false part (when false)
        var falsePart = Visit(node.WhenFalse);
        if (falsePart is not Expression falseExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {falsePart?.GetType().Name}");
        }

        return new Ternary(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            conditionExpr,
            new JLeftPadded<Expression>(questionSpace, trueExpr),
            new JLeftPadded<Expression>(colonSpace, falseExpr),
            _typeMapping?.Type(node)
        );
    }

    public override J VisitAssignmentExpression(AssignmentExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Visit left operand (the variable being assigned)
        var left = Visit(node.Left);
        if (left is not Expression leftExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {left?.GetType().Name}");
        }

        // Extract space before the operator
        var operatorPrefix = ExtractSpaceBefore(node.OperatorToken);

        // Skip past the operator token
        _cursor = node.OperatorToken.Span.End;

        // Visit right operand (the value being assigned)
        var right = Visit(node.Right);
        if (right is not Expression rightExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {right?.GetType().Name}");
        }

        // Simple assignment (=) vs compound assignment (+=, -=, etc.)
        if (node.Kind() == SyntaxKind.SimpleAssignmentExpression)
        {
            return new Assignment(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                leftExpr,
                new JLeftPadded<Expression>(operatorPrefix, rightExpr),
                _typeMapping?.Type(node)
            );
        }
        else
        {
            // Compound assignment
            var opType = MapAssignmentOperator(node.Kind());
            return new AssignmentOperation(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                leftExpr,
                new JLeftPadded<AssignmentOperation.OperatorType>(operatorPrefix, opType),
                rightExpr,
                _typeMapping?.Type(node)
            );
        }
    }

    private static AssignmentOperation.OperatorType MapAssignmentOperator(SyntaxKind kind)
    {
        return kind switch
        {
            SyntaxKind.AddAssignmentExpression => AssignmentOperation.OperatorType.Addition,
            SyntaxKind.SubtractAssignmentExpression => AssignmentOperation.OperatorType.Subtraction,
            SyntaxKind.MultiplyAssignmentExpression => AssignmentOperation.OperatorType.Multiplication,
            SyntaxKind.DivideAssignmentExpression => AssignmentOperation.OperatorType.Division,
            SyntaxKind.ModuloAssignmentExpression => AssignmentOperation.OperatorType.Modulo,
            SyntaxKind.AndAssignmentExpression => AssignmentOperation.OperatorType.BitAnd,
            SyntaxKind.OrAssignmentExpression => AssignmentOperation.OperatorType.BitOr,
            SyntaxKind.ExclusiveOrAssignmentExpression => AssignmentOperation.OperatorType.BitXor,
            SyntaxKind.LeftShiftAssignmentExpression => AssignmentOperation.OperatorType.LeftShift,
            SyntaxKind.RightShiftAssignmentExpression => AssignmentOperation.OperatorType.RightShift,
            SyntaxKind.UnsignedRightShiftAssignmentExpression => AssignmentOperation.OperatorType.UnsignedRightShift,
            SyntaxKind.CoalesceAssignmentExpression => AssignmentOperation.OperatorType.Coalesce,
            _ => throw new InvalidOperationException($"Unsupported assignment operator: {kind}")
        };
    }

    public override J VisitParenthesizedExpression(ParenthesizedExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Skip past the open paren
        _cursor = node.OpenParenToken.Span.End;

        // Visit the inner expression
        var inner = Visit(node.Expression);
        if (inner is not Expression innerExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {inner?.GetType().Name}");
        }

        // Extract space before the close paren
        var closeParenPrefix = ExtractSpaceBefore(node.CloseParenToken);

        // Skip past the close paren
        _cursor = node.CloseParenToken.Span.End;

        return new Parentheses<Expression>(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JRightPadded<Expression>(innerExpr, closeParenPrefix, Markers.Empty)
        );
    }

    public override J VisitTupleExpression(TupleExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Skip past the open paren
        _cursor = node.OpenParenToken.Span.End;

        var args = new List<JRightPadded<Expression>>();
        for (int i = 0; i < node.Arguments.Count; i++)
        {
            var arg = node.Arguments[i];

            Expression expr;
            if (arg.NameColon != null)
            {
                // Named tuple element: name: value
                var namePrefix = ExtractSpaceBefore(arg.NameColon.Name.Identifier);
                _cursor = arg.NameColon.Name.Identifier.Span.End;

                var nameIdentifier = new Identifier(
                    Guid.NewGuid(),
                    namePrefix,
                    Markers.Empty,
                    arg.NameColon.Name.Identifier.Text,
                    null
                );

                // Space before the colon
                var colonSpace = ExtractSpaceBefore(arg.NameColon.ColonToken);
                _cursor = arg.NameColon.ColonToken.Span.End;

                // Parse the value expression
                var valueExpr = Visit(arg.Expression);
                if (valueExpr is not Expression value)
                {
                    throw new InvalidOperationException($"Expected Expression but got {valueExpr?.GetType().Name}");
                }

                expr = new NamedExpression(
                    Guid.NewGuid(),
                    Space.Empty,
                    Markers.Empty,
                    new JRightPadded<Identifier>(nameIdentifier, colonSpace, Markers.Empty),
                    value
                );
            }
            else
            {
                // Unnamed element
                var argExpr = Visit(arg.Expression);
                if (argExpr is not Expression e)
                {
                    throw new InvalidOperationException($"Expected Expression but got {argExpr?.GetType().Name}");
                }
                expr = e;
            }

            Space afterSpace = Space.Empty;
            if (i < node.Arguments.Count - 1)
            {
                var separatorToken = node.Arguments.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(separatorToken);
                _cursor = separatorToken.Span.End;
            }
            else
            {
                afterSpace = ExtractSpaceBefore(node.CloseParenToken);
            }

            args.Add(new JRightPadded<Expression>(expr, afterSpace, Markers.Empty));
        }

        _cursor = node.CloseParenToken.Span.End;

        return new TupleExpression(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JContainer<Expression>(Space.Empty, args, Markers.Empty)
        );
    }

    public override J VisitCastExpression(CastExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse the (type) part
        var openParenPrefix = ExtractSpaceBefore(node.OpenParenToken);
        _cursor = node.OpenParenToken.Span.End;

        var typeTree = VisitType(node.Type);
        if (typeTree is not TypeTree type)
        {
            throw new InvalidOperationException($"Expected TypeTree but got {typeTree?.GetType().Name}");
        }

        var closeParenPrefix = ExtractSpaceBefore(node.CloseParenToken);
        _cursor = node.CloseParenToken.Span.End;

        var clazz = new ControlParentheses<TypeTree>(
            Guid.NewGuid(),
            openParenPrefix,
            Markers.Empty,
            new JRightPadded<TypeTree>(type, closeParenPrefix, Markers.Empty)
        );

        // Parse the expression being cast
        var expr = Visit(node.Expression);
        if (expr is not Expression expression)
        {
            throw new InvalidOperationException($"Expected Expression but got {expr?.GetType().Name}");
        }

        return new TypeCast(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            clazz,
            expression
        );
    }

    public override J VisitElementAccessExpression(ElementAccessExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Visit the expression being indexed
        var indexed = Visit(node.Expression);
        if (indexed is not Expression indexedExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {indexed?.GetType().Name}");
        }

        // Parse the bracketed argument list (handles single and multi-dimensional)
        return ParseBracketedArgumentList(node.ArgumentList, prefix, indexedExpr);
    }

    private ArrayAccess ParseBracketedArgumentList(BracketedArgumentListSyntax argList, Space prefix, Expression indexed)
    {
        // Space before the '['
        var bracketPrefix = ExtractSpaceBefore(argList.OpenBracketToken);
        _cursor = argList.OpenBracketToken.Span.End;

        if (argList.Arguments.Count == 0)
        {
            throw new InvalidOperationException("Element access with no indices");
        }

        if (argList.Arguments.Count == 1)
        {
            // Single-index access: arr[i]
            var arg = argList.Arguments[0];
            var indexExpr = Visit(arg.Expression);
            if (indexExpr is not Expression index)
            {
                throw new InvalidOperationException($"Expected Expression but got {indexExpr?.GetType().Name}");
            }

            var closeBracketSpace = ExtractSpaceBefore(argList.CloseBracketToken);
            _cursor = argList.CloseBracketToken.Span.End;

            return new ArrayAccess(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                indexed,
                new ArrayDimension(
                    Guid.NewGuid(),
                    bracketPrefix,
                    Markers.Empty,
                    new JRightPadded<Expression>(index, closeBracketSpace, Markers.Empty)
                ),
                _typeMapping?.Type(argList.Parent!)
            );
        }

        // Multi-dimensional access: matrix[i, j, k, ...]
        // Model as nested ArrayAccess with MultiDimensionalArray marker on outer ones
        // Structure: ArrayAccess(marker) -> ArrayAccess(marker) -> ... -> ArrayAccess(no marker)

        // Build from innermost to outermost
        // First index creates the innermost ArrayAccess (no marker)
        var firstArg = argList.Arguments[0];
        var firstIndex = Visit(firstArg.Expression);
        if (firstIndex is not Expression firstIndexExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {firstIndex?.GetType().Name}");
        }

        // Space after first index, before first comma
        var firstSeparator = argList.Arguments.GetSeparator(0);
        var currentSpaceBeforeComma = ExtractSpaceBefore(firstSeparator);
        _cursor = firstSeparator.Span.End;

        // Innermost ArrayAccess (no marker)
        // Its dimension's After is empty because comma follows, not ]
        ArrayAccess current = new ArrayAccess(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            indexed,
            new ArrayDimension(
                Guid.NewGuid(),
                bracketPrefix,
                Markers.Empty,
                new JRightPadded<Expression>(firstIndexExpr, Space.Empty, Markers.Empty)
            ),
            null
        );

        // Wrap with subsequent indices, each with MultiDimensionalArray marker
        for (int i = 1; i < argList.Arguments.Count; i++)
        {
            var arg = argList.Arguments[i];
            var indexExpr = Visit(arg.Expression);
            if (indexExpr is not Expression index)
            {
                throw new InvalidOperationException($"Expected Expression but got {indexExpr?.GetType().Name}");
            }

            bool isLastIndex = i == argList.Arguments.Count - 1;

            if (!isLastIndex)
            {
                // Not the last index - there's another comma after
                var separator = argList.Arguments.GetSeparator(i);
                var nextSpaceBeforeComma = ExtractSpaceBefore(separator);
                _cursor = separator.Span.End;

                current = new ArrayAccess(
                    Guid.NewGuid(),
                    currentSpaceBeforeComma, // Prefix = space before this comma
                    Markers.Empty.Add(MultiDimensionalArray.Instance),
                    current,
                    new ArrayDimension(
                        Guid.NewGuid(),
                        Space.Empty, // Space after comma is in index expression prefix
                        Markers.Empty,
                        new JRightPadded<Expression>(index, Space.Empty, Markers.Empty)
                    ),
                    null
                );
                currentSpaceBeforeComma = nextSpaceBeforeComma;
            }
            else
            {
                // Last index - ] follows
                var closeBracketSpace = ExtractSpaceBefore(argList.CloseBracketToken);
                _cursor = argList.CloseBracketToken.Span.End;

                current = new ArrayAccess(
                    Guid.NewGuid(),
                    currentSpaceBeforeComma, // Prefix = space before this comma
                    Markers.Empty.Add(MultiDimensionalArray.Instance),
                    current,
                    new ArrayDimension(
                        Guid.NewGuid(),
                        Space.Empty, // Space after comma is in index expression prefix
                        Markers.Empty,
                        new JRightPadded<Expression>(index, closeBracketSpace, Markers.Empty)
                    ),
                    _typeMapping?.Type(argList.Parent!)
                );
            }
        }

        return current;
    }

    public override J VisitMemberAccessExpression(MemberAccessExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Visit the target expression (left side of the dot)
        var target = Visit(node.Expression);
        if (target is not Expression targetExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {target?.GetType().Name}");
        }

        // Space before the dot
        var dotPrefix = ExtractSpaceBefore(node.OperatorToken);
        _cursor = node.OperatorToken.Span.End;

        // Parse the member name
        Identifier name;
        if (node.Name is GenericNameSyntax genericName)
        {
            // Generic member access (e.g., foo.Bar<T>) — in non-invocation context
            // Parse the name and advance past type arguments
            var namePrefix = ExtractSpaceBefore(genericName.Identifier);
            _cursor = genericName.Identifier.Span.End;
            name = new Identifier(
                Guid.NewGuid(),
                namePrefix,
                Markers.Empty,
                genericName.Identifier.Text,
                null
            );
            // Advance cursor past the type argument list
            _cursor = genericName.TypeArgumentList.Span.End;
        }
        else
        {
            // SimpleNameSyntax (non-generic)
            var simpleName = (SimpleNameSyntax)node.Name;
            var namePrefix = ExtractSpaceBefore(simpleName.Identifier);
            _cursor = simpleName.Identifier.Span.End;
            name = new Identifier(
                Guid.NewGuid(),
                namePrefix,
                Markers.Empty,
                simpleName.Identifier.Text,
                null
            );
        }

        if (node.OperatorToken.Kind() == SyntaxKind.MinusGreaterThanToken)
        {
            return new PointerFieldAccess(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                targetExpr,
                new JLeftPadded<Identifier>(dotPrefix, name),
                _typeMapping?.Type(node)
            );
        }

        return new FieldAccess(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            targetExpr,
            new JLeftPadded<Identifier>(dotPrefix, name),
            _typeMapping?.Type(node)
        );
    }

    public override J VisitInvocationExpression(InvocationExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        JRightPadded<Expression>? select = null;
        Identifier name;
        JContainer<Expression>? typeParameters = null;

        // Parse the expression being invoked
        if (node.Expression is MemberAccessExpressionSyntax memberAccess)
        {
            // Qualified call: foo.Bar() or foo.Bar<T>()
            var target = Visit(memberAccess.Expression);
            if (target is not Expression targetExpr)
            {
                throw new InvalidOperationException($"Expected Expression but got {target?.GetType().Name}");
            }

            // Space before the dot
            var dotPrefix = ExtractSpaceBefore(memberAccess.OperatorToken);
            _cursor = memberAccess.OperatorToken.Span.End;

            select = new JRightPadded<Expression>(targetExpr, dotPrefix, Markers.Empty);

            // Parse the method name (possibly generic)
            if (memberAccess.Name is GenericNameSyntax genericName)
            {
                var namePrefix = ExtractSpaceBefore(genericName.Identifier);
                _cursor = genericName.Identifier.Span.End;
                name = new Identifier(
                    Guid.NewGuid(),
                    namePrefix,
                    Markers.Empty,
                    genericName.Identifier.Text,
                    null
                );
                typeParameters = ParseTypeArgumentList(genericName.TypeArgumentList);
            }
            else
            {
                // SimpleNameSyntax (non-generic)
                var simpleName = (SimpleNameSyntax)memberAccess.Name;
                var namePrefix = ExtractSpaceBefore(simpleName.Identifier);
                _cursor = simpleName.Identifier.Span.End;
                name = new Identifier(
                    Guid.NewGuid(),
                    namePrefix,
                    Markers.Empty,
                    simpleName.Identifier.Text,
                    null
                );
            }
        }
        else if (node.Expression is GenericNameSyntax genericName)
        {
            // Unqualified generic call: Bar<T>()
            var namePrefix = ExtractSpaceBefore(genericName.Identifier);
            _cursor = genericName.Identifier.Span.End;
            name = new Identifier(
                Guid.NewGuid(),
                namePrefix,
                Markers.Empty,
                genericName.Identifier.Text,
                null
            );
            typeParameters = ParseTypeArgumentList(genericName.TypeArgumentList);
        }
        else if (node.Expression is IdentifierNameSyntax identifierName)
        {
            // Unqualified call: Bar()
            var namePrefix = ExtractSpaceBefore(identifierName.Identifier);
            _cursor = identifierName.Identifier.Span.End;
            name = new Identifier(
                Guid.NewGuid(),
                namePrefix,
                Markers.Empty,
                identifierName.Identifier.Text,
                null
            );
        }
        else
        {
            // Delegate/lambda invocation: expr() where expr is an arbitrary expression
            // Model as MethodInvocation with Name="Invoke" and DelegateInvocation marker
            // Semantically, action() is sugar for action.Invoke()
            var target = Visit(node.Expression);
            if (target is not Expression targetExpr)
            {
                throw new InvalidOperationException($"Expected Expression but got {target?.GetType().Name}");
            }

            var delegateArgs = ParseArgumentList(node.ArgumentList);

            // Create MethodInvocation with DelegateInvocation marker to indicate syntactic sugar
            return new MethodInvocation(
                Guid.NewGuid(),
                prefix,
                Markers.Build([DelegateInvocation.Instance]),
                new JRightPadded<Expression>(targetExpr, Space.Empty, Markers.Empty),
                new Identifier(Guid.NewGuid(), Space.Empty, Markers.Empty, "Invoke", null),
                null,
                delegateArgs,
                _typeMapping?.MethodType(node)
            );
        }

        // Parse arguments for method invocation
        var arguments = ParseArgumentList(node.ArgumentList);

        return new MethodInvocation(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            select,
            name,
            typeParameters,
            arguments,
            _typeMapping?.MethodType(node)
        );
    }

    public override J VisitSimpleLambdaExpression(SimpleLambdaExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse modifiers (async, static)
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(),
                modPrefix,
                Markers.Empty,
                MapModifier(mod.Kind()),
                []
            ));
        }

        // Single parameter without parentheses: x => ...
        var param = ConvertLambdaParameter(node.Parameter);
        var paramElement = new JRightPadded<J>(param, Space.Empty, Markers.Empty);

        var parameters = new Lambda.Parameters(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Empty,
            false, // Not parenthesized
            [paramElement]
        );

        // Space before =>
        var arrowSpace = ExtractSpaceBefore(node.ArrowToken);
        _cursor = node.ArrowToken.Span.End;

        // Parse body
        var body = Visit(node.Body);
        if (body is not J bodyJ)
        {
            throw new InvalidOperationException($"Expected J for lambda body but got {body?.GetType().Name}");
        }

        var jLambda = new Lambda(
            Guid.NewGuid(),
            modifiers.Count > 0 ? Space.Empty : prefix, // If modifiers, prefix goes on CsLambda
            Markers.Empty,
            parameters,
            arrowSpace,
            bodyJ,
            _typeMapping?.Type(node)
        );

        // Wrap in CsLambda if we have modifiers
        if (modifiers.Count > 0)
        {
            return new CsLambda(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                modifiers,
                null, // No return type for simple lambdas
                jLambda
            );
        }

        return jLambda;
    }

    public override J VisitParenthesizedLambdaExpression(ParenthesizedLambdaExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse modifiers (async, static)
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(),
                modPrefix,
                Markers.Empty,
                MapModifier(mod.Kind()),
                []
            ));
        }

        // Parse optional return type (C# 10+)
        TypeTree? returnType = null;
        if (node.ReturnType != null)
        {
            returnType = VisitType(node.ReturnType);
        }

        // Capture space before open paren - this becomes Lambda.Prefix when there are modifiers/return type
        var lambdaPrefix = (modifiers.Count > 0 || returnType != null)
            ? ExtractSpaceBefore(node.ParameterList.OpenParenToken)
            : Space.Empty;

        // Skip open paren - it's implied by Parenthesized=true
        _cursor = node.ParameterList.OpenParenToken.Span.End;

        // For empty parens, Prefix is the space inside (after '(' and before ')')
        // For non-empty parens, Prefix is space before first element
        var paramsPrefix = Space.Empty;

        var elements = new List<JRightPadded<J>>();
        for (int i = 0; i < node.ParameterList.Parameters.Count; i++)
        {
            var p = node.ParameterList.Parameters[i];
            var param = ConvertLambdaParameter(p);

            Space afterSpace;
            if (i < node.ParameterList.Parameters.Count - 1)
            {
                var separatorToken = node.ParameterList.Parameters.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(separatorToken);
                _cursor = separatorToken.Span.End;
            }
            else
            {
                afterSpace = ExtractSpaceBefore(node.ParameterList.CloseParenToken);
            }

            elements.Add(new JRightPadded<J>(param, afterSpace, Markers.Empty));
        }

        // If no parameters, capture the space inside the parens
        if (elements.Count == 0)
        {
            paramsPrefix = ExtractSpaceBefore(node.ParameterList.CloseParenToken);
        }

        _cursor = node.ParameterList.CloseParenToken.Span.End;

        var parameters = new Lambda.Parameters(
            Guid.NewGuid(),
            paramsPrefix,
            Markers.Empty,
            true, // Parenthesized
            elements
        );

        // Space before =>
        var arrowSpace = ExtractSpaceBefore(node.ArrowToken);
        _cursor = node.ArrowToken.Span.End;

        // Parse body
        var body = Visit(node.Body);
        if (body is not J bodyJ)
        {
            throw new InvalidOperationException($"Expected J for lambda body but got {body?.GetType().Name}");
        }

        var jLambda = new Lambda(
            Guid.NewGuid(),
            (modifiers.Count > 0 || returnType != null) ? lambdaPrefix : prefix, // If modifiers/returnType, prefix is space before '('
            Markers.Empty,
            parameters,
            arrowSpace,
            bodyJ,
            _typeMapping?.Type(node)
        );

        // Wrap in CsLambda if we have modifiers or return type
        if (modifiers.Count > 0 || returnType != null)
        {
            return new CsLambda(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                modifiers,
                returnType,
                jLambda
            );
        }

        return jLambda;
    }

    /// <summary>
    /// Converts a lambda parameter. For lambdas, parameters can be:
    /// - Just an identifier (type inferred): x
    /// - Typed: int x
    /// - With modifiers: ref int x
    /// </summary>
    private J ConvertLambdaParameter(ParameterSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // If parameter has no type, return just an identifier
        if (node.Type == null)
        {
            _cursor = node.Identifier.Span.End;
            return new Identifier(
                Guid.NewGuid(),
                prefix, // Use the param prefix as identifier prefix
                Markers.Empty,
                node.Identifier.Text,
                null
            );
        }

        // Otherwise, return a VariableDeclarations (same as method parameters)
        // Parse modifiers (ref, out, params, etc.)
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(),
                modPrefix,
                Markers.Empty,
                MapModifier(mod.Kind()),
                []
            ));
        }

        // Parse the type
        var typeExpr = VisitType(node.Type);

        // Parse the parameter name
        var namePrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var name = new Identifier(
            Guid.NewGuid(),
            namePrefix,
            Markers.Empty,
            node.Identifier.Text,
            null
        );

        // Parse default value if present (C# 12+ for lambdas)
        JLeftPadded<Expression>? initializer = null;
        if (node.Default != null)
        {
            var equalsPrefix = ExtractSpaceBefore(node.Default.EqualsToken);
            _cursor = node.Default.EqualsToken.Span.End;

            var initExpr = Visit(node.Default.Value);
            if (initExpr is Expression expr)
            {
                initializer = new JLeftPadded<Expression>(equalsPrefix, expr);
            }
        }

        var namedVar = new NamedVariable(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Empty,
            name,
            [],
            initializer,
            _typeMapping?.VariableType(node)
        );

        return new VariableDeclarations(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            [],
            modifiers,
            typeExpr,
            null,
            [],
            [new JRightPadded<NamedVariable>(namedVar, Space.Empty, Markers.Empty)]
        );
    }

    public override J VisitObjectCreationExpression(ObjectCreationExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Skip 'new' keyword
        _cursor = node.NewKeyword.Span.End;

        // Space after 'new' keyword (before type)
        var newSpace = ExtractPrefix(node.Type);

        // Parse the type being constructed
        var clazz = VisitType(node.Type);

        // Parse arguments if present
        JContainer<Expression> arguments;
        if (node.ArgumentList != null)
        {
            arguments = ParseArgumentList(node.ArgumentList);
        }
        else
        {
            // No argument list (e.g., new Foo { X = 1 } without parens)
            // Use empty container with OmitParentheses marker
            arguments = new JContainer<Expression>(
                Space.Empty,
                [new JRightPadded<Expression>(
                    new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty),
                    Space.Empty,
                    Markers.Empty
                )],
                Markers.Build([OmitParentheses.Instance])
            );
        }

        // Parse initializer if present (e.g., new Foo { X = 1, Y = 2 })
        Block? body = null;
        if (node.Initializer != null)
        {
            body = WrapInitializerAsBlock(node.Initializer);
        }

        return new NewClass(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            null,  // enclosing
            newSpace,
            clazz,
            arguments,
            body,
            _typeMapping?.MethodType(node)
        );
    }

    public override J VisitImplicitObjectCreationExpression(ImplicitObjectCreationExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Skip 'new' keyword
        _cursor = node.NewKeyword.Span.End;

        // No type name for implicit object creation (e.g., `new()`)
        // Space between 'new' and '(' is captured by ParseArgumentList's argsPrefix
        var arguments = ParseArgumentList(node.ArgumentList);

        // Parse initializer if present
        Block? body = null;
        if (node.Initializer != null)
        {
            body = WrapInitializerAsBlock(node.Initializer);
        }

        return new NewClass(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            null,  // enclosing
            Space.Empty,  // no type between new and args
            null,  // clazz (implicit type)
            arguments,
            null,  // body
            _typeMapping?.MethodType(node)
        );
    }

    public override J VisitArrayCreationExpression(ArrayCreationExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Skip 'new' keyword
        _cursor = node.NewKeyword.Span.End;

        // Parse the array type - this includes the element type and rank specifiers
        // For "new int[10]", the type is "int[10]" (ArrayTypeSyntax)
        // We need to extract the element type and the dimensions separately

        // Parse element type (without the array rank specifiers)
        // VisitType will extract the prefix (space after 'new')
        TypeTree? typeExpression = null;
        if (node.Type.ElementType != null)
        {
            typeExpression = VisitType(node.Type.ElementType);
        }

        // Parse dimensions from rank specifiers
        var dimensions = new List<ArrayDimension>();
        foreach (var rankSpec in node.Type.RankSpecifiers)
        {
            // Get space before [
            var bracketPrefix = ExtractSpaceBefore(rankSpec.OpenBracketToken);
            _cursor = rankSpec.OpenBracketToken.Span.End;

            // Parse sizes if present (e.g., [10] or [2, 3])
            if (rankSpec.Sizes.Count > 0)
            {
                for (int i = 0; i < rankSpec.Sizes.Count; i++)
                {
                    var size = rankSpec.Sizes[i];

                    Space dimPrefix;
                    if (i == 0)
                    {
                        dimPrefix = bracketPrefix;
                    }
                    else
                    {
                        // Get space before comma (already parsed, just get space after)
                        dimPrefix = ExtractPrefix(size);
                    }

                    // Check if this is an OmittedArraySizeExpressionSyntax (e.g., new int[])
                    if (size is OmittedArraySizeExpressionSyntax)
                    {
                        // Empty dimension for unsized array
                        var afterSpace = ExtractSpaceBefore(rankSpec.CloseBracketToken);

                        dimensions.Add(new ArrayDimension(
                            Guid.NewGuid(),
                            dimPrefix,
                            Markers.Empty,
                            new JRightPadded<Expression>(
                                new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty),
                                afterSpace,
                                Markers.Empty
                            )
                        ));
                    }
                    else
                    {
                        var sizeExpr = Visit(size);
                        if (sizeExpr is not Expression expr)
                        {
                            throw new InvalidOperationException($"Expected Expression but got {sizeExpr?.GetType().Name}");
                        }

                        // Get space after this expression (before comma or ])
                        Space afterSpace;
                        if (i < rankSpec.Sizes.Count - 1)
                        {
                            // Space before the comma
                            afterSpace = ExtractSpaceBefore(rankSpec.Sizes.GetSeparator(i));
                            _cursor = rankSpec.Sizes.GetSeparator(i).Span.End;
                        }
                        else
                        {
                            // Space before the ]
                            afterSpace = ExtractSpaceBefore(rankSpec.CloseBracketToken);
                        }

                        dimensions.Add(new ArrayDimension(
                            Guid.NewGuid(),
                            dimPrefix,
                            Markers.Empty,
                            new JRightPadded<Expression>(expr, afterSpace, Markers.Empty)
                        ));
                    }
                }
            }
            else
            {
                // Empty rank specifier like int[]
                var afterSpace = ExtractSpaceBefore(rankSpec.CloseBracketToken);
                dimensions.Add(new ArrayDimension(
                    Guid.NewGuid(),
                    bracketPrefix,
                    Markers.Empty,
                    new JRightPadded<Expression>(
                        new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty),
                        afterSpace,
                        Markers.Empty
                    )
                ));
            }

            _cursor = rankSpec.CloseBracketToken.Span.End;
        }

        // Parse initializer if present: { 1, 2, 3 }
        JContainer<Expression>? initializer = null;
        if (node.Initializer != null)
        {
            initializer = ParseArrayInitializer(node.Initializer);
        }

        return new NewArray(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            typeExpression,
            dimensions,
            initializer,
            _typeMapping?.Type(node)
        );
    }

    public override J VisitImplicitArrayCreationExpression(ImplicitArrayCreationExpressionSyntax node)
    {
        // new[] { 1, 2, 3 } - implicitly typed array
        var prefix = ExtractPrefix(node);

        // Skip 'new' keyword
        _cursor = node.NewKeyword.Span.End;

        // Parse the brackets (which have no type)
        var bracketPrefix = ExtractSpaceBefore(node.OpenBracketToken);
        _cursor = node.OpenBracketToken.Span.End;

        var innerSpace = ExtractSpaceBefore(node.CloseBracketToken);
        _cursor = node.CloseBracketToken.Span.End;

        var dimensions = new List<ArrayDimension>
        {
            new ArrayDimension(
                Guid.NewGuid(),
                bracketPrefix,
                Markers.Empty,
                new JRightPadded<Expression>(
                    new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty),
                    innerSpace,
                    Markers.Empty
                )
            )
        };

        // Parse initializer
        var initializer = ParseArrayInitializer(node.Initializer);

        return new NewArray(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            null,  // No type expression for implicit arrays
            dimensions,
            initializer,
            _typeMapping?.Type(node)
        );
    }

    private JContainer<Expression> ParseArrayInitializer(InitializerExpressionSyntax initializer)
    {
        var openBracePrefix = ExtractSpaceBefore(initializer.OpenBraceToken);
        _cursor = initializer.OpenBraceToken.Span.End;

        var elements = new List<JRightPadded<Expression>>();
        for (int i = 0; i < initializer.Expressions.Count; i++)
        {
            var expr = initializer.Expressions[i];
            var element = Visit(expr);
            if (element is not Expression elementExpr)
            {
                throw new InvalidOperationException($"Expected Expression but got {element?.GetType().Name}");
            }

            Space afterSpace;
            if (i < initializer.Expressions.Count - 1)
            {
                afterSpace = ExtractSpaceBefore(initializer.Expressions.GetSeparator(i));
                _cursor = initializer.Expressions.GetSeparator(i).Span.End;
            }
            else
            {
                afterSpace = ExtractSpaceBefore(initializer.CloseBraceToken);
            }

            elements.Add(new JRightPadded<Expression>(elementExpr, afterSpace, Markers.Empty));
        }

        _cursor = initializer.CloseBraceToken.Span.End;

        return new JContainer<Expression>(openBracePrefix, elements, Markers.Empty);
    }

    public override J VisitConditionalAccessExpression(ConditionalAccessExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse the target expression (the thing being conditionally accessed)
        var target = Visit(node.Expression);
        if (target is not Expression targetExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {target?.GetType().Name}");
        }

        // Space before the ?. operator
        var operatorSpace = ExtractSpaceBefore(node.OperatorToken);
        _cursor = node.OperatorToken.Span.End;

        // The WhenNotNull part determines what we're accessing
        if (node.WhenNotNull is InvocationExpressionSyntax invocation)
        {
            // x?.Bar() - method invocation with null-safe access
            if (invocation.Expression is MemberBindingExpressionSyntax memberBinding)
            {
                // Skip the . in the member binding (it's part of ?.)
                _cursor = memberBinding.OperatorToken.Span.End;

                // Parse the method name with NullSafe marker
                Identifier name;
                JContainer<Expression>? typeParameters = null;

                if (memberBinding.Name is GenericNameSyntax genericName)
                {
                    var namePrefix = ExtractSpaceBefore(genericName.Identifier);
                    _cursor = genericName.Identifier.Span.End;
                    name = new Identifier(
                        Guid.NewGuid(),
                        namePrefix,
                        Markers.Build([NullSafe.Instance]),
                        genericName.Identifier.Text,
                        null
                    );
                    typeParameters = ParseTypeArgumentList(genericName.TypeArgumentList);
                }
                else
                {
                    var namePrefix = ExtractSpaceBefore(memberBinding.Name.Identifier);
                    _cursor = memberBinding.Name.Identifier.Span.End;
                    name = new Identifier(
                        Guid.NewGuid(),
                        namePrefix,
                        Markers.Build([NullSafe.Instance]),
                        memberBinding.Name.Identifier.Text,
                        null
                    );
                }

                var arguments = ParseArgumentList(invocation.ArgumentList);

                return new MethodInvocation(
                    Guid.NewGuid(),
                    prefix,
                    Markers.Empty,
                    new JRightPadded<Expression>(targetExpr, operatorSpace, Markers.Empty),
                    name,
                    typeParameters,
                    arguments,
                    null
                );
            }
            else if (invocation.Expression is MemberAccessExpressionSyntax memberAccess &&
                     memberAccess.Expression is ElementBindingExpressionSyntax elementBindingInMember)
            {
                // x?[0].Method() - element access followed by normal method call
                // First build the null-conditional element access
                var arrayAccess = ParseNullConditionalElementAccess(Space.Empty, targetExpr, operatorSpace, elementBindingInMember);

                // Then build the method invocation on it
                var dotSpace = ExtractSpaceBefore(memberAccess.OperatorToken);
                _cursor = memberAccess.OperatorToken.Span.End;

                Identifier methodName;
                JContainer<Expression>? typeParameters = null;

                if (memberAccess.Name is GenericNameSyntax genericMethodName)
                {
                    var namePrefix = ExtractSpaceBefore(genericMethodName.Identifier);
                    _cursor = genericMethodName.Identifier.Span.End;
                    methodName = new Identifier(
                        Guid.NewGuid(),
                        namePrefix,
                        Markers.Empty,
                        genericMethodName.Identifier.Text,
                        null
                    );
                    typeParameters = ParseTypeArgumentList(genericMethodName.TypeArgumentList);
                }
                else
                {
                    var namePrefix = ExtractSpaceBefore(memberAccess.Name.Identifier);
                    _cursor = memberAccess.Name.Identifier.Span.End;
                    methodName = new Identifier(
                        Guid.NewGuid(),
                        namePrefix,
                        Markers.Empty,
                        memberAccess.Name.Identifier.Text,
                        null
                    );
                }

                var arguments = ParseArgumentList(invocation.ArgumentList);

                return new MethodInvocation(
                    Guid.NewGuid(),
                    prefix,
                    Markers.Empty,
                    new JRightPadded<Expression>(arrayAccess, dotSpace, Markers.Empty),
                    methodName,
                    typeParameters,
                    arguments,
                    null
                );
            }
        }
        else if (node.WhenNotNull is MemberBindingExpressionSyntax memberBinding)
        {
            // x?.Property - field/property access with null-safe access
            _cursor = memberBinding.OperatorToken.Span.End;

            var namePrefix = ExtractSpaceBefore(memberBinding.Name.Identifier);
            _cursor = memberBinding.Name.Identifier.Span.End;
            var name = new Identifier(
                Guid.NewGuid(),
                namePrefix,
                Markers.Build([NullSafe.Instance]),
                memberBinding.Name.Identifier.Text,
                null
            );

            return new FieldAccess(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                targetExpr,
                new JLeftPadded<Identifier>(operatorSpace, name),
                null
            );
        }
        else if (node.WhenNotNull is ElementBindingExpressionSyntax elementBinding)
        {
            // x?[0] - null-conditional element access
            return ParseNullConditionalElementAccess(prefix, targetExpr, operatorSpace, elementBinding);
        }
        else if (node.WhenNotNull is ConditionalAccessExpressionSyntax innerConditional)
        {
            // Chained conditional access: x?.ToUpper()?.ToLower() or x?[0]?.ToUpper()
            // The inner Expression is the first access (method or element)
            // We need to combine our target with that, then process the rest

            // First, build the access from target + inner.Expression
            Expression firstAccess;
            if (innerConditional.Expression is InvocationExpressionSyntax innerInvocation &&
                innerInvocation.Expression is MemberBindingExpressionSyntax innerMemberBinding)
            {
                // Skip the . in the member binding
                _cursor = innerMemberBinding.OperatorToken.Span.End;

                Identifier firstName;
                JContainer<Expression>? firstTypeParams = null;

                if (innerMemberBinding.Name is GenericNameSyntax genericName)
                {
                    var namePrefix = ExtractSpaceBefore(genericName.Identifier);
                    _cursor = genericName.Identifier.Span.End;
                    firstName = new Identifier(
                        Guid.NewGuid(),
                        namePrefix,
                        Markers.Build([NullSafe.Instance]),
                        genericName.Identifier.Text,
                        null
                    );
                    firstTypeParams = ParseTypeArgumentList(genericName.TypeArgumentList);
                }
                else
                {
                    var namePrefix = ExtractSpaceBefore(innerMemberBinding.Name.Identifier);
                    _cursor = innerMemberBinding.Name.Identifier.Span.End;
                    firstName = new Identifier(
                        Guid.NewGuid(),
                        namePrefix,
                        Markers.Build([NullSafe.Instance]),
                        innerMemberBinding.Name.Identifier.Text,
                        null
                    );
                }

                var firstArgs = ParseArgumentList(innerInvocation.ArgumentList);

                firstAccess = new MethodInvocation(
                    Guid.NewGuid(),
                    Space.Empty,
                    Markers.Empty,
                    new JRightPadded<Expression>(targetExpr, operatorSpace, Markers.Empty),
                    firstName,
                    firstTypeParams,
                    firstArgs,
                    null
                );
            }
            else if (innerConditional.Expression is ElementBindingExpressionSyntax innerElementBinding)
            {
                // x?[0]?.Something - element access followed by more conditional access
                firstAccess = ParseNullConditionalElementAccess(Space.Empty, targetExpr, operatorSpace, innerElementBinding);
            }
            else
            {
                throw new InvalidOperationException($"Unsupported inner conditional expression: {innerConditional.Expression.GetType().Name}");
            }

            // Now process the second ?. and beyond
            var innerOperatorSpace = ExtractSpaceBefore(innerConditional.OperatorToken);
            _cursor = innerConditional.OperatorToken.Span.End;

            // Process WhenNotNull with the firstAccess as the new target
            return ProcessConditionalWhenNotNull(prefix, firstAccess, innerOperatorSpace, innerConditional.WhenNotNull);
        }

        throw new InvalidOperationException($"Unsupported conditional access pattern: {node.WhenNotNull.GetType().Name}");
    }

    /// <summary>
    /// Helper method to parse a null-conditional element access (x?[0]).
    /// </summary>
    private ArrayAccess ParseNullConditionalElementAccess(Space prefix, Expression target, Space operatorSpace, ElementBindingExpressionSyntax elementBinding)
    {
        var argList = elementBinding.ArgumentList;

        // For single-index, handle simply
        if (argList.Arguments.Count == 1)
        {
            // Space before '[' (which becomes ?[)
            var bracketPrefix = ExtractSpaceBefore(argList.OpenBracketToken);
            _cursor = argList.OpenBracketToken.Span.End;

            var arg = argList.Arguments[0];
            var indexExpr = Visit(arg.Expression);
            if (indexExpr is not Expression index)
            {
                throw new InvalidOperationException($"Expected Expression but got {indexExpr?.GetType().Name}");
            }

            var closeBracketSpace = ExtractSpaceBefore(argList.CloseBracketToken);
            _cursor = argList.CloseBracketToken.Span.End;

            // Combine operatorSpace (before ?) with bracketPrefix (before [) into dimension prefix
            // The NullSafe marker on dimension tells printer to print ?[
            var combinedPrefix = CombineSpaces(operatorSpace, bracketPrefix);

            return new ArrayAccess(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                target,
                new ArrayDimension(
                    Guid.NewGuid(),
                    combinedPrefix,
                    Markers.Build([NullSafe.Instance]),
                    new JRightPadded<Expression>(index, closeBracketSpace, Markers.Empty)
                ),
                null
            );
        }

        // Multi-dimensional null-conditional: x?[i, j]
        // Build nested ArrayAccess, innermost has NullSafe marker on dimension
        var firstArg = argList.Arguments[0];
        var firstIndex = Visit(firstArg.Expression);
        if (firstIndex is not Expression firstIndexExpr)
        {
            throw new InvalidOperationException($"Expected Expression but got {firstIndex?.GetType().Name}");
        }

        var firstBracketPrefix = ExtractSpaceBefore(argList.OpenBracketToken);
        _cursor = argList.OpenBracketToken.Span.End;
        // Re-parse first index since we moved cursor
        firstIndex = Visit(firstArg.Expression);
        firstIndexExpr = (Expression)firstIndex!;

        var firstSeparator = argList.Arguments.GetSeparator(0);
        var currentSpaceBeforeComma = ExtractSpaceBefore(firstSeparator);
        _cursor = firstSeparator.Span.End;

        var combinedFirstPrefix = CombineSpaces(operatorSpace, firstBracketPrefix);

        // Innermost ArrayAccess with NullSafe marker on dimension
        ArrayAccess current = new ArrayAccess(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            target,
            new ArrayDimension(
                Guid.NewGuid(),
                combinedFirstPrefix,
                Markers.Build([NullSafe.Instance]),
                new JRightPadded<Expression>(firstIndexExpr, Space.Empty, Markers.Empty)
            ),
            null
        );

        // Wrap with subsequent indices
        for (int i = 1; i < argList.Arguments.Count; i++)
        {
            var arg = argList.Arguments[i];
            var indexExpr = Visit(arg.Expression);
            if (indexExpr is not Expression index)
            {
                throw new InvalidOperationException($"Expected Expression but got {indexExpr?.GetType().Name}");
            }

            bool isLastIndex = i == argList.Arguments.Count - 1;

            if (!isLastIndex)
            {
                var separator = argList.Arguments.GetSeparator(i);
                var nextSpaceBeforeComma = ExtractSpaceBefore(separator);
                _cursor = separator.Span.End;

                current = new ArrayAccess(
                    Guid.NewGuid(),
                    currentSpaceBeforeComma,
                    Markers.Empty.Add(MultiDimensionalArray.Instance),
                    current,
                    new ArrayDimension(
                        Guid.NewGuid(),
                        Space.Empty,
                        Markers.Empty,
                        new JRightPadded<Expression>(index, Space.Empty, Markers.Empty)
                    ),
                    null
                );
                currentSpaceBeforeComma = nextSpaceBeforeComma;
            }
            else
            {
                var closeBracketSpace = ExtractSpaceBefore(argList.CloseBracketToken);
                _cursor = argList.CloseBracketToken.Span.End;

                current = new ArrayAccess(
                    Guid.NewGuid(),
                    currentSpaceBeforeComma,
                    Markers.Empty.Add(MultiDimensionalArray.Instance),
                    current,
                    new ArrayDimension(
                        Guid.NewGuid(),
                        Space.Empty,
                        Markers.Empty,
                        new JRightPadded<Expression>(index, closeBracketSpace, Markers.Empty)
                    ),
                    null
                );
            }
        }

        return current;
    }

    /// <summary>
    /// Combines two spaces, preserving whitespace and comments from both.
    /// </summary>
    private static Space CombineSpaces(Space first, Space second)
    {
        if (first.IsEmpty) return second;
        if (second.IsEmpty) return first;

        var comments = new List<Comment>(first.Comments);
        comments.AddRange(second.Comments);
        return new Space(first.Whitespace + second.Whitespace, comments);
    }

    /// <summary>
    /// Helper method to process the WhenNotNull part of a conditional access with a given target.
    /// </summary>
    private Expression ProcessConditionalWhenNotNull(Space prefix, Expression target, Space operatorSpace, ExpressionSyntax whenNotNull)
    {
        if (whenNotNull is InvocationExpressionSyntax invocation &&
            invocation.Expression is MemberBindingExpressionSyntax memberBinding)
        {
            // Skip the . in the member binding
            _cursor = memberBinding.OperatorToken.Span.End;

            Identifier name;
            JContainer<Expression>? typeParameters = null;

            if (memberBinding.Name is GenericNameSyntax genericName)
            {
                var namePrefix = ExtractSpaceBefore(genericName.Identifier);
                _cursor = genericName.Identifier.Span.End;
                name = new Identifier(
                    Guid.NewGuid(),
                    namePrefix,
                    Markers.Build([NullSafe.Instance]),
                    genericName.Identifier.Text,
                    null
                );
                typeParameters = ParseTypeArgumentList(genericName.TypeArgumentList);
            }
            else
            {
                var namePrefix = ExtractSpaceBefore(memberBinding.Name.Identifier);
                _cursor = memberBinding.Name.Identifier.Span.End;
                name = new Identifier(
                    Guid.NewGuid(),
                    namePrefix,
                    Markers.Build([NullSafe.Instance]),
                    memberBinding.Name.Identifier.Text,
                    null
                );
            }

            var arguments = ParseArgumentList(invocation.ArgumentList);

            return new MethodInvocation(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                new JRightPadded<Expression>(target, operatorSpace, Markers.Empty),
                name,
                typeParameters,
                arguments,
                null
            );
        }
        else if (whenNotNull is ElementBindingExpressionSyntax elementBinding)
        {
            // x?.ToUpper()?[0] - element access after conditional method
            return ParseNullConditionalElementAccess(prefix, target, operatorSpace, elementBinding);
        }
        else if (whenNotNull is ConditionalAccessExpressionSyntax innerConditional)
        {
            // More chained access - recurse
            Expression firstCall;
            if (innerConditional.Expression is InvocationExpressionSyntax innerInvocation &&
                innerInvocation.Expression is MemberBindingExpressionSyntax innerMemberBinding)
            {
                _cursor = innerMemberBinding.OperatorToken.Span.End;

                Identifier firstName;
                JContainer<Expression>? firstTypeParams = null;

                if (innerMemberBinding.Name is GenericNameSyntax genericName)
                {
                    var namePrefix = ExtractSpaceBefore(genericName.Identifier);
                    _cursor = genericName.Identifier.Span.End;
                    firstName = new Identifier(
                        Guid.NewGuid(),
                        namePrefix,
                        Markers.Build([NullSafe.Instance]),
                        genericName.Identifier.Text,
                        null
                    );
                    firstTypeParams = ParseTypeArgumentList(genericName.TypeArgumentList);
                }
                else
                {
                    var namePrefix = ExtractSpaceBefore(innerMemberBinding.Name.Identifier);
                    _cursor = innerMemberBinding.Name.Identifier.Span.End;
                    firstName = new Identifier(
                        Guid.NewGuid(),
                        namePrefix,
                        Markers.Build([NullSafe.Instance]),
                        innerMemberBinding.Name.Identifier.Text,
                        null
                    );
                }

                var firstArgs = ParseArgumentList(innerInvocation.ArgumentList);

                firstCall = new MethodInvocation(
                    Guid.NewGuid(),
                    Space.Empty,
                    Markers.Empty,
                    new JRightPadded<Expression>(target, operatorSpace, Markers.Empty),
                    firstName,
                    firstTypeParams,
                    firstArgs,
                    null
                );
            }
            else if (innerConditional.Expression is ElementBindingExpressionSyntax innerElementBinding)
            {
                // x?[0]?.Something - element access followed by more chained access
                firstCall = ParseNullConditionalElementAccess(Space.Empty, target, operatorSpace, innerElementBinding);
            }
            else
            {
                throw new InvalidOperationException($"Unsupported inner conditional expression: {innerConditional.Expression.GetType().Name}");
            }

            var innerOperatorSpace = ExtractSpaceBefore(innerConditional.OperatorToken);
            _cursor = innerConditional.OperatorToken.Span.End;

            return ProcessConditionalWhenNotNull(prefix, firstCall, innerOperatorSpace, innerConditional.WhenNotNull);
        }

        throw new InvalidOperationException($"Unsupported WhenNotNull pattern: {whenNotNull.GetType().Name}");
    }

    private JContainer<Expression> ParseArgumentList(ArgumentListSyntax argList)
    {
        var argsPrefix = ExtractSpaceBefore(argList.OpenParenToken);
        _cursor = argList.OpenParenToken.Span.End;

        var args = new List<JRightPadded<Expression>>();
        for (int i = 0; i < argList.Arguments.Count; i++)
        {
            var arg = argList.Arguments[i];

            Expression expr;
            if (arg.NameColon != null)
            {
                // Named argument: name: value
                var namePrefix = ExtractSpaceBefore(arg.NameColon.Name.Identifier);
                _cursor = arg.NameColon.Name.Identifier.Span.End;

                var nameIdentifier = new Identifier(
                    Guid.NewGuid(),
                    namePrefix,
                    Markers.Empty,
                    arg.NameColon.Name.Identifier.Text,
                    null
                );

                // Space before the colon
                var colonSpace = ExtractSpaceBefore(arg.NameColon.ColonToken);
                _cursor = arg.NameColon.ColonToken.Span.End;

                // Parse the value expression (may include ref modifier)
                var valueExpr = ParseArgumentExpression(arg);
                if (valueExpr is not Expression value)
                {
                    throw new InvalidOperationException($"Expected Expression but got {valueExpr?.GetType().Name}");
                }

                expr = new NamedExpression(
                    Guid.NewGuid(),
                    Space.Empty,
                    Markers.Empty,
                    new JRightPadded<Identifier>(nameIdentifier, colonSpace, Markers.Empty),
                    value
                );
            }
            else
            {
                // Positional argument (may include ref modifier)
                expr = ParseArgumentExpression(arg);
            }

            Space afterSpace = Space.Empty;
            if (i < argList.Arguments.Count - 1)
            {
                var separatorToken = argList.Arguments.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(separatorToken);
                _cursor = separatorToken.Span.End;
            }
            else
            {
                afterSpace = ExtractSpaceBefore(argList.CloseParenToken);
            }
            args.Add(new JRightPadded<Expression>(expr, afterSpace, Markers.Empty));
        }

        // Handle empty argument list - capture any space before )
        if (args.Count == 0)
        {
            var emptySpace = ExtractSpaceBefore(argList.CloseParenToken);
            if (!string.IsNullOrEmpty(emptySpace.Whitespace) || emptySpace.Comments.Count > 0)
            {
                // Use an Empty element to hold the trailing space before )
                args.Add(new JRightPadded<Expression>(
                    new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty),
                    emptySpace,
                    Markers.Empty
                ));
            }
        }

        _cursor = argList.CloseParenToken.Span.End;

        return new JContainer<Expression>(argsPrefix, args, Markers.Empty);
    }

    /// <summary>
    /// Parses a type parameter list for generic types/methods, merging any constraint clauses
    /// into ConstrainedTypeParameter objects stored in J.TypeParameter.Bounds[0].
    /// Example: &lt;T, U&gt; where T : class, IDisposable where U : new()
    /// </summary>
    private JContainer<TypeParameter> ParseTypeParameterList(
        TypeParameterListSyntax typeParamList,
        SyntaxList<TypeParameterConstraintClauseSyntax> constraintClauses)
    {
        var containerPrefix = ExtractSpaceBefore(typeParamList.LessThanToken);
        _cursor = typeParamList.LessThanToken.Span.End;

        // Build a lookup from type parameter name to its constraint clause
        var constraintsByName = new Dictionary<string, TypeParameterConstraintClauseSyntax>();
        foreach (var clause in constraintClauses)
        {
            constraintsByName[clause.Name.Identifier.Text] = clause;
        }

        var typeParams = new List<JRightPadded<TypeParameter>>();
        for (var i = 0; i < typeParamList.Parameters.Count; i++)
        {
            var param = typeParamList.Parameters[i];

            // Parse attribute lists on type parameter (e.g., [MaybeNull] T)
            var attrLists = new List<AttributeList>();
            foreach (var attrListSyntax in param.AttributeLists)
            {
                attrLists.Add((AttributeList)Visit(attrListSyntax)!);
            }

            // Parse variance modifier (in/out) if present
            JLeftPadded<ConstrainedTypeParameter.VarianceKind>? variance = null;
            if (param.VarianceKeyword.IsKind(SyntaxKind.InKeyword))
            {
                var varPrefix = ExtractSpaceBefore(param.VarianceKeyword);
                _cursor = param.VarianceKeyword.Span.End;
                variance = new JLeftPadded<ConstrainedTypeParameter.VarianceKind>(
                    varPrefix, ConstrainedTypeParameter.VarianceKind.In);
            }
            else if (param.VarianceKeyword.IsKind(SyntaxKind.OutKeyword))
            {
                var varPrefix = ExtractSpaceBefore(param.VarianceKeyword);
                _cursor = param.VarianceKeyword.Span.End;
                variance = new JLeftPadded<ConstrainedTypeParameter.VarianceKind>(
                    varPrefix, ConstrainedTypeParameter.VarianceKind.Out);
            }

            // Parse the type parameter name
            var namePrefix = ExtractSpaceBefore(param.Identifier);
            _cursor = param.Identifier.Span.End;
            var name = new Identifier(
                Guid.NewGuid(),
                namePrefix,
                Markers.Empty,
                param.Identifier.Text,
                null
            );

            // After space is before the comma or closing >
            Space afterSpace;
            if (i < typeParamList.Parameters.Count - 1)
            {
                var separator = typeParamList.Parameters.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(separator);
                _cursor = separator.Span.End;
            }
            else
            {
                afterSpace = ExtractSpaceBefore(typeParamList.GreaterThanToken);
            }

            // Create a ConstrainedTypeParameter without constraint data for now
            // (constraints will be merged after parsing the constraint clauses)
            var ctp = new ConstrainedTypeParameter(
                Guid.NewGuid(),
                Space.Empty,
                Markers.Empty,
                attrLists,
                variance,
                name,
                null, // WhereConstraint (populated below if constraint clause exists)
                null, // Constraints (populated below if constraint clause exists)
                null  // Type
            );

            // Wrap in J.TypeParameter with the ConstrainedTypeParameter in Bounds[0]
            var bounds = new JContainer<TypeTree>(
                Space.Empty,
                [new JRightPadded<TypeTree>(ctp, Space.Empty, Markers.Empty)],
                Markers.Empty
            );

            var typeParam = new TypeParameter(
                Guid.NewGuid(),
                Space.Empty,
                Markers.Empty,
                [],    // Annotations
                [],    // Modifiers
                name,  // Name
                bounds
            );

            typeParams.Add(new JRightPadded<TypeParameter>(typeParam, afterSpace, Markers.Empty));
        }

        _cursor = typeParamList.GreaterThanToken.Span.End;

        // Now parse constraint clauses and merge them into the corresponding type parameters
        foreach (var clause in constraintClauses)
        {
            // Space before "where" keyword
            var wherePrefix = ExtractSpaceBefore(clause.WhereKeyword);
            _cursor = clause.WhereKeyword.Span.End;

            // Parse the constraint name identifier (e.g., T in "where T :")
            var constraintNamePrefix = ExtractSpaceBefore(clause.Name.Identifier);
            _cursor = clause.Name.Identifier.Span.End;
            var whereIdentifier = new Identifier(
                Guid.NewGuid(),
                constraintNamePrefix,
                Markers.Empty,
                clause.Name.Identifier.Text,
                null
            );

            // Space before colon
            var beforeColon = ExtractSpaceBefore(clause.ColonToken);
            _cursor = clause.ColonToken.Span.End;

            // The WhereConstraint is a JLeftPadded<Identifier> where:
            // - Left padding = space before "where" keyword
            // - Identifier = the type param name in the constraint, right-padded with space before ":"
            var whereConstraint = new JLeftPadded<Identifier>(wherePrefix, whereIdentifier);

            // Parse individual constraints
            var constraints = new List<JRightPadded<Expression>>();
            for (var i = 0; i < clause.Constraints.Count; i++)
            {
                var constraint = clause.Constraints[i];
                var constraintExpr = ParseConstraintExpression(constraint);

                Space constraintAfterSpace;
                if (i < clause.Constraints.Count - 1)
                {
                    var separator = clause.Constraints.GetSeparator(i);
                    constraintAfterSpace = ExtractSpaceBefore(separator);
                    _cursor = separator.Span.End;
                }
                else
                {
                    constraintAfterSpace = Space.Empty;
                }

                constraints.Add(new JRightPadded<Expression>(constraintExpr, constraintAfterSpace, Markers.Empty));
            }

            var constraintsContainer = new JContainer<Expression>(beforeColon, constraints, Markers.Empty);

            // Find the matching type parameter and update its ConstrainedTypeParameter
            var paramName = clause.Name.Identifier.Text;
            for (var pi = 0; pi < typeParams.Count; pi++)
            {
                var tp = typeParams[pi];
                if (tp.Element.Bounds != null)
                {
                    var ctp = (ConstrainedTypeParameter)tp.Element.Bounds.Elements[0].Element;
                    if (ctp.Name.SimpleName == paramName)
                    {
                        var updatedCtp = ctp.WithWhereConstraint(whereConstraint).WithConstraints(constraintsContainer);
                        var updatedBounds = new JContainer<TypeTree>(
                            tp.Element.Bounds.Before,
                            [new JRightPadded<TypeTree>(updatedCtp, Space.Empty, Markers.Empty)],
                            tp.Element.Bounds.Markers
                        );
                        typeParams[pi] = tp.WithElement(tp.Element.WithBounds(updatedBounds));
                        break;
                    }
                }
            }
        }

        return new JContainer<TypeParameter>(containerPrefix, typeParams, Markers.Empty);
    }

    /// <summary>
    /// Parses a single type parameter constraint into an Expression.
    /// Type constraints (e.g., IDisposable) are parsed as TypeTree (which extends Expression).
    /// Other constraints (class, struct, new(), default) are parsed as their respective Cs types.
    /// </summary>
    private Expression ParseConstraintExpression(TypeParameterConstraintSyntax constraint)
    {
        switch (constraint)
        {
            case TypeConstraintSyntax typeConstraint:
            {
                // TypeTree types (Identifier, ParameterizedType, etc.) implement Expression
                return (Expression)Visit(typeConstraint.Type)!;
            }

            case ClassOrStructConstraintSyntax classOrStruct:
            {
                var prefix = ExtractSpaceBefore(classOrStruct.ClassOrStructKeyword);
                _cursor = classOrStruct.ClassOrStructKeyword.Span.End;
                if (classOrStruct.QuestionToken != default)
                {
                    _cursor = classOrStruct.QuestionToken.Span.End;
                }
                var kind = classOrStruct.ClassOrStructKeyword.IsKind(SyntaxKind.ClassKeyword)
                    ? ClassOrStructConstraint.TypeKind.Class
                    : ClassOrStructConstraint.TypeKind.Struct;
                return new ClassOrStructConstraint(
                    Guid.NewGuid(),
                    prefix,
                    Markers.Empty,
                    kind
                );
            }

            case ConstructorConstraintSyntax newConstraint:
            {
                var prefix = ExtractSpaceBefore(newConstraint.NewKeyword);
                _cursor = newConstraint.CloseParenToken.Span.End;
                return new ConstructorConstraint(
                    Guid.NewGuid(),
                    prefix,
                    Markers.Empty
                );
            }

            case DefaultConstraintSyntax defaultConstraint:
            {
                var prefix = ExtractSpaceBefore(defaultConstraint.DefaultKeyword);
                _cursor = defaultConstraint.DefaultKeyword.Span.End;
                return new DefaultConstraint(
                    Guid.NewGuid(),
                    prefix,
                    Markers.Empty
                );
            }

            default:
                throw new NotSupportedException($"Unknown constraint type: {constraint.GetType().Name}");
        }
    }

    /// <summary>
    /// Parses a primary constructor parameter list for C# 12 classes/structs.
    /// Returns a synthesized MethodDeclaration following the Kotlin pattern.
    /// Example: class Foo(int x, string y) { }
    /// </summary>
    private MethodDeclaration ParsePrimaryConstructor(ParameterListSyntax paramList)
    {
        var paramsPrefix = ExtractSpaceBefore(paramList.OpenParenToken);
        _cursor = paramList.OpenParenToken.Span.End;

        var parameters = new List<JRightPadded<Statement>>();
        for (var i = 0; i < paramList.Parameters.Count; i++)
        {
            var p = paramList.Parameters[i];
            var paramStatement = ConvertParameter(p);
            if (paramStatement is Statement stmt)
            {
                Space afterSpace;
                if (i < paramList.Parameters.Count - 1)
                {
                    var separatorToken = paramList.Parameters.GetSeparator(i);
                    afterSpace = ExtractSpaceBefore(separatorToken);
                    _cursor = separatorToken.Span.End;
                }
                else
                {
                    afterSpace = ExtractSpaceBefore(paramList.CloseParenToken);
                }
                parameters.Add(new JRightPadded<Statement>(stmt, afterSpace, Markers.Empty));
            }
        }

        _cursor = paramList.CloseParenToken.Span.End;

        var paramsContainer = new JContainer<Statement>(paramsPrefix, parameters, Markers.Empty);

        // Create a synthesized method name with Implicit marker (following Kotlin pattern)
        var methodName = new Identifier(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Build([new Implicit(Guid.NewGuid())]),
            "<constructor>",
            null
        );

        // Create the synthesized MethodDeclaration with PrimaryConstructor marker
        return new MethodDeclaration(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Build([new PrimaryConstructor(Guid.NewGuid())]),
            [],      // LeadingAnnotations
            [],      // Modifiers
            null,    // TypeParameters
            null,    // ReturnType (constructors don't have return types)
            methodName,
            paramsContainer,
            null,    // Throws
            null,    // Body
            null,    // DefaultValue
            null     // MethodType
        );
    }

    /// <summary>
    /// Parses an argument expression, handling ref/out/in modifiers and declaration expressions.
    /// </summary>
    private Expression ParseArgumentExpression(ArgumentSyntax arg)
    {
        // Check for ref/out/in modifier
        if (!arg.RefKindKeyword.IsKind(SyntaxKind.None))
        {
            // Capture space before the ref/out/in keyword
            var refPrefix = ExtractSpaceBefore(arg.RefKindKeyword);

            var refKind = arg.RefKindKeyword.Kind() switch
            {
                SyntaxKind.OutKeyword => RefKind.Out,
                SyntaxKind.RefKeyword => RefKind.Ref,
                SyntaxKind.InKeyword => RefKind.In,
                _ => throw new InvalidOperationException($"Unexpected ref kind: {arg.RefKindKeyword.Kind()}")
            };

            // Move cursor past the ref keyword
            _cursor = arg.RefKindKeyword.Span.End;

            Expression innerExpr;
            if (arg.Expression is DeclarationExpressionSyntax declExpr)
            {
                // out var x, out int result
                innerExpr = ParseDeclarationExpression(declExpr);
            }
            else
            {
                // ref x, out result, in value
                var exprResult = Visit(arg.Expression);
                if (exprResult is not Expression e)
                {
                    throw new InvalidOperationException($"Expected Expression but got {exprResult?.GetType().Name}");
                }
                innerExpr = e;
            }

            return new RefExpression(
                Guid.NewGuid(),
                refPrefix,
                Markers.Empty,
                refKind,
                innerExpr
            );
        }
        else
        {
            // Regular positional argument without ref modifier
            var argExpr = Visit(arg.Expression);
            if (argExpr is not Expression e)
            {
                throw new InvalidOperationException($"Expected Expression but got {argExpr?.GetType().Name}");
            }
            return e;
        }
    }

    /// <summary>
    /// Parses a declaration expression (e.g., "var x" in "out var x").
    /// </summary>
    private DeclarationExpression ParseDeclarationExpression(DeclarationExpressionSyntax declExpr)
    {
        var prefix = ExtractPrefix(declExpr);

        // Parse the type (var or explicit type)
        var typeExpr = VisitType(declExpr.Type);

        // Parse the variable designation
        Expression variables = ParseVariableDesignation(declExpr.Designation);

        return new DeclarationExpression(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            typeExpr,
            variables
        );
    }

    public override J VisitDeclarationExpression(DeclarationExpressionSyntax node)
    {
        return ParseDeclarationExpression(node);
    }

    private Expression ParseVariableDesignation(VariableDesignationSyntax designation)
    {
        if (designation is SingleVariableDesignationSyntax singleVar)
        {
            var varPrefix = ExtractSpaceBefore(singleVar.Identifier);
            _cursor = singleVar.Identifier.Span.End;

            var varName = new Identifier(
                Guid.NewGuid(),
                Space.Empty,
                Markers.Empty,
                singleVar.Identifier.Text,
                null
            );

            return new SingleVariableDesignation(
                Guid.NewGuid(),
                varPrefix,
                Markers.Empty,
                varName
            );
        }
        else if (designation is DiscardDesignationSyntax discard)
        {
            var discardPrefix = ExtractSpaceBefore(discard.UnderscoreToken);
            _cursor = discard.UnderscoreToken.Span.End;

            var discardName = new Identifier(
                Guid.NewGuid(),
                Space.Empty,
                Markers.Empty,
                "_",
                null
            );

            return new DiscardVariableDesignation(
                Guid.NewGuid(),
                discardPrefix,
                Markers.Empty,
                discardName
            );
        }
        else if (designation is ParenthesizedVariableDesignationSyntax parenDesig)
        {
            var parenPrefix = ExtractSpaceBefore(parenDesig.OpenParenToken);
            _cursor = parenDesig.OpenParenToken.Span.End;

            var variables = new List<JRightPadded<VariableDesignation>>();
            for (int i = 0; i < parenDesig.Variables.Count; i++)
            {
                var v = parenDesig.Variables[i];
                var varExpr = (VariableDesignation)ParseVariableDesignation(v);

                Space afterSpace = Space.Empty;
                if (i < parenDesig.Variables.Count - 1)
                {
                    var separator = parenDesig.Variables.GetSeparator(i);
                    afterSpace = ExtractSpaceBefore(separator);
                    _cursor = separator.Span.End;
                }
                else
                {
                    afterSpace = ExtractSpaceBefore(parenDesig.CloseParenToken);
                }
                variables.Add(new JRightPadded<VariableDesignation>(varExpr, afterSpace, Markers.Empty));
            }
            _cursor = parenDesig.CloseParenToken.Span.End;

            return new ParenthesizedVariableDesignation(
                Guid.NewGuid(),
                parenPrefix,
                Markers.Empty,
                new JContainer<VariableDesignation>(Space.Empty, variables, Markers.Empty),
                null // type
            );
        }
        else
        {
            throw new InvalidOperationException($"Unsupported designation type: {designation.GetType().Name}");
        }
    }

    private JContainer<Expression> ParseTypeArgumentList(TypeArgumentListSyntax typeArgList)
    {
        var typeArgsPrefix = ExtractSpaceBefore(typeArgList.LessThanToken);
        _cursor = typeArgList.LessThanToken.Span.End;

        var typeArgs = new List<JRightPadded<Expression>>();
        for (int i = 0; i < typeArgList.Arguments.Count; i++)
        {
            var typeArg = typeArgList.Arguments[i];
            var typeExpr = VisitType(typeArg);
            if (typeExpr is not Expression expr)
            {
                throw new InvalidOperationException($"Expected Expression but got {typeExpr?.GetType().Name}");
            }

            Space afterSpace = Space.Empty;
            if (i < typeArgList.Arguments.Count - 1)
            {
                var separatorToken = typeArgList.Arguments.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(separatorToken);
                _cursor = separatorToken.Span.End;
            }
            else
            {
                afterSpace = ExtractSpaceBefore(typeArgList.GreaterThanToken);
            }
            typeArgs.Add(new JRightPadded<Expression>(expr, afterSpace, Markers.Empty));
        }

        _cursor = typeArgList.GreaterThanToken.Span.End;

        return new JContainer<Expression>(typeArgsPrefix, typeArgs, Markers.Empty);
    }

    /// <summary>
    /// Parses a VariableDeclarationSyntax (type + variables) into a VariableDeclarations.
    /// Used by VisitLocalDeclarationStatement and VisitFixedStatement.
    /// </summary>
    public override J VisitVariableDeclaration(VariableDeclarationSyntax node)
    {
        // Don't extract prefix here — let VisitType capture any space before the type
        // as the type expression's prefix. This prevents the space between preceding
        // modifiers (e.g. "const") and the type from being assigned to the
        // VariableDeclarations prefix, which callers like VisitLocalDeclarationStatement
        // overwrite with the statement-level prefix.

        // Parse the type
        var typeExpr = VisitType(node.Type);

        // Parse variables
        var variables = new List<JRightPadded<NamedVariable>>();
        for (int i = 0; i < node.Variables.Count; i++)
        {
            var v = node.Variables[i];
            var varPrefix = ExtractPrefix(v);

            // Parse the variable name
            var namePrefix = ExtractSpaceBefore(v.Identifier);
            _cursor = v.Identifier.Span.End;
            var name = new Identifier(
                Guid.NewGuid(),
                namePrefix,
                Markers.Empty,
                v.Identifier.Text,
                null
            );

            // Parse initializer if present
            JLeftPadded<Expression>? initializer = null;
            if (v.Initializer != null)
            {
                var equalsPrefix = ExtractSpaceBefore(v.Initializer.EqualsToken);
                _cursor = v.Initializer.EqualsToken.Span.End;

                var initExpr = Visit(v.Initializer.Value);
                if (initExpr is Expression expr)
                {
                    initializer = new JLeftPadded<Expression>(equalsPrefix, expr);
                }
            }

            var namedVar = new NamedVariable(
                Guid.NewGuid(),
                varPrefix,
                Markers.Empty,
                name,
                [],
                initializer,
                _typeMapping?.VariableType(v)
            );

            // Handle comma separator for multiple variables
            Space afterSpace = Space.Empty;
            if (i < node.Variables.Count - 1)
            {
                var separatorToken = node.Variables.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(separatorToken);
                _cursor = separatorToken.Span.End;
            }

            variables.Add(new JRightPadded<NamedVariable>(namedVar, afterSpace, Markers.Empty));
        }

        return new VariableDeclarations(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Empty,
            [],
            [],
            typeExpr,
            null,
            [],
            variables
        );
    }

    public override J VisitLocalDeclarationStatement(LocalDeclarationStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse modifiers (const, etc.)
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(),
                modPrefix,
                Markers.Empty,
                MapModifier(mod.Kind()),
                []
            ));
        }

        // Parse the variable declaration (type + variables)
        var varDecl = (VariableDeclarations)Visit(node.Declaration)!;

        // Consume the semicolon
        SkipTo(node.SemicolonToken.SpanStart);
        SkipToken(node.SemicolonToken);

        // Merge modifiers and prefix from the statement level into the VariableDeclarations
        return varDecl.WithPrefix(prefix).WithModifiers(modifiers);
    }

    public override J VisitFieldDeclaration(FieldDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse modifiers — attributes on fields are skipped for now as
        // LeadingAnnotations takes IList<Modifier> in the C# model
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(),
                modPrefix,
                Markers.Empty,
                MapModifier(mod.Kind()),
                []
            ));
        }

        // Parse the variable declaration
        var declaration = node.Declaration;

        // Parse the type
        var typeExpr = VisitType(declaration.Type);

        // Parse variables
        var variables = new List<JRightPadded<NamedVariable>>();
        for (int i = 0; i < declaration.Variables.Count; i++)
        {
            var v = declaration.Variables[i];
            var varPrefix = ExtractPrefix(v);

            // Parse the variable name
            var namePrefix = ExtractSpaceBefore(v.Identifier);
            _cursor = v.Identifier.Span.End;
            var name = new Identifier(
                Guid.NewGuid(),
                namePrefix,
                Markers.Empty,
                v.Identifier.Text,
                null
            );

            // Parse initializer if present
            JLeftPadded<Expression>? initializer = null;
            if (v.Initializer != null)
            {
                var equalsPrefix = ExtractSpaceBefore(v.Initializer.EqualsToken);
                _cursor = v.Initializer.EqualsToken.Span.End;

                var initExpr = Visit(v.Initializer.Value);
                if (initExpr is Expression expr)
                {
                    initializer = new JLeftPadded<Expression>(equalsPrefix, expr);
                }
            }

            var namedVar = new NamedVariable(
                Guid.NewGuid(),
                varPrefix,
                Markers.Empty,
                name,
                [],
                initializer,
                _typeMapping?.VariableType(v)
            );

            // Handle comma separator for multiple variables
            Space afterSpace = Space.Empty;
            if (i < declaration.Variables.Count - 1)
            {
                var separatorToken = declaration.Variables.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(separatorToken);
                _cursor = separatorToken.Span.End;
            }

            variables.Add(new JRightPadded<NamedVariable>(namedVar, afterSpace, Markers.Empty));
        }

        // Consume the semicolon
        SkipTo(node.SemicolonToken.SpanStart);
        SkipToken(node.SemicolonToken);

        return new VariableDeclarations(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            [],
            modifiers,
            typeExpr,
            null,
            [],
            variables
        );
    }

    public override J VisitEventFieldDeclaration(EventFieldDeclarationSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse modifiers — includes the 'event' keyword
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(),
                modPrefix,
                Markers.Empty,
                MapModifier(mod.Kind()),
                []
            ));
        }

        // Parse the 'event' keyword as a modifier
        var eventPrefix = ExtractSpaceBefore(node.EventKeyword);
        _cursor = node.EventKeyword.Span.End;
        modifiers.Add(new Modifier(
            Guid.NewGuid(),
            eventPrefix,
            Markers.Empty,
            Modifier.ModifierType.LanguageExtension,
            [],
            "event"
        ));

        // Parse the variable declaration (same as field declaration)
        var declaration = node.Declaration;

        // Parse the type
        var typeExpr = VisitType(declaration.Type);

        // Parse variables
        var variables = new List<JRightPadded<NamedVariable>>();
        for (int i = 0; i < declaration.Variables.Count; i++)
        {
            var v = declaration.Variables[i];
            var varPrefix = ExtractPrefix(v);

            var namePrefix = ExtractSpaceBefore(v.Identifier);
            _cursor = v.Identifier.Span.End;
            var name = new Identifier(
                Guid.NewGuid(),
                namePrefix,
                Markers.Empty,
                v.Identifier.Text,
                null
            );

            JLeftPadded<Expression>? initializer = null;
            if (v.Initializer != null)
            {
                var equalsPrefix = ExtractSpaceBefore(v.Initializer.EqualsToken);
                _cursor = v.Initializer.EqualsToken.Span.End;

                var initExpr = Visit(v.Initializer.Value);
                if (initExpr is Expression expr)
                {
                    initializer = new JLeftPadded<Expression>(equalsPrefix, expr);
                }
            }

            var namedVar = new NamedVariable(
                Guid.NewGuid(),
                varPrefix,
                Markers.Empty,
                name,
                [],
                initializer,
                _typeMapping?.VariableType(v)
            );

            Space afterSpace = Space.Empty;
            if (i < declaration.Variables.Count - 1)
            {
                var separatorToken = declaration.Variables.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(separatorToken);
                _cursor = separatorToken.Span.End;
            }

            variables.Add(new JRightPadded<NamedVariable>(namedVar, afterSpace, Markers.Empty));
        }

        SkipTo(node.SemicolonToken.SpanStart);
        SkipToken(node.SemicolonToken);

        return new VariableDeclarations(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            [],
            modifiers,
            typeExpr,
            null,
            [],
            variables
        );
    }

    public override J VisitLocalFunctionStatement(LocalFunctionStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse modifiers (async, static, etc.)
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(),
                modPrefix,
                Markers.Empty,
                MapModifier(mod.Kind()),
                []
            ));
        }

        // Parse return type
        var returnType = VisitType(node.ReturnType);

        // Parse the function name
        var namePrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var name = new Identifier(
            Guid.NewGuid(),
            namePrefix,
            Markers.Empty,
            node.Identifier.Text,
            null
        );

        // Parse parameters
        var paramsPrefix = ExtractSpaceBefore(node.ParameterList.OpenParenToken);
        _cursor = node.ParameterList.OpenParenToken.Span.End;

        var parameters = new List<JRightPadded<Statement>>();
        for (int i = 0; i < node.ParameterList.Parameters.Count; i++)
        {
            var p = node.ParameterList.Parameters[i];
            var paramStatement = ConvertParameter(p);
            if (paramStatement is Statement stmt)
            {
                Space afterSpace = Space.Empty;
                if (i < node.ParameterList.Parameters.Count - 1)
                {
                    var separatorToken = node.ParameterList.Parameters.GetSeparator(i);
                    afterSpace = ExtractSpaceBefore(separatorToken);
                    _cursor = separatorToken.Span.End;
                }
                else
                {
                    afterSpace = ExtractSpaceBefore(node.ParameterList.CloseParenToken);
                }
                parameters.Add(new JRightPadded<Statement>(stmt, afterSpace, Markers.Empty));
            }
        }

        if (parameters.Count == 0)
        {
            var emptySpace = ExtractSpaceBefore(node.ParameterList.CloseParenToken);
            paramsPrefix = new Space(paramsPrefix.Whitespace, paramsPrefix.Comments);
        }

        _cursor = node.ParameterList.CloseParenToken.Span.End;

        var paramsContainer = new JContainer<Statement>(
            paramsPrefix,
            parameters,
            Markers.Empty
        );

        // Parse body
        Block? body = null;
        if (node.Body != null)
        {
            body = (Block)VisitBlock(node.Body);
        }
        else if (node.ExpressionBody != null)
        {
            var arrowPrefix = ExtractSpaceBefore(node.ExpressionBody.ArrowToken);
            _cursor = node.ExpressionBody.ArrowToken.Span.End;
            var expr = (Expression)Visit(node.ExpressionBody.Expression)!;

            var returnStmt = new Return(Guid.NewGuid(), Space.Empty, Markers.Empty, expr);
            body = new Block(Guid.NewGuid(), arrowPrefix, Markers.Empty,
                new JRightPadded<bool>(false, Space.Empty, Markers.Empty),
                [new JRightPadded<Statement>(returnStmt, Space.Empty, Markers.Empty)],
                Space.Empty);

            _cursor = node.SemicolonToken.Span.End;
        }

        return new MethodDeclaration(
            Guid.NewGuid(),
            prefix,
            node.ExpressionBody != null
                ? Markers.Build([new ExpressionBodied(Guid.NewGuid())])
                : Markers.Empty,
            [],
            modifiers,
            null, // TypeParameters
            returnType,
            name,
            paramsContainer,
            null, // Throws
            body,
            null, // DefaultValue
            _typeMapping?.MethodType(node)
        );
    }

    /// <summary>
    /// Wraps an InitializerExpressionSyntax into a J.Block containing a single
    /// StatementExpression(InitializerExpression). Used for NewClass.body.
    /// </summary>
    private Block WrapInitializerAsBlock(InitializerExpressionSyntax initSyntax)
    {
        var initExpr = (InitializerExpression)VisitInitializerExpression(initSyntax);
        // Wrap InitializerExpression in a Block. The Block prefix gets the space before the initializer.
        // Move the InitializerExpression prefix to the Block prefix.
        var blockPrefix = initExpr.Prefix;
        initExpr = initExpr.WithPrefix(Space.Empty);
        return new Block(
            Guid.NewGuid(),
            blockPrefix,
            Markers.Empty,
            new JRightPadded<bool>(false, Space.Empty, Markers.Empty),
            [new JRightPadded<Statement>(
                new ExpressionStatement(Guid.NewGuid(), initExpr),
                Space.Empty,
                Markers.Empty
            )],
            Space.Empty
        );
    }

    private TypeTree? VisitType(TypeSyntax type)
    {
        var prefix = ExtractPrefix(type);

        if (type is PredefinedTypeSyntax predefined)
        {
            _cursor = predefined.Keyword.Span.End;

            // Object is a class in Java, not a primitive - treat it as an Identifier
            if (predefined.Keyword.Kind() == SyntaxKind.ObjectKeyword)
            {
                return new Identifier(
                    Guid.NewGuid(),
                    prefix,
                    Markers.Empty,
                    "object",
                    null
                );
            }

            var kind = MapPredefinedType(predefined.Keyword.Kind());
            return new Primitive(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                kind
            );
        }
        else if (type is IdentifierNameSyntax identifier)
        {
            // Could be 'var' or a user-defined type
            _cursor = identifier.Identifier.Span.End;
            if (identifier.Identifier.Text == "var")
            {
                return new Identifier(
                    Guid.NewGuid(),
                    prefix,
                    Markers.Empty,
                    "var",
                    null
                );
            }
            else
            {
                return new Identifier(
                    Guid.NewGuid(),
                    prefix,
                    Markers.Empty,
                    identifier.Identifier.Text,
                    _typeMapping?.Type(identifier)
                );
            }
        }
        else if (type is GenericNameSyntax genericName)
        {
            // Handle generic types like List<int>, Dictionary<string, int>
            _cursor = genericName.Identifier.Span.End;
            var clazz = new Identifier(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                genericName.Identifier.Text,
                null
            );

            var typeParams = ParseTypeArgumentList(genericName.TypeArgumentList);

            return new ParameterizedType(
                Guid.NewGuid(),
                Space.Empty,
                Markers.Empty,
                clazz,
                typeParams,
                _typeMapping?.Type(type)
            );
        }
        else if (type is QualifiedNameSyntax qualified)
        {
            // Handle qualified names like System.Collections.Generic
            return VisitQualifiedName(qualified, prefix);
        }
        else if (type is ArrayTypeSyntax arrayType)
        {
            // Handle array types: int[], int[,], int[][]
            // Build nested ArrayType for each rank specifier
            var elementType = VisitType(arrayType.ElementType);
            if (elementType == null)
            {
                throw new InvalidOperationException($"Unable to parse array element type: {arrayType.ElementType}");
            }

            // C# can have multiple rank specifiers: int[][,] has two specifiers
            // Process from innermost (first) to outermost (last)
            TypeTree result = elementType;
            foreach (var rankSpec in arrayType.RankSpecifiers)
            {
                // Get the space before the [
                var bracketPrefix = ExtractSpaceBefore(rankSpec.OpenBracketToken);
                _cursor = rankSpec.OpenBracketToken.Span.End;

                // Get the space before the ]
                var innerSpace = ExtractSpaceBefore(rankSpec.CloseBracketToken);
                _cursor = rankSpec.CloseBracketToken.Span.End;

                result = new ArrayType(
                    Guid.NewGuid(),
                    result == elementType ? prefix : Space.Empty,
                    Markers.Empty,
                    result,
                    null,
                    new JLeftPadded<Space>(bracketPrefix, innerSpace),
                    null
                );
            }

            return result;
        }
        else if (type is TupleTypeSyntax tupleType)
        {
            // Handle tuple types like (int, string) or (int x, string y)
            _cursor = tupleType.OpenParenToken.Span.End;

            var elements = new List<JRightPadded<VariableDeclarations>>();
            for (int i = 0; i < tupleType.Elements.Count; i++)
            {
                var element = tupleType.Elements[i];
                var elemPrefix = ExtractPrefix(element);

                // Parse the type
                var elemType = VisitType(element.Type);

                // Parse the optional name
                var variables = new List<JRightPadded<NamedVariable>>();
                if (element.Identifier != default && !element.Identifier.IsMissing)
                {
                    var namePrefix = ExtractSpaceBefore(element.Identifier);
                    _cursor = element.Identifier.Span.End;
                    var name = new Identifier(Guid.NewGuid(), namePrefix, Markers.Empty, element.Identifier.Text, null);
                    var namedVar = new NamedVariable(Guid.NewGuid(), Space.Empty, Markers.Empty, name, [], null, null);
                    variables.Add(new JRightPadded<NamedVariable>(namedVar, Space.Empty, Markers.Empty));
                }

                var varDecl = new VariableDeclarations(
                    Guid.NewGuid(),
                    elemPrefix,
                    Markers.Empty,
                    [], [], elemType, null, [], variables
                );

                // Get space after element (before comma or close paren)
                var afterSpace = Space.Empty;
                if (i < tupleType.Elements.Count - 1)
                {
                    var sep = tupleType.Elements.GetSeparator(i);
                    afterSpace = ExtractSpaceBefore(sep);
                    _cursor = sep.Span.End;
                }
                else
                {
                    afterSpace = ExtractSpaceBefore(tupleType.CloseParenToken);
                }

                elements.Add(new JRightPadded<VariableDeclarations>(varDecl, afterSpace, Markers.Empty));
            }

            _cursor = tupleType.CloseParenToken.Span.End;

            return new TupleType(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                new JContainer<VariableDeclarations>(Space.Empty, elements, Markers.Empty)
            );
        }
        else if (type is NullableTypeSyntax nullableType)
        {
            var elementType = VisitType(nullableType.ElementType);
            if (elementType == null)
            {
                throw new InvalidOperationException($"Unable to parse nullable element type: {nullableType.ElementType}");
            }

            var questionPrefix = ExtractSpaceBefore(nullableType.QuestionToken);
            _cursor = nullableType.QuestionToken.Span.End;

            return new NullableType(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                [],
                new JRightPadded<TypeTree>(elementType, questionPrefix, Markers.Empty),
                _typeMapping?.Type(type)
            );
        }
        else if (type is PointerTypeSyntax pointerType)
        {
            var elementType = VisitType(pointerType.ElementType);
            if (elementType == null)
            {
                throw new InvalidOperationException($"Unable to parse pointer element type: {pointerType.ElementType}");
            }

            var starPrefix = ExtractSpaceBefore(pointerType.AsteriskToken);
            _cursor = pointerType.AsteriskToken.Span.End;

            return new PointerType(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                new JRightPadded<TypeTree>(elementType, starPrefix, Markers.Empty)
            );
        }

        else if (type is RefTypeSyntax refType)
        {
            _cursor = refType.RefKeyword.Span.End;

            Modifier? readonlyKeyword = null;
            if (refType.ReadOnlyKeyword != default)
            {
                var readonlyPrefix = ExtractSpaceBefore(refType.ReadOnlyKeyword);
                _cursor = refType.ReadOnlyKeyword.Span.End;
                readonlyKeyword = new Modifier(Guid.NewGuid(), readonlyPrefix, Markers.Empty, Modifier.ModifierType.Readonly, []);
            }

            var innerType = VisitType(refType.Type);
            if (innerType == null)
            {
                throw new InvalidOperationException($"Unable to parse ref element type: {refType.Type}");
            }

            return new RefType(Guid.NewGuid(), prefix, Markers.Empty, readonlyKeyword, innerType, _typeMapping?.Type(type));
        }
        else if (type is ScopedTypeSyntax scopedType)
        {
            // 'scoped' is a modifier on the enclosing declaration, not a type wrapper.
            // We advance past the keyword and recurse on the inner type.
            // The 'scoped' modifier is handled at the parameter/variable declaration level.
            _cursor = scopedType.ScopedKeyword.Span.End;
            return VisitType(scopedType.Type);
        }
        else if (type is FunctionPointerTypeSyntax functionPointerType)
        {
            return (TypeTree)VisitFunctionPointerType(functionPointerType);
        }
        else if (type is AliasQualifiedNameSyntax aliasQualified)
        {
            _cursor = aliasQualified.Alias.Identifier.Span.End;
            var alias = new Identifier(Guid.NewGuid(), prefix, Markers.Empty, aliasQualified.Alias.Identifier.Text, null);

            var colonColonPrefix = ExtractSpaceBefore(aliasQualified.ColonColonToken);
            _cursor = aliasQualified.ColonColonToken.Span.End;

            var name = (Expression)Visit(aliasQualified.Name)!;

            return new AliasQualifiedName(Guid.NewGuid(), Space.Empty, Markers.Empty,
                new JRightPadded<Identifier>(alias, colonColonPrefix, Markers.Empty), name);
        }

        // For now, handle other types as identifiers
        _cursor = type.Span.End;
        return new Identifier(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            type.ToString(),
            null
        );
    }

    private FieldAccess VisitQualifiedName(QualifiedNameSyntax qualified, Space prefix)
    {
        // Build up the qualified name as nested FieldAccess
        // System.Collections.Generic becomes FieldAccess(FieldAccess(System, Collections), Generic)

        // Get the left side (could be another qualified name or simple identifier)
        Expression left;
        if (qualified.Left is QualifiedNameSyntax leftQualified)
        {
            left = VisitQualifiedName(leftQualified, prefix);
            prefix = Space.Empty; // prefix was used by leftmost part
        }
        else if (qualified.Left is IdentifierNameSyntax leftIdent)
        {
            _cursor = leftIdent.Identifier.Span.End;
            left = new Identifier(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                leftIdent.Identifier.Text,
                null
            );
            prefix = Space.Empty;
        }
        else
        {
            // Fallback for other left types
            _cursor = qualified.Left.Span.End;
            left = new Identifier(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                qualified.Left.ToString(),
                null
            );
            prefix = Space.Empty;
        }

        // Get space before the dot
        var dotSpace = ExtractSpaceBefore(qualified.DotToken);
        _cursor = qualified.DotToken.Span.End;

        // Get the right side identifier
        var rightPrefix = ExtractPrefix(qualified.Right);
        _cursor = qualified.Right.Identifier.Span.End;
        var right = new Identifier(
            Guid.NewGuid(),
            rightPrefix,
            Markers.Empty,
            qualified.Right.Identifier.Text,
            null
        );

        return new FieldAccess(
            Guid.NewGuid(),
            Space.Empty, // prefix was applied to leftmost
            Markers.Empty,
            left,
            new JLeftPadded<Identifier>(dotSpace, right),
            null
        );
    }

    private static JavaType.PrimitiveKind MapPredefinedType(SyntaxKind kind)
    {
        return kind switch
        {
            SyntaxKind.IntKeyword => JavaType.PrimitiveKind.Int,
            SyntaxKind.LongKeyword => JavaType.PrimitiveKind.Long,
            SyntaxKind.ShortKeyword => JavaType.PrimitiveKind.Short,
            SyntaxKind.ByteKeyword => JavaType.PrimitiveKind.Byte,
            SyntaxKind.FloatKeyword => JavaType.PrimitiveKind.Float,
            SyntaxKind.DoubleKeyword => JavaType.PrimitiveKind.Double,
            SyntaxKind.BoolKeyword => JavaType.PrimitiveKind.Boolean,
            SyntaxKind.CharKeyword => JavaType.PrimitiveKind.Char,
            SyntaxKind.StringKeyword => JavaType.PrimitiveKind.String,
            SyntaxKind.VoidKeyword => JavaType.PrimitiveKind.Void,
            _ => JavaType.PrimitiveKind.None
        };
    }

    private static Modifier.ModifierType MapModifier(SyntaxKind kind)
    {
        return kind switch
        {
            SyntaxKind.PublicKeyword => Modifier.ModifierType.Public,
            SyntaxKind.PrivateKeyword => Modifier.ModifierType.Private,
            SyntaxKind.ProtectedKeyword => Modifier.ModifierType.Protected,
            SyntaxKind.InternalKeyword => Modifier.ModifierType.Internal,
            SyntaxKind.StaticKeyword => Modifier.ModifierType.Static,
            SyntaxKind.AbstractKeyword => Modifier.ModifierType.Abstract,
            SyntaxKind.SealedKeyword => Modifier.ModifierType.Sealed,
            SyntaxKind.VirtualKeyword => Modifier.ModifierType.Virtual,
            SyntaxKind.OverrideKeyword => Modifier.ModifierType.Override,
            SyntaxKind.ReadOnlyKeyword => Modifier.ModifierType.Readonly,
            SyntaxKind.ConstKeyword => Modifier.ModifierType.Const,
            SyntaxKind.NewKeyword => Modifier.ModifierType.New,
            SyntaxKind.ExternKeyword => Modifier.ModifierType.Extern,
            SyntaxKind.UnsafeKeyword => Modifier.ModifierType.Unsafe,
            SyntaxKind.PartialKeyword => Modifier.ModifierType.Partial,
            SyntaxKind.AsyncKeyword => Modifier.ModifierType.Async,
            SyntaxKind.VolatileKeyword => Modifier.ModifierType.Volatile,
            SyntaxKind.RefKeyword => Modifier.ModifierType.Ref,
            SyntaxKind.OutKeyword => Modifier.ModifierType.Out,
            SyntaxKind.InKeyword => Modifier.ModifierType.In,
            _ => Modifier.ModifierType.LanguageExtension
        };
    }

    #region Preprocessor Directive Processing

    /// <summary>
    /// Scans the source text between _cursor and targetPosition for preprocessor directive lines.
    /// Creates LST nodes for simple (non-conditional) directives.
    /// Stops processing when encountering conditional directives (#if, #elif, #else, #endif).
    /// </summary>
    private List<Statement> ProcessGapDirectives(int targetPosition)
    {
        var directives = new List<Statement>();

        while (_cursor < targetPosition)
        {
            var hashPos = FindDirectiveStart(_cursor, targetPosition);
            if (hashPos < 0) break;

            // Extract the keyword after '#'
            var kwStart = hashPos + 1;
            while (kwStart < targetPosition && _source[kwStart] == ' ') kwStart++;
            var keyword = GetDirectiveKeyword(kwStart, targetPosition);

            // Stop at conditional directives — let them be consumed by ExtractPrefix
            if (keyword is "if" or "elif" or "else" or "endif")
                break;

            // Capture prefix (whitespace before '#')
            var prefix = _cursor < hashPos ? Space.Format(_source[_cursor..hashPos]) : Space.Empty;

            // Find end of directive content (before line ending)
            var contentEnd = hashPos;
            while (contentEnd < targetPosition && _source[contentEnd] != '\r' && _source[contentEnd] != '\n')
                contentEnd++;

            var directiveText = _source[hashPos..contentEnd];
            var directive = ParseDirectiveText(prefix, directiveText);
            if (directive != null)
                directives.Add(directive);

            _cursor = contentEnd;
        }

        return directives;
    }

    /// <summary>
    /// Finds the next '#' that starts a preprocessor directive (first non-whitespace on its line).
    /// </summary>
    private int FindDirectiveStart(int from, int to)
    {
        for (int i = from; i < to; i++)
        {
            if (_source[i] == '#')
            {
                // Check if only whitespace precedes it on this line
                var lineStart = i;
                while (lineStart > from && _source[lineStart - 1] != '\n')
                    lineStart--;
                var onlyWhitespace = true;
                for (int j = lineStart; j < i; j++)
                {
                    if (_source[j] != ' ' && _source[j] != '\t' && _source[j] != '\r')
                    {
                        onlyWhitespace = false;
                        break;
                    }
                }
                if (onlyWhitespace) return i;
            }
        }
        return -1;
    }

    /// <summary>
    /// Extracts the directive keyword starting at the given position.
    /// </summary>
    private string GetDirectiveKeyword(int pos, int maxPos)
    {
        var start = pos;
        while (pos < maxPos && char.IsLetter(_source[pos])) pos++;
        return pos > start ? _source[start..pos] : "";
    }

    /// <summary>
    /// Parses a directive line (starting with '#') into an LST node.
    /// </summary>
    private Statement? ParseDirectiveText(Space prefix, string text)
    {
        // Remove '#' and any whitespace between '#' and keyword
        var afterHash = text[1..].TrimStart();

        // Extract the first keyword
        var spaceIdx = afterHash.IndexOfAny([' ', '\t']);
        var keyword = spaceIdx >= 0 ? afterHash[..spaceIdx] : afterHash;
        var afterKeyword = spaceIdx >= 0 ? afterHash[spaceIdx..] : "";

        return keyword switch
        {
            "region" => ParseRegionDirective(prefix, afterKeyword),
            "endregion" => new EndRegionDirective(Guid.NewGuid(), prefix, Markers.Empty),
            "pragma" => ParsePragmaDirective(prefix, afterKeyword),
            "nullable" => ParseNullableDirective(prefix, afterKeyword),
            "define" => ParseDefineDirective(prefix, afterKeyword),
            "undef" => ParseUndefDirective(prefix, afterKeyword),
            "error" => ParseErrorDirective(prefix, afterKeyword),
            "warning" => ParseWarningDirective(prefix, afterKeyword),
            "line" => ParseLineDirective(prefix, afterKeyword),
            _ => null
        };
    }

    private RegionDirective ParseRegionDirective(Space prefix, string afterKeyword)
    {
        var name = afterKeyword.TrimStart();
        return new RegionDirective(Guid.NewGuid(), prefix, Markers.Empty,
            name.Length > 0 ? name : null);
    }

    private Statement? ParsePragmaDirective(Space prefix, string afterKeyword)
    {
        var rest = afterKeyword.TrimStart();
        if (!rest.StartsWith("warning")) return null;

        rest = rest[7..].TrimStart(); // skip "warning"

        PragmaWarningAction action;
        string afterAction;
        if (rest.StartsWith("disable"))
        {
            action = PragmaWarningAction.Disable;
            afterAction = rest[7..];
        }
        else if (rest.StartsWith("restore"))
        {
            action = PragmaWarningAction.Restore;
            afterAction = rest[7..];
        }
        else
        {
            return null;
        }

        var codes = new List<JRightPadded<Expression>>();
        if (afterAction.Length > 0)
        {
            var parts = afterAction.Split(',');
            foreach (var part in parts)
            {
                var trimmed = part.TrimStart();
                if (trimmed.Length == 0) continue;
                var spaceLen = part.Length - trimmed.Length;
                var codePrefix = spaceLen > 0 ? Space.Format(part[..spaceLen]) : Space.Empty;
                var code = new Identifier(Guid.NewGuid(), codePrefix, Markers.Empty, trimmed, null);
                codes.Add(new JRightPadded<Expression>(code, Space.Empty, Markers.Empty));
            }
        }

        return new PragmaWarningDirective(Guid.NewGuid(), prefix, Markers.Empty, action, codes);
    }

    private NullableDirective ParseNullableDirective(Space prefix, string afterKeyword)
    {
        var parts = afterKeyword.TrimStart().Split(' ', StringSplitOptions.RemoveEmptyEntries);

        var setting = parts.Length > 0 ? parts[0] switch
        {
            "enable" => NullableSetting.Enable,
            "disable" => NullableSetting.Disable,
            "restore" => NullableSetting.Restore,
            _ => NullableSetting.Enable
        } : NullableSetting.Enable;

        NullableTarget? target = null;
        if (parts.Length > 1)
        {
            target = parts[1] switch
            {
                "annotations" => NullableTarget.Annotations,
                "warnings" => NullableTarget.Warnings,
                _ => null
            };
        }

        return new NullableDirective(Guid.NewGuid(), prefix, Markers.Empty, setting, target);
    }

    private DefineDirective ParseDefineDirective(Space prefix, string afterKeyword)
    {
        var spaceLen = afterKeyword.Length - afterKeyword.TrimStart().Length;
        var symbolPrefix = spaceLen > 0 ? Space.Format(afterKeyword[..spaceLen]) : Space.Empty;
        var symbolName = afterKeyword.TrimStart();
        return new DefineDirective(Guid.NewGuid(), prefix, Markers.Empty,
            new Identifier(Guid.NewGuid(), symbolPrefix, Markers.Empty, symbolName, null));
    }

    private UndefDirective ParseUndefDirective(Space prefix, string afterKeyword)
    {
        var spaceLen = afterKeyword.Length - afterKeyword.TrimStart().Length;
        var symbolPrefix = spaceLen > 0 ? Space.Format(afterKeyword[..spaceLen]) : Space.Empty;
        var symbolName = afterKeyword.TrimStart();
        return new UndefDirective(Guid.NewGuid(), prefix, Markers.Empty,
            new Identifier(Guid.NewGuid(), symbolPrefix, Markers.Empty, symbolName, null));
    }

    private ErrorDirective ParseErrorDirective(Space prefix, string afterKeyword)
    {
        return new ErrorDirective(Guid.NewGuid(), prefix, Markers.Empty, afterKeyword.TrimStart());
    }

    private WarningDirective ParseWarningDirective(Space prefix, string afterKeyword)
    {
        return new WarningDirective(Guid.NewGuid(), prefix, Markers.Empty, afterKeyword.TrimStart());
    }

    private LineDirective ParseLineDirective(Space prefix, string afterKeyword)
    {
        var rest = afterKeyword.TrimStart();

        if (rest.StartsWith("hidden"))
            return new LineDirective(Guid.NewGuid(), prefix, Markers.Empty, LineKind.Hidden, null, null);
        if (rest.StartsWith("default"))
            return new LineDirective(Guid.NewGuid(), prefix, Markers.Empty, LineKind.Default, null, null);

        // Numeric: " 200" or " 200 \"file.cs\""
        var spaceLen = afterKeyword.Length - afterKeyword.TrimStart().Length;
        var linePrefix = spaceLen > 0 ? Space.Format(afterKeyword[..spaceLen]) : Space.Empty;

        // Parse line number
        var numEnd = 0;
        while (numEnd < rest.Length && char.IsDigit(rest[numEnd])) numEnd++;
        var lineNum = rest[..numEnd];
        var line = new Literal(
            Guid.NewGuid(), linePrefix, Markers.Empty,
            int.Parse(lineNum), lineNum, null,
            new JavaType.Primitive(JavaType.PrimitiveKind.Int)
        );

        // Optional file
        Expression? file = null;
        if (numEnd < rest.Length)
        {
            var fileRest = rest[numEnd..];
            var fileSpaceLen = fileRest.Length - fileRest.TrimStart().Length;
            var fileTrimmed = fileRest.TrimStart();
            if (fileTrimmed.StartsWith('"'))
            {
                var filePrefix = fileSpaceLen > 0 ? Space.Format(fileRest[..fileSpaceLen]) : Space.Empty;
                file = new Literal(
                    Guid.NewGuid(), filePrefix, Markers.Empty,
                    fileTrimmed.Trim('"'), fileTrimmed, null,
                    new JavaType.Primitive(JavaType.PrimitiveKind.String)
                );
            }
        }

        return new LineDirective(Guid.NewGuid(), prefix, Markers.Empty, LineKind.Numeric, line, file);
    }

    #endregion

    #region LINQ

    public override J VisitQueryExpression(QueryExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        var fromClause = (FromClause)VisitFromClause(node.FromClause);
        var body = (QueryBody)VisitQueryBody(node.Body);

        return new QueryExpression(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            fromClause,
            body
        );
    }

    public override J VisitQueryBody(QueryBodySyntax node)
    {
        var prefix = ExtractPrefix(node);

        var clauses = new List<QueryClause>();
        foreach (var clause in node.Clauses)
        {
            var visited = Visit(clause);
            if (visited is QueryClause qc)
                clauses.Add(qc);
        }

        SelectOrGroupClause? selectOrGroup = null;
        if (node.SelectOrGroup != null)
        {
            var visited = Visit(node.SelectOrGroup);
            selectOrGroup = visited as SelectOrGroupClause;
        }

        QueryContinuation? continuation = null;
        if (node.Continuation != null)
        {
            continuation = (QueryContinuation)VisitQueryContinuation(node.Continuation);
        }

        return new QueryBody(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            clauses,
            selectOrGroup,
            continuation
        );
    }

    public override J VisitFromClause(FromClauseSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.FromKeyword.Span.End;

        // Optional type
        TypeTree? typeIdentifier = null;
        if (node.Type != null)
        {
            typeIdentifier = (TypeTree)Visit(node.Type);
        }

        // Identifier with right padding (space before "in")
        var identPrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var ident = new Identifier(
            Guid.NewGuid(), identPrefix, Markers.Empty,
            node.Identifier.Text, null
        );
        var afterIdent = ExtractSpaceBefore(node.InKeyword);
        _cursor = node.InKeyword.Span.End;

        // Expression
        var expression = (Expression)Visit(node.Expression);

        return new FromClause(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            typeIdentifier,
            new JRightPadded<Identifier>(ident, afterIdent, Markers.Empty),
            expression
        );
    }

    public override J VisitLetClause(LetClauseSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.LetKeyword.Span.End;

        // Identifier with right padding (space before "=")
        var identPrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var ident = new Identifier(
            Guid.NewGuid(), identPrefix, Markers.Empty,
            node.Identifier.Text, null
        );
        var afterIdent = ExtractSpaceBefore(node.EqualsToken);
        _cursor = node.EqualsToken.Span.End;

        var expression = (Expression)Visit(node.Expression);

        return new LetClause(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JRightPadded<Identifier>(ident, afterIdent, Markers.Empty),
            expression
        );
    }

    public override J VisitJoinClause(JoinClauseSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.JoinKeyword.Span.End;

        // Identifier
        var identPrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var ident = new Identifier(
            Guid.NewGuid(), identPrefix, Markers.Empty,
            node.Identifier.Text, null
        );
        var afterIdent = ExtractSpaceBefore(node.InKeyword);
        _cursor = node.InKeyword.Span.End;

        // In expression
        var inExpr = (Expression)Visit(node.InExpression);
        var afterInExpr = ExtractSpaceBefore(node.OnKeyword);
        _cursor = node.OnKeyword.Span.End;

        // Left expression (on)
        var leftExpr = (Expression)Visit(node.LeftExpression);
        var afterLeftExpr = ExtractSpaceBefore(node.EqualsKeyword);
        _cursor = node.EqualsKeyword.Span.End;

        // Right expression (equals)
        var rightExpr = (Expression)Visit(node.RightExpression);

        // Optional into
        JLeftPadded<JoinIntoClause>? into = null;
        if (node.Into != null)
        {
            var intoPrefix = ExtractPrefix(node.Into);
            var joinInto = (JoinIntoClause)VisitJoinIntoClause(node.Into);
            into = new JLeftPadded<JoinIntoClause>(intoPrefix, joinInto);
        }

        return new JoinClause(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JRightPadded<Identifier>(ident, afterIdent, Markers.Empty),
            new JRightPadded<Expression>(inExpr, afterInExpr, Markers.Empty),
            new JRightPadded<Expression>(leftExpr, afterLeftExpr, Markers.Empty),
            rightExpr,
            into
        );
    }

    public override J VisitJoinIntoClause(JoinIntoClauseSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.IntoKeyword.Span.End;

        var identPrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var ident = new Identifier(
            Guid.NewGuid(), identPrefix, Markers.Empty,
            node.Identifier.Text, null
        );

        return new JoinIntoClause(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            ident
        );
    }

    public override J VisitWhereClause(WhereClauseSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.WhereKeyword.Span.End;

        var condition = (Expression)Visit(node.Condition);

        return new WhereClause(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            condition
        );
    }

    public override J VisitOrderByClause(OrderByClauseSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.OrderByKeyword.Span.End;

        var orderings = new List<JRightPadded<Ordering>>();
        for (int i = 0; i < node.Orderings.Count; i++)
        {
            var orderingSyntax = node.Orderings[i];
            var ordering = (Ordering)VisitOrdering(orderingSyntax);

            Space after = Space.Empty;
            if (i < node.Orderings.SeparatorCount)
            {
                var separator = node.Orderings.GetSeparator(i);
                after = ExtractSpaceBefore(separator);
                _cursor = separator.Span.End;
            }

            orderings.Add(new JRightPadded<Ordering>(ordering, after, Markers.Empty));
        }

        return new OrderByClause(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            orderings
        );
    }

    public override J VisitOrdering(OrderingSyntax node)
    {
        var prefix = ExtractPrefix(node);

        var expression = (Expression)Visit(node.Expression);

        DirectionKind? direction = null;
        Space afterExpr = Space.Empty;
        if (node.AscendingOrDescendingKeyword != default)
        {
            afterExpr = ExtractSpaceBefore(node.AscendingOrDescendingKeyword);
            _cursor = node.AscendingOrDescendingKeyword.Span.End;
            direction = node.AscendingOrDescendingKeyword.Kind() == SyntaxKind.DescendingKeyword
                ? DirectionKind.Descending
                : DirectionKind.Ascending;
        }

        return new Ordering(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JRightPadded<Expression>(expression, afterExpr, Markers.Empty),
            direction
        );
    }

    public override J VisitSelectClause(SelectClauseSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.SelectKeyword.Span.End;

        var expression = (Expression)Visit(node.Expression);

        return new SelectClause(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            expression
        );
    }

    public override J VisitGroupClause(GroupClauseSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.GroupKeyword.Span.End;

        var groupExpr = (Expression)Visit(node.GroupExpression);
        var afterGroupExpr = ExtractSpaceBefore(node.ByKeyword);
        _cursor = node.ByKeyword.Span.End;

        var key = (Expression)Visit(node.ByExpression);

        return new GroupClause(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            new JRightPadded<Expression>(groupExpr, afterGroupExpr, Markers.Empty),
            key
        );
    }

    public override J VisitQueryContinuation(QueryContinuationSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.IntoKeyword.Span.End;

        var identPrefix = ExtractSpaceBefore(node.Identifier);
        _cursor = node.Identifier.Span.End;
        var ident = new Identifier(
            Guid.NewGuid(), identPrefix, Markers.Empty,
            node.Identifier.Text, null
        );

        var body = (QueryBody)VisitQueryBody(node.Body);

        return new QueryContinuation(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            ident,
            body
        );
    }

    #endregion

    private Space ExtractSpaceBefore(SyntaxToken token)
    {
        var start = token.SpanStart;
        if (_cursor >= start)
        {
            return Space.Empty;
        }

        var whitespace = _source[_cursor..start];
        _cursor = start;
        return Space.Format(whitespace);
    }

    private static Binary.OperatorType MapBinaryOperator(SyntaxKind kind)
    {
        return kind switch
        {
            SyntaxKind.AddExpression => Binary.OperatorType.Addition,
            SyntaxKind.SubtractExpression => Binary.OperatorType.Subtraction,
            SyntaxKind.MultiplyExpression => Binary.OperatorType.Multiplication,
            SyntaxKind.DivideExpression => Binary.OperatorType.Division,
            SyntaxKind.ModuloExpression => Binary.OperatorType.Modulo,
            SyntaxKind.LessThanExpression => Binary.OperatorType.LessThan,
            SyntaxKind.GreaterThanExpression => Binary.OperatorType.GreaterThan,
            SyntaxKind.LessThanOrEqualExpression => Binary.OperatorType.LessThanOrEqual,
            SyntaxKind.GreaterThanOrEqualExpression => Binary.OperatorType.GreaterThanOrEqual,
            SyntaxKind.EqualsExpression => Binary.OperatorType.Equal,
            SyntaxKind.NotEqualsExpression => Binary.OperatorType.NotEqual,
            SyntaxKind.BitwiseAndExpression => Binary.OperatorType.BitAnd,
            SyntaxKind.BitwiseOrExpression => Binary.OperatorType.BitOr,
            SyntaxKind.ExclusiveOrExpression => Binary.OperatorType.BitXor,
            SyntaxKind.LeftShiftExpression => Binary.OperatorType.LeftShift,
            SyntaxKind.RightShiftExpression => Binary.OperatorType.RightShift,
            SyntaxKind.LogicalOrExpression => Binary.OperatorType.Or,
            SyntaxKind.LogicalAndExpression => Binary.OperatorType.And,
            _ => throw new InvalidOperationException($"Unsupported binary operator: {kind}")
        };
    }

    private Space ExtractPrefix(SyntaxNode node)
    {
        var start = node.SpanStart;
        if (_cursor >= start)
        {
            return Space.Empty;
        }

        var whitespace = _source[_cursor..start];
        _cursor = start;

        // TODO: Parse comments from trivia
        return Space.Format(whitespace);
    }

    private Space ExtractRemaining()
    {
        if (_cursor >= _source.Length)
        {
            return Space.Empty;
        }

        var remaining = _source[_cursor..];
        _cursor = _source.Length;
        return Space.Format(remaining);
    }

    private void SkipTo(int position)
    {
        if (position > _cursor)
        {
            _cursor = position;
        }
    }

    private void SkipToken(SyntaxToken token)
    {
        _cursor = token.Span.End;
    }

    public override J VisitGotoStatement(GotoStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.GotoKeyword.Span.End;

        Keyword? caseOrDefaultKeyword = null;
        if (node.CaseOrDefaultKeyword.Kind() == SyntaxKind.CaseKeyword)
        {
            var kwPrefix = ExtractSpaceBefore(node.CaseOrDefaultKeyword);
            _cursor = node.CaseOrDefaultKeyword.Span.End;
            caseOrDefaultKeyword = new Keyword(Guid.NewGuid(), kwPrefix, Markers.Empty, KeywordKind.Case);
        }
        else if (node.CaseOrDefaultKeyword.Kind() == SyntaxKind.DefaultKeyword)
        {
            var kwPrefix = ExtractSpaceBefore(node.CaseOrDefaultKeyword);
            _cursor = node.CaseOrDefaultKeyword.Span.End;
            caseOrDefaultKeyword = new Keyword(Guid.NewGuid(), kwPrefix, Markers.Empty, KeywordKind.Default);
        }

        Expression? target = null;
        if (node.Expression != null)
        {
            target = (Expression)Visit(node.Expression)!;
        }

        _cursor = node.SemicolonToken.Span.End;
        return new GotoStatement(Guid.NewGuid(), prefix, Markers.Empty, caseOrDefaultKeyword, target);
    }

    public override J VisitUsingStatement(UsingStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);

        Keyword? awaitKeyword = null;
        if (node.AwaitKeyword != default)
        {
            var awaitPrefix = ExtractSpaceBefore(node.AwaitKeyword);
            _cursor = node.AwaitKeyword.Span.End;
            awaitKeyword = new Keyword(Guid.NewGuid(), awaitPrefix, Markers.Empty, KeywordKind.Await);
        }

        _cursor = node.UsingKeyword.Span.End;

        var openParenPrefix = ExtractSpaceBefore(node.OpenParenToken);
        _cursor = node.OpenParenToken.Span.End;

        Expression innerExpr;
        if (node.Declaration != null)
        {
            var declPrefix = ExtractPrefix(node.Declaration);
            var typeExpr = VisitType(node.Declaration.Type);

            var variables = new List<JRightPadded<NamedVariable>>();
            for (int i = 0; i < node.Declaration.Variables.Count; i++)
            {
                var v = node.Declaration.Variables[i];
                var varPrefix = ExtractPrefix(v);

                var namePrefix = ExtractSpaceBefore(v.Identifier);
                _cursor = v.Identifier.Span.End;
                var name = new Identifier(Guid.NewGuid(), namePrefix, Markers.Empty, v.Identifier.Text, null);

                JLeftPadded<Expression>? initializer = null;
                if (v.Initializer != null)
                {
                    var equalsPrefix = ExtractSpaceBefore(v.Initializer.EqualsToken);
                    _cursor = v.Initializer.EqualsToken.Span.End;
                    var initExpr = Visit(v.Initializer.Value);
                    if (initExpr is Expression expr)
                    {
                        initializer = new JLeftPadded<Expression>(equalsPrefix, expr);
                    }
                }

                var namedVar = new NamedVariable(Guid.NewGuid(), varPrefix, Markers.Empty, name, [], initializer, _typeMapping?.VariableType(v));

                Space afterSpace = Space.Empty;
                if (i < node.Declaration.Variables.Count - 1)
                {
                    var separatorToken = node.Declaration.Variables.GetSeparator(i);
                    afterSpace = ExtractSpaceBefore(separatorToken);
                    _cursor = separatorToken.Span.End;
                }

                variables.Add(new JRightPadded<NamedVariable>(namedVar, afterSpace, Markers.Empty));
            }

            var varDecl = new VariableDeclarations(Guid.NewGuid(), declPrefix, Markers.Empty, [], [], typeExpr, null, [], variables);
            innerExpr = new StatementExpression(Guid.NewGuid(), Space.Empty, Markers.Empty, varDecl);
        }
        else
        {
            innerExpr = (Expression)Visit(node.Expression!)!;
        }

        var closeParenPrefix = ExtractSpaceBefore(node.CloseParenToken);
        _cursor = node.CloseParenToken.Span.End;

        var expressionPadded = new JLeftPadded<Expression>(openParenPrefix, innerExpr);
        var statement = (Statement)Visit(node.Statement)!;

        return new UsingStatement(Guid.NewGuid(), prefix, Markers.Empty, awaitKeyword, expressionPadded, statement);
    }

    public override J VisitImplicitElementAccess(ImplicitElementAccessSyntax node)
    {
        var prefix = ExtractPrefix(node);

        var bracketPrefix = ExtractSpaceBefore(node.ArgumentList.OpenBracketToken);
        _cursor = node.ArgumentList.OpenBracketToken.Span.End;

        var args = new List<JRightPadded<CsArgument>>();
        for (int i = 0; i < node.ArgumentList.Arguments.Count; i++)
        {
            var arg = node.ArgumentList.Arguments[i];
            var argExpr = (Expression)Visit(arg.Expression)!;

            var csArg = new CsArgument(Guid.NewGuid(), Space.Empty, Markers.Empty, null, null, argExpr);

            Space afterSpace;
            if (i < node.ArgumentList.Arguments.Count - 1)
            {
                var separatorToken = node.ArgumentList.Arguments.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(separatorToken);
                _cursor = separatorToken.Span.End;
            }
            else
            {
                afterSpace = ExtractSpaceBefore(node.ArgumentList.CloseBracketToken);
            }

            args.Add(new JRightPadded<CsArgument>(csArg, afterSpace, Markers.Empty));
        }

        _cursor = node.ArgumentList.CloseBracketToken.Span.End;
        return new ImplicitElementAccess(Guid.NewGuid(), prefix, Markers.Empty, new JContainer<CsArgument>(bracketPrefix, args, Markers.Empty));
    }

    public override J VisitSlicePattern(SlicePatternSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.DotDotToken.Span.End;
        return new SlicePattern(Guid.NewGuid(), prefix, Markers.Empty);
    }

    public override J VisitListPattern(ListPatternSyntax node)
    {
        var prefix = ExtractPrefix(node);

        var bracketPrefix = ExtractSpaceBefore(node.OpenBracketToken);
        _cursor = node.OpenBracketToken.Span.End;

        var patterns = new List<JRightPadded<Pattern>>();
        for (int i = 0; i < node.Patterns.Count; i++)
        {
            var pattern = (Pattern)Visit(node.Patterns[i])!;

            Space afterSpace;
            if (i < node.Patterns.Count - 1)
            {
                var separatorToken = node.Patterns.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(separatorToken);
                _cursor = separatorToken.Span.End;
            }
            else
            {
                afterSpace = ExtractSpaceBefore(node.CloseBracketToken);
            }

            patterns.Add(new JRightPadded<Pattern>(pattern, afterSpace, Markers.Empty));
        }

        _cursor = node.CloseBracketToken.Span.End;
        return new ListPattern(Guid.NewGuid(), prefix, Markers.Empty, new JContainer<Pattern>(bracketPrefix, patterns, Markers.Empty), null);
    }

    public override J VisitAliasQualifiedName(AliasQualifiedNameSyntax node)
    {
        return VisitType(node) ?? throw new InvalidOperationException($"Unable to parse AliasQualifiedName: {node}");
    }

    public override J VisitForEachVariableStatement(ForEachVariableStatementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.ForEachKeyword.Span.End;

        // Parse open paren
        var controlPrefix = ExtractSpaceBefore(node.OpenParenToken);
        _cursor = node.OpenParenToken.Span.End;

        // Parse the variable expression (e.g., var (x, y) or a tuple pattern)
        var variable = (Expression)Visit(node.Variable)!;

        // Parse 'in' keyword
        var inPrefix = ExtractSpaceBefore(node.InKeyword);
        _cursor = node.InKeyword.Span.End;

        // Parse iterable expression
        var iterable = (Expression)Visit(node.Expression)!;

        // Close paren
        var closeParenPrefix = ExtractSpaceBefore(node.CloseParenToken);
        _cursor = node.CloseParenToken.Span.End;

        var control = new ForEachVariableLoopControl(
            Guid.NewGuid(),
            controlPrefix,
            Markers.Empty,
            new JRightPadded<Expression>(variable, inPrefix, Markers.Empty),
            new JRightPadded<Expression>(iterable, closeParenPrefix, Markers.Empty)
        );

        // Parse body
        var bodyStatement = (Statement)Visit(node.Statement)!;

        return new ForEachVariableLoop(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            control,
            new JRightPadded<Statement>(bodyStatement, Space.Empty, Markers.Empty)
        );
    }

    public override J VisitAnonymousMethodExpression(AnonymousMethodExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);

        // Parse modifiers (async)
        var modifiers = new List<Modifier>();
        foreach (var mod in node.Modifiers)
        {
            var modPrefix = ExtractSpaceBefore(mod);
            _cursor = mod.Span.End;
            modifiers.Add(new Modifier(
                Guid.NewGuid(),
                modPrefix,
                Markers.Empty,
                MapModifier(mod.Kind()),
                []
            ));
        }

        // 'delegate' keyword
        _cursor = node.DelegateKeyword.Span.End;

        // Parse optional parameter list
        var elements = new List<JRightPadded<J>>();
        var paramsPrefix = Space.Empty;
        var parenthesized = node.ParameterList != null;

        if (node.ParameterList != null)
        {
            _cursor = node.ParameterList.OpenParenToken.Span.End;

            for (int i = 0; i < node.ParameterList.Parameters.Count; i++)
            {
                var p = node.ParameterList.Parameters[i];
                var param = ConvertLambdaParameter(p);

                Space afterSpace;
                if (i < node.ParameterList.Parameters.Count - 1)
                {
                    var separatorToken = node.ParameterList.Parameters.GetSeparator(i);
                    afterSpace = ExtractSpaceBefore(separatorToken);
                    _cursor = separatorToken.Span.End;
                }
                else
                {
                    afterSpace = ExtractSpaceBefore(node.ParameterList.CloseParenToken);
                }

                elements.Add(new JRightPadded<J>(param, afterSpace, Markers.Empty));
            }

            if (elements.Count == 0)
            {
                paramsPrefix = ExtractSpaceBefore(node.ParameterList.CloseParenToken);
            }

            _cursor = node.ParameterList.CloseParenToken.Span.End;
        }

        var parameters = new Lambda.Parameters(
            Guid.NewGuid(),
            paramsPrefix,
            Markers.Empty,
            parenthesized,
            elements
        );

        // Parse body (always a block for anonymous methods)
        var body = Visit(node.Body);
        if (body is not J bodyJ)
        {
            throw new InvalidOperationException($"Expected J for anonymous method body but got {body?.GetType().Name}");
        }

        var markers = Markers.Empty.Add(new AnonymousMethod(Guid.NewGuid()));

        var jLambda = new Lambda(
            Guid.NewGuid(),
            modifiers.Count > 0 ? Space.Empty : prefix,
            markers,
            parameters,
            Space.Empty, // No arrow in anonymous methods
            bodyJ,
            _typeMapping?.Type(node)
        );

        if (modifiers.Count > 0)
        {
            return new CsLambda(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                modifiers,
                null,
                jLambda
            );
        }

        return jLambda;
    }

    public override J VisitImplicitStackAllocArrayCreationExpression(ImplicitStackAllocArrayCreationExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.StackAllocKeyword.Span.End;

        // Advance past the [] tokens
        var arrayPrefix = ExtractSpaceBefore(node.OpenBracketToken);
        _cursor = node.CloseBracketToken.Span.End;

        // Parse initializer
        JContainer<Expression>? initializer = null;
        if (node.Initializer != null)
        {
            initializer = ParseArrayInitializer(node.Initializer);
        }

        // Create NewArray with no explicit type (implicit stackalloc[] { ... })
        var newArray = new NewArray(
            Guid.NewGuid(), arrayPrefix, Markers.Empty,
            null, // No type expression for implicit stack alloc
            [new ArrayDimension(
                Guid.NewGuid(), Space.Empty, Markers.Empty,
                new JRightPadded<Expression>(
                    new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty),
                    Space.Empty, Markers.Empty))],
            initializer, null);

        return new StackAllocExpression(Guid.NewGuid(), prefix, Markers.Empty, newArray);
    }

    public override J VisitAnonymousObjectCreationExpression(AnonymousObjectCreationExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.NewKeyword.Span.End;

        // Parse the initializer block
        var beforeBrace = ExtractSpaceBefore(node.OpenBraceToken);
        _cursor = node.OpenBraceToken.Span.End;

        var elements = new List<JRightPadded<Expression>>();
        for (int i = 0; i < node.Initializers.Count; i++)
        {
            var expr = (Expression)Visit(node.Initializers[i])!;
            Space afterSpace;
            if (i < node.Initializers.SeparatorCount)
            {
                var sep = node.Initializers.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(sep);
                _cursor = sep.Span.End;
            }
            else
            {
                afterSpace = ExtractSpaceBefore(node.CloseBraceToken);
            }
            elements.Add(new JRightPadded<Expression>(expr, afterSpace, Markers.Empty));
        }

        if (node.Initializers.Count == 0)
        {
            // Empty: new { }
            beforeBrace = ExtractSpaceBefore(node.CloseBraceToken);
        }

        _cursor = node.CloseBraceToken.Span.End;

        return new AnonymousObjectCreationExpression(Guid.NewGuid(), prefix, Markers.Empty,
            new JContainer<Expression>(beforeBrace, elements, Markers.Empty), null);
    }

    public override J VisitAnonymousObjectMemberDeclarator(AnonymousObjectMemberDeclaratorSyntax node)
    {
        // Inside anonymous object: Name = value or just value
        if (node.NameEquals != null)
        {
            var prefix = ExtractPrefix(node);
            var name = new Identifier(Guid.NewGuid(), Space.Empty, Markers.Empty, node.NameEquals.Name.Identifier.Text, null);
            _cursor = node.NameEquals.Name.Span.End;

            var eqSpace = ExtractSpaceBefore(node.NameEquals.EqualsToken);
            _cursor = node.NameEquals.EqualsToken.Span.End;

            var value = (Expression)Visit(node.Expression)!;
            return new Assignment(Guid.NewGuid(), prefix, Markers.Empty, name,
                new JLeftPadded<Expression>(eqSpace, value), null);
        }
        else
        {
            return (Expression)Visit(node.Expression)!;
        }
    }

    public override J VisitWithExpression(WithExpressionSyntax node)
    {
        var prefix = ExtractPrefix(node);
        var expression = (Expression)Visit(node.Expression)!;

        var withSpace = ExtractSpaceBefore(node.WithKeyword);
        _cursor = node.WithKeyword.Span.End;

        var initializer = (Expression)Visit(node.Initializer)!;

        return new WithExpression(Guid.NewGuid(), prefix, Markers.Empty,
            expression, new JLeftPadded<Expression>(withSpace, initializer), null);
    }

    public override J VisitSpreadElement(SpreadElementSyntax node)
    {
        var prefix = ExtractPrefix(node);
        _cursor = node.OperatorToken.Span.End;

        var expression = (Expression)Visit(node.Expression)!;

        return new SpreadExpression(Guid.NewGuid(), prefix, Markers.Empty, expression, null);
    }

    public override J VisitFunctionPointerType(FunctionPointerTypeSyntax node)
    {
        var prefix = ExtractPrefix(node);
        // Advance past 'delegate' keyword
        _cursor = node.DelegateKeyword.Span.End;
        // Advance past '*'
        _cursor = node.AsteriskToken.Span.End;

        JLeftPadded<CallingConventionKind>? callingConvention = null;
        JContainer<Identifier>? unmanagedConventionTypes = null;

        if (node.CallingConvention != null)
        {
            var convSpace = ExtractSpaceBefore(node.CallingConvention.ManagedOrUnmanagedKeyword);
            _cursor = node.CallingConvention.ManagedOrUnmanagedKeyword.Span.End;

            var kind = node.CallingConvention.ManagedOrUnmanagedKeyword.Text == "managed"
                ? CallingConventionKind.Managed
                : CallingConventionKind.Unmanaged;

            callingConvention = new JLeftPadded<CallingConventionKind>(convSpace, kind);

            if (node.CallingConvention.UnmanagedCallingConventionList != null)
            {
                var bracketSpace = ExtractSpaceBefore(node.CallingConvention.UnmanagedCallingConventionList.OpenBracketToken);
                _cursor = node.CallingConvention.UnmanagedCallingConventionList.OpenBracketToken.Span.End;

                var types = new List<JRightPadded<Identifier>>();
                var callingConventionList = node.CallingConvention.UnmanagedCallingConventionList.CallingConventions;
                for (int i = 0; i < callingConventionList.Count; i++)
                {
                    var convType = callingConventionList[i];
                    var typePrefix = ExtractSpaceBefore(convType.Name);
                    _cursor = convType.Name.Span.End;
                    var id = new Identifier(Guid.NewGuid(), typePrefix, Markers.Empty, convType.Name.Text, null);

                    Space afterSpace;
                    if (i < callingConventionList.SeparatorCount)
                    {
                        var sep = callingConventionList.GetSeparator(i);
                        afterSpace = ExtractSpaceBefore(sep);
                        _cursor = sep.Span.End;
                    }
                    else
                    {
                        afterSpace = ExtractSpaceBefore(node.CallingConvention.UnmanagedCallingConventionList.CloseBracketToken);
                    }
                    types.Add(new JRightPadded<Identifier>(id, afterSpace, Markers.Empty));
                }
                _cursor = node.CallingConvention.UnmanagedCallingConventionList.CloseBracketToken.Span.End;

                unmanagedConventionTypes = new JContainer<Identifier>(bracketSpace, types, Markers.Empty);
            }
        }

        // Parse parameter list: <type1, type2, ..., returnType>
        var angleBracketSpace = ExtractSpaceBefore(node.ParameterList.LessThanToken);
        _cursor = node.ParameterList.LessThanToken.Span.End;

        var paramTypes = new List<JRightPadded<TypeTree>>();
        for (int i = 0; i < node.ParameterList.Parameters.Count; i++)
        {
            var param = node.ParameterList.Parameters[i];
            var typeExpr = VisitType(param.Type)!;

            Space afterSpace;
            if (i < node.ParameterList.Parameters.SeparatorCount)
            {
                var sep = node.ParameterList.Parameters.GetSeparator(i);
                afterSpace = ExtractSpaceBefore(sep);
                _cursor = sep.Span.End;
            }
            else
            {
                afterSpace = ExtractSpaceBefore(node.ParameterList.GreaterThanToken);
            }
            paramTypes.Add(new JRightPadded<TypeTree>(typeExpr, afterSpace, Markers.Empty));
        }
        _cursor = node.ParameterList.GreaterThanToken.Span.End;

        return new FunctionPointerType(Guid.NewGuid(), prefix, Markers.Empty,
            callingConvention, unmanagedConventionTypes,
            new JContainer<TypeTree>(angleBracketSpace, paramTypes, Markers.Empty), null);
    }

    private static JavaType.Primitive? GetPrimitiveType(SyntaxKind kind)
    {
        return kind switch
        {
            SyntaxKind.StringLiteralExpression => new JavaType.Primitive(JavaType.PrimitiveKind.String),
            SyntaxKind.NumericLiteralExpression => new JavaType.Primitive(JavaType.PrimitiveKind.Int),
            SyntaxKind.TrueLiteralExpression => new JavaType.Primitive(JavaType.PrimitiveKind.Boolean),
            SyntaxKind.FalseLiteralExpression => new JavaType.Primitive(JavaType.PrimitiveKind.Boolean),
            SyntaxKind.CharacterLiteralExpression => new JavaType.Primitive(JavaType.PrimitiveKind.Char),
            SyntaxKind.NullLiteralExpression => new JavaType.Primitive(JavaType.PrimitiveKind.Null),
            _ => null
        };
    }
}
