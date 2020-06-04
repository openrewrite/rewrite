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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import org.openrewrite.internal.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class Environment {
    private static final Logger logger = LoggerFactory.getLogger(Environment.class);
    private final Map<String, Profile> profiles = new HashMap<>();

    public final Environment load(ProfileSource profileSource) {
        for (Profile profile : profileSource.load()) {
            profiles.compute(profile.getName(), (name, existing) -> profile.merge(existing));
        }

        return this;
    }

    public final Environment scanProfiles(String... whitelistResources) {
        try (ScanResult result = new ClassGraph()
                .whitelistPaths(whitelistResources)
                .enableMemoryMapping()
                .scan()) {
            for (Resource resource : result.getAllResources()) {
                if (resource.getPath().endsWith(".yml") || resource.getPath().endsWith(".yaml")) {
                    scanProfileYaml(resource::open);
                }
            }
        }

        return this;
    }

    public final Environment scanProfiles(Path... paths) {
        for (Path path : paths) {
            File file = path.toFile();
            if(file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                scanProfileYaml(() -> new FileInputStream(file));
            }
        }

        return this;
    }

    private void scanProfileYaml(Callable<InputStream> is) {
        try (InputStream yaml = is.call()) {
            load(new YamlProfileSource(yaml));
        } catch (Exception e) {
            logger.warn("Unable to scan profile YML", e);
        }
    }

    @Nullable
    public Profile getProfile(String profileName) {
        return profiles.get(profileName);
    }
}
