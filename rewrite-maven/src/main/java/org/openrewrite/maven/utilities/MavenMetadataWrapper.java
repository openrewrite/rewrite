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
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.semver.VersionComparator;

import lombok.Builder;

/**
 * Wrapper for MavenMetadata to provide additional functionality
 */
@Builder
public class MavenMetadataWrapper {

	/**
	 * MavenMetadata to wrap and deliver the versions to filter
	 */
	@lombok.NonNull
	@NonNull
	private MavenMetadata mavenMetadata;

	/**
	 * VersionComparator comparator to validate the versions
	 */
	@lombok.NonNull
	@NonNull
	private VersionComparator versionComparator;

	/**
	 * Version to use in the validation process
	 */
	@Nullable
	private String version;

	/**
	 * Extra filter to apply to the versions
	 */
	@Nullable
	private Predicate<? super String> extraFilter;

	public List<String> filter() {return internalFilter(versionComparator, version).collect(Collectors.toList());}

	public Optional<String> max() {
		return internalFilter(versionComparator, version).max((v1, v2) -> versionComparator.compare(null, v1, v2));
	}

	@NotNull
	private Stream<String> internalFilter(@NotNull VersionComparator versionComparator, String version) {
		Stream<String> stream = mavenMetadata.getVersioning().getVersions().stream()
				.filter(v -> versionComparator.isValid(version, v));
		return extraFilter == null ? stream : stream.filter(extraFilter);
	}
}
