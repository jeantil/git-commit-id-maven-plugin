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

package pl.project13.maven.git;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jgit.api.Git;
import org.junit.After;
import org.junit.Before;
import pl.project13.core.CommitIdPropertiesOutputFormat;

/**
 * Base class for various Testcases to verify that the git-commit-id-plugin works properly.
 */
public abstract class GitIntegrationTest {

  private static final String SANDBOX_DIR = "target" + File.separator + "sandbox" + File.separator;
  protected static final String evaluateOnCommit = "HEAD";

  /** Sandbox directory with unique name for current test. */
  private String currSandbox;

  protected GitCommitIdMojo mojo;
  protected FileSystemMavenSandbox mavenSandbox;

  @Before
  public void setUp() throws Exception {
    // generate unique sandbox for this test
    File sandbox;
    do {
      currSandbox =
          SANDBOX_DIR
              + "sandbox"
              + Integer.toString(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));
      sandbox = new File(currSandbox);
    } while (sandbox.exists());

    mavenSandbox = new FileSystemMavenSandbox(currSandbox);
    mojo = spy(GitCommitIdMojo.class);
    initializeMojoWithDefaults(mojo);
  }

  @After
  public void tearDown() throws Exception {
    final boolean keep = mavenSandbox != null && mavenSandbox.isKeepSandboxWhenFinishedTest();

    mojo = null;
    mavenSandbox = null;

    final File sandbox = new File(currSandbox);
    try {
      if (sandbox.exists() && !keep) {
        FileUtils.deleteDirectory(sandbox);
      }
    } catch (IOException e) {
      System.out.println("Unable to delete sandbox. Scheduling deleteOnExit: " + currSandbox);
      sandbox.deleteOnExit();
    }
  }

  protected Git git(String dir) throws IOException, InterruptedException {
    return Git.open(dotGitDir(Optional.of(dir)));
  }

  protected Git git() throws IOException, InterruptedException {
    return Git.open(dotGitDir(projectDir()));
  }

  protected Optional<String> projectDir() {
    return Optional.empty();
  }

  @Nonnull
  protected File dotGitDir(@Nonnull Optional<String> projectDir) {
    if (projectDir.isPresent()) {
      return new File(currSandbox + File.separator + projectDir.get() + File.separator + ".git");
    } else {
      return new File(currSandbox + File.separator + ".git");
    }
  }

  public static void initializeMojoWithDefaults(GitCommitIdMojo mojo) {
    mojo.verbose = false;
    mojo.skipPoms = true;
    mojo.abbrevLength = 7;
    mojo.generateGitPropertiesFile = false;
    mojo.format = CommitIdPropertiesOutputFormat.PROPERTIES.toString();
    mojo.generateGitPropertiesFilename = "src/main/resources/git.properties";
    mojo.generateGitPropertiesFileWithEscapedUnicode = true;
    mojo.prefix = "git";
    mojo.dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ";
    mojo.failOnNoGitDirectory = true;
    mojo.useNativeGit = false;
    mojo.commitIdGenerationMode = "full";
    mojo.evaluateOnCommit = evaluateOnCommit;
    mojo.nativeGitTimeoutInMs = (30 * 1000);
    mojo.session = mockSession();
    mojo.settings = mockSettings();
  }

  public void setProjectToExecuteMojoIn(@Nonnull MavenProject project) {
    mojo.project = project;
    mojo.dotGitDirectory = new File(project.getBasedir(), ".git");
    mojo.reactorProjects = getReactorProjects(project);
  }

  private static MavenSession mockSession() {
    MavenSession session = mock(MavenSession.class);
    when(session.getUserProperties()).thenReturn(new Properties());
    when(session.getSystemProperties()).thenReturn(new Properties());
    return session;
  }

  private static Settings mockSettings() {
    Settings settings = mock(Settings.class);
    when(settings.isOffline()).thenReturn(false);
    return settings;
  }

  private static List<MavenProject> getReactorProjects(@Nonnull MavenProject project) {
    List<MavenProject> reactorProjects = new ArrayList<>();
    MavenProject mavenProject = project;
    while (mavenProject != null) {
      reactorProjects.add(mavenProject);
      mavenProject = mavenProject.getParent();
    }
    return reactorProjects;
  }
}
