package org.sonar.ide.eclipse.internal;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;
import org.sonar.ide.wsclient.RemoteSonar;
import org.sonar.wsclient.Host;

/**
 * This is experimental class, which maybe removed in future.
 * Used for migration to new API.
 * 
 * @author Evgeny Mandrikov
 */
public final class EclipseSonar extends RemoteSonar {

  public static EclipseSonar getInstance(IProject project) {
    ProjectProperties properties = ProjectProperties.getInstance(project);
    Host host = SonarPlugin.getServerManager().createServer(properties.getUrl());
    return new EclipseSonar(host);
  }

  /**
   * It's better to use {@link #getInstance(IProject)} instead of it.
   */
  public EclipseSonar(Host host) {
    super(host);
  }

  /**
   * For Eclipse use {@link #search(IResource)} instead of it. {@inheritDoc}
   */
  @Override
  @Deprecated
  public SourceCode search(String key) {
    return super.search(key);
  }

  private static void displayError(Throwable e) {
    SonarPlugin.getDefault().displayError(IStatus.ERROR, e.getMessage(), e, true);
  }

  /**
   * @return null, if not found
   */
  public SourceCode search(IResource resource) {
    if (resource instanceof IProject) {
      String key = EclipseResourceUtils.getInstance().getProjectKey(resource);
      return search(key);
    } else if (resource instanceof IFolder) {
      // TODO hangle packages
    } else if (resource instanceof IFile) {
      String key = EclipseResourceUtils.getInstance().getFileKey(resource);
      SourceCode code = search(key);
      IFile file = (IFile) resource;
      try {
        String content = IOUtils.toString(file.getContents(), file.getCharset());
        code.setLocalContent(content);
      } catch (CoreException e) {
        displayError(e);
      } catch (IOException e) {
        displayError(e);
      }
      return code;
    }
    // TODO
    return null;
  }

}
