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
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using OpenRewrite.Test;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Core;

/// <summary>
/// Two sources a test gives with the same text are two source files, as two such files in a repository are.
/// </summary>
public class IdenticalSourcesTests : RewriteTest
{
    [Fact]
    public void EachIsASourceFile()
    {
        RewriteRun(
            spec => spec.SetRecipe(new NumberClassesBySourceFiles()),
            CSharp("class A { }", "class A2 { }"),
            CSharp("class A { }", "class A2 { }")
        );
    }

    [Fact]
    public void EachIsASourceFileBesideAnEditorConfig()
    {
        RewriteRun(
            spec => spec.SetRecipe(new NumberClassesBySourceFiles()),
            EditorConfig("root = true\n"),
            CSharp("class A { }", "class A2 { }"),
            CSharp("class A { }", "class A2 { }")
        );
    }

    /// <summary>Renames each class <c>A</c> to <c>A</c> followed by the number of source files it scanned.</summary>
    private class NumberClassesBySourceFiles : ScanningRecipe<NumberClassesBySourceFiles.Count>
    {
        public override string DisplayName => "Number classes by source files";
        public override string Description => "Renames each class A after the number of source files.";

        public override Count GetInitialValue(ExecutionContext ctx) => new();

        public override ITreeVisitor<ExecutionContext> GetScanner(Count count) => new Scanner(count);

        public override ITreeVisitor<ExecutionContext> GetVisitor(Count count) => new Renamer(count);

        public class Count
        {
            public int SourceFiles;
        }

        private class Scanner(Count count) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitCompilationUnit(CompilationUnit cu, ExecutionContext ctx)
            {
                count.SourceFiles++;
                return cu;
            }
        }

        private class Renamer(Count count) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitClassDeclaration(ClassDeclaration classDecl, ExecutionContext ctx)
            {
                return classDecl.Name.SimpleName == "A"
                    ? classDecl.WithName(classDecl.Name.WithSimpleName("A" + count.SourceFiles))
                    : classDecl;
            }
        }
    }
}
