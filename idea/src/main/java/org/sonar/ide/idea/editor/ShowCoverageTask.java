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

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonar.ide.api.Logs;
import org.sonar.ide.shared.coverage.CoverageData;

import java.awt.*;

/**
 * @author Evgeny Mandrikov
 */
public class ShowCoverageTask extends AbstractSonarTask {
  private static final Key<Boolean> SONAR_COVERAGE_DATA_KEY = Key.create("SONAR_COVERAGE_DATA_KEY");

  private static final Color UNCOVERED = new Color(250, 200, 200);
  private static final Color PARTIALLY_COVERED = Color.YELLOW;
  private static final Color FULLY_COVERED = Color.GREEN;

  public ShowCoverageTask(@Nullable Editor editor, String resourceKey) {
    super(editor, "Loading violations from Sonar for " + resourceKey, resourceKey);
  }

  @Override
  public void run(@NotNull ProgressIndicator progressIndicator) {
    // Load coverage
    final CoverageData coverageData = getIdeaSonar().search(getPsiFile()).getCoverage();
    // Add to UI
    UIUtil.invokeLaterIfNeeded(new Runnable() {
      @Override
      public void run() {
        removeSonarHighlighters(getMarkupModel());
        for (int line = 1; line <= getDocument().getLineCount(); line++) {
          StringBuilder sb = new StringBuilder("Hits: ")
              .append(coverageData.getHitsByLine(line));
          if (coverageData.getBranchHitsByLine(line) != null) {
            sb.append(" (").append(coverageData.getBranchHitsByLine(line)).append(')');
          }
          String tooltip = sb.toString();
          CoverageData.CoverageStatus status = coverageData.getCoverageStatus(line);
          switch (status) {
            case FULLY_COVERED:
              addCoverageHighlighter(line, FULLY_COVERED, tooltip);
              break;
            case PARTIALLY_COVERED:
              addCoverageHighlighter(line, PARTIALLY_COVERED, tooltip);
              break;
            case UNCOVERED:
              addCoverageHighlighter(line, UNCOVERED, tooltip);
              break;
            case NO_DATA:
              break;
            default:
              Logs.INFO.error("Unknown coverage status for line {} in {}", line, getResourceKey());
              break;
          }
        }
      }
    });
  }

  protected void addCoverageHighlighter(int line, Color color, String tooltip) {
    TextAttributes attr = new TextAttributes();
    attr.setBackgroundColor(color);
    RangeHighlighter highlighter = getMarkupModel().addLineHighlighter(line - 1, HighlighterLayer.FIRST, attr);
    highlighter.putUserData(SONAR_COVERAGE_DATA_KEY, true);
    /* TODO
    highlighter.setLineMarkerRenderer(new LineMarkerRenderer(){
      @Override
      public void paint(Editor editor, Graphics graphics, Rectangle rectangle) {
        graphics.setColor(Color.GREEN);
        graphics.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
      }
    });
    */

    highlighter.setThinErrorStripeMark(false);
    highlighter.setGutterIconRenderer(new CoverageGutterIconRenderer(color, tooltip));
    highlighter.setGreedyToRight(false);
    highlighter.setGreedyToLeft(true);
  }

  public static void removeSonarHighlighters(MarkupModel markupModel) {
    removeSonarHighlighters(markupModel, SONAR_COVERAGE_DATA_KEY);
  }
}
