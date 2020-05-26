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
package org.openrewrite.xml.search.maven;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MavenPom {
    private final List<Dependency> dependencies = new ArrayList<>();
    private final Map<String, String> properties = new HashMap<>();

    public MavenPom merge(MavenPom info) {
        this.dependencies.addAll(info.dependencies);
        this.properties.putAll(info.properties);
        return this;
    }

    public MavenPom withDependency(Dependency dependency) {
        this.dependencies.add(dependency);
        return this;
    }

    public MavenPom withProperty(String key, String value) {
        this.properties.put(key, value);
        return this;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
