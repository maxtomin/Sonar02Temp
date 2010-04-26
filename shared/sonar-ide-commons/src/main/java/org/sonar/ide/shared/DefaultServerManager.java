package org.sonar.ide.shared;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.sonar.ide.api.Logs;
import org.sonar.ide.api.SonarIdeException;
import org.sonar.ide.client.SonarClient;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;

/**
 * @author Jérémie Lagarde
 */
public class DefaultServerManager {

  private static final String     SERVER_CACHE_NAME = ".serverlist";        //$NON-NLS-1$

  protected final ArrayList<Host> serverList        = new ArrayList<Host>();
  protected String                path;

  public DefaultServerManager() {
    this(null);
  }

  public DefaultServerManager(String path) {
    this.path = path;
    try {
      load();
    } catch (Exception e) {
      Logs.INFO.warn("default server manager error!", e);
    }
  }

  public void addServer(String location, String username, String password) throws Exception {
    addServer(new Host(location, username, password));
  }

  public void addServer(Host server) {
    if (findServer(server.getHost()) != null) {
      throw new SonarIdeException("Duplicate server: " + server.getHost()); //$NON-NLS-1$
    }
    serverList.add(server);
    commit();
    notifyListeners(IServerSetListener.SERVER_ADDED);
  }

  public List<Host> getServers() {
    return serverList;
  }

  public boolean removeServer(String host) {
    Host server = findServer(host);
    if (server == null) {
      return false;
    }
    boolean result = false;
    result = serverList.remove(server);
    notifyListeners(IServerSetListener.SERVER_REMOVED);
    commit();
    return result;
  }

  public Host createServer(String url) {
    if (StringUtils.isBlank(url))
      return null;
    Host host = findServer(url);
    if (host == null) {
      host = new Host(url);
      addServer(host);
      commit();
    }
    return host;
  }

  public Host findServer(String host) {
    Host server = null;
    for (Host element : serverList) {
      if (element.getHost().equals(host)) {
        server = element;
        break;
      }
    }
    return server;
  }

  public Sonar getSonar(String url) {
    final Host server = createServer(url);
    return new SonarClient(server.getHost(), server.getUsername(), server.getPassword());
  }

  public boolean testSonar(String url, String user, String password) throws Exception {
    SonarClient sonar = new SonarClient(url, user, password);
    return sonar.isAvailable();
  }

  protected File getServerListFile() {
    if (StringUtils.isBlank(path))
      path = System.getProperty("user.home");
    return new File(path + File.separator + SERVER_CACHE_NAME);
  }

  private void commit() {
    File serverListFile = getServerListFile();
    FileOutputStream fos = null;
    PrintWriter writer = null;
    try {
      fos = new FileOutputStream(serverListFile);
      writer = new PrintWriter(fos);
      for (Host server : serverList) {
        writer.println(server.getHost() + "|" + server.getUsername() + "|" + server.getPassword());
      }
      writer.flush();
      fos.flush();
    } catch (Exception ex) {
      throw new SonarIdeException("error in commit server manager", ex);
    } finally {
      if (writer != null) {
        writer.close();
      }
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException e) {
          throw new SonarIdeException("error in commit server manager", e);
        }
      }
    }
  }

  private void load() throws Exception {
    serverList.clear();
    File serverListFile = getServerListFile();
    if (!serverListFile.exists()) {
      return;
    }
    FileInputStream fis = null;
    BufferedReader reader = null;
    try {
      fis = new FileInputStream(serverListFile);
      reader = new BufferedReader(new InputStreamReader(fis));
      String line = null;
      do {
        line = reader.readLine();
        if (line != null && line.trim().length() > 0) {
          String[] infos = StringUtils.split(line, "|");
          if (infos.length == 1)
            serverList.add(new Host(infos[0]));
          if (infos.length == 3)
            serverList.add(new Host(infos[0], infos[1], infos[2]));
        }
      } while (line != null);
    } finally {
      if (fis != null) {
        fis.close();
      }
      if (reader != null) {
        reader.close();
      }
    }
  }

  public interface IServerSetListener {
    public static final int SERVER_ADDED   = 0;
    public static final int SERVER_EDIT    = 1;
    public static final int SERVER_REMOVED = 2;

    public void serverSetChanged(int type, List<Host> serverList);
  }

  protected final List<IServerSetListener> serverSetListeners = new ArrayList<IServerSetListener>();

  public boolean addServerSetListener(IServerSetListener listener) {
    return serverSetListeners.add(listener);
  }

  public boolean removeServerSetListener(IServerSetListener listener) {
    return serverSetListeners.remove(listener);
  }

  protected void notifyListeners(final int eventType) {
    for (final IServerSetListener listener : serverSetListeners) {
      listener.serverSetChanged(eventType, serverList);
    }
  }
}