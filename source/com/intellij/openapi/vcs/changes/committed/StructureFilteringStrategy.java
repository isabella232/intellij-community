package com.intellij.openapi.vcs.changes.committed;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNodeRenderer;
import com.intellij.openapi.vcs.changes.ui.TreeModelBuilder;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.util.ui.Tree;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author yole
 */
public class StructureFilteringStrategy implements ChangeListFilteringStrategy {
  private CopyOnWriteArrayList<ChangeListener> myListeners = new CopyOnWriteArrayList<ChangeListener>();
  private MyUI myUI;
  private final Project myProject;
  private final List<FilePath> mySelection = new ArrayList<FilePath>();

  public StructureFilteringStrategy(final Project project) {
    myProject = project;
  }

  public String toString() {
    return VcsBundle.message("filter.structure.name");
  }

  @Nullable
  public JComponent getFilterUI() {
    if (myUI == null) {
      myUI = new MyUI();
    }
    return myUI;
  }

  public void setFilterBase(List<CommittedChangeList> changeLists) {
    if (myUI == null) {
      myUI = new MyUI();
    }
    myUI.buildModel(changeLists);
  }

  public void addChangeListener(ChangeListener listener) {
    myListeners.add(listener);
  }

  public void removeChangeListener(ChangeListener listener) {
    myListeners.remove(listener);
  }

  public List<CommittedChangeList> filterChangeLists(List<CommittedChangeList> changeLists) {
    if (mySelection.size() == 0) {
      return changeLists;
    }
    final ArrayList<CommittedChangeList> result = new ArrayList<CommittedChangeList>();
    for(CommittedChangeList list: changeLists) {
      if (listMatchesSelection(list)) {
        result.add(list);
      }
    }
    return result;
  }

  private boolean listMatchesSelection(final CommittedChangeList list) {
    for(Change change: list.getChanges()) {
      FilePath path = ChangesUtil.getFilePath(change);
      for(FilePath selPath: mySelection) {
        if (path.isUnder(selPath, false)) {
          return true;
        }
      }
    }
    return false;
  }

  private class MyUI extends JPanel {
    private Tree myStructureTree;
    private boolean myRendererInitialized;

    public MyUI() {
      setLayout(new BorderLayout());
      myStructureTree = new Tree();
      myStructureTree.setRootVisible(false);
      myStructureTree.setShowsRootHandles(true);
      myStructureTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
        public void valueChanged(final TreeSelectionEvent e) {
          mySelection.clear();
          DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
          if (node.getUserObject() instanceof FilePath) {
            mySelection.add((FilePath) node.getUserObject());
          }

          for(ChangeListener listener: myListeners) {
            listener.stateChanged(new ChangeEvent(this));
          }
        }
      });
      add(new JScrollPane(myStructureTree), BorderLayout.CENTER);
    }

    public void buildModel(final List<CommittedChangeList> changeLists) {
      final Set<FilePath> filePaths = new HashSet<FilePath>();
      for(CommittedChangeList changeList: changeLists) {
        for(Change change: changeList.getChanges()) {
          final FilePath filePath = ChangesUtil.getFilePath(change);
          final FilePath parentPath = filePath.getParentPath();
          if (parentPath == null) {
            filePaths.add(filePath);
          }
          else {
            filePaths.add(parentPath);
          }
        }
      }
      final TreeModelBuilder builder = new TreeModelBuilder(myProject, false);
      myStructureTree.setModel(builder.buildModelFromFilePaths(filePaths));
      if (!myRendererInitialized) {
        myRendererInitialized = true;
        myStructureTree.setCellRenderer(new ChangesBrowserNodeRenderer(myProject, false, false));
      }
    }
  }
}