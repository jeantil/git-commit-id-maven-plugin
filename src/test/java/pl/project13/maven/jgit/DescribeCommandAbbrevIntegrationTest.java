/*
 * This file is part of git-commit-id-maven-plugin
 * Originally invented by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
 *
 * git-commit-id-maven-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * git-commit-id-maven-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with git-commit-id-maven-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.project13.maven.jgit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import javax.annotation.Nonnull;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;
import pl.project13.core.jgit.DescribeCommand;
import pl.project13.core.jgit.DescribeResult;
import pl.project13.log.DummyTestLoggerBridge;
import pl.project13.maven.git.AvailableGitTestRepo;
import pl.project13.maven.git.GitIntegrationTest;

/**
 * Testcases to verify that the {@link DescribeCommand} works properly.
 */
public class DescribeCommandAbbrevIntegrationTest extends GitIntegrationTest {

  static final String PROJECT_NAME = "my-jar-project";

  @Override
  protected Optional<String> projectDir() {
    return Optional.of(PROJECT_NAME);
  }

  /**
   * Test for such situation:
   *
   * <pre>
   * master!tag-test$ lg
   *   b6a73ed - (HEAD, master) third addition (8 hours ago) <p>Konrad Malawski</p>
   *   d37a598 - (lightweight-tag) second line (8 hours ago) <p>Konrad Malawski</p>
   *   9597545 - (annotated-tag) initial commit (8 hours ago) <p>Konrad Malawski</p>
   *
   * master!tag-test$ describe --abbrev=1
   *   annotated-tag-2-gb6a7
   *
   * master!tag-test$ describe --abbrev=2
   *   annotated-tag-2-gb6a7
   * </pre>
   *
   * <p>Notice that git will not use less than 4 chars for the abbrev, and in large repositories, it
   * will use the abbrev so long that it's guaranteed to be unique.
   */
  @Test
  public void shouldGiveTheCommitIdAndDirtyMarkerWhenNothingElseCanBeFound() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG)
        .create();

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      // when
      DescribeResult res =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge())
              .abbrev(2) // 2 is enough to be unique in this small repo
              .call();

      // then
      // git will notice this, and fallback to use 4 chars
      String smallestAbbrevGitWillUse = abbrev("b6a73ed747dd8dc98642d731ddbf09824efb9d48", 2);

      assertThat(res.prefixedCommitId()).isEqualTo("g" + smallestAbbrevGitWillUse);
    }
  }

  @Test
  public void onGitCommitIdsRepo_shouldNoticeThat2CharsIsTooLittleToBeUniqueAndUse4CharsInstead()
      throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.GIT_COMMIT_ID)
        .create();

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      // when
      DescribeResult res =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge())
              .abbrev(2) // way too small to be unique in git-commit-id's repo!
              .call();

      // then
      // git will notice this, and fallback to use 4 chars
      String smallestAbbrevGitWillUse = abbrev("7181832b7d9afdeb86c32cf51214abfca63625be", 4);

      assertThat(res.prefixedCommitId()).isEqualTo("g" + smallestAbbrevGitWillUse);
    }
  }

  String abbrev(@Nonnull String id, int n) {
    return id.substring(0, n);
  }
}
