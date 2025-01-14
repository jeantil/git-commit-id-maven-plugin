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

package pl.project13.core.jgit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;
import pl.project13.maven.git.AvailableGitTestRepo;
import pl.project13.maven.git.GitIntegrationTest;

/**
 * Testcases to verify that the {@link DescribeResult} works properly.
 */
public class DescribeResultTest extends GitIntegrationTest {

  static final String PROJECT_NAME = "my-jar-project";

  static final String VERSION = "v2.5";
  static final String DEFAULT_ABBREV_COMMIT_ID = "b6a73ed";
  static final String FULL_HEAD_COMMIT_ID = "b6a73ed747dd8dc98642d731ddbf09824efb9d48";
  public static final ObjectId HEAD_OBJECT_ID = ObjectId.fromString(FULL_HEAD_COMMIT_ID);
  static final String G_DEFAULT_ABBREV_COMMIT_ID = "g" + DEFAULT_ABBREV_COMMIT_ID;
  static final String DIRTY_MARKER = "-DEV";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG)
        .create();
  }

  @Override
  protected Optional<String> projectDir() {
    return Optional.of(PROJECT_NAME);
  }

  @Test
  public void shouldToStringForTag() throws Exception {
    // given
    try (final Git git = git()) {
      git.reset().setMode(ResetCommand.ResetType.HARD).call();

      DescribeResult res = new DescribeResult(VERSION);

      // when
      String s = res.toString();

      // then
      assertThat(s).isEqualTo(VERSION);
    }
  }

  @Test
  public void shouldToStringForDirtyTag() throws Exception {
    // given
    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      git.reset().setMode(ResetCommand.ResetType.HARD).call();

      DescribeResult res =
          new DescribeResult(
              repo.newObjectReader(), VERSION, 2, HEAD_OBJECT_ID, true, DIRTY_MARKER);

      // when
      String s = res.toString();

      // then
      assertThat(s).isEqualTo(VERSION + "-" + 2 + "-" + G_DEFAULT_ABBREV_COMMIT_ID + DIRTY_MARKER);
    }
  }

  @Test
  public void shouldToStringForDirtyTagAnd10Abbrev() throws Exception {
    // given
    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      git.reset().setMode(ResetCommand.ResetType.HARD).call();

      DescribeResult res =
          new DescribeResult(repo.newObjectReader(), VERSION, 2, HEAD_OBJECT_ID, true, DIRTY_MARKER)
              .withCommitIdAbbrev(10);

      String expectedHash = "gb6a73ed747";

      // when
      String s = res.toString();

      // then
      assertThat(s).isEqualTo(VERSION + "-" + 2 + "-" + expectedHash + DIRTY_MARKER);
    }
  }

  @Test
  public void shouldToStringFor2CommitsAwayFromTag() throws Exception {
    // given
    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      git.reset().setMode(ResetCommand.ResetType.HARD).call();

      DescribeResult res = new DescribeResult(repo.newObjectReader(), VERSION, 2, HEAD_OBJECT_ID);

      // when
      String s = res.toString();

      // then
      assertThat(s).isEqualTo(VERSION + "-" + 2 + "-" + G_DEFAULT_ABBREV_COMMIT_ID);
    }
  }

  @Test
  public void shouldToStringForNoTagJustACommit() throws Exception {
    // given
    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      git.reset().setMode(ResetCommand.ResetType.HARD).call();

      DescribeResult res = new DescribeResult(repo.newObjectReader(), HEAD_OBJECT_ID);

      // when
      String s = res.toString();

      // then
      assertThat(s).isEqualTo(DEFAULT_ABBREV_COMMIT_ID);
    }
  }
}
