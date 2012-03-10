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

package org.sonar.ide.ui;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.lang.StringUtils;
import org.sonar.ide.client.MetadataClient;

import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

/**
 * @author Evgeny Mandrikov
 */
public class ModulePanel extends AbstractConfigPanel {
  private final MetadataClient metadataClient;

  private final JComboBox<String> artifactId;
  private final JComboBox<String> groupId;
  private final JComboBox<String> branch;
  private final JButton reloadButton;

  private String currentArtifactId;
  private String currentGroupId;
  private String currentBranch;
  private boolean modified;

  public ModulePanel(MetadataClient metadataClient) {
    this.metadataClient = metadataClient;

    groupId = new JComboBox<String>();
    groupId.setEditable(true);
    groupId.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        groupChanged();
      }
    });


    artifactId = new JComboBox<String>();
    artifactId.setEditable(true);
    artifactId.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        artifactChanged();
      }
    });


    branch = new JComboBox<String>();
    branch.setEditable(true);
    branch.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        branchChanged();
      }
    });

    reloadButton = new JButton("Reload");
    //fix minimum width to avoid width jumps between Reload and Loaging...
    Dimension preferredSize = reloadButton.getPreferredSize();
    preferredSize.setSize(90, preferredSize.height);
    reloadButton.setPreferredSize(preferredSize);
    reloadButton.setMinimumSize(preferredSize);
    reloadButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        reload();
      }
    });


    DefaultFormBuilder formBuilder = new DefaultFormBuilder(new FormLayout(""));
    formBuilder.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    formBuilder.appendColumn("right:pref");
    formBuilder.appendColumn("3dlu");
    formBuilder.appendColumn("fill:p:g");
    formBuilder.append("Group ID:", groupId);
    formBuilder.append("Artifact ID:", artifactId);
    formBuilder.append("Branch:", branch);

    add(formBuilder.getPanel());

    reload();
  }

  private void reload() {
    reloadButton.setText("Loading...");
    reloadButton.setEnabled(false);
    groupId.setEnabled(false);
    artifactId.setEnabled(false);
    branch.setEnabled(false);
    
    new SwingWorker<Void, Void>() {

      @Override
      protected Void doInBackground() throws Exception {
        metadataClient.load();
        return null;
      }

      @Override
      protected void done() {
        reloadButton.setText("Reload");
        reloadButton.setEnabled(true);
        groupId.setEnabled(true);
        artifactId.setEnabled(true);
        branch.setEnabled(true);

        refreshGroups();
        setGroupId(currentGroupId);

        refreshArtifacts();
        setArtifactId(currentArtifactId);

        refreshBranches();
        setBranch(currentBranch);
      }
    }.execute();
  }


  private void refreshGroups() {
    if (metadataClient.isAvailable()) {
      groupId.setModel(new ListComboBoxModel(metadataClient.getGroups()));
    }
  }


  private void refreshArtifacts() {
    if (metadataClient.isAvailable() && currentGroupId != null) {
      artifactId.setModel(new ListComboBoxModel(metadataClient.getArtifacts(currentGroupId)));
    }
  }

  private void refreshBranches() {
    if (metadataClient.isAvailable() && currentGroupId != null && currentArtifactId != null) {
      branch.setModel(new ListComboBoxModel(metadataClient.getBranches(currentGroupId, currentArtifactId)));
    }
  }

  private void groupChanged() {
    String newGroupId = (String) groupId.getSelectedItem();
    if (!StringUtils.equals(currentGroupId, newGroupId)) {
      currentGroupId = newGroupId;
      modified = true;
      refreshArtifacts();
    }
  }

  private void artifactChanged() {
    String newArtifactId = (String) artifactId.getSelectedItem();
    if (!StringUtils.equals(currentArtifactId, newArtifactId)) {
      currentArtifactId = newArtifactId;
      modified = true;
      refreshBranches();
    }
  }

  private void branchChanged() {
    String newBranch = (String) branch.getSelectedItem();
    if (!StringUtils.equals(currentBranch, newBranch)) {
      currentBranch = newBranch;
      modified = true;
    }
  }

  public String getArtifactId() {
    return currentArtifactId;
  }

  public void setArtifactId(String artifactId) {
    currentArtifactId = artifactId;
    this.artifactId.getEditor().setItem(artifactId);
    refreshBranches();
  }

  public String getGroupId() {
    return currentGroupId;
  }

  public void setGroupId(String groupId) {
    currentGroupId = groupId;
    this.groupId.getEditor().setItem(groupId);
    refreshArtifacts();
  }

  public String getBranch() {
    return currentBranch;
  }

  public void setBranch(String branch) {
    currentBranch = branch;
    this.branch.getEditor().setItem(branch);
  }

  @Override
  public boolean isModified() {
    return modified;
  }

  public static void main(String[] args) {
    MetadataClient client = new MetadataClient(null);
    client.addResource("group1:art1");
    client.addResource("group1:art2");
    client.addResource("group2:art3:branch1");
    client.addResource("group2:art3:branch2");
    client.setAvailable(true);
    SwingAppRunner.run(new ModulePanel(client));
  }
}

class ListComboBoxModel extends AbstractListModel<String> implements ComboBoxModel<String> {
  private final List<String> items;
  private String selectedItem;

  public String getSelectedItem() {
    return selectedItem;
  }

  public void setSelectedItem(Object selectedItem) {
    this.selectedItem = (String) selectedItem;
  }

  public int getSize() {
    return items.size();  //To change body of implemented methods use File | Settings | File Templates.
  }

  public String getElementAt(int index) {
    return items.get(index);
  }

  ListComboBoxModel(List<String> items) {
    this.items = items;
  }
}
