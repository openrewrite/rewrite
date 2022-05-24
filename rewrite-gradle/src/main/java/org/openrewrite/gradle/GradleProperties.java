/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

class GradleProperties {
    private GradleProperties() {}

    static final String DEFAULT_VERSION = "7.4.2";
    static final String DEFAULT_DISTRIBUTION = "bin";
    static final Path WRAPPER_JAR_LOCATION = Paths.get("gradle/wrapper/gradle-wrapper.jar");
    static final Path WRAPPER_PROPERTIES_LOCATION = Paths.get("gradle/wrapper/gradle-wrapper.properties");
    static final Path WRAPPER_SCRIPT_LOCATION = Paths.get("gradlew");
    static final Path WRAPPER_BATCH_LOCATION = Paths.get("gradlew.bat");

    static final Pattern VERSION_EXTRACTING_PATTERN = Pattern.compile("/gradle-([-a-zA-Z\\d.+]+)-[a-zA-Z]+.zip");
}
