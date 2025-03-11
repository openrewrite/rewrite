/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.RecipesThatMadeChanges;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.*;
import java.util.*;

public class RecipeSerializer {
    private final ObjectMapper mapper;
    private final Map<Recipe, Recipe> cache = new HashMap<>();

    public RecipeSerializer() {
        SmileFactory f = new SmileFactory();
        f.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);

        ObjectMapper m = JsonMapper.builder(f)
                // to be able to construct classes that have @Data and a single field
                // see https://cowtowncoder.medium.com/jackson-2-12-most-wanted-3-5-246624e2d3d0
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
                .build()
                .setAnnotationIntrospector(new NullableAwareAnnotationIntrospector())
                .registerModules(new ParameterNamesModule(), new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        maybeAddKotlinModule(m);

        this.mapper = m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
                .withCreatorVisibility(JsonAutoDetect.Visibility.NON_PRIVATE)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
    }

    public void write(Recipe recipe, OutputStream out) {
        try {
            mapper.writeValue(out, recipe);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] write(Recipe recipe) {
        try {
            return mapper.writeValueAsBytes(recipe);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Recipe read(InputStream input) {
        try {
            return mapper.readValue(input, Recipe.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Recipe read(byte[] bytes) {
        try {
            return mapper.readValue(bytes, Recipe.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * If it is available on the runtime classpath, add jackson's Kotlin module to the provided object mapper.
     * Does nothing if jackson's kotlin module is not available.
     */
    public static void maybeAddKotlinModule(ObjectMapper mapper) {
        try {
            Class<?> kmbClass = RecipeSerializer.class.getClassLoader()
                    .loadClass("com.fasterxml.jackson.module.kotlin.KotlinModule$Builder");
            Constructor<?> kmbConstructor = kmbClass.getConstructor();
            Object kotlinModuleBuilder = kmbConstructor.newInstance();
            Method kmbBuildMethod = kmbClass.getMethod("build");
            Module kotlinModule = (Module) kmbBuildMethod.invoke(kotlinModuleBuilder);
            mapper.registerModule(kotlinModule);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            // KotlinModule is optional
        }
    }

    public void validateSerializability(SourceFile sourceFile) {
        try {
            SourceFile shrinkwrapped = ShrinkwrapRecipe.shrinkwrap(sourceFile, cache);
            mapper.readValue(mapper.writeValueAsBytes(shrinkwrapped), sourceFile.getClass());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Supports JSpecify annotations to determine if a property is required or not.
     */
    static class NullableAwareAnnotationIntrospector extends JacksonAnnotationIntrospector {
        @Override
        public Boolean hasRequiredMarker(AnnotatedMember m) {
            Boolean jsonPropertyRequired = super.hasRequiredMarker(m);
            if (Objects.equals(jsonPropertyRequired, Boolean.TRUE)) {
                return true;
            }

            if (m.hasAnnotation(Nullable.class)) {
                return false;
            } else if (m instanceof AnnotatedField) {
                Field field = (Field) m.getAnnotated();
                if (field.getAnnotatedType().isAnnotationPresent(Nullable.class)) {
                    return false;
                } else if (isNullMarked(m.getDeclaringClass())) {
                    return true;
                }
            } else if (m instanceof AnnotatedParameter) {
                Parameter parameter = getParameter((AnnotatedParameter) m);
                if (parameter != null && parameter.getAnnotatedType().isAnnotationPresent(Nullable.class)) {
                    return false;
                } else if (isNullMarked(m.getDeclaringClass())) {
                    return true;
                }
            }
            return null;
        }

        private static boolean isNullMarked(Class<?> clazz) {
            return !clazz.isAnnotationPresent(NullUnmarked.class) &&
                   (clazz.isAnnotationPresent(NullMarked.class) ||
                    (clazz.getEnclosingClass() != null && isNullMarked(clazz.getEnclosingClass())) ||
                    (clazz.getEnclosingClass() == null && clazz.getPackage().isAnnotationPresent(NullMarked.class)));
        }

        private @Nullable Parameter getParameter(AnnotatedParameter parameter) {
            AnnotatedWithParams owner = parameter.getOwner();
            if (owner instanceof AnnotatedConstructor) {
                return ((AnnotatedConstructor) owner).getAnnotated().getParameters()[parameter.getIndex()];
            }
            if (owner instanceof AnnotatedMethod) {
                return (((AnnotatedMethod) owner).getAnnotated()).getParameters()[parameter.getIndex()];
            }

            return null;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    static class ShrinkwrapRecipe extends Recipe {
        RecipeDescriptor originalDescriptor;

        @Override
        public String getName() {
            return originalDescriptor.getName();
        }

        @Override
        public String getDisplayName() {
            return originalDescriptor.getDisplayName();
        }

        @Override
        public String getDescription() {
            return originalDescriptor.getDescription();
        }

        @Override
        protected RecipeDescriptor createRecipeDescriptor() {
            return originalDescriptor;
        }

        public static SourceFile shrinkwrap(SourceFile sourceFile, Map<Recipe, Recipe> cache) {
            return (SourceFile) new TreeVisitor<Tree, Integer>() {
                @Override
                public @Nullable Tree visit(@Nullable Tree tree, Integer integer) {
                    if (tree instanceof SourceFile) {
                        SourceFile sf = (SourceFile) tree;
                        return sf.withMarkers(visitMarkers(sf.getMarkers(), integer));
                    }
                    return tree;
                }

                @Override
                public <M extends Marker> M visitMarker(Marker marker, Integer p) {
                    if (marker instanceof RecipesThatMadeChanges) {
                        RecipesThatMadeChanges rtmc = (RecipesThatMadeChanges) marker;
                        List<List<Recipe>> lists = shrinkwrapRecipesThatMadeChanges(rtmc.getRecipes(), cache);
                        //noinspection unchecked
                        return (M) rtmc.withRecipes(lists);
                    }

                    // Eliminate any markers that come from recipe JARs (which wouldn't be render-able anyway).
                    if (marker.getClass().getClassLoader() != RecipeScheduler.class.getClassLoader()) {
                        //noinspection DataFlowIssue,ConstantConditions
                        return null;
                    }

                    return super.visitMarker(marker, p);
                }
            }.visitNonNull(sourceFile, 0);
        }

        public static List<List<Recipe>> shrinkwrapRecipesThatMadeChanges(Collection<List<Recipe>> recipesThatMadeChanges, Map<Recipe, Recipe> cache) {
            List<List<Recipe>> shrinkWrapped = new ArrayList<>();
            for (List<Recipe> recipeStack : recipesThatMadeChanges) {
                shrinkWrapped.add(ListUtils.map(recipeStack, r ->
                        cache.computeIfAbsent(r, r2 -> new ShrinkwrapRecipe(r2.getDescriptor()
                                .withRecipeList(Collections.emptyList())))));
            }
            return shrinkWrapped;
        }
    }
}

