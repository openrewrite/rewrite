using OpenRewrite.Test;

namespace OpenRewrite.Tests;

public class CSharpSyntaxFragments
{
    public static IEnumerable<SourceTestCase> GetData()
    {
        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_Single", """

            class C<T> where T : allows ref struct
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_Single_MissingRefAndStruct", """

            class C<T> where T : allows
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_TwoInARow", """

            class C<T> where T : allows ref struct, ref struct
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_TwoInARow_MissingRef", """

            class C<T> where T : allows ref struct, struct
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_TwoAllowsInARow", """

            class C<T> where T : allows ref struct, allows ref struct
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_FollowedByWhere_01", """

            class C<T, S> where T : allows ref struct where S : class
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_FollowedByWhere_02", """

            class C<T, S> where T : struct, allows ref struct where S : class
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_AfterStruct", """

            class C<T> where T : struct, allows ref struct
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_AfterClass", """

            class C<T> where T : class, allows ref struct
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_AfterDefault", """

            class C<T> where T : default, allows ref struct
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_AfterUnmanaged", """

            class C<T> where T : unmanaged, allows ref struct
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_AfterNotNull", """

            class C<T> where T : notnull, allows ref struct
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_AfterTypeConstraint", """

            class C<T> where T : SomeType, allows ref struct
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_AfterNew", """

            class C<T> where T : new(), allows ref struct
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_AfterMultiple", """

            class C<T> where T : struct, SomeType, new(), allows ref struct
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_BeforeClass", """

            class C<T> where T : allows ref struct, class
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_BeforeDefault", """

            class C<T> where T : allows ref struct, default
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_BeforeUnmanaged", """

            class C<T> where T : allows ref struct, unmanaged
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_BeforeNotNull", """

            class C<T> where T : allows ref struct, notnull
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_BeforeTypeConstraint", """

            class C<T> where T : allows ref struct, SomeType
            {}
            """);

        yield return new SourceTestCase("AllowsConstraintParsing.RefStruct_BeforeNew", """

            class C<T> where T : allows ref struct, new()
            {}
            """);

        yield return new SourceTestCase("AsyncParsingTests.AsyncAsType_Indexer_ExpressionBody_ErrorCase", "interface async { async this[async i] => null; }");

        yield return new SourceTestCase("DeclarationParsingTests.TestExternAlias", "extern alias a;");

        yield return new SourceTestCase("DeclarationParsingTests.TestUsing", "using a;");

        yield return new SourceTestCase("DeclarationParsingTests.TestUsingStatic", "using static a;");

        yield return new SourceTestCase("DeclarationParsingTests.TestUsingDottedName", "using a.b;");

        yield return new SourceTestCase("DeclarationParsingTests.TestUsingStaticDottedName", "using static a.b;");

        yield return new SourceTestCase("DeclarationParsingTests.TestUsingStaticGenericName", "using static a<int?>;");

        yield return new SourceTestCase("DeclarationParsingTests.TestUsingAliasName", "using a = b;");

        yield return new SourceTestCase("DeclarationParsingTests.TestUsingAliasGenericName", "using a = b<c>;");

        yield return new SourceTestCase("DeclarationParsingTests.TestGlobalAttribute", "[assembly:a]");

        yield return new SourceTestCase("DeclarationParsingTests.TestGlobalAttribute_Verbatim", "[@assembly:a]");

        yield return new SourceTestCase("DeclarationParsingTests.TestGlobalAttribute_Escape", @"[as\u0073embly:a]");

        yield return new SourceTestCase("DeclarationParsingTests.TestGlobalModuleAttribute", "[module:a]");

        yield return new SourceTestCase("DeclarationParsingTests.TestGlobalModuleAttribute_Verbatim", "[@module:a]");

        yield return new SourceTestCase("DeclarationParsingTests.TestGlobalAttributeWithParentheses", "[assembly:a()]");

        yield return new SourceTestCase("DeclarationParsingTests.TestGlobalAttributeWithMultipleArguments", "[assembly:a(b, c)]");

        yield return new SourceTestCase("DeclarationParsingTests.TestGlobalAttributeWithNamedArguments", "[assembly:a(b = c)]");

        yield return new SourceTestCase("DeclarationParsingTests.TestGlobalAttributeWithMultipleAttributes", "[assembly:a, b]");

        yield return new SourceTestCase("DeclarationParsingTests.TestMultipleGlobalAttributeDeclarations", "[assembly:a] [assembly:b]");

        yield return new SourceTestCase("DeclarationParsingTests.TestNamespace", "namespace a { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestFileScopedNamespace", "namespace a;");

        yield return new SourceTestCase("DeclarationParsingTests.TestNamespaceWithDottedName", "namespace a.b.c { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestNamespaceWithUsing", "namespace a { using b.c; }");

        yield return new SourceTestCase("DeclarationParsingTests.TestFileScopedNamespaceWithUsing", "namespace a; using b.c;");

        yield return new SourceTestCase("DeclarationParsingTests.TestNamespaceWithExternAlias", "namespace a { extern alias b; }");

        yield return new SourceTestCase("DeclarationParsingTests.TestFileScopedNamespaceWithExternAlias", "namespace a; extern alias b;");

        yield return new SourceTestCase("DeclarationParsingTests.TestNamespaceWithNestedNamespace", "namespace a { namespace b { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClass", "class a { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithPublic", "public class a { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithInternal", "internal class a { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithStatic", "static class a { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithSealed", "sealed class a { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithAbstract", "abstract class a { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithPartial", "partial class a { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithAttribute", "[attr] class a { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithMultipleAttributes", "[attr1] [attr2] class a { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithMultipleAttributesInAList", "[attr1, attr2] class a { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithBaseType", "class a : b { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithMultipleBases", "class a : b, c { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithTypeConstraintBound", "class a<b> where b : c { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestNonGenericClassWithTypeConstraintBound", "class a where b : c { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestNonGenericMethodWithTypeConstraintBound", "class a { void M() where b : c { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithNewConstraintBound", "class a<b> where b : new() { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithClassConstraintBound", "class a<b> where b : class { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithStructConstraintBound", "class a<b> where b : struct { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithMultipleConstraintBounds", "class a<b> where b : class, c, new() { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithMultipleConstraints", "class a<b> where b : c where b : new() { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassWithMultipleBasesAndConstraints", "class a<b> : c, d where b : class, e, new() { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestInterface", "interface a { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestGenericInterface", "interface A<B> { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestGenericInterfaceWithAttributesAndVariance", "interface A<[B] out C> { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestStruct", "struct a { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestNestedClass", "class a { class b { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestNestedPrivateClass", "class a { private class b { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestNestedProtectedClass", "class a { protected class b { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestNestedProtectedInternalClass", "class a { protected internal class b { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestNestedInternalProtectedClass", "class a { internal protected class b { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestNestedPublicClass", "class a { public class b { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestNestedInternalClass", "class a { internal class b { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestDelegate", "delegate a b();");

        yield return new SourceTestCase("DeclarationParsingTests.TestDelegateWithRefReturnType", "delegate ref a b();");

        yield return new SourceTestCase("DeclarationParsingTests.TestDelegateWithRefReadonlyReturnType", "delegate ref readonly a b();");

        yield return new SourceTestCase("DeclarationParsingTests.TestDelegateWithParameter", "delegate a b(c d);");

        yield return new SourceTestCase("DeclarationParsingTests.TestDelegateWithMultipleParameters", "delegate a b(c d, e f);");

        yield return new SourceTestCase("DeclarationParsingTests.TestDelegateWithRefParameter", "delegate a b(ref c d);");

        yield return new SourceTestCase("DeclarationParsingTests.TestDelegateWithOutParameter", "delegate a b(out c d);");

        yield return new SourceTestCase("DeclarationParsingTests.TestDelegateWithParamsParameter", "delegate a b(params c d);");

        yield return new SourceTestCase("DeclarationParsingTests.TestDelegateWithArgListParameter", "delegate a b(__arglist);");

        yield return new SourceTestCase("DeclarationParsingTests.TestDelegateWithParameterAttribute", "delegate a b([attr] c d);");

        yield return new SourceTestCase("DeclarationParsingTests.TestNestedDelegate", "class a { delegate b c(); }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassMethod", "class a { b X() { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassMethodWithRefReturn", "class a { ref b X() { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassMethodWithRefReadonlyReturn", "class a { ref readonly b X() { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassMethodWithPartial", "class a { partial void M() { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestStructMethodWithReadonly", "struct a { readonly void M() { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestReadOnlyRefReturning", "struct a { readonly ref readonly int M() { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestStructExpressionPropertyWithReadonly", "struct a { readonly int M => 42; }");

        yield return new SourceTestCase("DeclarationParsingTests.TestStructGetterPropertyWithReadonly", "struct a { int P { readonly get { return 42; } } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassMethodWithParameter", "class a { b X(c d) { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassMethodWithMultipleParameters", "class a { b X(c d, e f) { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassMethodWithArgListParameter", "class a { b X(__arglist) { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestGenericClassMethod", "class a { b<c> M() { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestGenericClassMethodWithTypeConstraintBound", "class a { b X<c>() where b : d { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestGenericClassConstructor", """

            class Class1<T>{
                public Class1() { }
            }

            """);

        yield return new SourceTestCase("DeclarationParsingTests.TestClassConstructor", "class a { a() { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassDestructor", "class a { ~a() { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassField", "class a { b c; }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassConstField", "class a { const b c = d; }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassFieldWithInitializer", "class a { b c = e; }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassFieldWithArrayInitializer", "class a { b c = { }; }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassFieldWithMultipleVariables", "class a { b c, d, e; }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassFieldWithMultipleVariablesAndInitializers", "class a { b c = x, d = y, e = z; }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassFixedField", "class a { fixed b c[10]; }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassProperty", "class a { b c { get; set; } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassPropertyWithRefReturn", "class a { ref b c { get; set; } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassPropertyWithRefReadonlyReturn", "class a { ref readonly b c { get; set; } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassPropertyWithBodies", "class a { b c { get { } set { } } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassAutoPropertyWithInitializer", "class a { b c { get; set; } = d; }");

        yield return new SourceTestCase("DeclarationParsingTests.InitializerOnNonAutoProp", "class C { int P { set {} } = 0; }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassPropertyExplicit", "class a { b I.c { get; set; } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassEventProperty", "class a { event b c { add { } remove { } } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassEventPropertyExplicit", "class a { event b I.c { add { } remove { } } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassIndexer", "class a { b this[c d] { get; set; } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassIndexerWithRefReturn", "class a { ref b this[c d] { get; set; } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassIndexerWithRefReadonlyReturn", "class a { ref readonly b this[c d] { get; set; } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassIndexerWithMultipleParameters", "class a { b this[c d, e f] { get; set; } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassIndexerExplicit", "class a { b I.this[c d] { get; set; } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassRightShiftOperatorMethod", "class a { b operator >> (c d, e f) { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassUnsignedRightShiftOperatorMethod", "class a { b operator >>> (c d, e f) { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassImplicitConversionOperatorMethod", "class a { implicit operator b (c d) { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestClassExplicitConversionOperatorMethod", "class a { explicit operator b (c d) { } }");

        yield return new SourceTestCase("DeclarationParsingTests.TestNamespaceDeclarationsBadNames", "namespace A::B { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestNamespaceDeclarationsBadNames1", @"namespace A::B { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestNamespaceDeclarationsBadNames2", @"namespace A<B> { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestNamespaceDeclarationsBadNames3", @"namespace A<,> { }");

        yield return new SourceTestCase("DeclarationParsingTests.TestPartialEnum", @"partial enum E{}");

        yield return new SourceTestCase("DeclarationParsingTests.TestEscapedConstructor", """

            class @class
            {
                public @class()
                {
                }
            }

            """);

        yield return new SourceTestCase("DeclarationParsingTests.TestAnonymousMethodWithDefaultParameter", """

            delegate void F(int x);
            class C {
               void M() {
                 F f = delegate (int x = 0) { };
               }
            }

            """);

        yield return new SourceTestCase("DeclarationParsingTests.RegressIfDevTrueUnicode", """

            class P
            {
            static void Main()
            {
            #if tru\u0065
            System.Console.WriteLine("Good, backwards compatible");
            #else
            System.Console.WriteLine("Bad, breaking change");
            #endif
            }
            }

            """);

        yield return new SourceTestCase("DeclarationParsingTests.RegressLongDirectiveIdentifierDefn", """

            //130 chars (max is 128)
            #define A234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
            class P
            {
            static void Main()
            {
            //first 128 chars of defined value
            #if A2345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678
            System.Console.WriteLine("Good, backwards compatible");
            #else
            System.Console.WriteLine("Bad, breaking change");
            #endif
            }
            }

            """);

        yield return new SourceTestCase("DeclarationParsingTests.RegressLongDirectiveIdentifierUse", """

            //128 chars (max)
            #define A2345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678
            class P
            {
            static void Main()
            {
            //defined value + two chars (larger than max)
            #if A234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
            System.Console.WriteLine("Good, backwards compatible");
            #else
            System.Console.WriteLine("Bad, breaking change");
            #endif
            }
            }

            """);

        yield return new SourceTestCase("DeclarationParsingTests.ValidFixedBufferTypes", """

            unsafe struct s
            {
                public fixed bool _Type1[10];
                internal fixed int _Type3[10];
                private fixed short _Type4[10];
                unsafe fixed long _Type5[10];
                new fixed char _Type6[10];
            }

            """);

        yield return new SourceTestCase("DeclarationParsingTests.ValidFixedBufferTypesMultipleDeclarationsOnSameLine", """

            unsafe struct s
            {
                public fixed bool _Type1[10], _Type2[10], _Type3[20];
            }

            """);

        yield return new SourceTestCase("DeclarationParsingTests.ValidFixedBufferTypesWithCountFromConstantOrLiteral", """

            unsafe struct s
            {
                public const int abc = 10;
                public fixed bool _Type1[abc];
                public fixed bool _Type2[20];
                }

            """);

        yield return new SourceTestCase("DeclarationParsingTests.ValidFixedBufferTypesAllValidTypes", """

            unsafe struct s
            {
                public fixed bool _Type1[10];
                public fixed byte _Type12[10];
                public fixed int _Type2[10];
                public fixed short _Type3[10];
                public fixed long _Type4[10];
                public fixed char _Type5[10];
                public fixed sbyte _Type6[10];
                public fixed ushort _Type7[10];
                public fixed uint _Type8[10];
                public fixed ulong _Type9[10];
                public fixed float _Type10[10];
                public fixed double _Type11[10];
             }



            """);

        yield return new SourceTestCase("DeclarationParsingTests.TupleArgument01", """

            class C1
            {
                static (T, T) Test1<T>(int a, (byte, byte) arg0)
                {
                    return default((T, T));
                }

                static (T, T) Test2<T>(ref (byte, byte) arg0)
                {
                    return default((T, T));
                }
            }

            """);

        yield return new SourceTestCase("DeclarationParsingTests.TupleArgument02", """

            class C1
            {
                static (T, T) Test3<T>((byte, byte) arg0)
                {
                    return default((T, T));
                }

                (T, T) Test3<T>((byte a, byte b)[] arg0)
                {
                    return default((T, T));
                }
            }

            """);

        yield return new SourceTestCase("DeclarationParsingTests.Interface_SemicolonBody", """

            interface C
            ;
            """);

        yield return new SourceTestCase("DeclarationParsingTests.Interface_SemicolonBodyAfterBase_01", """

            interface C : I1
            ;
            """);

        yield return new SourceTestCase("DeclarationParsingTests.Interface_SemicolonBodyAfterBase_02", """

            interface C : I1, I2
            ;
            """);

        yield return new SourceTestCase("DeclarationParsingTests.Interface_SemicolonBodyAfterConstraint_01", """

            interface C where T1 : U1
            ;
            """);

        yield return new SourceTestCase("DeclarationParsingTests.Interface_SemicolonBodyAfterConstraint_02", """

            interface C where T1 : U1 where T2 : U2
            ;
            """);

        yield return new SourceTestCase("DeconstructionTests.TupleArray", "(T, T)[] id;");

        yield return new SourceTestCase("DeconstructionTests.ParenthesizedExpression", "(x).ToString();");

        yield return new SourceTestCase("DeconstructionTests.TupleLiteralStatement", "(x, x).ToString();");

        yield return new SourceTestCase("DeconstructionTests.Statement4", "((x)).ToString();");

        yield return new SourceTestCase("DeconstructionTests.Statement5", "((x, y) = M()).ToString();");

        yield return new SourceTestCase("DeconstructionTests.CastWithTupleType", "(((x, y))z).Goo();");

        yield return new SourceTestCase("DeconstructionTests.NotACast", "((Int32.MaxValue, Int32.MaxValue)).ToString();");

        yield return new SourceTestCase("DeconstructionTests.AlsoNotACast", "((x, y)).ToString();");

        yield return new SourceTestCase("DeconstructionTests.StillNotACast", "((((x, y)))).ToString();");

        yield return new SourceTestCase("DeconstructionTests.LambdaInExpressionStatement", "(a) => a;");

        yield return new SourceTestCase("DeconstructionTests.LambdaWithBodyInExpressionStatement", "(a, b) => { };");

        yield return new SourceTestCase("DeconstructionTests.NullableTuple", "(x, y)? z = M();");
        

        yield return new SourceTestCase("ExpressionParsingTests.TestConditionalAccessNotVersion5", "a.b?.c.d?[1]?.e()?.f;);");

        yield return new SourceTestCase("ExpressionParsingTests.TestConditionalAccess", "a.b?.c.d?[1]?.e()?.f;");

        yield return new SourceTestCase("ExpressionParsingTests.TypeArgumentShiftAmbiguity_01", """

            class C
            {
                void M()
                {
                    //int a = 1;
                    //int i = 1;
                    var j = a < i >> 2;
                }
            }

            """);

        yield return new SourceTestCase("ExpressionParsingTests.TypeArgumentShiftAmbiguity_02", """

            class C
            {
                void M()
                {
                    //const int a = 1;
                    //const int i = 2;
                    switch (false)
                    {
                        case a < i >> 2: break;
                    }
                }
            }

            """);

        yield return new SourceTestCase("ExpressionParsingTests.TypeArgumentShiftAmbiguity_03", """

            class C
            {
                void M()
                {
                    M(out a < i >> 2);
                }
            }

            """);

        yield return new SourceTestCase("ExpressionParsingTests.TypeArgumentShiftAmbiguity_04", """

            class C
            {
                void M()
                {
                    // (e is a<i>) > 2
                    var j = e is a < i >> 2;
                }
            }

            """);

        yield return new SourceTestCase("ExpressionParsingTests.TypeArgumentShiftAmbiguity_05", """

            class C
            {
                void M()
                {
                    var j = e is a < i >>> 2;
                }
            }

            """);

        yield return new SourceTestCase("ExpressionParsingTests.TypeArgumentShiftAmbiguity_08", """

            class C
            {
                void M()
                {
                    M(out a < i >>> 2);
                }
            }

            """);

        yield return new SourceTestCase("ExpressionParsingTests.TypeArgumentShiftAmbiguity_09", """

            class C
            {
                void M()
                {
                    //const int a = 1;
                    //const int i = 2;
                    switch (false)
                    {
                        case a < i >>> 2: break;
                    }
                }
            }

            """);

        yield return new SourceTestCase("ExpressionParsingTests.TypeArgumentShiftAmbiguity_10", """

            class C
            {
                void M()
                {
                    //int a = 1;
                    //int i = 1;
                    var j = a < i >>> 2;
                }
            }

            """);

        yield return new SourceTestCase("FunctionPointerTests.LangVersion8", "delegate* unmanaged[cdecl]<string, Goo, int> ptr;");

        yield return new SourceTestCase("LocalFunctionParsingTests.DiagnosticsWithoutExperimental", """

            class c
            {
                void m()
                {
                    int local() => 0;
                }
                void m2()
                {
                    int local() { return 0; }
                }
            }
            """);

        yield return new SourceTestCase("LocalFunctionParsingTests.StaticFunctions", """
            class Program
            {
                void M()
                {
                    static void F() { }
                }
            }
            """);

        yield return new SourceTestCase("LocalFunctionParsingTests.AsyncStaticFunctions", """
            class Program
            {
                void M()
                {
                    static async void F1() { }
                    async static void F2() { }
                }
            }
            """);

        yield return new SourceTestCase("MemberDeclarationParsingTests.OperatorDeclaration_ExplicitImplementation_01", "public int N.I.operator +(int x, int y) => x + y;");

        yield return new SourceTestCase("MemberDeclarationParsingTests.OperatorDeclaration_ExplicitImplementation_11", "public int N.I.operator +(int x, int y) => x + y;");

        yield return new SourceTestCase("MemberDeclarationParsingTests.OperatorDeclaration_ExplicitImplementation_23", "int N.I.operator +(int x, int y) => x + y;");

        yield return new SourceTestCase("MemberDeclarationParsingTests.OperatorDeclaration_ExplicitImplementation_33", "int N.I.operator +(int x, int y) => x + y;");

        yield return new SourceTestCase("MemberDeclarationParsingTests.ConversionDeclaration_ExplicitImplementation_01", "implicit N.I.operator int(int x) => x;");

        yield return new SourceTestCase("MemberDeclarationParsingTests.ConversionDeclaration_ExplicitImplementation_11", "explicit N.I.operator int(int x) => x;");

        yield return new SourceTestCase("ParserErrorMessageTests.CS1031ERR_TypeExpected02_Tuple", """
            namespace x
            {
                public class @a
                {
                    public static void Main()
                    {
                        var e = new ();
                    }
                }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.CS1031ERR_TypeExpected02WithCSharp6_Tuple", """
            namespace x
            {
                public class @a
                {
                    public static void Main()
                    {
                        var e = new ();
                    }
                }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.CS1031ERR_TypeExpected02WithCSharp7_Tuple", """
            namespace x
            {
                public class @a
                {
                    public static void Main()
                    {
                        var e = new ();
                    }
                }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.PartialTypesBeforeVersionTwo", """

            partial class C
            {
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.PartialMethodsVersionThree", """

            class C
            {
                partial int Goo() { }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.QueryBeforeVersionThree", """

            class C
            {
                void Goo()
                {
                    var q = from a in b
                            select c;
                }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.AnonymousTypeBeforeVersionThree", """

            class C
            {
                void Goo()
                {
                    var q = new { };
                }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.ImplicitArrayBeforeVersionThree", """

            class C
            {
                void Goo()
                {
                    var q = new [] { };
                }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.ObjectInitializerBeforeVersionThree", """

            class C
            {
                void Goo()
                {
                    var q = new Goo { };
                }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.LambdaBeforeVersionThree", """

            class C
            {
                void Goo()
                {
                    var q = a => b;
                }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.ExceptionFilterBeforeVersionSix", """

            public class C
            {
                public static int Main()
                {
                    try { } catch when (true) {}
                }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.InterpolatedStringBeforeCSharp6", """

            class C
            {
                string M()
                {
                    return $"hello";
                }
            }
            """);

        yield return new SourceTestCase("ParserErrorMessageTests.InterpolatedStringWithReplacementBeforeCSharp6", """

            class C
            {
                string M()
                {
                    string other = "world";
                    return $"hello + {other}";
                }
            }
            """);

        yield return new SourceTestCase("ParserErrorMessageTests.AsyncBeforeCSharp5", """

            class C
            {
                async void M() { }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.AsyncWithOtherModifiersBeforeCSharp5", """

            class C
            {
                async static void M() { }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.AsyncLambdaBeforeCSharp5", """

            class C
            {
                static void Main()
                {
                    Func<int, Task<int>> f = async x => x;
                }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.AsyncDelegateBeforeCSharp5", """

            class C
            {
                static void Main()
                {
                    Func<int, Task<int>> f = async delegate (int x) { return x; };
                }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.NamedArgumentBeforeCSharp4", """

            [Attr(x:1)]
            class C
            {
                C()
                {
                    M(y:2);
                }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.GlobalKeywordBeforeCSharp2", """

            class C : global::B
            {
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.AliasQualifiedNameBeforeCSharp2", """

            class C : A::B
            {
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.OptionalParameterBeforeCSharp4", """

            class C
            {
                void M(int x = 1) { }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.ObjectInitializerBeforeCSharp3", """

            class C
            {
                void M()
                {
                    return new C { Goo = 1 };
                }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.CollectionInitializerBeforeCSharp3", """

            class C
            {
                void M()
                {
                    return new C { 1, 2, 3 };
                }
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.CrefGenericBeforeCSharp2", """

            /// <see cref='C{T}'/>
            class C
            {
            }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.CrefAliasQualifiedNameBeforeCSharp2", """

            /// <see cref='Alias::Goo'/>
            /// <see cref='global::Goo'/>
            class C { }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.PragmaBeforeCSharp2", """

            #pragma warning disable 1584
            #pragma checksum "file.txt" "{00000000-0000-0000-0000-000000000000}" "2453"
            class C { }

            """);

        yield return new SourceTestCase("ParserErrorMessageTests.PragmaBeforeCSharp2_InDisabledCode", """

            #if UNDEF
            #pragma warning disable 1584
            #pragma checksum "file.txt" "{00000000-0000-0000-0000-000000000000}" "2453"
            #endif
            class C { }

            """);

        yield return new SourceTestCase("PatternParsingTests.ParenthesizedSwitchCase", """

            switch (e)
            {
                case (0): break;
                case (-1): break;
                case (+2): break;
                case (~3): break;
            }

            """);

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLine1", """"

            class C
            {
                void M()
                {
                    var v = $""" """;
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineSingleQuoteInside", """"

            class C
            {
                void M()
                {
                    var v = $""" " """;
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineDoubleQuoteInside", """"

            class C
            {
                void M()
                {
                    var v = $""" "" """;
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationInside", """"

            class C
            {
                void M()
                {
                    var v = $"""{0}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationInsideSpacesOutside", """"

            class C
            {
                void M()
                {
                    var v = $""" {0} """;
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationInsideSpacesInside", """"

            class C
            {
                void M()
                {
                    var v = $"""{ 0 }""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationInsideSpacesInsideAndOutside", """"

            class C
            {
                void M()
                {
                    var v = $""" { 0 } """;
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationMultipleCurliesAllowed1", """"

            class C
            {
                void M()
                {
                    var v = $$"""{{0}}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationMultipleCurliesAllowed2", """"

            class C
            {
                void M()
                {
                    var v = $$"""{{{0}}}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationMultipleCurliesAllowed4", """"

            class C
            {
                void M()
                {
                    var v = $$"""{{{0}}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingNormalString", """"

            class C
            {
                void M()
                {
                    var v = $"""{"a"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingVerbatimString1", """"

            class C
            {
                void M()
                {
                    var v = $"""{@"a"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingVerbatimString2", """"

            class C
            {
                void M()
                {
                    var v = $"""{@"
            a"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingInterpolatedString1", """"

            class C
            {
                void M()
                {
                    var v = $"""{$"a"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingInterpolatedString2", """"

            class C
            {
                void M()
                {
                    var v = $"""{$"{0}"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingVerbatimInterpolatedString1", """"

            class C
            {
                void M()
                {
                    var v = $"""{$@"{0}"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingVerbatimInterpolatedString2", """"

            class C
            {
                void M()
                {
                    var v = $"""{@$"{0}"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingVerbatimInterpolatedString3", """"

            class C
            {
                void M()
                {
                    var v = $"""{$@"{
            0}"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingVerbatimInterpolatedString4", """"

            class C
            {
                void M()
                {
                    var v = $"""{
            $@"{
            0}"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingRawStringLiteral1", """"

            class C
            {
                void M()
                {
                    var v = $"""{"""a"""}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingRawStringLiteral2", """"

            class C
            {
                void M()
                {
                    var v = $"""{"""
              a
              """}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingRawInterpolatedStringLiteral1", """"

            class C
            {
                void M()
                {
                    var v = $"""{$""" """}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingRawInterpolatedStringLiteral2", """""

            class C
            {
                void M()
                {
                    var v = $"""{$"""" """"}""";
                }
            }
            """"");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingRawInterpolatedStringLiteral3", """"

            class C
            {
                void M()
                {
                    var v = $"""{$"""{0}"""}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingRawInterpolatedStringLiteral4", """"

            class C
            {
                void M()
                {
                    var v = $"""{$"""{
            0}"""}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingRawInterpolatedStringLiteral5", """"

            class C
            {
                void M()
                {
                    var v = $"""{
            $"""{
            0}"""}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingRawInterpolatedStringLiteral6", """"

            class C
            {
                void M()
                {
                    var v = $"""{$$"""{{0}}"""}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingRawInterpolatedStringLiteral7", """"

            class C
            {
                void M()
                {
                    var v = $"""{$$"""{{{0}}}"""}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingRawInterpolatedStringLiteral8", """"

            class C
            {
                void M()
                {
                    var v = $$"""{{{$"""{0}"""}}}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingClosingBraceAsCharacterLiteral", """"

            class C
            {
                void M()
                {
                    var v = $"""{'}'}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingClosingBraceAsRegularStringLiteral", """"

            class C
            {
                void M()
                {
                    var v = $"""{"}"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingClosingBraceAsVerbatimStringLiteral", """"

            class C
            {
                void M()
                {
                    var v = $"""{@"}"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.SingleLineInterpolationContainingClosingBraceAsRawStringLiteral", """"

            class C
            {
                void M()
                {
                    var v = $"""{"""}"""}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterNormalMiddleNormalInnerNormal", """

            class C
            {
                void M()
                {
                    var v = $"{$"{$"{0}"}"}";
                }
            }
            """);

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterNormalMiddleNormalInnerVerbatim", """

            class C
            {
                void M()
                {
                    var v = $"{$"{$@"{0}"}"}";
                }
            }
            """);

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterNormalMiddleNormalInnerRaw", """"

            class C
            {
                void M()
                {
                    var v = $"{$"{$"""{0}"""}"}";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterNormalMiddleVerbatimInnerNormal", """

            class C
            {
                void M()
                {
                    var v = $"{@$"{$"{0}"}"}";
                }
            }
            """);

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterNormalMiddleVerbatimInnerVerbatim", """

            class C
            {
                void M()
                {
                    var v = $"{@$"{@$"{0}"}"}";
                }
            }
            """);

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterNormalMiddleVerbatimInnerRaw", """"

            class C
            {
                void M()
                {
                    var v = $"{@$"{$"""{0}"""}"}";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterNormalMiddleRawInnerNormal", """"

            class C
            {
                void M()
                {
                    var v = $"{$"""{$"{0}"}"""}";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterNormalMiddleRawInnerVerbatim", """"

            class C
            {
                void M()
                {
                    var v = $"{$"""{@$"{0}"}"""}";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterNormalMiddleRawInnerRaw", """"

            class C
            {
                void M()
                {
                    var v = $"{$"""{$"""{0}"""}"""}";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterVerbatimMiddleNormalInnerNormal", """

            class C
            {
                void M()
                {
                    var v = $@"{$"{$"{0}"}"}";
                }
            }
            """);

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterVerbatimMiddleNormalInnerVerbatim", """

            class C
            {
                void M()
                {
                    var v = $@"{$"{$@"{0}"}"}";
                }
            }
            """);

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterVerbatimMiddleNormalInnerRaw", """"

            class C
            {
                void M()
                {
                    var v = $@"{$"{$"""{0}"""}"}";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterVerbatimMiddleVerbatimInnerNormal", """

            class C
            {
                void M()
                {
                    var v = $@"{@$"{$"{0}"}"}";
                }
            }
            """);

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterVerbatimMiddleVerbatimInnerVerbatim", """

            class C
            {
                void M()
                {
                    var v = $@"{@$"{@$"{0}"}"}";
                }
            }
            """);

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterVerbatimMiddleVerbatimInnerRaw", """"

            class C
            {
                void M()
                {
                    var v = $@"{@$"{$"""{0}"""}"}";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterVerbatimMiddleRawInnerNormal", """"

            class C
            {
                void M()
                {
                    var v = $@"{$"""{$"{0}"}"""}";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterVerbatimMiddleRawInnerVerbatim", """"

            class C
            {
                void M()
                {
                    var v = $@"{$"""{@$"{0}"}"""}";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterVerbatimMiddleRawInnerRaw", """"

            class C
            {
                void M()
                {
                    var v = $@"{$"""{$"""{0}"""}"""}";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterRawMiddleNormalInnerNormal", """"

            class C
            {
                void M()
                {
                    var v = $"""{$"{$"{0}"}"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterRawMiddleNormalInnerVerbatim", """"

            class C
            {
                void M()
                {
                    var v = $"""{$"{$@"{0}"}"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterRawMiddleNormalInnerRaw", """"

            class C
            {
                void M()
                {
                    var v = $"""{$"{$"""{0}"""}"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterRawMiddleVerbatimInnerNormal", """"

            class C
            {
                void M()
                {
                    var v = $"""{@$"{$"{0}"}"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterRawMiddleVerbatimInnerVerbatim", """"

            class C
            {
                void M()
                {
                    var v = $"""{@$"{@$"{0}"}"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterRawMiddleVerbatimInnerRaw", """"

            class C
            {
                void M()
                {
                    var v = $"""{@$"{$"""{0}"""}"}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterRawMiddleRawInnerNormal", """"

            class C
            {
                void M()
                {
                    var v = $"""{$"""{$"{0}"}"""}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterRawMiddleRawInnerVerbatim", """"

            class C
            {
                void M()
                {
                    var v = $"""{$"""{@$"{0}"}"""}""";
                }
            }
            """");

        yield return new SourceTestCase("RawInterpolatedStringLiteralParsingTests.OuterRawMiddleRawInnerRaw", """"

            class C
            {
                void M()
                {
                    var v = $"""{$"""{$"""{0}"""}"""}""";
                }
            }
            """");

        yield return new SourceTestCase("ReadOnlyStructs.ReadOnlyStructSimple", """

            class Program
            {
                readonly struct S1{}

                public readonly struct S2{}

                readonly public struct S3{}
            }

            """);

        yield return new SourceTestCase("ReadOnlyStructs.ReadOnlyStructSimpleLangVer", """

            class Program
            {
                readonly struct S1{}

                public readonly struct S2{}

                readonly public struct S3{}
            }

            """);

        yield return new SourceTestCase("ReadOnlyStructs.ReadOnlyClassErr", """

            class Program
            {
                readonly class S1{}

                public readonly delegate ref readonly int S2();

                readonly public interface S3{}
            }

            """);

        yield return new SourceTestCase("ReadOnlyStructs.ReadOnlyRefStruct", """

            class Program
            {
                readonly ref struct S1{}

                unsafe readonly public ref struct S2{}
            }

            """);

        yield return new SourceTestCase("ReadOnlyStructs.ReadOnlyStructPartialMatchingModifiers", """

            class Program
            {
                readonly partial struct S1{}

                readonly partial struct S1{}

                readonly ref partial struct S2{}

                readonly ref partial struct S2{}
            }

            """);

        yield return new SourceTestCase("ReadOnlyStructs.ReadOnlyStructPartialNotMatchingModifiers", """

            class Program
            {
                readonly partial struct S1{}

                readonly ref partial struct S1{}

                readonly partial struct S2{}

                partial struct S2{}

                readonly ref partial struct S3{}

                partial struct S3{}
            }

            """);

        yield return new SourceTestCase("RecordParsingTests.FieldNamedData", """

            class C
            {
                int data;
            }
            """);

        yield return new SourceTestCase("RecordParsingTests.RecordParsing01", "record C(int X, int Y);");

        yield return new SourceTestCase("RecordParsingTests.RecordParsing02", "record C(int X, int Y);");

        yield return new SourceTestCase("RecordParsingTests.RecordParsing03", "record C;");

        yield return new SourceTestCase("RecordParsingTests.RecordParsing04", "record C { public int record; }");

        yield return new SourceTestCase("RecordParsingTests.RecordParsing05", "record Point;");

        yield return new SourceTestCase("RecordParsingTests.RecordParsing07", "interface P(int x, int y);");

        yield return new SourceTestCase("RecordParsingTests.RecordParsing_BlockBodyAndSemiColon", "record C { };");

        yield return new SourceTestCase("RecordParsingTests.WithParsingLangVer", """

            class C
            {
                int x = 0 with {};
            }
            """);

        yield return new SourceTestCase("RecordParsingTests.WithParsing2", """

            class C
            {
                int M()
                {
                    int x = M() with { } + 3;
                }
            }
            """);

        yield return new SourceTestCase("RecordParsingTests.WithParsing17", """x = x with { X = "2" };""");

        yield return new SourceTestCase("RecordParsingTests.Base_03", "interface C : B;");

        yield return new SourceTestCase("RecordParsingTests.Base_04", "interface C(int X, int Y) : B;");

        yield return new SourceTestCase("RecordParsingTests.Base_05", "interface C : B(X, Y);");

        yield return new SourceTestCase("RecordParsingTests.RecordStructParsing", "record struct C(int X, int Y);");

        yield return new SourceTestCase("RecordParsingTests.RecordStructParsing_WithBody", "record struct C(int X, int Y) { }");

        yield return new SourceTestCase("RecordParsingTests.RecordClassParsing", "record class C(int X, int Y);");

        yield return new SourceTestCase("RecordParsingTests.StructNamedRecord_CSharp8", "struct record { }");

        yield return new SourceTestCase("RecordParsingTests.StructNamedRecord_CSharp9", "struct record { }");

        yield return new SourceTestCase("RecordParsingTests.StructNamedRecord_CSharp10", "struct record { }");

        yield return new SourceTestCase("RecordParsingTests.RecordStructParsing_Partial", "partial record struct S;");

        yield return new SourceTestCase("RecordParsingTests.RecordClassParsing_Partial", "partial record class S;");

        yield return new SourceTestCase("RecordParsingTests.RecordParsing_Partial", "partial record S;");

        yield return new SourceTestCase("RecordParsingTests.RecordStructParsing_Partial_WithParameterList", "partial record struct S(int X);");

        yield return new SourceTestCase("RecordParsingTests.RecordStructParsing_Partial_WithParameterList_AndMembers", "partial record struct S(int X) { }");

        yield return new SourceTestCase("RecordParsingTests.RecordStructParsing_Readonly", "readonly record struct S;");

        yield return new SourceTestCase("RecordParsingTests.RecordStructParsing_ReadonlyPartial", "readonly partial record struct S;");

        yield return new SourceTestCase("RecordParsingTests.RecordStructParsing_Ref", "ref record struct S;");

        yield return new SourceTestCase("RecordParsingTests.RecordParsing_Ref", "ref record R;");

        yield return new SourceTestCase("RecordParsingTests.RecordStructParsing_BaseListWithParens", "record struct S : Base(1);");

        yield return new SourceTestCase("RecordParsingTests.RecordStructParsing_BaseListWithParens_WithPositionalParameterList", "record struct S(int X) : Base(1);");

        yield return new SourceTestCase("RefReadonlyTests.RefReadonlyReturn_CSharp7", """

            unsafe class Program
            {
                delegate ref readonly int D1();

                static ref readonly T M<T>()
                {
                    return ref (new T[1])[0];
                }

                public virtual ref readonly int* P1 => throw null;

                public ref readonly int[][] this[int i] => throw null;
            }

            """);

        yield return new SourceTestCase("RefReadonlyTests.InArgs_CSharp7", """

            class Program
            {
                static void M(in int x)
                {
                }

                int this[in int x]
                {
                    get
                    {
                        return 1;
                    }
                }

                static void Test1()
                {
                    int x = 1;
                    M(in x);

                    _ = (new Program())[in x];
                }
            }

            """);

        yield return new SourceTestCase("RefReadonlyTests.RefReadonlyReturn_UnexpectedBindTime", """


            class Program
            {
                static void Main()
                {
                    ref readonly int local = ref (new int[1])[0];

                    (ref readonly int, ref readonly int Alice)? t = null;

                    System.Collections.Generic.List<ref readonly int> x = null;

                    Use(local);
                    Use(t);
                    Use(x);
                }

                static void Use<T>(T dummy)
                {
                }
            }

            """);

        yield return new SourceTestCase("RefStructs.RefStructSimple", """

            class Program
            {
                ref struct S1{}

                public ref struct S2{}
            }

            """);

        yield return new SourceTestCase("RefStructs.RefStructSimpleLangVer", """

            class Program
            {
                ref struct S1{}

                public ref struct S2{}
            }

            """);

        yield return new SourceTestCase("StatementParsingTests.TestName", "a();");

        yield return new SourceTestCase("StatementParsingTests.TestDottedName", "a.b();");

        yield return new SourceTestCase("StatementParsingTests.TestGenericName", "a<b>();");

        yield return new SourceTestCase("StatementParsingTests.TestGenericDotName", "a<b>.c();");

        yield return new SourceTestCase("StatementParsingTests.TestDotGenericName", "a.b<c>();");

        yield return new SourceTestCase("StatementParsingTests.TestLocalDeclarationStatement", "T a;");

        yield return new SourceTestCase("StatementParsingTests.TestLocalDeclarationStatementWithVar", "var a;");

        yield return new SourceTestCase("StatementParsingTests.TestLocalDeclarationStatementWithTuple", "(int, int) a;");

        yield return new SourceTestCase("StatementParsingTests.TestLocalDeclarationStatementWithNamedTuple", "(T x, (U k, V l, W m) y) a;");

        yield return new SourceTestCase("StatementParsingTests.TestLocalDeclarationStatementWithDynamic", "dynamic a;");

        yield return new SourceTestCase("StatementParsingTests.TestLocalDeclarationStatementWithGenericType", "T<a> b;");

        yield return new SourceTestCase("StatementParsingTests.TestLocalDeclarationStatementWithDottedType", "T.X.Y a;");

        yield return new SourceTestCase("StatementParsingTests.TestLocalDeclarationStatementWithMixedType", "T<t>.X<x>.Y<y> a;");

        yield return new SourceTestCase("StatementParsingTests.TestLocalDeclarationStatementWithArrayType", "T[][,][,,] a;");

        yield return new SourceTestCase("StatementParsingTests.TestLocalDeclarationStatementWithPointerType", "T* a;");

        yield return new SourceTestCase("StatementParsingTests.TestLocalDeclarationStatementWithNullableType", "T? a;");

        yield return new SourceTestCase("StatementParsingTests.TestLocalDeclarationStatementWithMultipleVariables", "T a, b, c;");

        yield return new SourceTestCase("StatementParsingTests.TestLocalDeclarationStatementWithInitializer", "T a = b;");

        yield return new SourceTestCase("StatementParsingTests.TestLocalDeclarationStatementWithMultipleVariablesAndInitializers", "T a = va, b = vb, c = vc;");

        yield return new SourceTestCase("StatementParsingTests.TestLocalDeclarationStatementWithArrayInitializer", "T a = {b, c};");

        yield return new SourceTestCase("StatementParsingTests.TestConstLocalDeclarationStatement", "const T a = b;");

        yield return new SourceTestCase("StatementParsingTests.TestRefLocalDeclarationStatement", "ref T a;");

        yield return new SourceTestCase("StatementParsingTests.TestRefLocalDeclarationStatementWithInitializer", "ref T a = ref b;");

        yield return new SourceTestCase("StatementParsingTests.TestRefLocalDeclarationStatementWithMultipleInitializers", "ref T a = ref b, c = ref d;");

        yield return new SourceTestCase("StatementParsingTests.TestFixedStatement", "fixed(T a = b) { }");

        yield return new SourceTestCase("StatementParsingTests.TestFixedVarStatement", "fixed(var a = b) { }");

        yield return new SourceTestCase("StatementParsingTests.TestFixedStatementWithMultipleVariables", "fixed(T a = b, c = d) { }");

        yield return new SourceTestCase("StatementParsingTests.TestEmptyStatement", ";");

        yield return new SourceTestCase("StatementParsingTests.TestLabeledStatement", "label: ;");

        yield return new SourceTestCase("StatementParsingTests.TestBreakStatement", "break;");

        yield return new SourceTestCase("StatementParsingTests.TestContinueStatement", "continue;");

        yield return new SourceTestCase("StatementParsingTests.TestGotoStatement", "goto label;");

        yield return new SourceTestCase("StatementParsingTests.TestGotoCaseStatement", "goto case label;");

        yield return new SourceTestCase("StatementParsingTests.TestGotoDefault", "goto default;");

        yield return new SourceTestCase("StatementParsingTests.TestReturn", "return;");

        yield return new SourceTestCase("StatementParsingTests.TestReturnExpression", "return a;");

        yield return new SourceTestCase("StatementParsingTests.TestYieldReturnExpression", "yield return a;");

        yield return new SourceTestCase("StatementParsingTests.TestYieldBreakExpression", "yield break;");

        yield return new SourceTestCase("StatementParsingTests.TestThrow", "throw;");

        yield return new SourceTestCase("StatementParsingTests.TestThrowExpression", "throw a;");

        yield return new SourceTestCase("StatementParsingTests.TestTryCatch", "try { } catch(T e) { }");

        yield return new SourceTestCase("StatementParsingTests.TestTryCatchWithNoExceptionName", "try { } catch(T) { }");

        yield return new SourceTestCase("StatementParsingTests.TestTryCatchWithNoExceptionDeclaration", "try { } catch { }");

        yield return new SourceTestCase("StatementParsingTests.TestTryCatchWithMultipleCatches", "try { } catch(T e) { } catch(T2) { } catch { }");

        yield return new SourceTestCase("StatementParsingTests.TestTryFinally", "try { } finally { }");

        yield return new SourceTestCase("StatementParsingTests.TestTryCatchWithMultipleCatchesAndFinally", "try { } catch(T e) { } catch(T2) { } catch { } finally { }");

        yield return new SourceTestCase("StatementParsingTests.TestChecked", "checked { }");

        yield return new SourceTestCase("StatementParsingTests.TestUnchecked", "unchecked { }");

        yield return new SourceTestCase("StatementParsingTests.TestUnsafe", "unsafe { }");

        yield return new SourceTestCase("StatementParsingTests.TestWhile", "while(a) { }");

        yield return new SourceTestCase("StatementParsingTests.TestDoWhile", "do { } while (a);");

        yield return new SourceTestCase("StatementParsingTests.TestFor", "for(;;) { }");

        yield return new SourceTestCase("StatementParsingTests.TestForWithVariableDeclaration", "for(T a = 0;;) { }");

        yield return new SourceTestCase("StatementParsingTests.TestForWithVarDeclaration", "for(var a = 0;;) { }");

        yield return new SourceTestCase("StatementParsingTests.TestForWithMultipleVariableDeclarations", "for(T a = 0, b = 1;;) { }");

        yield return new SourceTestCase("StatementParsingTests.TestForWithRefVariableDeclaration", "for(ref T a = ref b, c = ref d;;) { }");

        yield return new SourceTestCase("StatementParsingTests.TestForWithVariableInitializer", "for(a = 0;;) { }");

        yield return new SourceTestCase("StatementParsingTests.TestForWithMultipleVariableInitializers", "for(a = 0, b = 1;;) { }");

        yield return new SourceTestCase("StatementParsingTests.TestForWithCondition", "for(; a;) { }");

        yield return new SourceTestCase("StatementParsingTests.TestForWithIncrementor", "for(; ; a++) { }");

        yield return new SourceTestCase("StatementParsingTests.TestForWithMultipleIncrementors", "for(; ; a++, b++) { }");

        yield return new SourceTestCase("StatementParsingTests.TestForWithDeclarationConditionAndIncrementor", "for(T a = 0; a < 10; a++) { }");

        yield return new SourceTestCase("StatementParsingTests.TestForEach", "foreach(T a in b) { }");

        yield return new SourceTestCase("StatementParsingTests.TestForEachWithVar", "foreach(var a in b) { }");

        yield return new SourceTestCase("StatementParsingTests.TestIf", "if (a) { }");

        yield return new SourceTestCase("StatementParsingTests.TestIfElse", "if (a) { } else { }");

        yield return new SourceTestCase("StatementParsingTests.TestIfElseIf", "if (a) { } else if (b) { }");

        yield return new SourceTestCase("StatementParsingTests.TestLock", "lock (a) { }");

        yield return new SourceTestCase("StatementParsingTests.TestSwitch", "switch (a) { }");

        yield return new SourceTestCase("StatementParsingTests.TestSwitchWithCase", "switch (a) { case b:; }");

        yield return new SourceTestCase("StatementParsingTests.TestSwitchWithMultipleCases", "switch (a) { case b:; case c:; }");

        yield return new SourceTestCase("StatementParsingTests.TestSwitchWithDefaultCase", "switch (a) { default:; }");

        yield return new SourceTestCase("StatementParsingTests.TestSwitchWithMultipleLabelsOnOneCase", "switch (a) { case b: case c:; }");

        yield return new SourceTestCase("StatementParsingTests.TestSwitchWithMultipleStatementsOnOneCase", "switch (a) { case b: s1(); s2(); }");

        yield return new SourceTestCase("StatementParsingTests.TestUsingWithExpression", "using (a) { }");

        yield return new SourceTestCase("StatementParsingTests.TestUsingWithDeclaration", "using (T a = b) { }");

        yield return new SourceTestCase("StatementParsingTests.TestUsingVarWithDeclaration", "using T a = b;");

        yield return new SourceTestCase("StatementParsingTests.TestUsingWithVarDeclaration", "using (var a = b) { }");

        yield return new SourceTestCase("StatementParsingTests.TestUsingVarWithVarDeclaration", "using var a = b;");

        yield return new SourceTestCase("StatementParsingTests.TestAwaitUsingWithVarDeclaration", "await using var a = b;");

        yield return new SourceTestCase("StatementParsingTests.TestUsingWithDeclarationWithMultipleVariables", "using (T a = b, c = d) { }");

        yield return new SourceTestCase("StatementParsingTests.TestUsingVarWithDeclarationWithMultipleVariables", "using T a = b, c = d;");

        yield return new SourceTestCase("StatementParsingTests.TestUsingSpecialCase1", "using (f ? x = a : x = b) { }");

        yield return new SourceTestCase("StatementParsingTests.TestUsingVarSpecialCase1", "using var x = f ? a : b;");

        yield return new SourceTestCase("StatementParsingTests.TestUsingSpecialCase2", "using (f ? x = a) { }");

        yield return new SourceTestCase("StatementParsingTests.TestUsingVarSpecialCase2", "using f ? x = a;");

        yield return new SourceTestCase("StatementParsingTests.TestUsingSpecialCase3", "using (f ? x, y) { }");

        yield return new SourceTestCase("StatementParsingTests.TestUsingVarSpecialCase3", "using f ? x, y;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.StaticUsingDirectiveRefType", @"using static x = ref int;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectiveNamePointer1", """
            using x = A*;

            struct A { }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectiveNamePointer2", """
            using unsafe x = A*;

            struct A { }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectiveFunctionPointer1", @"using x = delegate*<int, void>;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectiveFunctionPointer2", @"using unsafe x = delegate*<int, void>;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.UsingUnsafeNonAlias", @"using unsafe System;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectivePredefinedType_CSharp11", @"using x = int;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectivePredefinedType_CSharp12", @"using x = int;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectivePredefinedType_Preview", @"using x = int;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectiveRefType", @"using x = ref int;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectiveRefReadonlyType", @"using x = ref readonly int;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectivePredefinedTypePointer1", @"using x = int*;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectivePredefinedTypePointer2", @"using unsafe x = int*;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectivePredefinedTypePointer3", """

            using unsafe X = int*;

            namespace N
            {
                using Y = X;
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectivePredefinedTypePointer4", """

            using unsafe X = int*;

            namespace N
            {
                using unsafe Y = X;
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectivePredefinedTypePointer5", """

            using X = int*;

            namespace N
            {
                using unsafe Y = X;
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectivePredefinedTypePointer6", """

            using unsafe X = int*;

            namespace N
            {
                using Y = X[];
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectivePredefinedTypePointer7", """

            using unsafe X = int*;

            namespace N
            {
                using unsafe Y = X[];
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectiveTuple1", @"using x = (int, int);");

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectiveTuple2", """
            using X = (int, int);

            class C
            {
                X x = (0, 0);
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectiveTuple3", """
            using X = (int, int);

            class C
            {
                X x = (true, false);
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingNullableValueType", @"using x = int?;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingNullableReferenceType1", @"using x = string?;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingNullableReferenceType2", """
            #nullable enable
            using X = string?;
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingNullableReferenceType3", """
            using X = string;
            namespace N
            {
                using Y = X?;
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingNullableReferenceType4", """
            #nullable enable
            using X = string;
            namespace N
            {
                using Y = X?;
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingVoidPointer1", """
            using unsafe VP = void*;

            class C
            {
                void M(VP vp) { }
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingVoidPointer2", """
            using unsafe VP = void*;

            class C
            {
                unsafe void M(VP vp) { }
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingVoidPointer3", """
            using VP = void*;

            class C
            {
                unsafe void M(VP vp) { }
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.UsingDirectiveDynamic1", """

            using dynamic;
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectiveDynamic1", """

            using D = dynamic;

            class C
            {
                void M(D d)
                {
                    d.Goo();
                }
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectiveDynamic2", """

            using D = System.Collections.Generic.List<dynamic>;

            class C
            {
                void M(D d)
                {
                    d[0].Goo();
                }
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectiveDynamic3", """

            using D = dynamic[];

            class C
            {
                void M(D d)
                {
                    d[0].Goo();
                }
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectiveDynamic4", """

            using D = dynamic;

            class dynamic
            {
                void M(D d)
                {
                    d.Goo();
                }
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDirectiveDynamic5", """

            // Note: this is weird, but is supported by language.  It checks just that the ValueText is `dynamic`, not the raw text.
            using D = @dynamic;

            class C
            {
                void M(D d)
                {
                    d.Goo();
                }
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDuplicate1", """
            using X = int?;
            using X = System;
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDuplicate2", """
            using X = int?;
            using X = int;
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingDuplicate3", """
            using X = int?;
            using X = System.Int32;
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.AliasUsingNotDuplicate1", """
            using X = int?;
            namespace N;
            using X = int;
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.TestObsolete1", """
            using System;
            using X = C;

            [Obsolete("", error: true)]
            class C
            {
            }

            class D
            {
                X x;
                C c;
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.TestObsolete2", """
            using System;
            using X = C[];

            [Obsolete("", error: true)]
            class C
            {
            }

            class D
            {
                X x1;
                C[] c1;
            }
            """);

        yield return new SourceTestCase("UsingDirectiveParsingTests.UsingStaticUnsafe_SafeType_CSharp11_NoUnsafeFlag", @"using static unsafe System.Console;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.UsingStaticUnsafe_SafeType_CSharp11_UnsafeFlag", @"using static unsafe System.Console;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.UsingStaticUnsafe_SafeType_CSharp12_NoUnsafeFlag", @"using static unsafe System.Console;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.UsingStaticUnsafe_SafeType_CSharp12_UnsafeFlag", @"using static unsafe System.Console;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.UsingStaticUnsafe_UnsafeType_CSharp11_NoUnsafeFlag", @"using static unsafe System.Collections.Generic.List<int*[]>;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.UsingStaticUnsafe_UnsafeType_CSharp11_UnsafeFlag", @"using static unsafe System.Collections.Generic.List<int*[]>;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.UsingStaticUnsafe_UnsafeType_CSharp12_NoUnsafeFlag", @"using static unsafe System.Collections.Generic.List<int*[]>;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.UsingStaticUnsafe_UnsafeType_CSharp12_UnsafeFlag", @"using static unsafe System.Collections.Generic.List<int*[]>;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.UsingStatic_UnsafeType_CSharp11_NoUnsafeFlag", @"using static System.Collections.Generic.List<int*[]>;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.UsingStatic_UnsafeType_CSharp11_UnsafeFlag", @"using static System.Collections.Generic.List<int*[]>;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.UsingStatic_UnsafeType_CSharp12_NoUnsafeFlag", @"using static System.Collections.Generic.List<int*[]>;");

        yield return new SourceTestCase("UsingDirectiveParsingTests.UsingStatic_UnsafeType_CSharp12_UnsafeFlag", @"using static System.Collections.Generic.List<int*[]>;");
    }
}