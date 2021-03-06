/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.idea.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diff.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.idea.utils.actions.SonarAction;
import org.sonar.ide.idea.utils.actions.SonarActionUtils;
import org.sonar.ide.idea.vfs.SonarVirtualFile;

/**
 * TODO Experimental SONARIDE-97
 *
 * @author Evgeny Mandrikov
 */
public class OpenDiffToolAction extends SonarAction {

  @Override
  public void actionPerformed(AnActionEvent event) {
    Project project = SonarActionUtils.getProject(event);
    VirtualFile file = SonarActionUtils.getVirtualFile(event);
    Document doc1 = FileDocumentManager.getInstance().getDocument(file);
    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(doc1);
    // Load code from Sonar
    SourceCode source = SonarActionUtils.getIdeaSonar(event).search(psiFile);
    String content = source.getRemoteContent();
    // Prepare content for diff
    SonarVirtualFile sonarVirtualFile = new SonarVirtualFile("Test.java", content.getBytes(), source.getKey());
    DiffContent sonarDiffContent = createDiffContent(project, sonarVirtualFile);
    Document doc2 = sonarDiffContent.getDocument();
    doc2.setReadOnly(true);
    // Show diff
    DiffRequest request = getDiffRequest(project, doc1, doc2);
    DiffManager.getInstance().getDiffTool().show(request);
  }

  private DiffRequest getDiffRequest(final Project project, Document doc1, Document doc2) {
    return new SonarDiffRequest(project, new DocumentContent(doc2), new DocumentContent(doc1));
  }

  private static class SonarDiffRequest extends DiffRequest {
    private DocumentContent doc1, doc2;

    private SonarDiffRequest(Project project, DocumentContent doc1, DocumentContent doc2) {
      super(project);
      this.doc1 = doc1;
      this.doc2 = doc2;
    }

    @Override
    public DiffContent[] getContents() {
      return new DiffContent[]{doc1, doc2};
    }

    @Override
    public String[] getContentTitles() {
      return new String[]{"Sonar server", "Local copy"};
    }

    @Override
    public String getWindowTitle() {
      return "Diff"; // TODO
    }
  }

  private DiffContent createDiffContent(final Project project, final VirtualFile virtualFile) {
    return new FileContent(project, virtualFile);
  }
}
