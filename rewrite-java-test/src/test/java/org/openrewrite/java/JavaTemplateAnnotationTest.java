/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;


/**
 * These JavaTemplate tests are specific to the annotation matching syntax.
 */
class JavaTemplateAnnotationTest implements RewriteTest {


    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceAnnotation(
                "javax.annotation.Nullable",
                "org.checkerframework.checker.nullness.qual.Nullable",
                null));
    }

    @Test
    void enumParsing() {
        rewriteRun(java(
                        """
                                import com.google.common.collect.ImmutableMap;
                                import javax.annotation.Nullable;

                                public enum NullableRecipeValidationEnum {
                                    INVALID("invalid"),
                                    CLICKED("clicked");

                                    private static final ImmutableMap<String, NullableRecipeValidationEnum> VALUE_TO_ACTION_TYPE;
                                    static {
                                        final ImmutableMap.Builder<String, NullableRecipeValidationEnum> builder = new ImmutableMap.Builder<>();
                                        for (final NullableRecipeValidationEnum enumTest : values()) {
                                            builder.put(enumTest.value, enumTest);
                                        }
                                        VALUE_TO_ACTION_TYPE = builder.build();
                                    }


                                    private final String value;

                                    NullableRecipeValidationEnum(@Nullable final String value){
                                        this.value = value;
                                    }

                                }
                                """,
                        """
                                import com.google.common.collect.ImmutableMap;
                                import org.checkerframework.checker.nullness.qual.Nullable;

                                public enum NullableRecipeValidationEnum {
                                    INVALID("invalid"),
                                    CLICKED("clicked");

                                    private static final ImmutableMap<String, NullableRecipeValidationEnum> VALUE_TO_ACTION_TYPE;
                                    static {
                                        final ImmutableMap.Builder<String, NullableRecipeValidationEnum> builder = new ImmutableMap.Builder<>();
                                        for (final NullableRecipeValidationEnum enumTest : values()) {
                                            builder.put(enumTest.value, enumTest);
                                        }
                                        VALUE_TO_ACTION_TYPE = builder.build();
                                    }


                                    private final String value;

                                    NullableRecipeValidationEnum(@Nullable final String value){
                                        this.value = value;
                                    }

                                }
                                """)
        );
    }

    @Test
    void functionParsing() {
        rewriteRun(
                java(
                        """
                                import com.google.common.base.Function;

                                import javax.annotation.Nullable;

                                public class AttachmentMetadata {
                                    public final String filename;
                                    public final String extension;
                                    public final String contentType;
                                    public final long size;

                                    public AttachmentMetadata(@Nullable final String filename,
                                                           final String contentType,
                                                           final long size) {
                                        this.filename = filename;
                                        final int dot = (filename == null) ? -1 : filename.lastIndexOf('.');
                                        this.extension = (dot == -1) ? null : filename.substring(dot + 1).toLowerCase();
                                        this.contentType = contentType;
                                        this.size = size;
                                    }

                                    public static final Function<AttachmentMetadata, String> ATTACHMENT_METADATA_TO_FILENAME_EXTENSION = new Function<AttachmentMetadata, String>() {
                                        @Nullable
                                        @Override
                                        public String apply(@Nullable final AttachmentMetadata attachment) {
                                            return (attachment == null) ? null : attachment.extension;
                                        }
                                    };

                                }
                                """,
                        """
                                import com.google.common.base.Function;

                                import org.checkerframework.checker.nullness.qual.Nullable;

                                public class AttachmentMetadata {
                                    public final String filename;
                                    public final String extension;
                                    public final String contentType;
                                    public final long size;

                                    public AttachmentMetadata(@Nullable final String filename,
                                                           final String contentType,
                                                           final long size) {
                                        this.filename = filename;
                                        final int dot = (filename == null) ? -1 : filename.lastIndexOf('.');
                                        this.extension = (dot == -1) ? null : filename.substring(dot + 1).toLowerCase();
                                        this.contentType = contentType;
                                        this.size = size;
                                    }

                                    public static final Function<AttachmentMetadata, String> ATTACHMENT_METADATA_TO_FILENAME_EXTENSION = new Function<AttachmentMetadata, String>() {
                                        @Nullable
                                        @Override
                                        public String apply(@Nullable final AttachmentMetadata attachment) {
                                            return (attachment == null) ? null : attachment.extension;
                                        }
                                    };
                                }
                                """));
    }

}

