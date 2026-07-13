/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.maven.internal.engine;

import lombok.Value;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Profile;

import java.util.List;
import java.util.Map;

/**
 * The settings/context inputs a {@code ModelBuildingRequest} needs, translated out of rewrite's world by
 * {@link SettingsBridge}. Slice B threads these onto the request ({@code setProfiles}, {@code setActiveProfileIds},
 * {@code setUserProperties}); Maven then owns profile activation (property/jdk/os/file) and interpolation.
 */
@Value
public class EffectiveSettings {

    /** Settings-declared profiles as Maven model profiles (id, activation, repositories); Maven activates them. */
    List<Profile> externalProfiles;

    /** Explicitly-active profile ids: settings {@code <activeProfiles>} plus parser/host-supplied ids. */
    List<String> activeProfiles;

    /** {@code -D}-equivalent user properties (parser {@code .property()} + ctx), Maven's interpolation inputs. */
    Map<String, String> userProperties;
}
