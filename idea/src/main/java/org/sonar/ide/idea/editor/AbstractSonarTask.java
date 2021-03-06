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

package org.sonar.ide.idea.editor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonar.ide.idea.IdeaSonar;
import org.sonar.ide.idea.utils.SonarUtils;

/**
 * @author Evgeny Mandrikov
 */
public abstract class AbstractSonarTask extends Task.Backgroundable {
  private Editor editor;
  private String resourceKey;

  protected AbstractSonarTask(@Nullable Editor editor, @NotNull String title, String resourceKey) {
    super(editor.getProject(), title);
    this.editor = editor;
    this.resourceKey = resourceKey;
  }

  public Document getDocument() {
    return editor.getDocument();
  }

  public PsiFile getPsiFile() {
    return ApplicationManager.getApplication().runReadAction(new Computable<PsiFile>() {
      @Override
      public PsiFile compute() {
        return PsiDocumentManager.getInstance(getProject()).getPsiFile(editor.getDocument());
      }
    });
  }

  public String getResourceKey() {
    return resourceKey;
  }

  public MarkupModel getMarkupModel() {
    return editor.getMarkupModel();
  }

  public IdeaSonar getIdeaSonar() {
    return SonarUtils.getIdeaSonar(getProject());
  }

  /**
   * Removes highlighters with specified key.
   * Should be called before adding new markers to avoid duplicate markers.
   *
   * @param markupModel markup model
   * @param key         key
   */
  protected static void removeSonarHighlighters(MarkupModel markupModel, Key<Boolean> key) {
    for (RangeHighlighter rangeHighlighter : markupModel.getAllHighlighters()) {
      Boolean marker = rangeHighlighter.getUserData(key);
      if (marker != null) {
        markupModel.removeHighlighter(rangeHighlighter);
      }
    }
  }
}
