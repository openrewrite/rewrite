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
package org.openrewrite.maven.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Collection;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
public class MavenRepositoryCredentials {
    String id;
    String username;
    String password;

    public boolean matches(MavenRepository repository) {
        return id.equals(repository.getId());
    }

    public static MavenRepository apply(Collection<MavenRepositoryCredentials> credentials, MavenRepository repo) {
        MavenRepository mapped = repo;
        for (MavenRepositoryCredentials credential : credentials) {
            mapped = credential.apply(mapped);
        }
        return mapped;
    }

    public MavenRepository apply(MavenRepository repo) {
        return matches(repo) ?
                repo.withUsername(username).withPassword(password) :
                repo;
    }
}
