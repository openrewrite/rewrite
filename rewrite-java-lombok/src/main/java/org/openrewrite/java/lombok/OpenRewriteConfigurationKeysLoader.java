/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.lombok;

import lombok.ConfigurationKeys;
import lombok.core.LombokConfiguration;
import lombok.core.configuration.*;

public class OpenRewriteConfigurationKeysLoader implements ConfigurationKeysLoader {
    private static final FileSystemSourceCache cache = new FileSystemSourceCache();

    static {
        final ConfigurationFileToSource fileToSource = cache.fileToSource(new ConfigurationParser(ConfigurationProblemReporter.CONSOLE));
        LombokConfiguration.overrideConfigurationResolverFactory(sourceLocation -> new OpenRewriteConfigurationResolver(cache.forUri(sourceLocation), fileToSource));
    }
}

class OpenRewriteConfigurationResolver extends BubblingConfigurationResolver {
    public OpenRewriteConfigurationResolver(ConfigurationFile start, ConfigurationFileToSource fileMapper) {
        super(start, fileMapper);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T resolve(ConfigurationKey<T> key) {
        if (key == ConfigurationKeys.ADD_LOMBOK_GENERATED_ANNOTATIONS) {
            // ensure the `lombok.Generated` annotation is always added to generated declarations
            return (T) Boolean.TRUE;
        }
        return super.resolve(key);
    }
}
