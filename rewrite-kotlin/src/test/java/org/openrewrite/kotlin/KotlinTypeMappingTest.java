/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*;
import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings("ConstantConditions")
class KotlinTypeMappingTest {
    private static final String goat = StringUtils.readFully(KotlinTypeMappingTest.class.getResourceAsStream("/KotlinTypeGoat.kt"));
    private static final K.CompilationUnit cu;
    private static final K.ClassDeclaration goatClassDeclaration;

    static {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(REQUIRE_PRINT_EQUALS_INPUT, false);
        cu = (K.CompilationUnit) KotlinParser.builder()
            .logCompilationWarningsAndErrors(true)
            .build()
            .parseInputs(singletonList(new Parser.Input(Path.of("KotlinTypeGoat.kt"), () -> new ByteArrayInputStream(goat.getBytes(StandardCharsets.UTF_8)))), null, ctx)
            .findFirst()
            .orElseThrow();

        goatClassDeclaration = cu
          .getStatements()
          .stream()
          .filter(K.ClassDeclaration.class::isInstance)
          .findFirst()
          .map(K.ClassDeclaration.class::cast)
          .orElseThrow();
    }

    private static final JavaType.Parameterized goatType =
      requireNonNull(TypeUtils.asParameterized(goatClassDeclaration.getType()));

    public JavaType.Method methodType(String methodName) {
        JavaType.Method type = goatType.getMethods().stream()
          .filter(m -> m.getName().equals(methodName))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("Expected to find matching method named " + methodName));
        assertThat(type.getDeclaringType().toString()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat");
        return type;
    }

    public K.Property getProperty(String fieldName) {
        return goatClassDeclaration.getClassDeclaration().getBody().getStatements().stream()
                .filter(K.Property.class::isInstance)
                .map(K.Property.class::cast)
                .filter(mv -> mv.getVariableDeclarations().getVariables().stream().anyMatch(v -> v.getSimpleName().equals(fieldName)))
                .findFirst()
                .orElse(null);
    }

    public JavaType firstMethodParameter(String methodName) {
        return methodType(methodName).getParameterTypes().getFirst();
    }

    @Test
    void extendsKotlinAny() {
        assertThat(goatType.getSupertype().getFullyQualifiedName()).isEqualTo("kotlin.Any");
    }

    @Test
    void fieldType() {
        K.Property property = getProperty("field");
        J.VariableDeclarations.NamedVariable variable = property.getVariableDeclarations().getVariables().getFirst();
        J.Identifier id = variable.getName();
        assertThat(variable.getType()).isEqualTo(id.getType());
        assertThat(id.getFieldType()).isInstanceOf(JavaType.Variable.class);
        assertThat(id.getFieldType().toString()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=field,type=kotlin.Int}");
        assertThat(id.getType()).isInstanceOf(JavaType.Class.class);
        assertThat(id.getType().toString()).isEqualTo("kotlin.Int");

        J.MethodDeclaration getter = property.getAccessors().getElements().stream().filter(x -> "get".equals(x.getName().getSimpleName())).findFirst().orElse(null);
        JavaType.FullyQualified declaringType = getter.getMethodType().getDeclaringType();
        assertThat(declaringType.getFullyQualifiedName()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat");
        assertThat(getter.getMethodType().getName()).isEqualTo("get");
        assertThat(getter.getMethodType().getReturnType()).isEqualTo(id.getType());
        assertThat(getter.getName().getType()).isEqualTo(getter.getMethodType());
        assertThat(getter.getMethodType().toString().substring(declaringType.toString().length())).isEqualTo("{name=get,return=kotlin.Int,parameters=[]}");

        J.MethodDeclaration setter = property.getAccessors().getElements().stream().filter(x -> "set".equals(x.getName().getSimpleName())).findFirst().orElse(null);
        declaringType = setter.getMethodType().getDeclaringType();
        assertThat(declaringType.getFullyQualifiedName()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat");
        assertThat(setter.getMethodType().getName()).isEqualTo("set");
        assertThat(setter.getMethodType()).isEqualTo(setter.getName().getType());
        assertThat(setter.getMethodType().toString().substring(declaringType.toString().length())).isEqualTo("{name=set,return=kotlin.Unit,parameters=[kotlin.Int]}");
    }

    @Test
    void fileField() {
        J.VariableDeclarations.NamedVariable nv = cu.getStatements().stream()
          .filter(J.VariableDeclarations.class::isInstance)
          .flatMap(it -> ((J.VariableDeclarations) it).getVariables().stream())
          .filter(it -> "field".equals(it.getSimpleName())).findFirst().orElseThrow();

        assertThat(nv.getName().getType().toString()).isEqualTo("kotlin.Int");
        assertThat(nv.getName().getFieldType()).isEqualTo(nv.getVariableType());
        assertThat(nv.getVariableType().toString())
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoatKt{name=field,type=kotlin.Int}");
    }

    @Test
    void fileFunction() {
        J.MethodDeclaration md = cu.getStatements().stream()
            .filter(J.MethodDeclaration.class::isInstance)
              .map(J.MethodDeclaration.class::cast)
                .filter(it -> "function".equals(it.getSimpleName())).findFirst().orElseThrow();

        assertThat(md.getName().getType()).isEqualTo(md.getMethodType());
        assertThat(md.getMethodType().toString())
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoatKt{name=function,return=kotlin.Unit,parameters=[org.openrewrite.kotlin.C]}");

        J.VariableDeclarations.NamedVariable nv = ((J.VariableDeclarations) md.getParameters().getFirst()).getVariables().getFirst();
        assertThat(nv.getVariableType()).isEqualTo(nv.getName().getFieldType());
        assertThat(nv.getType().toString()).isEqualTo("org.openrewrite.kotlin.C");
        assertThat(nv.getVariableType().toString())
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoatKt{name=function,return=kotlin.Unit,parameters=[org.openrewrite.kotlin.C]}{name=arg,type=org.openrewrite.kotlin.C}");

        J.VariableDeclarations.NamedVariable inMethod = ((J.VariableDeclarations) md.getBody().getStatements().getFirst()).getVariables().getFirst();
        assertThat(inMethod.getVariableType()).isEqualTo(inMethod.getName().getFieldType());
        assertThat(inMethod.getType().toString()).isEqualTo("kotlin.Int");
        assertThat(inMethod.getVariableType().toString())
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoatKt{name=function,return=kotlin.Unit,parameters=[org.openrewrite.kotlin.C]}{name=inFun,type=kotlin.Int}");
    }

    @Test
    void kotlinAnyHasNoSuperType() {
        assertThat(goatType.getSupertype().getSupertype()).isNull();
    }

    @Test
    void className() {
        JavaType.Class clazz = (JavaType.Class) this.firstMethodParameter("clazz");
        assertThat(TypeUtils.asFullyQualified(clazz).getFullyQualifiedName()).isEqualTo("org.openrewrite.kotlin.C");
    }

    @Test
    void interfacesContainImplicitAbstractFlag() {
        JavaType.Class clazz = (JavaType.Class) firstMethodParameter("clazz");
        JavaType.Method methodType = methodType("clazz");
        assertThat(clazz.getFlags()).contains(Flag.Abstract);
        assertThat(methodType.getFlags()).contains(Flag.Abstract);
    }

    @Test
    void constructor() {
        JavaType.Method ctor = methodType("<constructor>");
        assertThat(ctor.getDeclaringType().getFullyQualifiedName()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat");
    }

    @Test
    void parameterized() {
        JavaType.Parameterized parameterized = (JavaType.Parameterized) firstMethodParameter("parameterized");
        assertThat(parameterized.getType().getFullyQualifiedName()).isEqualTo("org.openrewrite.kotlin.PT");
        assertThat(TypeUtils.asFullyQualified(parameterized.getTypeParameters().getFirst()).getFullyQualifiedName()).isEqualTo("org.openrewrite.kotlin.C");

        J.MethodDeclaration md = goatClassDeclaration.getClassDeclaration().getBody().getStatements().stream()
          .filter(it -> (it instanceof J.MethodDeclaration md1) && "parameterized".equals(md1.getSimpleName()))
          .map(J.MethodDeclaration.class::cast).findFirst().orElseThrow();
        assertThat(md.getMethodType().toString())
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=parameterized,return=org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>,parameters=[org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>]}");
        J.VariableDeclarations vd = (J.VariableDeclarations) md.getParameters().getFirst();
        assertThat(vd.getTypeExpression().getType().toString())
          .isEqualTo("org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>");
        assertThat(((J.ParameterizedType) vd.getTypeExpression()).getClazz().getType().toString())
          .isEqualTo("org.openrewrite.kotlin.PT");
    }

    @Test
    void primitive() {
        JavaType.Class kotlinPrimitive = (JavaType.Class) firstMethodParameter("primitive");
        assertThat(kotlinPrimitive.getFullyQualifiedName()).isEqualTo("kotlin.Int");
    }

    @Test
    void generic() {
        JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) TypeUtils.asParameterized(firstMethodParameter("generic")).getTypeParameters().getFirst();
        assertThat(generic.getName()).isEqualTo("?");
        assertThat(generic.getVariance()).isEqualTo(COVARIANT);
        assertThat(TypeUtils.asFullyQualified(generic.getBounds().getFirst()).getFullyQualifiedName()).isEqualTo("org.openrewrite.kotlin.C");
    }

    @Test
    void genericContravariant() {
        JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) TypeUtils.asParameterized(firstMethodParameter("genericContravariant")).getTypeParameters().getFirst();
        assertThat(generic.getName()).isEqualTo("?");
        assertThat(generic.getVariance()).isEqualTo(CONTRAVARIANT);
        assertThat(TypeUtils.asFullyQualified(generic.getBounds().getFirst()).getFullyQualifiedName()).
          isEqualTo("org.openrewrite.kotlin.C");
    }

    @Test
    void genericMultipleBounds() {
        List<JavaType> typeParameters = goatType.getTypeParameters();
        JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) typeParameters.getLast();
        assertThat(generic.getName()).isEqualTo("S");
        assertThat(generic.getVariance()).isEqualTo(COVARIANT);
        assertThat(TypeUtils.asFullyQualified(generic.getBounds().getFirst()).getFullyQualifiedName()).isEqualTo("org.openrewrite.kotlin.PT");
        assertThat(TypeUtils.asFullyQualified(generic.getBounds().get(1)).getFullyQualifiedName()).
          isEqualTo("org.openrewrite.kotlin.C");
    }

    @Test
    void genericUnbounded() {
        JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) TypeUtils.asParameterized(firstMethodParameter("genericUnbounded")).getTypeParameters().getFirst();
        assertThat(generic.getName()).isEqualTo("U");
        assertThat(generic.getVariance()).isEqualTo(INVARIANT);
        assertThat(generic.getBounds()).isEmpty();
    }

    @Test
    void genericRecursive() {
        JavaType.Parameterized param = (JavaType.Parameterized) firstMethodParameter("genericRecursive");
        JavaType typeParam = param.getTypeParameters().getFirst();
        JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) typeParam;
        assertThat(generic.getName()).isEqualTo("?");
        assertThat(generic.getVariance()).isEqualTo(COVARIANT);
        assertThat(TypeUtils.asParameterized(generic.getBounds().getFirst())).isNotNull();

        JavaType.GenericTypeVariable elemType = (JavaType.GenericTypeVariable) TypeUtils.asParameterized(generic.getBounds().getFirst()).getTypeParameters().getFirst();
        assertThat(elemType.getName()).isEqualTo("U");
        assertThat(elemType.getVariance()).isEqualTo(COVARIANT);
        assertThat(elemType.getBounds()).hasSize(1);
    }

    @Test
    void innerClass() {
        JavaType.FullyQualified clazz = TypeUtils.asFullyQualified(firstMethodParameter("inner"));
        assertThat(clazz.getFullyQualifiedName()).isEqualTo("org.openrewrite.kotlin.C$Inner");
    }

    @Test
    void inheritedJavaTypeGoat() {
        JavaType.Parameterized clazz = (JavaType.Parameterized) firstMethodParameter("inheritedKotlinTypeGoat");
        assertThat(clazz.getTypeParameters().getFirst().toString()).isEqualTo("Generic{T}");
        assertThat(clazz.getTypeParameters().get(1).toString()).isEqualTo("Generic{U extends org.openrewrite.kotlin.PT<Generic{U}> & org.openrewrite.kotlin.C}");
        assertThat(clazz.toString()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat$InheritedKotlinTypeGoat<Generic{T}, Generic{U extends org.openrewrite.kotlin.PT<Generic{U}> & org.openrewrite.kotlin.C}>");
    }

    @Test
    void genericIntersectionType() {
        JavaType.GenericTypeVariable clazz = (JavaType.GenericTypeVariable) firstMethodParameter("genericIntersection");
        assertThat(clazz.getBounds().getFirst().toString()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat$TypeA");
        assertThat(clazz.getBounds().get(1).toString()).isEqualTo("org.openrewrite.kotlin.PT<Generic{U extends org.openrewrite.kotlin.KotlinTypeGoat$TypeA & org.openrewrite.kotlin.C}>");
        assertThat(clazz.getBounds().get(2).toString()).isEqualTo("org.openrewrite.kotlin.C");
        assertThat(clazz.toString()).isEqualTo("Generic{U extends org.openrewrite.kotlin.KotlinTypeGoat$TypeA & org.openrewrite.kotlin.PT<Generic{U}> & org.openrewrite.kotlin.C}");
    }

    @Test
    void enumTypeA() {
        JavaType.Class clazz = (JavaType.Class) firstMethodParameter("enumTypeA");
        JavaType.Method type = clazz.getMethods().stream()
          .filter(m -> "<constructor>".equals(m.getName()))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("No constructor found"));
        assertThat(type.toString()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat$EnumTypeA{name=<constructor>,return=org.openrewrite.kotlin.KotlinTypeGoat$EnumTypeA,parameters=[]}");

        JavaType.FullyQualified supertype = clazz.getSupertype();
        assertThat(supertype).isNotNull();
        assertThat(supertype.toString()).isEqualTo("kotlin.Enum<org.openrewrite.kotlin.KotlinTypeGoat$EnumTypeA>");
    }

    @Test
    void enumTypeB() {
        JavaType.Class clazz = (JavaType.Class) firstMethodParameter("enumTypeB");
        JavaType.Method type = clazz.getMethods().stream()
          .filter(m -> "<constructor>".equals(m.getName()))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("No constructor found"));
        assertThat(type.toString()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat$EnumTypeB{name=<constructor>,return=org.openrewrite.kotlin.KotlinTypeGoat$EnumTypeB,parameters=[org.openrewrite.kotlin.KotlinTypeGoat$TypeA]}");

        JavaType.FullyQualified supertype = clazz.getSupertype();
        assertThat(supertype).isNotNull();
        assertThat(supertype.toString()).isEqualTo("kotlin.Enum<org.openrewrite.kotlin.KotlinTypeGoat$EnumTypeB>");
    }

    @Test
    void ignoreSourceRetentionAnnotations() {
        JavaType.Parameterized goat = goatType;
        assertThat(goat.getAnnotations()).hasSize(1);
        assertThat(goat.getAnnotations().getFirst().getClassName()).isEqualTo("AnnotationWithRuntimeRetention");

        JavaType.Method clazzMethod = methodType("clazz");
        assertThat(clazzMethod.getAnnotations()).hasSize(1);
        assertThat(clazzMethod.getAnnotations().getFirst().getClassName()).isEqualTo("AnnotationWithRuntimeRetention");
    }

    @Test
    void receiver() {
        JavaType.Method receiverMethod = methodType("receiver");
        assertThat(receiverMethod.toString()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=receiver,return=kotlin.Unit,parameters=[org.openrewrite.kotlin.KotlinTypeGoat$TypeA,org.openrewrite.kotlin.C]}");
    }

    @Test
    void recursiveIntersection() {
        JavaType.GenericTypeVariable clazz = TypeUtils.asGeneric(firstMethodParameter("recursiveIntersection"));
        assertThat(clazz.toString()).isEqualTo("Generic{U extends org.openrewrite.kotlin.KotlinTypeGoat$Extension<Generic{U}> & org.openrewrite.kotlin.Intersection<Generic{U}>}");
    }

    @Test
    void javaLangObject() {
        // These assertions are all based on the JavaTypeMapper.
        JavaType.Class c = (JavaType.Class) firstMethodParameter("javaType");
        assertThat(c.getFullyQualifiedName()).isEqualTo("java.lang.Object");
        assertThat(c.getSupertype()).isNull();
        assertThat(c.getMethods()).hasSize(13);

        // Assert generic type parameters have the correct type bounds.
        JavaType.Method method = c.getMethods().stream().filter(it -> "getClass".equals(it.getName())).findFirst().orElse(null);
        assertThat(method).isNotNull();
        assertThat(method.toString()).isEqualTo("java.lang.Object{name=getClass,return=java.lang.Class<Generic{?}>,parameters=[]}");

        JavaType.Parameterized returnType = (JavaType.Parameterized) method.getReturnType();
        // Assert the type of the parameterized type contains the type parameter from the source.
        assertThat(returnType.getType().getTypeParameters().getFirst().toString()).isEqualTo("Generic{T}");
    }

    @Nested
    @SuppressWarnings({"KotlinConstantConditions", "RedundantExplicitType"})
    class ParsingTest implements RewriteTest {

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/303")
        @Test
        void coneTypeProjection() {
            rewriteRun(
              kotlin(
                """
                  val newList = buildList {
                      addAll(listOf(""))
                  }
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        MethodMatcher methodMatcher = new MethodMatcher("kotlin.collections.MutableList addAll(..)");
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<AtomicBoolean>() {
                            @Override
                            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                                if (methodMatcher.matches(method)) {
                                    assertThat(method.getMethodType().toString()).isEqualTo("kotlin.collections.MutableList<Generic{E}>{name=addAll,return=kotlin.Boolean,parameters=[kotlin.collections.Collection<Generic{E}>]}");
                                    found.set(true);
                                }
                                return super.visitMethodInvocation(method, found);
                            }
                        }.visit(cu, found);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/199")
        @Test
        void coneFlexibleType() {
            //noinspection RemoveRedundantQualifierName
            rewriteRun(
              kotlin(
                """
                  import java.lang.invoke.TypeDescriptor.OfField

                  abstract class Foo<T> : OfField<Foo<Any>>
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<AtomicBoolean>() {
                            @Override
                            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, AtomicBoolean found) {
                                if ("Foo".equals(classDecl.getSimpleName())) {
                                    assertThat(classDecl.getImplements().getFirst().getType().toString()).isEqualTo("java.lang.invoke.TypeDescriptor$OfField<Foo<kotlin.Any>>");
                                    found.set(true);
                                }
                                return super.visitClassDeclaration(classDecl, found);
                            }
                        }.visit(cu, found);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @Test
        void genericIntersectionType() {
            rewriteRun(
              kotlin(
                """
                  val l = listOf ( "foo" to "1" , "bar" to 2 )
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        MethodMatcher methodMatcher = new MethodMatcher("kotlin.collections.CollectionsKt listOf(..)");
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<AtomicBoolean>() {
                            @Override
                            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                                if (methodMatcher.matches(method)) {
                                    assertThat(method.getMethodType().toString())
                                            .isEqualTo("kotlin.collections.CollectionsKt{name=listOf,return=kotlin.collections.List<kotlin.Pair<kotlin.String, Generic{kotlin.Comparable<Generic{?}> & java.io.Serializable}>>,parameters=[kotlin.Array<Generic{? extends Generic{T}}>]}");
                                    found.set(true);
                                }
                                return super.visitMethodInvocation(method, found);
                            }
                        }.visit(cu, found);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @Test
        void implicitInvoke() {
            rewriteRun(
              kotlin(
                """
                  val block: Collection<Any>.() -> Unit = {}
                  val r = listOf("descriptor").block()
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<AtomicBoolean>() {
                            final MethodMatcher matcher = new MethodMatcher("kotlin.Function1 block(..)");

                            @Override
                            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean atomicBoolean) {
                                if (matcher.matches(method)) {
                                    assertThat(method.getMethodType().toString()).isEqualTo("kotlin.Function1<kotlin.collections.Collection<kotlin.Any>, kotlin.Unit>{name=block,return=kotlin.Unit,parameters=[kotlin.collections.Collection<kotlin.Any>]}");
                                    found.set(true);
                                }
                                return super.visitMethodInvocation(method, atomicBoolean);
                            }
                        }.visit(cu, found);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @Test
        void coneProjection() {
            rewriteRun(
              kotlin(
                """
                  val map = mapOf(Pair("one", 1)) as? Map<*, *>
                  val s = map.orEmpty().entries.joinToString { (key, value) -> "$key: $value" }
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<AtomicBoolean>() {
                            @Override
                            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, AtomicBoolean found) {
                                if ("entries".equals(fieldAccess.getSimpleName())) {
                                    assertThat(fieldAccess.getName().getType().toString())
                                            .isEqualTo("kotlin.collections.Set<kotlin.collections.Map$Entry<Generic{?}, kotlin.Any>>");
                                    assertThat(fieldAccess.getName().getFieldType().toString())
                                            .isEqualTo("kotlin.collections.Map{name=entries,type=kotlin.collections.Set<kotlin.collections.Map$Entry<Generic{?}, kotlin.Any>>}");
                                    found.set(true);
                                }
                                return super.visitFieldAccess(fieldAccess, found);
                            }
                        }.visit(cu, found);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/366")
        @Test
        void doesNotNpe() {
            rewriteRun(
              kotlin(
                """
                  class BuilderClass
                  class A {
                      val builder = BuilderClass::class.java.constructors[0].newInstance()!!
                  }
                  """
              )
            );
        }

        @Test
        void println() {
            //noinspection RemoveRedundantQualifierName
            rewriteRun(
              kotlin(
                """
                  fun method() {
                      println("foo")
                  }
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<AtomicBoolean>() {
                            @Override
                            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean atomicBoolean) {
                                if ("println".equals(method.getSimpleName())) {
                                    assertThat(method.getMethodType().toString()).isEqualTo("kotlin.io.ConsoleKt{name=println,return=kotlin.Unit,parameters=[kotlin.Any]}");
                                    found.set(true);
                                }
                                return super.visitMethodInvocation(method, atomicBoolean);
                            }
                        }.visit(cu, found);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @Test
        void whenExpression() {
            rewriteRun(
              kotlin(
                """
                  @Suppress("UNUSED_VARIABLE")
                  fun method() {
                      val condition: Int = 11
                      val a = when {
                          condition < 20   ->    'c'
                          condition < 10   ->    -1
                          condition > 10   ->    (true)
                          else             ->    0.9
                      }
                  }
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<AtomicBoolean>() {
                            @Override
                            public K.When visitWhen(K.When when, AtomicBoolean found) {
                                if (when.getType() instanceof JavaType.GenericTypeVariable) {
                                    assertThat(when.getType().toString()).isEqualTo("Generic{kotlin.Comparable<Generic{?}> & java.io.Serializable}");
                                    found.set(true);
                                }
                                return super.visitWhen(when, found);
                            }
                        }.visit(cu, found);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @CsvSource(value = {
          "n++~kotlin.Int",
          "--n~kotlin.Int",
          "n += a~kotlin.Int",
          "n = a + b~kotlin.Int"
        }, delimiter = '~')
        @ParameterizedTest
        void operatorOverload(String p1, String p2) {
            rewriteRun(
              kotlin(
                """
                  class Foo {
                      fun bar(a: Int, b: Int) {
                          var n = 0
                          %s
                      }
                  }
                  """.formatted(p1), spec -> spec.afterRecipe(cu -> {
                    AtomicBoolean found = new AtomicBoolean(false);
                    new KotlinIsoVisitor<AtomicBoolean>() {
                        @Override
                        public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, AtomicBoolean atomicBoolean) {
                            if (p2.equals(assignOp.getType().toString())) {
                                found.set(true);
                            }
                            return super.visitAssignmentOperation(assignOp, atomicBoolean);
                        }

                        @Override
                        public J.Unary visitUnary(J.Unary unary, AtomicBoolean b) {
                            if (p2.equals(unary.getType().toString())) {
                                found.set(true);
                            }
                            return super.visitUnary(unary, b);
                        }

                        @Override
                        public J.Binary visitBinary(J.Binary binary, AtomicBoolean b) {
                            JavaType.Class mt = (JavaType.Class) binary.getType();
                            if (p2.equals(mt.toString())) {
                                found.set(true);
                            }
                            return super.visitBinary(binary, b);
                        }
                    }.visit(cu, found);
                    assertThat(found.get()).isTrue();
                })
              )
            );
        }

        @CsvSource(value = {
          // Method type on overload with no named arguments.
          "foo(\"\", 1, true)~org.example.openRewriteFile0Kt{name=foo,return=kotlin.Unit,parameters=[kotlin.String,kotlin.Int,kotlin.Boolean]}",
          // Method type on overload with named arguments.
          "foo(b = 1)~org.example.openRewriteFile0Kt{name=foo,return=kotlin.Unit,parameters=[kotlin.Int,kotlin.Boolean]}",
          // Method type when named arguments are declared out of order.
          "foo(trailingLambda = {}, noDefault = true, c = true, b = 1)~org.example.openRewriteFile0Kt{name=foo,return=kotlin.Unit,parameters=[kotlin.String,kotlin.Int,kotlin.Boolean,kotlin.Boolean,kotlin.Function0<kotlin.Unit>]}",
          // Method type with trailing lambda
          "foo(b = 1, noDefault = true) {}~org.example.openRewriteFile0Kt{name=foo,return=kotlin.Unit,parameters=[kotlin.String,kotlin.Int,kotlin.Boolean,kotlin.Boolean,kotlin.Function0<kotlin.Unit>]}"
        }, delimiter = '~')
        @ParameterizedTest
        void methodInvocationWithDefaults(String invocation, String methodType) {
            rewriteRun(
              kotlin(
                """
                  package org.example
                  
                  fun <T> foo(b: T, c: Boolean = true) {
                      foo("", b, true, c) {}
                  }
                  fun <T> foo(a: String = "", b: T, c: Boolean = true) {
                      foo(a, b, true, c) {}
                  }
                  fun <T> foo(a: String = "", b: T, noDefault: Boolean , c: Boolean = true, trailingLambda: () -> Unit) {
                  }
                  fun m() {
                      %s
                  }
                  """.formatted(invocation), spec -> spec.afterRecipe(cu -> {
                    MethodMatcher matcher = new MethodMatcher("*..* foo(..)");
                    AtomicBoolean methodFound = new AtomicBoolean(false);
                    new KotlinIsoVisitor<AtomicBoolean>() {
                        @Override
                        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, AtomicBoolean found) {
                            if (!"m".equals(method.getSimpleName())) {
                                return method;
                            }
                            return super.visitMethodDeclaration(method, found);
                        }

                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                            if (matcher.matches(method)) {
                                assertThat(method.getMethodType()).satisfies(m -> {
                                    assertThat(m.toString()).isEqualTo(methodType);
                                    assertThat(m.getDeclaringType())
                                      .satisfies(it -> {
                                          assertThat(it.getFullyQualifiedName()).isEqualTo("org.example.openRewriteFile0Kt");
                                          assertThat(it.getMethods()).extracting(JavaType.Method::getName).containsExactlyInAnyOrder("foo", "foo", "foo", "m");
                                      });
                                });
                                found.set(true);
                            }
                            return super.visitMethodInvocation(method, found);
                        }
                    }.visit(cu, methodFound);
                    assertThat(methodFound.get()).isTrue();
                })
              )
            );
        }

        @Test
        void operatorOverload() {
            rewriteRun(
              kotlin(
                """
                  @Suppress("UNUSED_PARAMETER")
                  class Foo {
                      operator fun contains(element: Int): Boolean {
                          return true
                      }
                      val b = 1 !in listOf(2)
                      val a = 1 !in Foo()
                  }
                  """,
                    spec -> spec.afterRecipe(cu -> new KotlinIsoVisitor<Integer>() {
                        @Override
                        public K.Binary visitBinary(K.Binary binary, Integer integer) {
                            JavaType type = binary.getType();
                            assertThat(type).isInstanceOf(JavaType.Class.class);
                            assertThat(((JavaType.Class) type).getFullyQualifiedName()).isEqualTo("kotlin.Boolean");
                            return binary;
                        }
                    }.visit(cu, 0))
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/464")
        @Test
        void fieldAccessOnSuperType() {
            rewriteRun(
              kotlin(
                """
                  open class A {
                      val id: Int = 0
                  }
                  class B : A() {
                      fun get(): Int {
                          return super.id
                      }
                  }
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<Integer>() {
                            @Override
                            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Integer integer) {
                                assertThat(fieldAccess.getType().toString()).isEqualTo("kotlin.Int");
                                found.set(true);
                                return super.visitFieldAccess(fieldAccess, integer);
                            }
                        }.visit(cu, 0);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/464")
        @Test
        void parameterizedType() {
            rewriteRun(
              kotlin(
                """
                  import java.util.ArrayList

                  class Foo {
                      val l: ArrayList<String> = ArrayList<String>()
                  }
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<Integer>() {
                            @Override
                            public J.ParameterizedType visitParameterizedType(J.ParameterizedType type, Integer integer) {
                                assertThat(type.getType().toString()).isEqualTo("java.util.ArrayList<kotlin.String>");
                                assertThat(type.getClazz().getType().toString()).isEqualTo("java.util.ArrayList");
                                found.set(true);
                                return super.visitParameterizedType(type, integer);
                            }
                        }.visit(cu, 0);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/464")
        @SuppressWarnings("UnusedReceiverParameter")
        @Test
        void parameterizedReceiver() {
            rewriteRun(
              kotlin(
                """
                  class SomeParameterized<T>
                  val SomeParameterized < Int > . receivedMember : Int
                      get ( ) = 42
                  """,
                    spec -> spec.afterRecipe(cu -> new KotlinIsoVisitor<Integer>() {
                        @Override
                        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Integer integer) {
                            assertThat(classDecl.getType().toString()).isEqualTo("SomeParameterized<Generic{T}>");
                            assertThat(classDecl.getName().getType().toString()).isEqualTo("SomeParameterized<Generic{T}>");
                            return super.visitClassDeclaration(classDecl, integer);
                        }

                        @Override
                        public K.Property visitProperty(K.Property property, Integer integer) {
                            assertThat(property.getReceiver().getType().toString()).isEqualTo("SomeParameterized<kotlin.Int>");
                            assertThat(((J.ParameterizedType) property.getReceiver()).getClazz().getType().toString()).isEqualTo("SomeParameterized");
                            return super.visitProperty(property, integer);
                        }

                        @Override
                        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer integer) {
                            assertThat(variable.getVariableType().toString()).isEqualTo("openRewriteFile0Kt{name=receivedMember,type=kotlin.Int}");
                            assertThat(variable.getName().getType().toString()).isEqualTo("kotlin.Int");
                            assertThat(variable.getName().getFieldType().toString()).isEqualTo("openRewriteFile0Kt{name=receivedMember,type=kotlin.Int}");
                            return super.visitVariable(variable, integer);
                        }
                    }.visit(cu, 0))
              )
            );
        }

        @Test
        void destructs() {
            rewriteRun(
              kotlin(
                """
                  @Suppress("UNUSED_VARIABLE")
                  fun foo() {
                      val ( a , b , c ) = Triple ( 1 , 2 , 3 )
                  }
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<AtomicBoolean>() {
                            @Override
                            public K.DestructuringDeclaration visitDestructuringDeclaration(K.DestructuringDeclaration destructuringDeclaration, AtomicBoolean found) {
                                found.set(true);
                                return super.visitDestructuringDeclaration(destructuringDeclaration, found);
                            }

                            @Override
                            public J.NewClass visitNewClass(J.NewClass newClass, AtomicBoolean found) {
                                if ("Triple".equals(((J.Identifier) newClass.getClazz()).getSimpleName())) {
                                    assertThat(newClass.getClazz().getType().toString()).isEqualTo("kotlin.Triple");
                                    assertThat(newClass.getConstructorType().toString()).isEqualTo("kotlin.Triple<kotlin.Int, kotlin.Int, kotlin.Int>{name=<constructor>,return=kotlin.Triple<kotlin.Int, kotlin.Int, kotlin.Int>,parameters=[kotlin.Int,kotlin.Int,kotlin.Int]}");
                                }
                                return super.visitNewClass(newClass, found);
                            }

                            @Override
                            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, AtomicBoolean found) {
                                switch (variable.getSimpleName()) {
                                    case "<destruct>" -> assertThat(variable.getName().getType().toString())
                                            .isEqualTo("kotlin.Triple<kotlin.Int, kotlin.Int, kotlin.Int>");
                                    case "a" -> {
                                        assertThat(variable.getVariableType().toString())
                                                .isEqualTo("openRewriteFile0Kt{name=foo,return=kotlin.Unit,parameters=[]}{name=a,type=kotlin.Int}");
                                        assertThat(variable.getInitializer()).isNull();
                                    }
                                    case "b" -> {
                                        assertThat(variable.getVariableType().toString())
                                                .isEqualTo("openRewriteFile0Kt{name=foo,return=kotlin.Unit,parameters=[]}{name=b,type=kotlin.Int}");
                                        assertThat(variable.getInitializer()).isNull();
                                    }
                                    case "c" -> {
                                        assertThat(variable.getVariableType().toString())
                                                .isEqualTo("openRewriteFile0Kt{name=foo,return=kotlin.Unit,parameters=[]}{name=c,type=kotlin.Int}");
                                        assertThat(variable.getInitializer()).isNull();
                                    }
                                }
                                return super.visitVariable(variable, found);
                            }
                        }.visit(cu, found);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/374")
        @SuppressWarnings("CanBePrimaryConstructorProperty")
        @Test
        void privateToThisModifier() {
            rewriteRun(
              kotlin(
                """
                  @file:Suppress("UNUSED_VARIABLE")
                  class A<in T>(t: T) {
                      private val t: T = t // visibility for t is PRIVATE_TO_THIS

                      fun test() {
                          val x: T = t // correct
                          val y: T = this.t // also correct
                      }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/373")
        @Test
        void javaTypeFullyQualified() {
            rewriteRun(
              kotlin(
                """
                  @file:Suppress("UNUSED_PARAMETER")
                  open class Object<T>
                  class Test(name: String, any: Any)

                  fun <T> foo(name: String) =
                      Test(name, object : Object<T>() {})
                  """
              )
            );
        }

        @Test
        void variableTypes() {
            rewriteRun(
              kotlin(
                """
                  @file:Suppress("UNUSED_VARIABLE")
                  val foo1: Int = 42
                  class Foo(val foo2: Int) {
                      val foo3: Int = 42
                      fun m(foo4: Int) {
                          val use: Int = foo4
                      }
                  }
                  """,
                    spec -> spec.afterRecipe(cu -> new KotlinIsoVisitor<Integer>() {
                        @Override
                        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer integer) {
                            switch (variable.getSimpleName()) {
                                case "foo1": {
                                    assertThat(variable.getVariableType().toString()).isEqualTo("openRewriteFile0Kt{name=foo1,type=kotlin.Int}");
                                    break;
                                }
                                case "foo2": {
                                    assertThat(variable.getVariableType().toString()).isEqualTo("Foo{name=foo2,type=kotlin.Int}");
                                    break;
                                }
                                case "foo3": {
                                    assertThat(variable.getVariableType().toString()).isEqualTo("Foo{name=foo3,type=kotlin.Int}");
                                    break;
                                }
                                case "foo4": {
                                    assertThat(variable.getVariableType().toString()).isEqualTo("Foo{name=m,return=kotlin.Unit,parameters=[kotlin.Int]}{name=foo4,type=kotlin.Int}");
                                    break;
                                }
                            }
                            return super.visitVariable(variable, integer);
                        }
                    }.visit(cu, 0))
              )
            );
        }

        @Test
        void packageStaticModifier() {
            rewriteRun(
              kotlin(
                """
                  import java.rmi.server.RemoteStub

                  class A : RemoteStub()
                  """
              )
            );
        }

        @Test
        void unknownIdentifier() {
            rewriteRun(
              spec -> spec.typeValidationOptions(TypeValidation.none()),
              kotlin(
                //language=none
                "class A : RemoteStub",
                    spec -> spec.afterRecipe(cu -> {
                        new KotlinIsoVisitor<Integer>() {
                            @Override
                            public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                                if ("RemoteStub".equals(identifier.getSimpleName())) {
                                    assertThat(identifier.getType()).isNull();
                                }
                                return super.visitIdentifier(identifier, integer);
                            }
                        }.visit(cu, 0);
                })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/461")
        @Test
        void multipleBounds() {
            rewriteRun(
              kotlin(
                """
                  interface A
                  interface B
                  interface C
                  interface D

                  class KotlinTypeGoat<T, S>  where   S : A, T : D, S : B, T : C
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<AtomicBoolean>() {
                            @Override
                            public K.ClassDeclaration visitClassDeclaration(K.ClassDeclaration classDeclaration, AtomicBoolean atomicBoolean) {
                                if ("KotlinTypeGoat".equals(classDeclaration.getClassDeclaration().getSimpleName())) {
                                    List<J.TypeParameter> constraints = classDeclaration.getTypeConstraints().getConstraints();
                                    for (int i = 0; i < constraints.size(); i++) {
                                        J.TypeParameter p = constraints.get(i);
                                        switch (i) {
                                            case 0:
                                                assertThat(p.getName().getType().toString()).isEqualTo("A");
                                                break;
                                            case 1:
                                                assertThat(p.getName().getType().toString()).isEqualTo("D");
                                                break;
                                            case 2:
                                                assertThat(p.getName().getType().toString()).isEqualTo("B");
                                                break;
                                            case 3:
                                                assertThat(p.getName().getType().toString()).isEqualTo("C");
                                                found.set(true);
                                                break;
                                        }
                                    }
                                }
                                return super.visitClassDeclaration(classDeclaration, atomicBoolean);
                            }
                        }.visit(cu, found);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @Test
        void nullJavaClassifierType() {
            rewriteRun(
              spec -> spec.parser(KotlinParser.builder().classpath("javapoet","compile-testing")),
              kotlin(
                //language=none  turn off language inspection, since the test does not have access to the class path.
                """
                  package org.openrewrite.kotlin

                  import com.google.testing.compile.Compilation
                  import com.google.testing.compile.CompilationSubject
                  import com.google.testing.compile.Compiler.javac
                  import com.squareup.javapoet.JavaFile

                  class Test {
                      fun assertCompilesJava(javaFiles: Collection<JavaFile>): Compilation {
                          val result = javac()
                              .withOptions("-parameters")
                              .compile(javaFiles.map(JavaFile::toJavaFileObject))
                          CompilationSubject.assertThat(result).succeededWithoutWarnings()
                          return result
                      }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/473")
        @ParameterizedTest
        @ValueSource(strings = {
          // Multiple levels of parameterized types
          "val map: Map<Map<Int, Int>, Map<Int?, Int?>> = mapOf()",
          // ConeTypeParameterType
          "val <T : Any> Collection<T>.nullable: Collection<T?>"
        })
        void typeParameters(String value) {
            rewriteRun(
              kotlin("%s".formatted(value),
                spec -> spec.afterRecipe(cu -> {
                    AtomicBoolean found = new AtomicBoolean(false);
                    new KotlinIsoVisitor<Integer>() {
                        @Override
                        public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                            if ("Int".equals(identifier.getSimpleName())) {
                                assertThat(identifier.getType().toString()).isEqualTo("kotlin.Int");
                                found.set(true);
                            } else if ("T".equals(identifier.getSimpleName())) {
                                assertThat(identifier.getType().toString()).isEqualTo("Generic{T extends kotlin.Any}");
                                found.set(true);
                            }
                            return super.visitIdentifier(identifier, integer);
                        }
                    }.visit(cu, 0);
                    assertThat(found.get()).isTrue();
                })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/479")
        @Test
        void quotedIdentifier() {
            //noinspection RemoveRedundantBackticks,RemoveRedundantQualifierName
            rewriteRun(
              kotlin(
                "val n = java.lang.Integer.`MAX_VALUE`",
                spec -> spec.afterRecipe(cu -> {
                      AtomicBoolean found = new AtomicBoolean(false);
                    new KotlinIsoVisitor<Integer>() {
                        @Override
                        public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                            if ("MAX_VALUE".equals(identifier.getSimpleName())) {
                                assertThat(identifier.getFieldType().getName()).isEqualTo("MAX_VALUE");
                                found.set(true);
                            }
                            return super.visitIdentifier(identifier, integer);
                        }
                    }.visit(cu, 0);
                    assertThat(found.get()).isTrue();
                })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/483")
        @Test
        void unusedVar() {
            //noinspection RemoveRedundantBackticks,RemoveRedundantQualifierName
            rewriteRun(
              kotlin(
                "val unused: (Int, Int) -> Int = { _, y -> y }",
                spec -> spec.afterRecipe(cu -> {
                    AtomicBoolean found = new AtomicBoolean(false);
                    new KotlinIsoVisitor<Integer>() {
                        @Override
                        public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                            if ("_".equals(identifier.getSimpleName())) {
                                assertThat(identifier.getFieldType().getName()).isEqualTo("_");
                                found.set(true);
                            }
                            return super.visitIdentifier(identifier, integer);
                        }
                    }.visit(cu, 0);
                    assertThat(found.get()).isTrue();
                })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/481")
        @Test
        void getAndSetMethodNames() {
            //noinspection RemoveRedundantBackticks,RemoveRedundantQualifierName
            rewriteRun(
              kotlin(
                """
                  class Test {
                      var stringRepresentation : String = ""
                          get ( ) = field
                          set ( value ) {
                              field = value
                          }
                  }
                  """,
                spec -> spec.afterRecipe(cu -> {
                    MethodMatcher getMatcher = new MethodMatcher("Test get(..)");
                    AtomicBoolean foundGet = new AtomicBoolean(false);

                    MethodMatcher setMatcher = new MethodMatcher("Test set(..)");
                    AtomicBoolean foundSet = new AtomicBoolean(false);
                    new KotlinIsoVisitor<Integer>() {
                        @Override
                        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Integer integer) {
                            if (getMatcher.matches(method.getMethodType())) {
                                foundGet.set(true);
                            } else if (setMatcher.matches(method.getMethodType())) {
                                foundSet.set(true);
                            }
                            return super.visitMethodDeclaration(method, integer);
                        }
                    }.visit(cu, 0);
                    assertThat(foundGet.get()).isTrue();
                    assertThat(foundSet.get()).isTrue();
                })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/483")
        @SuppressWarnings("HasPlatformType")
        @Test
        void isStaticFlag() {
            //noinspection RemoveRedundantBackticks,RemoveRedundantQualifierName
            rewriteRun(
              kotlin(
                "val i = Integer.valueOf(1)",
                spec -> spec.afterRecipe(cu -> {
                    AtomicBoolean found = new AtomicBoolean(false);
                    MethodMatcher matcher = new MethodMatcher("java.lang.Integer valueOf(..)");
                    new KotlinIsoVisitor<Integer>() {
                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                            if (matcher.matches(method)) {
                                assertThat(Flag.hasFlags(method.getMethodType().getFlagsBitMap(), Flag.Static)).isTrue();
                                found.set(true);
                            }
                            return super.visitMethodInvocation(method, integer);
                        }
                    }.visit(cu, 0);
                    assertThat(found.get()).isTrue();
                })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/493")
        @Test
        void escapedImport() {
            //noinspection RemoveRedundantBackticks
            rewriteRun(
              kotlin(
                """
                  @file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                  import `java`.`util`.`List`
                  """,
                spec -> spec.afterRecipe(cu -> {
                    AtomicBoolean found = new AtomicBoolean(false);
                    new KotlinIsoVisitor<Integer>() {
                        @Override
                        public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                            assertThat(identifier.getSimpleName().startsWith("`")).isFalse();
                            return super.visitIdentifier(identifier, integer);
                        }

                        @Override
                        public J.Import visitImport(J.Import _import, Integer integer) {
                            assertThat(_import.getQualid().getType().toString()).isEqualTo("java.util.List");
                            found.set(true);
                            return super.visitImport(_import, integer);
                        }
                    }.visit(cu, 0);
                    assertThat(found.get()).isTrue();
                })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/485")
        @SuppressWarnings("SuspiciousCallableReferenceInLambda")
        @Test
        void memberReference() {
            //noinspection RemoveRedundantBackticks
            rewriteRun(
              kotlin(
                """
                  class Test ( val answer : Int )
                  fun method ( ) {
                      val l = listOf ( Test ( 42 ) )
                      l . map { Test :: answer }
                  }
                  """,
                spec -> spec.afterRecipe(cu -> {
                    AtomicBoolean found = new AtomicBoolean(false);
                    new KotlinIsoVisitor<Integer>() {
                        @Override
                        public J.MemberReference visitMemberReference(J.MemberReference memberRef, Integer integer) {
                            assertThat(memberRef.getType().toString()).isEqualTo("kotlin.reflect.KProperty1<Test, kotlin.Int>");
                            found.set(true);
                            return super.visitMemberReference(memberRef, integer);
                        }
                    }.visit(cu, 0);
                    assertThat(found.get()).isTrue();
                })
              )
            );
        }

        @CsvSource(value = {
          "fun foo(l: MutableList<String>) { @Suppress l += \"x\" }~kotlin.Suppress",
          "val releaseDates: List< @Suppress String > = emptyList()~kotlin.Suppress"
        }, delimiter = '~')
        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/485")
        @ParameterizedTest
        void annotationOnKotlinConeType(String input, String type) {
            //noinspection RemoveRedundantBackticks
            rewriteRun(
              kotlin(
                      "%s".formatted(input),
                spec -> spec.afterRecipe(cu -> {
                    AtomicBoolean found = new AtomicBoolean(false);
                    new KotlinIsoVisitor<Integer>() {
                        @Override
                        public J.Annotation visitAnnotation(J.Annotation annotation, Integer integer) {
                            assertThat(annotation.getType().toString()).isEqualTo(type);
                            found.set(true);
                            return super.visitAnnotation(annotation, integer);
                        }
                    }.visit(cu, 0);
                    assertThat(found.get()).isTrue();
                })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/506")
        @Test
        void methodTypeOnMethodInvocation() {
            //noinspection RemoveRedundantBackticks
            rewriteRun(
              kotlin(
                "val arr = listOf(1, 2, 3)",
                spec -> spec.afterRecipe(cu -> {
                    AtomicBoolean found = new AtomicBoolean(false);
                    new KotlinIsoVisitor<Integer>() {
                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                            assertThat(method.getName().getType()).isEqualTo(method.getMethodType());
                            found.set(true);
                            return super.visitMethodInvocation(method, integer);
                        }
                    }.visit(cu, 0);
                    assertThat(found.get()).isTrue();
                })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/510")
        @Test
        void fullyQualifiedClassConstructor() {
            rewriteRun(
              kotlin(
                """
                 class Test {
                     val sb = java.lang.StringBuilder().toString()
                 }
                 """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicBoolean isFieldTargetNull = new AtomicBoolean(false);
                        AtomicBoolean isStringBuilderTyped = new AtomicBoolean(false);
                        new KotlinIsoVisitor<Integer>() {
                            @Override
                            public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                                if ("lang".equals(identifier.getSimpleName())) {
                                    assertThat(identifier.getType()).isNull();
                                    isFieldTargetNull.set(true);
                                }
                                if ("StringBuilder".equals(identifier.getSimpleName())) {
                                    assertThat(identifier.getType().toString()).isEqualTo("java.lang.StringBuilder");
                                    isStringBuilderTyped.set(true);
                                }
                                return super.visitIdentifier(identifier, integer);
                            }
                        }.visit(cu, 0);
                        assertThat(isFieldTargetNull.get()).isTrue();
                        assertThat(isStringBuilderTyped.get()).isTrue();
                    })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/510")
        @Test
        void innerClasses() {
            rewriteRun(
              kotlin(
                """
                  @Suppress("UNUSED_PARAMETER")
                  fun foo(l: List<Pair<String, String>>) {}
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicInteger count = new AtomicInteger(0);
                        new KotlinIsoVisitor<Integer>() {
                            @Override
                            public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                                switch (identifier.getSimpleName()) {
                                    case "List" -> {
                                        assertThat(identifier.getType().toString()).isEqualTo("kotlin.collections.List");
                                        count.incrementAndGet();
                                    }
                                    case "Pair" -> {
                                        assertThat(identifier.getType().toString()).isEqualTo("kotlin.Pair");
                                        count.incrementAndGet();
                                    }
                                    case "String" -> {
                                        assertThat(identifier.getType().toString()).isEqualTo("kotlin.String");
                                        count.incrementAndGet();
                                    }
                                }
                                return super.visitIdentifier(identifier, integer);
                            }
                        }.visit(cu, 0);
                        assertThat(count.get()).isEqualTo(4);
                    })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/510")
        @Test
        void parameterizedParentClasses() {
            rewriteRun(
              kotlin(
                """
                  val m: MutableMap.MutableEntry<String, String>? = null
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicInteger count = new AtomicInteger(0);
                        new KotlinIsoVisitor<Integer>() {
                            @Override
                            public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                                switch (identifier.getSimpleName()) {
                                    case "MutableMap" -> {
                                        assertThat(identifier.getType().toString()).isEqualTo("kotlin.collections.MutableMap<Generic{K}, Generic{V}>");
                                        count.incrementAndGet();
                                    }
                                    case "MutableEntry" -> {
                                        assertThat(identifier.getType().toString()).isEqualTo("kotlin.collections.MutableMap$MutableEntry<kotlin.String, kotlin.String>");
                                        count.incrementAndGet();
                                    }
                                    case "String" -> {
                                        assertThat(identifier.getType().toString()).isEqualTo("kotlin.String");
                                        count.incrementAndGet();
                                    }
                                }
                                return super.visitIdentifier(identifier, integer);
                            }
                        }.visit(cu, 0);
                        assertThat(count.get()).isEqualTo(4);
                    })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/510")
        @Test
        void coneTypeProjectInAndOut() {
            rewriteRun(
              kotlin(
                """
                  @file:Suppress("UNUSED_PARAMETER")
                  fun foo(a: Array<in String>) {}
                  fun bar(b: Array<out Number>) {}
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicInteger count = new AtomicInteger(0);
                        new KotlinIsoVisitor<Integer>() {
                            @Override
                            public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                                if ("String".equals(identifier.getSimpleName())) {
                                    assertThat(identifier.getType().toString()).isEqualTo("kotlin.String");
                                    count.incrementAndGet();
                                } else if ("Number".equals(identifier.getSimpleName())) {
                                    assertThat(identifier.getType().toString()).isEqualTo("kotlin.Number");
                                    count.incrementAndGet();
                                }
                                return super.visitIdentifier(identifier, integer);
                            }
                        }.visit(cu, 0);
                        assertThat(count.get()).isEqualTo(2);
                    })
              )
            );
        }

        @Test
        void returnTypeIsVoid() {
            rewriteRun(
              kotlin(
                """
                 import java.lang.AutoCloseable
                 abstract class Test : AutoCloseable
                 """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<Integer>() {
                            @Override
                            public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                                if ("AutoCloseable".equals(identifier.getSimpleName()) && identifier.getType() != null) {
                                    assertThat(((JavaType.Class) identifier.getType()).getMethods().getFirst().toString())
                                            .isEqualTo("java.lang.AutoCloseable{name=close,return=void,parameters=[]}");
                                    found.set(true);
                                }
                                return super.visitIdentifier(identifier, integer);
                            }
                        }.visit(cu, 0);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/533")
        @Test
        void enumConstructorType() {
            rewriteRun(
              kotlin(
                """
                  enum class Code {
                      YES ,
                  }
                  enum class Test ( val arg: Code , ) {
                      FOO ( Code.YES , ) {
                          // Body is required to reproduce issue
                      }
                  }
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<Integer>() {
                            @Override
                            public J.EnumValue visitEnumValue(J.EnumValue _enum, Integer integer) {
                                if ("FOO".equals(_enum.getName().getSimpleName())) {
                                    assertThat(_enum.getInitializer().getConstructorType().toString())
                                            .isEqualTo("Test{name=<constructor>,return=Test,parameters=[Code]}");
                                    found.set(true);
                                }
                                return super.visitEnumValue(_enum, integer);
                            }
                        }.visit(cu, 0);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @Test
        void functionTypeOnParams() {
            rewriteRun(
              kotlin(
                """
                 fun foo() :   suspend    ( param : Int )  -> Unit = { }
                 """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<Integer>() {
                            @Override
                            public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                                if ("param".equals(identifier.getSimpleName()) && identifier.getType() != null) {
                                    assertThat(identifier.getType().toString()).isEqualTo("kotlin.Int");
                                    found.set(true);
                                }
                                return super.visitIdentifier(identifier, integer);
                            }
                        }.visit(cu, 0);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/526")
        @Test
        void methodDeclarationType() {
            rewriteRun(
              kotlin(
                """
                 val arr = arrayOf(1, 2, 3)
                 """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<Integer>() {
                            @Override
                            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                                assertThat(method.getMethodType().toString()).isEqualTo("kotlin.Library{name=arrayOf,return=kotlin.Array<kotlin.Int>,parameters=[kotlin.Array<Generic{? extends Generic{T}}>]}");
                                found.set(true);
                                return super.visitMethodInvocation(method, integer);
                            }
                        }.visit(cu, 0);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @Test
        void anonymousConstructor() {
            rewriteRun(
              kotlin(
                """
                 val s = java.util.function.Supplier<String> {
                     @Suppress("UNCHECKED_CAST")
                     requireNotNull("x")
                 }
                 """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicInteger count = new AtomicInteger(0);
                        new KotlinIsoVisitor<Integer>() {
                            @Override
                            public J.NewClass visitNewClass(J.NewClass newClass, Integer integer) {
                                assertThat(newClass.getMethodType().toString()).isEqualTo("java.util.function.Supplier{name=<constructor>,return=java.util.function.Supplier<kotlin.String>,parameters=[kotlin.Function0<Generic{T extends kotlin.Any}>]}");
                                count.getAndIncrement();
                                assertThat(newClass.getClazz().getType().toString()).isEqualTo("java.util.function.Supplier<kotlin.String>");
                                count.getAndIncrement();
                                return super.visitNewClass(newClass, integer);
                            }
                        }.visit(cu, 0);
                        assertThat(count.get()).isEqualTo(2);
                    })
              )
            );
        }

        @Test
        void constructorMemberReferenceType() {
            rewriteRun(
              kotlin(
                """
                  open class A(
                    val foo : ( ( Any ) -> A) -> A
                  )
                  class B : A ( foo = { x -> ( :: A ) ( x ) } ) {
                    @Suppress("UNUSED_PARAMETER")
                    fun mRef(a: Any) {}
                  }
                  """,
                    spec -> spec.afterRecipe(cu -> {
                        AtomicBoolean found = new AtomicBoolean(false);
                        new KotlinIsoVisitor<Integer>() {
                            @Override
                            public J.MemberReference visitMemberReference(J.MemberReference memberRef, Integer integer) {
                                if ("A".equals(memberRef.getReference().getSimpleName())) {
                                    assertThat(memberRef.getMethodType().toString()).isEqualTo("A{name=<constructor>,return=A,parameters=[kotlin.Function1<kotlin.Function1<kotlin.Any, A>, A>]}");
                                    found.set(true);
                                }
                                return super.visitMemberReference(memberRef, integer);
                            }
                        }.visit(cu, 0);
                        assertThat(found.get()).isTrue();
                    })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/517")
        @Test
        void nestedFieldAccessType() {
            rewriteRun(
              kotlin(
                """
                  class A {
                      class B {
                          class A {
                              class C
                          }
                      }
                  }

                  val x = A.B.A.C()
                  """,
                spec -> spec.afterRecipe(cu -> {
                      AtomicInteger count = new AtomicInteger(0);
                      new KotlinIsoVisitor<Integer>() {
                          @Override
                          public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Integer n) {
                              // 1. A fieldAccess should have the matched name and type
                              assert fieldAccess.getType() != null;
                              String expectedType = fieldAccess.toString().replace(".","$");
                              String actualType = fieldAccess.getType().toString();
                              assertThat(expectedType).isEqualTo(actualType);
                              count.getAndIncrement();

                              // 2. The 1st element of the field access should have the right type
                              Expression target = fieldAccess.getTarget();
                              if (target instanceof J.Identifier id) {
                                  assert id.getType() != null;
                                  assertThat (id.getType().toString()).isEqualTo(id.getSimpleName());
                                  count.getAndIncrement();
                              }

                              return super.visitFieldAccess(fieldAccess, n);
                          }
                      }.visit(cu, 0);
                      assertThat(count.get()).isEqualTo(4);
                  }
                )
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/517")
        @Test
        void nestedFieldAccessWithPackage() {
            rewriteRun(
              kotlin(
                """
                  package    foo.bar
                  class A {
                      class B {
                          class A {
                              class C<T>
                          }
                      }
                  }
                  """
              ),
              kotlin(
                "val x = foo.bar.A.B.A.C<String>()",
                spec -> spec.afterRecipe(cu -> {
                      AtomicInteger count = new AtomicInteger(0);
                      new KotlinIsoVisitor<Integer>() {

                          @Override
                          public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                              if ("foo".equals(identifier.getSimpleName())) {
                                  assertThat(identifier.getType()).isNull();
                                  count.getAndIncrement();
                              }
                              return super.visitIdentifier(identifier, integer);
                          }

                          @Override
                          public J.ParameterizedType visitParameterizedType(J.ParameterizedType type, Integer integer) {
                              if ("C".equals(((J.FieldAccess) type.getClazz()).getSimpleName())) {
                                  assertThat(type.getType().toString()).isEqualTo("foo.bar.A$B$A$C<kotlin.String>");
                              }
                              return super.visitParameterizedType(type, integer);
                          }

                          @Override
                          public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Integer n) {
                              // A fieldAccess should have the matched name and type
                              String text = fieldAccess.toString();
                              String fieldAccessSignature = fieldAccess.getType() != null ? fieldAccess.getType().toString() : "";
                              String nameSignature = fieldAccess.getName().getType() != null ? fieldAccess.getName().getType().toString() : "";
                              assertThat(fieldAccessSignature).isEqualTo(nameSignature);

                              switch (text) {
                                  case "foo.bar.A.B.A.C":
                                      assertThat(fieldAccessSignature).isEqualTo("foo.bar.A$B$A$C");
                                      count.getAndIncrement();
                                      break;
                                  case "foo.bar.A.B.A":
                                      assertThat(fieldAccessSignature).isEqualTo("foo.bar.A$B$A");
                                      count.getAndIncrement();
                                      break;
                                  case "foo.bar.A.B":
                                      assertThat(fieldAccessSignature).isEqualTo("foo.bar.A$B");
                                      count.getAndIncrement();
                                      break;
                                  case "foo.bar.A":
                                      assertThat(fieldAccessSignature).isEqualTo("foo.bar.A");
                                      count.getAndIncrement();
                                      break;
                                  case "foo.bar":
                                      assertThat(fieldAccessSignature).isEmpty();
                                      count.getAndIncrement();
                                      break;
                              }

                              return super.visitFieldAccess(fieldAccess, n);
                          }
                      }.visit(cu, 0);
                      assertThat(count.get()).isEqualTo(6);
                  }
                )
              )
            );
        }

        @Test
        void packageFieldAccess() {
            rewriteRun(
              kotlin(
                """
                  package   foo.bar
                  class A
                  """
              ),
              kotlin(
                "val x = foo.bar.A()",
                spec -> spec.afterRecipe(cu -> {
                    AtomicBoolean found = new AtomicBoolean(false);
                    new KotlinIsoVisitor<Integer>() {
                        @Override
                        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Integer n) {
                            if ("foo.bar".equals(fieldAccess.toString())) {
                                String fieldAccessSignature = fieldAccess.getType() != null ? fieldAccess.getType().toString() : "";
                                assertThat(fieldAccessSignature).isEqualTo("Unknown");
                                found.set(true);
                            }
                            return super.visitFieldAccess(fieldAccess, n);
                        }
                    }.visit(cu, 0);
                    assertThat(found.get()).isTrue();
                })
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/590")
        @Test
        void callWithDefaultedGenericParameters() {
            rewriteRun(
              kotlin(
                """
                  internal data class Foo<T : Any>(val i: T, val j: T) {
                      fun f() {
                          return copy(i = 42)
                      }
                  }
                  """,
                spec -> spec.afterRecipe(cu -> {
                    AtomicBoolean found = new AtomicBoolean(false);
                    new KotlinIsoVisitor<Integer>() {
                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                            if ("copy".equals(method.getSimpleName())) {
                                String signature = method.getMethodType() != null ? method.getMethodType().toString() : "";
                                assertThat(signature).isEqualTo("Foo<Generic{T extends kotlin.Any}>{name=copy,return=Foo<Generic{T extends kotlin.Any}>,parameters=[kotlin.Int,Generic{T extends kotlin.Any}]}");
                                found.set(true);
                            }
                            return super.visitMethodInvocation(method, integer);
                        }
                    }.visit(cu, 0);
                    assertThat(found.get()).isTrue();
                })
              )
            );
        }
    }
}
