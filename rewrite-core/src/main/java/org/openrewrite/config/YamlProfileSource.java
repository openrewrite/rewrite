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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class YamlProfileSource implements ProfileSource {
    private static final Logger logger = LoggerFactory.getLogger(YamlProfileSource.class);
    private final Map<String, Profile> profiles = new HashMap<>();

    public YamlProfileSource(InputStream yaml) {
        try (InputStream i = yaml) {
            Constructor constructor = new Constructor(Profile.class);
            TypeDescription profileDesc = new TypeDescription(Profile.class);
            profileDesc.substituteProperty("profile", String.class,
                    "getName", "setName");
            constructor.addTypeDescription(profileDesc);

            for (Object profileObj : new Yaml(constructor).loadAll(i)) {
                Profile profile = (Profile) profileObj;
                profiles.compute(profile.getName(), (name, existing) -> profile.merge(existing));
            }
        } catch (IOException e) {
            logger.warn("Unable to load YAML profile source", e);
        }
    }

    @Override
    public Collection<Profile> load() {
        return profiles.values();
    }
}
