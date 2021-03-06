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

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.ide.api.IMeasure;
import org.sonar.ide.api.Logs;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.api.SourceCodeDiff;
import org.sonar.ide.shared.coverage.CoverageData;
import org.sonar.ide.shared.coverage.CoverageUtils;
import org.sonar.ide.shared.duplications.Duplication;
import org.sonar.ide.shared.duplications.DuplicationUtils;
import org.sonar.ide.shared.measures.MeasureData;
import org.sonar.ide.shared.profile.ProfileUtil;
import org.sonar.ide.shared.violations.ViolationUtils;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Metric;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.services.Rule;
import org.sonar.wsclient.services.RuleQuery;
import org.sonar.wsclient.services.Source;
import org.sonar.wsclient.services.SourceQuery;
import org.sonar.wsclient.services.Violation;
import org.sonar.wsclient.services.ViolationQuery;

import com.google.common.collect.Lists;

/**
 * @author Evgeny Mandrikov
 * @since 0.2
 */
class RemoteSourceCode implements SourceCode, ObsoleteControllable {

  private final String key;
  private final ObsoleteControllableImpl delegate = new ObsoleteControllableImpl(10, TimeUnit.MINUTES);
  private RemoteSonarIndex index;

  private String localContent;

  /**
   * Lazy initialization - see {@link #getRemoteContentAsArray()}.
   */
  private String[] remoteContent;

  /**
   * Lazy initialization - see {@link #getChildren()}.
   */
  private Set<SourceCode> children;

  /**
   * Lazy initialization - see {@link #getViolations()}.
   */
  private List<Violation> violations;

  /**
   * Lazy initialization - see {@link #getDuplications()}.
   */
  private List<Duplication> duplications;

  public RemoteSourceCode(String key) {
    this(key, null);
  }

  public RemoteSourceCode(String key, Date remoteDate) {
    this.key = key;
    updateTo(remoteDate);
  }

  /**
   * {@inheritDoc}
   */
  public String getKey() {
    return key;
  }

  /**
   * {@inheritDoc}
   */
  public Set<SourceCode> getChildren() {
    if (children == null) {
      ResourceQuery query = new ResourceQuery().setDepth(1).setResourceKeyOrId(getKey());
      Collection<Resource> resources = index.getSonar().findAll(query);
      children = new HashSet<SourceCode>();
      for (Resource resource : resources) {
        children.add(new RemoteSourceCode(resource.getKey(), resource.getDate()).setRemoteSonarIndex(index));
      }
    }
    return children;
  }

  /**
   * {@inheritDoc}
   */
  public SourceCode setLocalContent(final String content) {
    this.localContent = content;
    return this;
  }

  private String getLocalContent() {
    if (localContent == null) {
      return "";
    }
    return localContent;
  }

  private String[] getRemoteContentAsArray() {
    if (remoteContent == null) {
      Source source = index.getSonar().find(new SourceQuery(getKey()));
      remoteContent = SimpleSourceCodeDiffEngine.getLines(source);
    }
    return remoteContent;
  }

  public String getRemoteContent() {
    return StringUtils.join(getRemoteContentAsArray(), "\n");
  }

  private SourceCodeDiff getDiff() {
    return index.getDiffEngine().diff(SimpleSourceCodeDiffEngine.split(getLocalContent()), getRemoteContentAsArray());
  }

  /**
   * {@inheritDoc}
   */
  public List<IMeasure> getMeasures() {
    Map<String, Metric> metricsByKey = index.getMetrics();
    Set<String> keys = metricsByKey.keySet();
    String[] metricKeys = keys.toArray(new String[keys.size()]);
    ResourceQuery query = ResourceQuery.createForMetrics(getKey(), metricKeys);
    Resource resource = index.getSonar().find(query);
    List<IMeasure> result = Lists.newArrayList();
    for (Measure measure : resource.getMeasures()) {
      final Metric metric = metricsByKey.get(measure.getMetricKey());
      final String value = measure.getFormattedValue();
      // Hacks around SONAR-1620
      if ( !metric.getHidden() && !"DATA".equals(metric.getType()) && StringUtils.isNotBlank(measure.getFormattedValue())) {
        result.add(new MeasureData().setMetricDef(metric).setValue(value));
      }
    }
    return result;
  }

  /**
   * {@inheritDoc}
   */
  public CoverageData getCoverage() {
    final Resource resource = index.getSonar().find(
        ResourceQuery.createForMetrics(getKey(), CoverageUtils.COVERAGE_LINE_HITS_DATA_KEY, CoverageUtils.BRANCH_COVERAGE_HITS_DATA_KEY));
    final Measure measure = resource.getMeasure(CoverageUtils.COVERAGE_LINE_HITS_DATA_KEY);
    final Measure measure2 = resource.getMeasure(CoverageUtils.BRANCH_COVERAGE_HITS_DATA_KEY);
    if (measure2 != null) {
      return new CoverageData(CoverageUtils.unmarshall(measure.getData()), CoverageUtils.unmarshall(measure2.getData()));
    } else {
      return new CoverageData(CoverageUtils.unmarshall(measure.getData()), new HashMap<Integer, String>());
    }
  }

  /**
   * {@inheritDoc}
   */
  public List<Violation> getViolations() {
    if (violations == null) {
      Logs.INFO.info("Loading violations for {}", getKey());
      violations = index.getSonar().findAll(ViolationQuery.createForResource(getKey()));
      Logs.INFO.info("Loaded {} violations: {}", violations.size(), ViolationUtils.toString(violations));
    }
    return ViolationUtils.convertLines(violations, getDiff());
  }

  /**
   * {@inheritDoc}
   */
  public List<Duplication> getDuplications() {
    if (duplications == null) {
      Logs.INFO.info("Loading duplications for {}", getKey());
      final Resource resource = index.getSonar().find(ResourceQuery.createForMetrics(getKey(), DuplicationUtils.DUPLICATIONS_DATA));
      final Measure measure = resource.getMeasure(DuplicationUtils.DUPLICATIONS_DATA);
      if (measure == null) {
        return Collections.emptyList();
      }
      duplications = DuplicationUtils.parse(measure.getData());
      Logs.INFO.info("Loaded {} duplications: {}", duplications.size(), duplications);
    }
    return DuplicationUtils.convertLines(duplications, getDiff());
  }

  /**
   * {@inheritDoc}
   */
  public List<Rule> getRules() {
    Logs.INFO.info("Loading rules for {}", getKey());
    final Resource resource = index.getSonar().find(ResourceQuery.createForMetrics(getKey(), ProfileUtil.METRIC_KEY));
    final Measure measure = resource.getMeasure(ProfileUtil.METRIC_KEY);
    if (measure == null) {
      return Collections.emptyList();
    }
    final List<Rule> rules = getRemoteSonarIndex().getSonar().findAll(new RuleQuery(/* TODO */"java").setProfile(measure.getData()));
    Logs.INFO.info("Loaded {} rules for profile {}", rules.size(), measure.getData());
    return rules;
  }

  /**
   * {@inheritDoc}
   */
  public int compareTo(final SourceCode resource) {
    return key.compareTo(resource.getKey());
  }

  @Override
  public boolean equals(final Object obj) {
    return (obj instanceof RemoteSourceCode) && (key.equals(((RemoteSourceCode) obj).key));
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("key", key).toString();
  }

  protected RemoteSourceCode setRemoteSonarIndex(final RemoteSonarIndex index) {
    this.index = index;
    return this;
  }

  protected RemoteSonarIndex getRemoteSonarIndex() {
    return index;
  }

  public boolean obsoleteCheckRequired() {
    return delegate.obsoleteCheckRequired();
  }

  public boolean isObsolete(Date latestDate) {
    return delegate.isObsolete(latestDate);
  }

  public void updateTo(Date latestDate) {
    delegate.updateTo(latestDate);
  }
}
