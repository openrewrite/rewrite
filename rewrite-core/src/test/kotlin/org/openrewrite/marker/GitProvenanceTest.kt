/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.marker

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ConfigConstants.CONFIG_BRANCH_SECTION
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.transport.TagOpt
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.FS
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.openrewrite.Tree.randomId
import org.openrewrite.marker.ci.JenkinsBuildEnvironment
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.writeText

class GitProvenanceTest {
    companion object {
        @JvmStatic
        fun remotes() = arrayOf(
            "ssh://git@github.com/openrewrite/rewrite.git",
            "https://github.com/openrewrite/rewrite.git",
            "file:///openrewrite/rewrite.git",
            "http://localhost:7990/scm/openrewrite/rewrite.git",
            "git@github.com:openrewrite/rewrite.git"
        )
    }

    @ParameterizedTest
    @MethodSource("remotes")
    fun getOrganizationName(remote: String) {
        assertThat(GitProvenance(randomId(), remote, "main", "123", null, null).organizationName)
            .isEqualTo("openrewrite")
    }

    @ParameterizedTest
    @MethodSource("remotes")
    fun getRepositoryName(remote: String) {
        assertThat(GitProvenance(randomId(), remote, "main", "123", null, null).repositoryName)
            .isEqualTo("rewrite")
    }

    @Test
    fun localBranchPresent(@TempDir projectDir: Path) {
        Git.init().setDirectory(projectDir.toFile()).setInitialBranch("main").call().use {
            assertThat(GitProvenance.fromProjectDirectory(projectDir, null)!!.branch)
                .isEqualTo("main")
        }
    }

    @Test
    fun detachedHead(@TempDir projectDir: Path) {
        initGitWithOneCommit(projectDir).use { git ->
            git.checkout().setName(git.repository.resolve(Constants.HEAD).name).call()
            assertThat(GitProvenance.fromProjectDirectory(projectDir, null)!!.branch)
                .isEqualTo("main")
        }
    }

    @Test
    fun detachedHeadJenkinsLocalBranch(@TempDir projectDir: Path) {
        initGitWithOneCommit(projectDir).use { git ->
            git.checkout().setName(git.repository.resolve(Constants.HEAD).name).call()
            assertThat(
                GitProvenance.fromProjectDirectory(
                    projectDir,
                    JenkinsBuildEnvironment(
                        randomId(), "1", "1", "https://jenkins/job/1",
                        "https://jenkins", "job", "main", "origin/main"
                    )
                )!!.branch
            ).isEqualTo("main")
        }
    }

    @Test
    fun detachedHeadJenkinsNoLocalBranch(@TempDir projectDir: Path) {
        initGitWithOneCommit(projectDir).use { git ->
            git.checkout().setName(git.repository.resolve(Constants.HEAD).name).call()

            assertThat(
                GitProvenance.fromProjectDirectory(
                    projectDir,
                    JenkinsBuildEnvironment(randomId(), "1", "1", "https://jenkins/job/1",
                        "https://jenkins", "job", null, "origin/main")
                )!!.branch
            ).isEqualTo("main")
        }

    }

    private fun initGitWithOneCommit(projectDir: Path): Git {
        val git = Git.init().setDirectory(projectDir.toFile()).setInitialBranch("main").call()
        projectDir.resolve("test.txt").writeText("hi")
        git.add().addFilepattern("*").call()
        git.commit().setMessage("init").setSign(false).call()
        git.remoteAdd().setName("origin").setUri(URIish("git@github.com:openrewrite/doesnotexist.git")).call()

        // origins are still present in .git/config on Jenkins
        val config = git.repository.config
        config.setString(CONFIG_BRANCH_SECTION, "main", "remote", "origin")
        config.setString(CONFIG_BRANCH_SECTION, "main", "merge", "refs/heads/main")
        config.save()

        return git
    }

    @Test
    fun detachedHeadBehindBranchHead(@TempDir projectDir: Path) {
        Git.init().setDirectory(projectDir.toFile()).setInitialBranch("main").call().use { git ->
            projectDir.resolve("test.txt").writeText("hi")
            git.add().addFilepattern("*").call()
            git.commit().setMessage("init").setSign(false).call()
            val commit1 = git.repository.resolve(Constants.HEAD).name

            projectDir.resolve("test.txt").writeText("hi")
            git.add().addFilepattern("*").call()
            git.commit().setMessage("init").setSign(false).call()

            assertThat(git.repository.resolve(Constants.HEAD).name).isNotEqualTo(commit1)

            git.checkout().setName(commit1).call()

            assertThat(GitProvenance.fromProjectDirectory(projectDir, null)!!.branch)
                .isEqualTo("main")
        }
    }

    @Test
    fun shallowCloneDetachedHead(@TempDir projectDir: Path) {
        val remoteDir = projectDir.resolve("remote")
        val fileKey = RepositoryCache.FileKey.exact(remoteDir.toFile(), FS.DETECTED)
        val remoteRepo = fileKey.open(false)
        remoteRepo.create(true)

        // push an initial commit to the remote
        val cloneDir = projectDir.resolve("clone1")
        Git.cloneRepository().setURI(remoteRepo.directory.absolutePath).setDirectory(cloneDir.toFile()).call().use { git ->
            projectDir.resolve("test.txt").writeText("hi")
            git.add().addFilepattern("*").call()
            git.commit().setMessage("init").setSign(false).call()

            try {
                "git push -u origin main".runCommand(cloneDir)
                val commit = git.repository.resolve(Constants.HEAD).name

                // shallow clone the remote to another directory
                "git clone file:///${remoteRepo.directory.absolutePath} shallowClone --depth 1 --branch main".runCommand(
                    projectDir
                )
                val git2 = Git.open(projectDir.resolve("shallowClone").toFile())

                // creates detached head
                git2.checkout().setName(commit).call()

                assertThat(GitProvenance.fromProjectDirectory(projectDir.resolve("shallowClone"), null)!!.branch)
                    .isEqualTo(null)
            } catch (ignored: Throwable) {
                // can't run git command line
            }
        }
    }

    @Test
    fun noLocalBranchDeriveFromRemote(@TempDir projectDir: Path) {

        val remoteDir = projectDir.resolve("remote")
        val fileKey = RepositoryCache.FileKey.exact(remoteDir.toFile(), FS.DETECTED)
        val remoteRepo = fileKey.open(false)
        remoteRepo.create(true)

        // push an initial commit to the remote
        val cloneDir = projectDir.resolve("clone1")
        val gitSetup = Git.cloneRepository().setURI(remoteRepo.directory.absolutePath).setDirectory(cloneDir.toFile()).call()
        projectDir.resolve("test.txt").writeText("hi")
        gitSetup.add().addFilepattern("*").call()
        val commit = gitSetup.commit().setMessage("init").setSign(false).call()
        gitSetup.push().add("master").setRemote("origin").call()

        //Now create new workspace directory, git init and then fetch from remote.
        val workspaceDir = projectDir.resolve("workspace")
        Git.init().setDirectory(workspaceDir.toFile()).call().use { git ->
            git.remoteAdd().setName("origin").setUri(URIish(remoteRepo.directory.absolutePath)).call()
            git.fetch().setRemote("origin").setForceUpdate(true).setTagOpt(TagOpt.FETCH_TAGS).setRefSpecs("+refs/heads/*:refs/remotes/origin/*").call()
            git.checkout().setName(commit.name).call()
        }

        try {
            assertThat(GitProvenance.fromProjectDirectory(projectDir.resolve("workspace"), null)!!.branch)
                .isEqualTo("master")
        } catch (ignored: Throwable) {
            // can't run git command line
        }
    }

    private fun String.runCommand(workingDir: Path) {
        workingDir.toFile().mkdirs()
        ProcessBuilder(*split(" ").toTypedArray())
            .directory(workingDir.toFile())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor(5, TimeUnit.SECONDS)
    }
}
