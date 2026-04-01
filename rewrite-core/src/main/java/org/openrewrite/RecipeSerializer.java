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
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Getter
public class RecipeSerializer {
    private final ObjectMapper mapper;

    public RecipeSerializer() {
        SmileFactory f = new SmileFactory();
        f.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);

        ObjectMapper m = JsonMapper.builder(f)
                // to be able to construct classes that have @Data and a single field
                // see https://cowtowncoder.medium.com/jackson-2-12-most-wanted-3-5-246624e2d3d0
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
                .build()
                .registerModules(new ParameterNamesModule(), new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        maybeAddKotlinModule(m);

        this.mapper = m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
                .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
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
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException  | InstantiationException | IllegalAccessException e) {
            // KotlinModule is optional
        }
    }
}
