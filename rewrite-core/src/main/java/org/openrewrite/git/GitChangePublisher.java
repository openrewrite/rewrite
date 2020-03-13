package org.openrewrite.git;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.openrewrite.Change;
import org.openrewrite.ChangePublisher;
import org.openrewrite.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@RequiredArgsConstructor
public class GitChangePublisher implements ChangePublisher {
    private static final Logger logger = LoggerFactory.getLogger(GitChangePublisher.class);

    private final String user;
    private final String password;
    private final String commitMessage;

    @Override
    public void publish(Collection<Change<?>> changes) {
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

    public void publishChangesForRepository(Collection<Change<?>> changes) {
        Change<?> change = changes.iterator().next();
        Map<Metadata, String> metadata = change.getFixed().getMetadata();

        String remote = metadata.getOrDefault(GitMetadata.REMOTE, null);
        String headCommitId = metadata.getOrDefault(GitMetadata.HEAD_COMMIT_ID, null);
        String headTreeId = metadata.getOrDefault(GitMetadata.HEAD_TREE_ID, null);

        // git add
        // git write-tree
        // git commit-tree
        // git update-ref refs/heads/master COMMIT_ID
        // git symbolic-ref HEAD refs/heads/master
    }
}
