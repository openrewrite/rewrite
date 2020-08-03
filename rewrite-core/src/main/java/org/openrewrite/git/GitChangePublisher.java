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
package org.openrewrite.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.openrewrite.Change;
import org.openrewrite.ChangePublisher;
import org.openrewrite.Incubating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

@Incubating(since = "2.0.0")
@RequiredArgsConstructor
public class GitChangePublisher implements ChangePublisher {
    private static final Logger logger = LoggerFactory.getLogger(GitChangePublisher.class);

    private final String user;
    private final String password;
    private final String commitMessage;

    @Override
    public void publish(Collection<Change> changes) {
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(user, password);

        try {
            InMemoryRepository repository = new InMemoryRepository.Builder().build();
            Git git = new Git(repository);

            git.add()
                    .addFilepattern("gradle-enterprise.xml")
                    .call();

            git.commit()
                    .setMessage(commitMessage)
                    .setAuthor("jkschneider", "jkschneider@gmail.com")
                    .call();
        } catch (IOException | GitAPIException e) {
            logger.warn("Unable to commit change", e);
        }
    }

    public void publishChangesForRepository(Collection<Change> changes) {
//        Change change = changes.iterator().next();
//        Optional<GitMetadata> gitMetadata = change.getFixed().getMetadata(GitMetadata.class);

        // git add
        // git write-tree
        // git commit-tree
        // git update-ref refs/heads/master COMMIT_ID
        // git symbolic-ref HEAD refs/heads/master
    }
}
