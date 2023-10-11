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
        assertThat(property.getGetter().getMethodType()).isEqualTo(property.getGetter().getName().getType());
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
          .isEqualTo("KotlinTypeGoatKt{name=field,type=kotlin.Int}");
    }

    @Test
    void fileFunction() {
        J.MethodDeclaration md = cu.getStatements().stream()
            .filter(it -> it instanceof J.MethodDeclaration)
              .map(J.MethodDeclaration.class::cast)
                .filter(it -> "function".equals(it.getSimpleName())).findFirst().orElseThrow();

        assertThat(md.getName().getType()).isEqualTo(md.getMethodType());
        assertThat(md.getMethodType().toString())
          .isEqualTo("KotlinTypeGoatKt{name=function,return=kotlin.Unit,parameters=[org.openrewrite.kotlin.C]}");

        J.VariableDeclarations.NamedVariable nv = ((J.VariableDeclarations) md.getParameters().get(0)).getVariables().get(0);
        assertThat(nv.getVariableType()).isEqualTo(nv.getName().getFieldType());
        assertThat(nv.getType().toString()).isEqualTo("org.openrewrite.kotlin.C");
        assertThat(nv.getVariableType().toString())
          .isEqualTo("KotlinTypeGoatKt{name=function,return=kotlin.Unit,parameters=[org.openrewrite.kotlin.C]}{name=arg,type=org.openrewrite.kotlin.C}");
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
                                  .isEqualTo("kotlin.collections.CollectionsKt{name=listOf,return=kotlin.collections.List<kotlin.Pair<kotlin.String, Generic{kotlin.Comparable<Generic{*}> & java.io.Serializable}>>,parameters=[kotlin.Array<Generic{? extends Generic{T}}>]}");
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
                                  .isEqualTo("kotlin.collections.Set<kotlin.collections.Map$Entry<Generic{*}, kotlin.Any>>");
                                assertThat(fieldAccess.getName().getFieldType().toString())
                                  .isEqualTo("kotlin.collections.Map<Generic{*}, kotlin.Any>{name=entries,type=kotlin.collections.Set<kotlin.collections.Map$Entry<Generic{*}, kotlin.Any>>}");
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
                                assertThat(when.getType().toString()).isEqualTo("Generic{kotlin.Comparable<Generic{*}> & java.io.Serializable}");
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
                                assertThat(newClass.getClazz().getType().toString()).isEqualTo("kotlin.Triple<Generic{A}, Generic{B}, Generic{C}>");
                                assertThat(newClass.getConstructorType().toString()).isEqualTo("kotlin.Triple{name=<constructor>,return=kotlin.Triple,parameters=[Generic{A},Generic{B},Generic{C}]}");
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
                                      .isEqualTo("openRewriteFile0Kt{name=a,type=kotlin.Int}");
                                    assertThat(variable.getInitializer()).isInstanceOf(J.MethodInvocation.class);
                                    assertThat(((J.MethodInvocation) variable.getInitializer()).getMethodType().toString())
                                      .isEqualTo("kotlin.Triple<kotlin.Int, kotlin.Int, kotlin.Int>{name=component1,return=kotlin.Int,parameters=[]}");
                                }
                                case "b" -> {
                                    assertThat(variable.getVariableType().toString())
                                      .isEqualTo("openRewriteFile0Kt{name=b,type=kotlin.Int}");
                                    assertThat(variable.getInitializer()).isInstanceOf(J.MethodInvocation.class);
                                    assertThat(((J.MethodInvocation) variable.getInitializer()).getMethodType().toString())
                                      .isEqualTo("kotlin.Triple<kotlin.Int, kotlin.Int, kotlin.Int>{name=component2,return=kotlin.Int,parameters=[]}");
                                }
                                case "c" -> {
                                    assertThat(variable.getVariableType().toString())
                                      .isEqualTo("openRewriteFile0Kt{name=c,type=kotlin.Int}");
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
    }
}
