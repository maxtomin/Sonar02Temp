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

package org.sonar.ide.shared;

import org.junit.Test;
import org.sonar.ide.shared.violations.ViolationUtils;
import org.sonar.wsclient.services.Violation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Evgeny Mandrikov
 */
public class ViolationUtilsTest {
  @Test
  public void testConvertSeverity() throws Exception {
    assertThat(ViolationUtils.convertSeverity("Blocker"), is(0));
    assertThat(ViolationUtils.convertSeverity("Critical"), is(1));
    assertThat(ViolationUtils.convertSeverity("Major"), is(2));
    assertThat(ViolationUtils.convertSeverity("Minor"), is(3));
    assertThat(ViolationUtils.convertSeverity("Info"), is(4));
    assertThat(ViolationUtils.convertSeverity("Other"), is(4));
  }

  @Test
  public void testSortByPriority() throws Exception {
    Violation minor = newViolation("Minor", 1);
    Violation blocker = newViolation("Blocker", 1);

    List<Violation> violations = ViolationUtils.sortByPriority(Arrays.asList(minor, blocker));

    assertThat(violations.size(), is(2));
    assertThat(violations.get(0), sameInstance(blocker));
    assertThat(violations.get(1), sameInstance(minor));
  }

  @Test
  public void testDivideByLines() throws Exception {
    Violation v1 = newViolation("Minor", 2);
    Violation v2 = newViolation("Minor", 1);
    Violation v3 = newViolation("Blocker", 2);

    Map<Integer, List<Violation>> map = ViolationUtils.splitByLines(Arrays.asList(v1, v2, v3));

    assertThat(map.size(), is(2));
    assertThat(map.get(1).size(), is(1));
    assertThat(map.get(1), hasItem(v2));
    assertThat(map.get(2).size(), is(2));
    assertThat(map.get(2), hasItems(v1, v3));
  }

  @Test
  public void testGetDescription() {
    Violation violation = mock(Violation.class);
    when(violation.getRuleName()).thenReturn("Unused");
    when(violation.getMessage()).thenReturn("Avoid unused");
    assertThat(ViolationUtils.getDescription(violation), is("Unused : Avoid unused"));
  }

  protected Violation newViolation(String severity, int line) {
    Violation violation = new Violation();
    violation.setSeverity(severity);
    violation.setLine(line);
    return violation;
  }
}
