package org.sonar.ide.wsclient;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: Maxx
 * Date: 15/03/12
 * Time: 08:56
 * To change this template use File | Settings | File Templates.
 */
public class ObsoleteControllableImpl implements ObsoleteControllable {
  private final long obsoleteCheckPeriodMillis;
  private long currentVersionMillis;
  private long lastCheckedMillis;

  public ObsoleteControllableImpl(long obsoleteCheckPeriodMillis) {
    this.obsoleteCheckPeriodMillis = obsoleteCheckPeriodMillis;
  }

  public ObsoleteControllableImpl(long period, TimeUnit unit) {
    this(unit.toMillis(period));
  }

  public boolean obsoleteCheckRequired() {
    return System.currentTimeMillis() - lastCheckedMillis > obsoleteCheckPeriodMillis;
  }

  public boolean isObsolete(Date latestDate) {
    return latestDate.getTime() > currentVersionMillis;
  }

  public void updateTo(Date latestDate) {
    currentVersionMillis = latestDate.getTime();
    lastCheckedMillis = System.currentTimeMillis();
  }
}
