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

package org.sonar.ide.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.services.*;

/**
 * @author Evgeny Mandrikov
 */
public class SonarTestServer extends HttpServer {
  protected static final Logger LOG = LoggerFactory.getLogger(SonarTestServer.class);

  public SonarTestServer() {
    this(0, "");
  }

  /**
   * @param port    port
   * @param baseDir location of sonar-data directory
   */
  public SonarTestServer(int port, String baseDir) {
    setContextPath("/")
        .setHttpPort(port)
        .setBaseDir(baseDir)
        .addServlet(VersionServlet.class, ServerQuery.BASE_URL)
        .addServlet(ViolationServlet.class, ViolationQuery.BASE_URL)
        .addServlet(SourceServlet.class, SourceQuery.BASE_URL)
        .addServlet(MeasureServlet.class, ResourceQuery.BASE_URL)
        .addServlet(MetricServlet.class, MetricQuery.BASE_URL)
        .addServlet(RuleServlet.class, RuleQuery.BASE_URL);
  }

  public HttpServer start() throws Exception {
    super.start();
    LOG.info("Sonar test server started: {}", getBaseUrl());
    return this;
  }

  public void stop() {
    super.stop();
    LOG.info("Sonar test server stopped: {}", getBaseUrl());
  }

  public String getBaseUrl() {
    return getHttpUrl();
  }

  public Host getHost() {
    return new Host(getBaseUrl());
  }

  /**
   * Entry point.
   *
   * @param args command-line arguments
   * @throws Exception if something wrong
   */
  public static void main(String[] args) throws Exception {
    new SonarTestServer(9000, "sonar-data").start();
  }
}
