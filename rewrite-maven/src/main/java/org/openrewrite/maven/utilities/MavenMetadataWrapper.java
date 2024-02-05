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
package org.openrewrite.maven.utilities;

import java.util.List;
import java.util.stream.Collectors;

import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.semver.VersionComparator;

import lombok.AllArgsConstructor;

/**
 * Wrapper for MavenMetadata to provide additional functionality
 */
@AllArgsConstructor
public class MavenMetadataWrapper {

	private final MavenMetadata mavenMetadata;

	/**
	 * Filter the versions of a MavenMetadata using a VersionComparator validation
	 * @param versionComparator comparator to validate the versions
	 * @param version version to use in validation process
	 * @return filtered list of versions
	 */
	public List<String> filterWithVersionComparator(VersionComparator versionComparator, String version) {
		return mavenMetadata.getVersioning().getVersions().stream()
				.filter(v -> versionComparator.isValid(version, v))
				.collect(Collectors.toList());

	}
}
