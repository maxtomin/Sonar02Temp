package org.sonar.ide.wsclient;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Maxx
 * Date: 15/03/12
 * Time: 08:56
 * To change this template use File | Settings | File Templates.
 */
public interface ObsoleteControllable {
  public boolean obsoleteCheckRequired();
  public boolean isObsolete(Date latestDate);
  public void updateTo(Date latestDate);
}
