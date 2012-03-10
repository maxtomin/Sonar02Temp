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

package org.sonar.ide.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.shared.AbstractResourceUtils;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.services.*;

import java.util.*;

/**
 * Connects to SOnar and provides basic metadata (projects, Soanr version etc)
 * @author Max Tomin
 */
public class MetadataClient {
  private static final Logger LOG = LoggerFactory.getLogger(MetadataClient.class);

  private final Sonar sonar;
  private boolean available;
  private String version;

  /**
   * Groups -> Artifacts -> branches (optional)
   */
  private SortedMap<String, SortedMap<String, SortedSet<String>>> projects = new TreeMap<String, SortedMap<String, SortedSet<String>>>(); 

  public MetadataClient(String host, String username, String password) {
    this(new Sonar(new ExtendedHttpClient3Connector(new Host(host, username, password))));
  }

  public MetadataClient(Sonar sonar) {
    this.sonar = sonar;
  }

  public void load() {
    if (sonar == null) return;

    try {
      LOG.info("Connect");
      ServerQuery serverQuery = new ServerQuery();
      Server server = sonar.find(serverQuery);
      available = checkVersion(server);
      LOG.info(available ? "Connected to " + server.getId() + "(" + server.getVersion() + ")" : "Unable to connect");

      List<Resource> resources = sonar.findAll(new ResourceQuery().setScopes("PRJ"));
      for (Resource resource : resources) {
        addResource(resource.getKey());
      }

    } catch (ConnectionException e) {
      available = false;
      LOG.error("Unable to connect", e);
    }
  }

  /**
   * Add resource to the cache. Public usage is for testing purposes only.
   * @param resource
   */
  public void addResource(String resource) {
    String[] tokens = AbstractResourceUtils.parseProjectKey(resource);
    String groupId = null;
    String artifactId = null;
    String branch = null;
    if (tokens.length > 1) {
      groupId = tokens[0];
      artifactId = tokens[1];
    } else {
      artifactId = tokens[0];
    }

    if (tokens.length > 2) {
      branch = tokens[2];
    }

    SortedMap<String, SortedSet<String>> artifacts = projects.get(groupId);
    if (artifacts == null) {
      artifacts = new TreeMap<String, SortedSet<String>>();
      projects.put(groupId, artifacts);
    }

    SortedSet<String> branches = artifacts.get(artifactId);
    if (branches == null) {
      branches = new TreeSet<String>();
      artifacts.put(artifactId, branches);
    }
    
    if (branch != null) {
      branches.add(branch);
    }
  }

  private boolean checkVersion(Server server) {
    if (server == null) {
      return false;
    }
    version = server.getVersion();
    return version != null && version.startsWith("2.");
  }

  public boolean isAvailable() {
    return available;
  }

  /**
   * For test only
   */
  public void setAvailable(boolean available) {
    this.available = available;
  }

  public String getVersion() {
    return version;
  }

  /**
   * For test only
   */
  public void setVersion(String version) {
    this.version = version;
  }

  public List<String> getGroups() {
    return projects == null ? Collections.<String>emptyList() : new ArrayList<String>(projects.keySet());
  }

  public List<String> getArtifacts(String groupId) {
    SortedMap<String, SortedSet<String>> result = projects == null ? null : projects.get(groupId);
    return result == null ? Collections.<String>emptyList() : new ArrayList<String>(result.keySet());
  }

  public List<String> getBranches(String groupId, String artifactId) {
    SortedMap<String, SortedSet<String>> aritfacts = projects == null ? null : projects.get(groupId);
    SortedSet<String> branches = aritfacts == null ? null : aritfacts.get(artifactId);
    return branches == null ? Collections.<String>emptyList() : new ArrayList<String>(branches);
  }
}
