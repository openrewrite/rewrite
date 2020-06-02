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
package org.openrewrite.config;

import org.openrewrite.SourceVisitor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate on any static method on a {@link SourceVisitor} that takes a single argument of type
 * {@link org.eclipse.microprofile.config.Config} and returns an instance of the visitor to
 * enable auto-configuration of the visitor or null if the configuration doesn't suffice to
 * construct the visitor.
 *
 * Different mechanisms may scan for {@link AutoConfigure} enabled visitor builder methods to
 * automatically wire visitors found on the classpath. For example, we could scan for all
 * Java refactoring visitors and apply all auto-configurable visitors to the Java source files
 * in a project.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoConfigure {
}
