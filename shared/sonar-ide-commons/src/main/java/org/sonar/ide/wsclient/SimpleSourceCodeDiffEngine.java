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

package org.sonar.ide.wsclient;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.api.SourceCodeDiff;
import org.sonar.ide.api.SourceCodeDiffEngine;
import org.sonar.wsclient.services.Source;

/**
 * Diff engine based on Longest Common Subsequence algorithm.
 *
 * @author Max Tomin
 */
public class SimpleSourceCodeDiffEngine implements SourceCodeDiffEngine {
  public static SourceCodeDiffEngine getInstance() {
    return new SimpleSourceCodeDiffEngine();
  }

  public SourceCodeDiff diff(String local, String remote) {
    return diff(split(local), split(remote));
  }

  /**
   * {@inheritDoc}
   */
  public SourceCodeDiff diff(String[] localStrings, String[] remoteStrings) {
    SourceCodeDiff result = new SourceCodeDiff();

    int[] local = getHashCodes(localStrings);
    int[] remote = getHashCodes(remoteStrings);

    //LCS length matrix. M[i][j] is negative if A[i] = B[j]
    //therefore absolute value shall be used at all times
    int[][] matrix = new int[local.length][remote.length];

    //populate the matrix:
    for (int i = 0; i < local.length; i++) {
      for (int j = 0; j < remote.length; j++) {
        if (local[i] == remote[j]) {
          //LCS(A[0..i], [0..j] = 1 + LCS(A[0..i-1], [0..j-1])
          int length = 1;
          if (i > 0 && j > 0) {
            length += Math.abs(matrix[i-1][j-1]);
          }
          matrix[i][j] = -length;
        } else {
          //LCS(A[0..i], [0..j] = max(LCS(A[0..i], [0..j-1]), LCS(A[0..i-1], [0..j]))
          int upper = i == 0 ? 0 : Math.abs(matrix[i-1][j]); 
          int lefter = j == 0 ? 0 : Math.abs(matrix[i][j-1]);
          matrix[i][j] = Math.max(upper, lefter);
        }
      }
    }

    //backtrace and find mappings:
    int i = local.length - 1;
    int j = remote.length - 1;
    while (i >= 0 && j >= 0 && matrix[i][j] != 0) {
      if (matrix[i][j] < 0) {
        result.map(j, i);
        --i;
        --j;
      } else {
        int upper = i == 0 ? 0 : Math.abs(matrix[i-1][j]);
        int lefter = j == 0 ? 0 : Math.abs(matrix[i][j-1]);
        if (upper > lefter) {
          --i;
        } else {
          --j;
        }
      }
    }
    return result;
  }

  public static String[] split(String text) {
    return StringUtils.splitPreserveAllTokens(text, '\n');
  }

  /**
   * Returns hash code for specified string after removing whitespaces.
   *
   * @param str string
   * @return hash code for specified string after removing whitespaces
   */
  static int getHashCode(String str) {
    int h = 0;
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      if (!Character.isWhitespace(ch)) { //ignore whitespaces
        h = 31 * h + ch;
      }
    }
    return h;
  }

  /**
   * Returns hash codes for specified strings after removing whitespaces.
   *
   * @param str strings
   * @return hash codes for specified strings after removing whitespaces
   */
  private static int[] getHashCodes(String[] str) {
    int[] hashCodes = new int[str.length];
    for (int i = 0; i < str.length; i++) {
      hashCodes[i] = getHashCode(str[i]);
    }
    return hashCodes;
  }

  public static String[] getLines(Source source) {
    String[] remote = new String[source.getLinesById().lastKey()];
    for (int i = 0; i < remote.length; i++) {
      remote[i] = source.getLine(i + 1);
      if (remote[i] == null) {
        remote[i] = "";
      }
    }
    return remote;
  }

}
