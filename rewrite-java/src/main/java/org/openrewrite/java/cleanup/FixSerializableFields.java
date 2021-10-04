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
package org.openrewrite.java.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Value
@EqualsAndHashCode(callSuper = true)
public class FixSerializableFields extends Recipe {

    private static final JavaType.Class SERIALIZABLE_FQ =  JavaType.Class.build("java.io.Serializable");
    private static final JavaType.Class COLLECTION_FQ =  JavaType.Class.build("java.util.Collection");
    private static final JavaType.Class MAP_FQ =  JavaType.Class.build("java.util.Map");
    private static final SerializedMarker SERIALIZED_MARKER = new SerializedMarker(Tree.randomId());

    @Option(displayName = "Mark fields as transient",
            description = "Mark any fields that are not serializable as transient")
    Boolean markAllAsTransient;

    @Option(displayName = "Fully-qualified exclusions",
            description = "A list of fully-qualified names that should always be marked as transient vs being made `Serializable`",
            example = "org.example.BeanFactory",
            required = false)
    @Nullable
    List<String> fullyQualifiedExclusions;

    @Override
    public String getDisplayName() {
        return "Fields in a `Serializable` class should either be transient or serializable";
    }

    @Override
    public String getDescription() {
        return "The fields of a class that implements `Serializable` must also implement `Serializable` or be marked as `transient`.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1948");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(30);
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        Set<String> serializableTargets = new HashSet<>();

        if (markAllAsTransient == null || !markAllAsTransient) {
            //Search all serializable classes for fields that are not serializable and collect those FQNs.
            Set<String> serializableCandidates = new HashSet<>();
            FindSerializableCandidatesVisitor candidateSearchVisitor = new FindSerializableCandidatesVisitor();
            for (SourceFile s : before) {
                candidateSearchVisitor.visit(s, serializableCandidates);
            }

            //Iterate over the source set again looking for any class declarations that match one of the candidates.
            //If there is a class declaration that matches the candidate, that declaration will be modified to add
            //the Serializable interface.
            if (!serializableCandidates.isEmpty()) {
                JavaIsoVisitor<Set<String>> findSerializableTargets = new JavaIsoVisitor<Set<String>>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<String> targets) {

                        String fqn = classDecl.getType() == null ? "" : classDecl.getType().getFullyQualifiedName();
                        if ((fullyQualifiedExclusions == null || !fullyQualifiedExclusions.contains(fqn))
                            &&  serializableCandidates.contains(fqn)) {
                            targets.add(fqn);
                        }
                        return super.visitClassDeclaration(classDecl, targets);
                    }
                };
                for (SourceFile s : before) {
                    findSerializableTargets.visit(s, serializableTargets);
                }
            }
        }

        //Now iterate over the source set again, adding serializable to targets and marking anything else as transient
        //Each source file that is modified is marked to make sure we do not repeat unnecessary work.
        FixSerializableClassVisitor fixSerializableClassVisitor = new FixSerializableClassVisitor(serializableTargets);
        return ListUtils.map(before, s -> {
                    if (!(s instanceof J) && s.getMarkers().findFirst(SerializedMarker.class).isPresent()) {
                        return s;
                    }
                    return (SourceFile) fixSerializableClassVisitor.visit(s, ctx);
                }
        );
    }

    private static class FindSerializableCandidatesVisitor extends JavaVisitor<Set<String>> {
        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, Set<String> candidates) {
            J.ClassDeclaration c = visitAndCast(classDecl, candidates, super::visitClassDeclaration);
            if (implementsSerializable(c.getType())) {
                //If a class implements serializable, look for any fields that are not serializable.
                for (Statement s : classDecl.getBody().getStatements()) {
                    if (s instanceof J.VariableDeclarations) {
                        J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) s;
                        if (!variableDeclarations.hasModifier(J.Modifier.Type.Transient)
                                && !variableDeclarations.hasModifier(J.Modifier.Type.Static)) {

                            JavaType variableType = variableDeclarations.getType();
                            if (variableDeclarations.getTypeExpression() instanceof J.ParameterizedType
                                    && !variableDeclarations.getVariables().isEmpty()) {
                                variableType = variableDeclarations.getVariables().get(0).getType();
                            }
                            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(variableType);
                            String typeName = fq == null ? "" : fq.getFullyQualifiedName();
                            //Record each non-serializable type in the candidates via the notSerializableAction
                            implementsSerializable(variableType, candidates::add);
                        }
                    }
                }
            }
            return c;
        }
    }

    private static class FixSerializableClassVisitor extends JavaVisitor<ExecutionContext> {

        private Set<String> targets;

        private FixSerializableClassVisitor(Set<String> targets) {
            this.targets = targets;
        }

        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = visitAndCast(classDecl, ctx, super::visitClassDeclaration);
            JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(c.getType());
            boolean isClassSerializable = implementsSerializable(c.getType());
            if (!isClassSerializable && fullyQualified != null && targets.contains(fullyQualified.getFullyQualifiedName())) {
                    //If the class is one of the serializable targets, and it does not already implement Serializable, add it.
                    maybeAddImport("java.io.Serializable");
                    return c.withTemplate(
                            JavaTemplate.builder(this::getCursor, "Serializable").imports("java.io.Serializable").build(),
                            c.getCoordinates().addImplementsClause()
                    );
            }  else if (isClassSerializable) {
                //If the class implements serializable, mark any fields that are not serializable as transient.
                J.ClassDeclaration after = c.withBody(c.getBody().withStatements(
                        ListUtils.map(classDecl.getBody().getStatements(), s -> {
                            if (s instanceof J.VariableDeclarations) {
                                J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) s;
                                if (!variableDeclarations.hasModifier(J.Modifier.Type.Transient)
                                        && !variableDeclarations.hasModifier(J.Modifier.Type.Static)) {

                                    JavaType variableType = variableDeclarations.getType();
                                    if (variableDeclarations.getTypeExpression() instanceof J.ParameterizedType && !variableDeclarations.getVariables().isEmpty()) {
                                        variableType = variableDeclarations.getVariables().get(0).getType();
                                    }
                                    AtomicBoolean markAsTransient = new AtomicBoolean(true);
                                    if (!implementsSerializable(variableType, fqn -> markAsTransient.set(markAsTransient.get() && !targets.contains(fqn)))) {
                                        if (markAsTransient.get()) {
                                            return autoFormat(variableDeclarations.withModifiers(ModifierOrder.sortModifiers(
                                                    ListUtils.concat(variableDeclarations.getModifiers(),
                                                            new J.Modifier(
                                                                    Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                                                                    J.Modifier.Type.Transient, Collections.emptyList()
                                                            )
                                                    ))
                                            ), ctx).withPrefix(variableDeclarations.getPrefix());
                                        }
                                    }
                                }
                            }
                            return s;
                        })
                ));
                if (after != c) {
                    return after.withMarkers(after.getMarkers().addIfAbsent(SERIALIZED_MARKER));
                }
            }
            return c;
        }
    }

    @Value
    private static class SerializedMarker implements Marker {
        UUID id;
    }
    private static boolean implementsSerializable(@Nullable JavaType type) {
        return implementsSerializable(type, null);
    }

    /**
     * The logic for checking if a type is serializable is used in multiple contexts within this recipe. In the case
     * of a parameterized types, this method will recurse into those types when the Parameterized type is assignable
     * to a Collection or Map.
     *
     * @param type The type that will be checked if it implements serializable.
     * @param notSerializableAction An optional callback that for each FQN that is determined to be not serializable.
     * @return true if the class implement serializable.
     */
    private static boolean implementsSerializable(@Nullable JavaType type, @Nullable Consumer<String> notSerializableAction) {
        if (type == null) {
            return false;
        } else if (type instanceof JavaType.Primitive) {
            return true;
        } else if (type instanceof JavaType.Array) {
            return implementsSerializable(((JavaType.Array) type).getElemType(), notSerializableAction);
        } else if (type instanceof JavaType.Parameterized) {
            JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
            if (COLLECTION_FQ.isAssignableFrom(parameterized) || MAP_FQ.isAssignableFrom(parameterized)) {
                //If the type is either a collection or a map, make sure the type parameters are serializable. We
                //force all type parameters to be checked to correctly scoop up all non-serializable candidates.
                boolean typeParametersSerializable = true;
                for (JavaType typeParameter : parameterized.getTypeParameters()) {
                    typeParametersSerializable = typeParametersSerializable && implementsSerializable(typeParameter, notSerializableAction);
                }
                return typeParametersSerializable;
            }
            //All other parameterized types fall through
        }

        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
        boolean serializable = SERIALIZABLE_FQ.isAssignableFrom(TypeUtils.asFullyQualified(type));
        if (fq != null && notSerializableAction != null && !serializable) {
            notSerializableAction.accept(fq.getFullyQualifiedName());
        }
        return serializable;
    }
}
