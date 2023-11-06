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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*;
import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings("ConstantConditions")
public class KotlinTypeMappingTest {
    private static final String goat = StringUtils.readFully(KotlinTypeMappingTest.class.getResourceAsStream("/KotlinTypeGoat.kt"));
    private static final K.CompilationUnit cu;
    private static final K.ClassDeclaration goatClassDeclaration;

    static {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(REQUIRE_PRINT_EQUALS_INPUT, false);
        cu = (K.CompilationUnit) KotlinParser.builder()
            .logCompilationWarningsAndErrors(true)
            .build()
            .parseInputs(singletonList(new Parser.Input(Paths.get("KotlinTypeGoat.kt"), () -> new ByteArrayInputStream(goat.getBytes(StandardCharsets.UTF_8)))), null, ctx)
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

    public J.VariableDeclarations getField(String fieldName) {
        return goatClassDeclaration.getClassDeclaration().getBody().getStatements().stream()
          .filter(it -> it instanceof org.openrewrite.java.tree.J.VariableDeclarations || it instanceof K.Property)
          .map(it -> it instanceof K.Property ? ((K.Property) it).getVariableDeclarations() : (J.VariableDeclarations) it)
          .map(J.VariableDeclarations.class::cast)
          .filter(mv -> mv.getVariables().stream().anyMatch(v -> v.getSimpleName().equals(fieldName)))
          .findFirst()
          .orElse(null);
    }

    public K.Property getProperty(String fieldName) {
        return goatClassDeclaration.getClassDeclaration().getBody().getStatements().stream()
                .filter(it -> it instanceof K.Property)
                .map(K.Property.class::cast)
                .filter(mv -> mv.getVariableDeclarations().getVariables().stream().anyMatch(v -> v.getSimpleName().equals(fieldName)))
                .findFirst()
                .orElse(null);
    }

    public JavaType firstMethodParameter(String methodName) {
        return methodType(methodName).getParameterTypes().get(0);
    }

    @Test
    void extendsKotlinAny() {
        assertThat(goatType.getSupertype().getFullyQualifiedName()).isEqualTo("kotlin.Any");
    }

    @Test
    void fieldType() {
        K.Property property = getProperty("field");
        J.VariableDeclarations.NamedVariable variable = property.getVariableDeclarations().getVariables().get(0);
        J.Identifier id = variable.getName();
        assertThat(variable.getType()).isEqualTo(id.getType());
        assertThat(id.getFieldType()).isInstanceOf(JavaType.Variable.class);
        assertThat(id.getFieldType().toString()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=field,type=kotlin.Int}");
        assertThat(id.getType()).isInstanceOf(JavaType.Class.class);
        assertThat(id.getType().toString()).isEqualTo("kotlin.Int");

        JavaType.FullyQualified declaringType = property.getGetter().getMethodType().getDeclaringType();
        assertThat(declaringType.getFullyQualifiedName()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat");
        assertThat(property.getGetter().getMethodType().getName()).isEqualTo("accessor");
        assertThat(property.getGetter().getMethodType().getReturnType()).isEqualTo(id.getType());
        assertThat(property.getGetter().getName().getType()).isEqualTo(property.getGetter().getMethodType());
        assertThat(property.getGetter().getMethodType().toString().substring(declaringType.toString().length())).isEqualTo("{name=accessor,return=kotlin.Int,parameters=[]}");

        declaringType = property.getSetter().getMethodType().getDeclaringType();
        assertThat(declaringType.getFullyQualifiedName()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat");
        assertThat(property.getSetter().getMethodType().getName()).isEqualTo("accessor");
        assertThat(property.getSetter().getMethodType()).isEqualTo(property.getSetter().getName().getType());
        assertThat(property.getSetter().getMethodType().toString().substring(declaringType.toString().length())).isEqualTo("{name=accessor,return=kotlin.Unit,parameters=[kotlin.Int]}");
    }

    @Test
    void fileField() {
        J.VariableDeclarations.NamedVariable nv = cu.getStatements().stream()
          .filter(it -> it instanceof J.VariableDeclarations)
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
            .filter(it -> it instanceof J.MethodDeclaration)
              .map(J.MethodDeclaration.class::cast)
                .filter(it -> "function".equals(it.getSimpleName())).findFirst().orElseThrow();

        assertThat(md.getName().getType()).isEqualTo(md.getMethodType());
        assertThat(md.getMethodType().toString())
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoatKt{name=function,return=kotlin.Unit,parameters=[org.openrewrite.kotlin.C]}");

        J.VariableDeclarations.NamedVariable nv = ((J.VariableDeclarations) md.getParameters().get(0)).getVariables().get(0);
        assertThat(nv.getVariableType()).isEqualTo(nv.getName().getFieldType());
        assertThat(nv.getType().toString()).isEqualTo("org.openrewrite.kotlin.C");
        assertThat(nv.getVariableType().toString())
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoatKt{name=function,return=kotlin.Unit,parameters=[org.openrewrite.kotlin.C]}{name=arg,type=org.openrewrite.kotlin.C}");

        J.VariableDeclarations.NamedVariable inMethod = ((J.VariableDeclarations) md.getBody().getStatements().get(0)).getVariables().get(0);
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
        assertThat(TypeUtils.asFullyQualified(parameterized.getTypeParameters().get(0)).getFullyQualifiedName()).isEqualTo("org.openrewrite.kotlin.C");

        J.MethodDeclaration md = goatClassDeclaration.getClassDeclaration().getBody().getStatements().stream()
          .filter(it -> (it instanceof J.MethodDeclaration) && "parameterized".equals(((J.MethodDeclaration) it).getSimpleName()))
          .map(J.MethodDeclaration.class::cast).findFirst().orElseThrow();
        assertThat(md.getMethodType().toString())
          .isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat{name=parameterized,return=org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>,parameters=[org.openrewrite.kotlin.PT<org.openrewrite.kotlin.C>]}");
        J.VariableDeclarations vd = ((J.VariableDeclarations) md.getParameters().get(0));
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
        JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) TypeUtils.asParameterized(firstMethodParameter("generic")).getTypeParameters().get(0);
        assertThat(generic.getName()).isEqualTo("?");
        assertThat(generic.getVariance()).isEqualTo(COVARIANT);
        assertThat(TypeUtils.asFullyQualified(generic.getBounds().get(0)).getFullyQualifiedName()).isEqualTo("org.openrewrite.kotlin.C");
    }

    @Test
    void genericContravariant() {
        JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) TypeUtils.asParameterized(firstMethodParameter("genericContravariant")).getTypeParameters().get(0);
        assertThat(generic.getName()).isEqualTo("?");
        assertThat(generic.getVariance()).isEqualTo(CONTRAVARIANT);
        assertThat(TypeUtils.asFullyQualified(generic.getBounds().get(0)).getFullyQualifiedName()).
          isEqualTo("org.openrewrite.kotlin.C");
    }

    @Test
    void genericMultipleBounds() {
        List<JavaType> typeParameters = goatType.getTypeParameters();
        JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) typeParameters.get(typeParameters.size() - 1);
        assertThat(generic.getName()).isEqualTo("S");
        assertThat(generic.getVariance()).isEqualTo(COVARIANT);
        assertThat(TypeUtils.asFullyQualified(generic.getBounds().get(0)).getFullyQualifiedName()).isEqualTo("org.openrewrite.kotlin.PT");
        assertThat(TypeUtils.asFullyQualified(generic.getBounds().get(1)).getFullyQualifiedName()).
          isEqualTo("org.openrewrite.kotlin.C");
    }

    @Test
    void genericUnbounded() {
        JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) TypeUtils.asParameterized(firstMethodParameter("genericUnbounded")).getTypeParameters().get(0);
        assertThat(generic.getName()).isEqualTo("U");
        assertThat(generic.getVariance()).isEqualTo(INVARIANT);
        assertThat(generic.getBounds()).isEmpty();
    }

    @Test
    void genericRecursive() {
        JavaType.Parameterized param = (JavaType.Parameterized) firstMethodParameter("genericRecursive");
        JavaType typeParam = param.getTypeParameters().get(0);
        JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) typeParam;
        assertThat(generic.getName()).isEqualTo("?");
        assertThat(generic.getVariance()).isEqualTo(COVARIANT);
        assertThat(TypeUtils.asParameterized(generic.getBounds().get(0))).isNotNull();

        JavaType.GenericTypeVariable elemType = (JavaType.GenericTypeVariable) TypeUtils.asParameterized(generic.getBounds().get(0)).getTypeParameters().get(0);
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
        assertThat(clazz.getTypeParameters().get(0).toString()).isEqualTo("Generic{T}");
        assertThat(clazz.getTypeParameters().get(1).toString()).isEqualTo("Generic{U extends org.openrewrite.kotlin.PT<Generic{U}> & org.openrewrite.kotlin.C}");
        assertThat(clazz.toString()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat$InheritedKotlinTypeGoat<Generic{T}, Generic{U extends org.openrewrite.kotlin.PT<Generic{U}> & org.openrewrite.kotlin.C}>");
    }

    @Test
    void genericIntersectionType() {
        JavaType.GenericTypeVariable clazz = (JavaType.GenericTypeVariable) firstMethodParameter("genericIntersection");
        assertThat(clazz.getBounds().get(0).toString()).isEqualTo("org.openrewrite.kotlin.KotlinTypeGoat$TypeA");
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
        assertThat(goat.getAnnotations().get(0).getClassName()).isEqualTo("AnnotationWithRuntimeRetention");

        JavaType.Method clazzMethod = methodType("clazz");
        assertThat(clazzMethod.getAnnotations()).hasSize(1);
        assertThat(clazzMethod.getAnnotations().get(0).getClassName()).isEqualTo("AnnotationWithRuntimeRetention");
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
        assertThat(c.getMethods().size()).isEqualTo(12);

        // Assert generic type parameters have the correct type bounds.
        JavaType.Method method = c.getMethods().stream().filter(it -> "getClass".equals(it.getName())).findFirst().orElse(null);
        assertThat(method).isNotNull();
        assertThat(method.toString()).isEqualTo("java.lang.Object{name=getClass,return=java.lang.Class<Generic{?}>,parameters=[]}");

        JavaType.Parameterized returnType = (JavaType.Parameterized) method.getReturnType();
        // Assert the type of the parameterized type contains the type parameter from the source.
        assertThat(returnType.getType().getTypeParameters().get(0).toString()).isEqualTo("Generic{T}");
    }

    @Nested
    class ParsingTest implements RewriteTest {

        @Test
        @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/303")
        void coneTypeProjection() {
            rewriteRun(
              kotlin(
                """
                  val newList = buildList {
                      addAll(listOf(""))
                  }
                  """, spec -> spec.afterRecipe(cu -> {
                    MethodMatcher methodMatcher = new MethodMatcher("kotlin.collections.MutableList addAll(..)");
                    AtomicBoolean found = new AtomicBoolean(false);
                    new KotlinIsoVisitor<AtomicBoolean>() {
                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                            if (methodMatcher.matches(method)) {
                                assertThat(method.getMethodType().toString()).isEqualTo("kotlin.collections.MutableList<Generic{E}>{name=addAll,return=kotlin.Boolean,parameters=[kotlin.collections.List<kotlin.String>]}");
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
                  """, spec -> spec.afterRecipe(cu -> {
                    AtomicBoolean found = new AtomicBoolean(false);
                    new KotlinIsoVisitor<AtomicBoolean>() {
                        @Override
                        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, AtomicBoolean found) {
                            if ("Foo".equals(classDecl.getSimpleName())) {
                                assertThat(classDecl.getImplements().get(0).getType().toString()).isEqualTo("java.lang.invoke.TypeDescriptor$OfField<Foo<kotlin.Any>>");
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
                  """, spec -> spec.afterRecipe(cu -> {
                    MethodMatcher methodMatcher = new MethodMatcher("kotlin.collections.CollectionsKt listOf(..)");
                    AtomicBoolean found = new AtomicBoolean(false);
                    new KotlinIsoVisitor<AtomicBoolean>() {
                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                            if (methodMatcher.matches(method)) {
                                assertThat(method.getMethodType().toString())
                                  .isEqualTo("kotlin.collections.CollectionsKt{name=listOf,return=kotlin.collections.List<kotlin.Pair<kotlin.String, Generic{kotlin.Comparable<Generic{?}> & java.io.Serializable}>>,parameters=[kotlin.Array<Generic{? extends kotlin.Pair<kotlin.String, Generic{kotlin.Comparable<Generic{?}> & java.io.Serializable}>}>]}");
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
                  """, spec -> spec.afterRecipe(cu -> {
                    AtomicBoolean found = new AtomicBoolean(false);
                    new KotlinIsoVisitor<AtomicBoolean>() {
                        MethodMatcher matcher = new MethodMatcher("kotlin.Function1 invoke(..)");
                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean atomicBoolean) {
                            if (matcher.matches(method)) {
                                assertThat(method.getMethodType().toString()).isEqualTo("kotlin.Function1<kotlin.collections.Collection<kotlin.Any>, kotlin.Unit>{name=invoke,return=kotlin.Unit,parameters=[kotlin.collections.List<kotlin.String>]}");
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
                  """, spec -> spec.afterRecipe(cu -> {
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

        @SuppressWarnings({"KotlinConstantConditions", "UnusedUnaryOperator", "RedundantExplicitType"})
        @Test
        void whenExpression() {
            //noinspection RemoveRedundantQualifierName
            rewriteRun(
              kotlin(
                """
                  fun method() {
                      val condition: Int = 11
                      when {
                          condition < 20   ->    'c'
                          condition < 10   ->    -1
                          condition > 10   ->    (true)
                          else             ->    0.9
                      }
                  }
                  """, spec -> spec.afterRecipe(cu -> {
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

        @ParameterizedTest
        @CsvSource(value = {
          "n++~kotlin.Int",
          "--n~kotlin.Int",
          "n += a~kotlin.Int",
          "n = a + b~kotlin.Int{name=plus,return=kotlin.Int,parameters=[kotlin.Int]}"
        }, delimiter = '~')
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
                            JavaType.Method mt = (JavaType.Method) binary.getType();
                            if (p2.equals(mt.toString())) {
                                found.set(true);
                            }
                            return super.visitBinary(binary, b);
                        }
                    }.visit(cu, found);
                    assertThat(found.get()).isEqualTo(true);
                })
              )
            );
        }

        @Test
        void operatorOverload() {
            rewriteRun(
              kotlin(
                """
                  class Foo {
                      operator fun contains(element: Int): Boolean {
                          return true
                      }
                      val b = 1 !in listOf(2)
                      val a = 1 !in Foo()
                  }
                  """, spec -> spec.afterRecipe(cu -> {
                    MethodMatcher kotlinCollection = new MethodMatcher("kotlin.collections.List contains(..)");
                    AtomicBoolean kotlinCollectionFound = new AtomicBoolean(false);
                    MethodMatcher operatorOverload = new MethodMatcher("Foo contains(..)");
                    AtomicBoolean operatorOverloadFound = new AtomicBoolean(false);
                    new KotlinIsoVisitor<Integer>() {
                        @Override
                        public K.Binary visitBinary(K.Binary binary, Integer integer) {
                            JavaType.Method methodType = (JavaType.Method) binary.getType();
                            if (kotlinCollection.matches(methodType)) {
                                assertThat(methodType.toString())
                                  .isEqualTo("kotlin.collections.List<kotlin.Int>{name=contains,return=kotlin.Boolean,parameters=[kotlin.Int]}");
                                kotlinCollectionFound.set(true);
                            }
                            if (operatorOverload.matches(methodType)) {
                                assertThat(methodType.toString())
                                  .isEqualTo("Foo{name=contains,return=kotlin.Boolean,parameters=[kotlin.Int]}");
                                operatorOverloadFound.set(true);
                            }
                            return binary;
                        }
                    }.visit(cu, 0);
                    assertThat(kotlinCollectionFound.get()).isTrue();
                    assertThat(operatorOverloadFound.get()).isTrue();
                })
              )
            );
        }

        @Test
        void destructs() {
            rewriteRun(
              kotlin(
                """
                  fun foo() {
                      val ( a , b , c ) = Triple ( 1 , 2 , 3 )
                  }
                  """, spec -> spec.afterRecipe(cu -> {
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
                                assertThat(newClass.getClazz().getType().toString()).isEqualTo("kotlin.Triple<kotlin.Int, kotlin.Int, kotlin.Int>");
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
                                    assertThat(variable.getInitializer()).isInstanceOf(J.MethodInvocation.class);
                                    assertThat(((J.MethodInvocation) variable.getInitializer()).getMethodType().toString())
                                      .isEqualTo("kotlin.Triple<kotlin.Int, kotlin.Int, kotlin.Int>{name=component1,return=kotlin.Int,parameters=[]}");
                                }
                                case "b" -> {
                                    assertThat(variable.getVariableType().toString())
                                      .isEqualTo("openRewriteFile0Kt{name=foo,return=kotlin.Unit,parameters=[]}{name=b,type=kotlin.Int}");
                                    assertThat(variable.getInitializer()).isInstanceOf(J.MethodInvocation.class);
                                    assertThat(((J.MethodInvocation) variable.getInitializer()).getMethodType().toString())
                                      .isEqualTo("kotlin.Triple<kotlin.Int, kotlin.Int, kotlin.Int>{name=component2,return=kotlin.Int,parameters=[]}");
                                }
                                case "c" -> {
                                    assertThat(variable.getVariableType().toString())
                                      .isEqualTo("openRewriteFile0Kt{name=foo,return=kotlin.Unit,parameters=[]}{name=c,type=kotlin.Int}");
                                    assertThat(variable.getInitializer()).isInstanceOf(J.MethodInvocation.class);
                                    assertThat(((J.MethodInvocation) variable.getInitializer()).getMethodType().toString())
                                      .isEqualTo("kotlin.Triple<kotlin.Int, kotlin.Int, kotlin.Int>{name=component3,return=kotlin.Int,parameters=[]}");
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

        @Test
        void variableTypes() {
            rewriteRun(
              kotlin(
                """
                  val foo1: Int = 42
                  class Foo(val foo2: Int) {
                      val foo3: Int = 42
                      fun m(foo4: Int) {
                          val use: Int = foo4
                      }
                  }
                  """, spec -> spec.afterRecipe(cu -> {
                    new KotlinIsoVisitor<Integer>() {
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
                    }.visit(cu, 0);
                })
              )
            );
        }

        @Test
        void nullJavaClassifierType() {
            rewriteRun(
              spec -> spec.parser(KotlinParser.builder().classpath("javapoet","compile-testing")),
              kotlin(
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
    }
}
