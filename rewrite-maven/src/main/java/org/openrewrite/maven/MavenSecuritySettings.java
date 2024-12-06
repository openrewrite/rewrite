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
package org.openrewrite.maven;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.maven.internal.MavenXmlMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@AllArgsConstructor
@JacksonXmlRootElement(localName = "settingsSecurity")
public class MavenSecuritySettings {
    @Nullable
    String master;

    public static @Nullable MavenSecuritySettings parse(Parser.Input source, ExecutionContext ctx) {
        try {
            return new Interpolator().interpolate(
                    MavenXmlMapper.readMapper().readValue(source.getSource(ctx), MavenSecuritySettings.class));
        } catch (IOException e) {
            ctx.getOnError().accept(new IOException("Failed to parse " + source.getPath(), e));
            return null;
        }
    }

    public static @Nullable MavenSecuritySettings parse(Path settingsPath, ExecutionContext ctx) {
        return parse(new Parser.Input(settingsPath, () -> {
            try {
                return Files.newInputStream(settingsPath);
            } catch (IOException e) {
                ctx.getOnError().accept(new IOException("Failed to read settings-security.xml at " + settingsPath, e));
                return null;
            }
        }), ctx);
    }

    public static @Nullable MavenSecuritySettings readMavenSecuritySettingsFromDisk(ExecutionContext ctx) {
        final Optional<MavenSecuritySettings> userSettings = Optional.of(userSecuritySettingsPath())
                .filter(MavenSecuritySettings::exists)
                .map(path -> parse(path, ctx));
        final MavenSecuritySettings installSettings = findMavenHomeSettings().map(path -> parse(path, ctx)).orElse(null);
        return userSettings.map(mavenSecuritySettings -> mavenSecuritySettings.merge(installSettings))
                .orElse(installSettings);
    }

    private static Path userSecuritySettingsPath() {
        return Paths.get(System.getProperty("user.home")).resolve(".m2/settings-security.xml");
    }

    private static Optional<Path> findMavenHomeSettings() {
        for (String envVariable : Arrays.asList("MVN_HOME", "M2_HOME", "MAVEN_HOME")) {
            for (String s : Optional.ofNullable(System.getenv(envVariable)).map(Arrays::asList).orElse(emptyList())) {
                Path resolve = Paths.get(s).resolve("conf/settings-security.xml");
                if (exists(resolve)) {
                    return Optional.of(resolve);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean exists(Path path) {
        try {
            return path.toFile().exists();
        } catch (SecurityException e) {
            return false;
        }
    }

    public MavenSecuritySettings merge(@Nullable MavenSecuritySettings installSettings) {
        return installSettings == null ? this : new MavenSecuritySettings(
            master == null ? installSettings.master : master
        );
    }

    /**
     * Resolve all properties EXCEPT in the profiles section, which can be affected by
     * the POM using the settings.
     */
    private static class Interpolator {
        private static final PropertyPlaceholderHelper propertyPlaceholders = new PropertyPlaceholderHelper(
                "${", "}", null);

        private static final UnaryOperator<String> propertyResolver = key -> {
            String property = System.getProperty(key);
            if (property != null) {
                return property;
            }
            if (key.startsWith("env.")) {
                return System.getenv().get(key.substring(4));
            }
            return System.getenv().get(key);
        };

        public MavenSecuritySettings interpolate(MavenSecuritySettings mavenSecuritySettings) {
            return new MavenSecuritySettings(
                interpolate(mavenSecuritySettings.master)
            );
        }

        private @Nullable String interpolate(@Nullable String s) {
            return s == null ? null : propertyPlaceholders.replacePlaceholders(s, propertyResolver);
        }
    }
}
