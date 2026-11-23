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
package org.openrewrite.java.marker;

import org.openrewrite.marker.Marker;

/**
 * A marker that identifies the project a source file belongs to. Implemented by markers attached
 * at parse time, such as {@link JavaProject} (rewrite-java) and {@code GradleProject}
 * (rewrite-gradle). Lets code that only needs the project name look it up uniformly via
 * {@code getMarkers().findFirst(ProjectIdentity.class)} without coupling to a specific marker
 * type.
 */
public interface ProjectIdentity extends Marker {
    String getProjectName();
}
