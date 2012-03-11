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

import org.junit.Before;
import org.junit.Test;
import org.sonar.ide.api.SourceCodeDiff;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Max Tomin
 */
public class SimpleSourceCodeDiffEngineTest {

  private String[] chars(String str) {
    String[] result = new String[str.length()];
    for (int i = 0; i < str.length(); i++) {
       result[i] = Character.toString(str.charAt(i));
    }
    return result;
    
  }
  
  @Test
  public void testGetHashCode() {
    int hash1 = SimpleSourceCodeDiffEngine.getHashCode("int i;");
    int hash2 = SimpleSourceCodeDiffEngine.getHashCode("int\ti;");
    int hash3 = SimpleSourceCodeDiffEngine.getHashCode("int i;\n");
    int hash4 = SimpleSourceCodeDiffEngine.getHashCode("int i;\r\n");
    int hash5 = SimpleSourceCodeDiffEngine.getHashCode("int i;\r");
    int hash6 = SimpleSourceCodeDiffEngine.getHashCode("inti;");

    assertThat(hash2, equalTo(hash1));
    assertThat(hash3, equalTo(hash1));
    assertThat(hash4, equalTo(hash1));
    assertThat(hash5, equalTo(hash1));
    assertThat(hash6, equalTo(hash1));
  }

  @Test
  public void testSplit() {
    assertThat(SimpleSourceCodeDiffEngine.split("\ntest\n").length, is(3));
  }

  @Test
  public void testSame() {
    SourceCodeDiff result = new SimpleSourceCodeDiffEngine().diff(chars("abcd"), chars("abcd"));
    assertThat(result.remoteSize(), equalTo(4));
    assertThat(result.localLine(0), equalTo(0));
    assertThat(result.localLine(1), equalTo(1));
    assertThat(result.localLine(2), equalTo(2));
    assertThat(result.localLine(3), equalTo(3));
  }

  @Test
  public void testSame_WithDuplications() {
    SourceCodeDiff result = new SimpleSourceCodeDiffEngine().diff(chars("abab"), chars("abab"));
    assertThat(result.remoteSize(), equalTo(4));
    assertThat(result.localLine(0), equalTo(0));
    assertThat(result.localLine(1), equalTo(1));
    assertThat(result.localLine(2), equalTo(2));
    assertThat(result.localLine(3), equalTo(3));
  }


  @Test
  public void testAdded() {
    SourceCodeDiff result = new SimpleSourceCodeDiffEngine().diff(chars("abxcd"), chars("abcd"));
    assertThat(result.remoteSize(), equalTo(4));
    assertThat(result.localLine(0), equalTo(0));
    assertThat(result.localLine(1), equalTo(1));
    assertThat(result.localLine(2), equalTo(3));
    assertThat(result.localLine(3), equalTo(4));
  }

  @Test
  public void testAdded_WithDuplications() {
    SourceCodeDiff result = new SimpleSourceCodeDiffEngine().diff(chars("abxab"), chars("abab"));
    assertThat(result.remoteSize(), equalTo(4));
    assertThat(result.localLine(0), equalTo(0));
    assertThat(result.localLine(1), equalTo(1));
    assertThat(result.localLine(2), equalTo(3));
    assertThat(result.localLine(3), equalTo(4));
  }

  @Test
  public void testRemoved() {
    SourceCodeDiff result = new SimpleSourceCodeDiffEngine().diff(chars("abd"), chars("abcd"));
    assertThat(result.remoteSize(), equalTo(3));
    assertThat(result.localLine(0), equalTo(0));
    assertThat(result.localLine(1), equalTo(1));
    assertThat(result.localLine(2), equalTo(-1));
    assertThat(result.localLine(3), equalTo(2));
  }

  @Test
  public void testRemoved_WithDuplications() {
    SourceCodeDiff result = new SimpleSourceCodeDiffEngine().diff(chars("abab"), chars("ababab"));
    assertThat(result.remoteSize(), equalTo(4));
    assertThat(result.localLine(0), equalTo(-1));
    assertThat(result.localLine(1), equalTo(-1));
    assertThat(result.localLine(2), equalTo(0));
    assertThat(result.localLine(3), equalTo(1));
    assertThat(result.localLine(4), equalTo(2));
    assertThat(result.localLine(5), equalTo(3));
  }

  @Test
  public void testDuplicate() {
    SourceCodeDiff result = new SimpleSourceCodeDiffEngine().diff(chars("abbcd"), chars("abcd"));
    assertThat(result.remoteSize(), equalTo(4));
    assertThat(result.localLine(0), equalTo(0));
    assertThat(result.localLine(1), equalTo(2));
    assertThat(result.localLine(2), equalTo(3));
    assertThat(result.localLine(3), equalTo(4));
  }
}
