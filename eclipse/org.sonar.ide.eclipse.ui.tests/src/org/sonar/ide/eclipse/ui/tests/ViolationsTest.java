package org.sonar.ide.eclipse.ui.tests;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Test;
import org.sonar.ide.eclipse.SonarPlugin;

public class ViolationsTest extends UITestCase {

  @Test
  public void testRefreshViolations() throws Exception {
    configureDefaultSonarServer(getTestServer().getBaseUrl());

    final String projectName = "SimpleProject";
    importNonMavenProject(projectName);

    final SWTBotShell shell = showSonarPropertiesPage(projectName);
    shell.bot().textWithLabel("GroupId :").setText(getGroupId(projectName));

    shell.bot().button("Apply").click();
    shell.bot().button("Cancel").click();
    bot.waitUntil(Conditions.shellCloses(shell));

    final SWTBotTree tree = selectProject(projectName);

    assertThat(getMarkers(projectName).length, is(0));

    ContextMenuHelper.clickContextMenu(tree, "Sonar", "Refresh violations");
    waitForAllBuildsToComplete();
    bot.sleep(1000 * 10); // TODO Godin: looks like waitForAllBuildsToComplete(); doesn't work

    assertThat(getMarkers(projectName).length, greaterThan(0));
  }

  private IMarker[] getMarkers(final String projectName) throws Exception {
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    return project.findMarkers(SonarPlugin.MARKER_VIOLATION_ID, true, IResource.DEPTH_INFINITE);
  }
}
