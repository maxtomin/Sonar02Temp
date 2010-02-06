package org.sonar.ide.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.wsclient.Server;

import java.io.*;
import java.util.Properties;

/**
 * @author Evgeny Mandrikov
 */
public final class SonarProperties {
  public static final Logger LOG = LoggerFactory.getLogger(SonarProperties.class);

  public static final String HOST_DEFAULT = "http://localhost:9000";

  public static final String HOST_PROPERTY = "host";
  public static final String USERNAME_PROPERTY = "username";
  public static final String PASSWORD_PROPERTY = "password";

  private static SonarProperties instance;

  private String path;
  private Server server;

  public static synchronized SonarProperties getInstance() {
    if (instance == null) {
      instance = new SonarProperties(getDefaultPath());
    }
    return instance;
  }

  public SonarProperties(String path) {
    this.path = path;
    this.server = new Server(HOST_DEFAULT);
    reload();
  }

  public Server getServer() {
    return server;
  }

  public void reload() {
    Properties properties = new Properties();
    if (path != null) {
      File file = new File(path);
      if (file.exists()) {
        FileInputStream inputStream = null;
        try {
          inputStream = new FileInputStream(file);
          properties.load(inputStream);
        } catch (IOException e) {
          LOG.error(e.getMessage(), e);
        } finally {
          closeStream(inputStream);
        }
      }
    }
    server.setHost(properties.getProperty(HOST_PROPERTY, HOST_DEFAULT));
    server.setPassword(properties.getProperty(PASSWORD_PROPERTY));
    server.setUsername(properties.getProperty(USERNAME_PROPERTY));
  }

  private static void addProperty(Properties properties, String key, String value) {
    if (value != null) {
      properties.setProperty(key, value);
    }
  }

  public void save() {
    Properties properties = new Properties();
    addProperty(properties, HOST_PROPERTY, server.getHost());
    addProperty(properties, USERNAME_PROPERTY, server.getUsername());
    addProperty(properties, PASSWORD_PROPERTY, server.getPassword());
    FileOutputStream outputStream = null;
    try {
      outputStream = new FileOutputStream(path);
      properties.store(outputStream, "Sonar Settings");
    } catch (FileNotFoundException e) {
      LOG.error(e.getMessage(), e);
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    } finally {
      closeStream(outputStream);
    }
  }

  public static String getDefaultPath() {
    return System.getProperty("user.home") + File.separator + ".sonar-ide.properties";
  }

  protected void closeStream(Closeable stream) {
    if (stream != null) {
      try {
        stream.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }
}
