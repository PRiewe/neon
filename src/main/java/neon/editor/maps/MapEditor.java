/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2013 - Maarten Driesen
 *
 *	This program is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neon.editor.maps;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import lombok.Getter;
import lombok.Setter;
import neon.editor.DataStore;
import neon.editor.Editor;
import neon.editor.resources.IObject;
import neon.editor.resources.IRegion;
import neon.editor.resources.Instance;
import neon.editor.resources.RMap;
import neon.editor.resources.RZone;

public class MapEditor {
  private static String terrain;
  private static JToggleButton drawButton;
  private static JToggleButton selectButton;
  @Setter private static UndoAction undoAction;
  private final DataStore dataStore;
  private JScrollPane mapScrollPane;
  @Getter private final JTree mapTree;

  private JTabbedPane tabs;
  private JCheckBox levelBox;
  private JSpinner levelSpinner;

  public MapEditor(JTabbedPane tabs, JPanel panel, DataStore dataStore) {
    this.dataStore = dataStore;
    this.tabs = tabs;

    // tree met maps
    mapTree = new JTree(new DefaultMutableTreeNode("maps"));
    mapTree.setRootVisible(false);
    mapTree.setShowsRootHandles(true);
    mapTree.addMouseListener(new MapTreeListener(mapTree, tabs, this, dataStore));
    mapTree.setVisible(false);
    mapScrollPane = new JScrollPane(mapTree);
    mapScrollPane.setBorder(new TitledBorder("Maps"));
    panel.add(mapScrollPane, BorderLayout.CENTER);

    // toolbars
    JToolBar zoomBar = new JToolBar();
    ToolBarListener toolBarListener = new ToolBarListener();
    zoomBar.add(new JLabel("Zoom: "));
    JButton minButton = new JButton("-");
    minButton.addActionListener(toolBarListener);
    JButton plusButton = new JButton("+");
    plusButton.addActionListener(toolBarListener);
    zoomBar.add(minButton);
    zoomBar.add(plusButton);
    JToolBar layerBar = new JToolBar();
    levelSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Byte.MAX_VALUE, 1));
    levelSpinner.addChangeListener(toolBarListener);
    levelBox = new JCheckBox("View only current layer");
    levelBox.setActionCommand("layer");
    levelBox.addActionListener(toolBarListener);
    layerBar.add(new JLabel("Layer: "));
    layerBar.add(levelSpinner);
    layerBar.addSeparator();
    layerBar.add(levelBox);
    JToolBar editBar = new JToolBar();
    drawButton = new JToggleButton(new ImageIcon(Editor.class.getResource("brush.png")));
    drawButton.setToolTipText("Draw mode");
    drawButton.addActionListener(toolBarListener);
    editBar.add(drawButton);
    selectButton = new JToggleButton(new ImageIcon(Editor.class.getResource("mouse.png")));
    selectButton.setToolTipText("Select mode");
    selectButton.addActionListener(toolBarListener);
    selectButton.setSelected(true);
    editBar.add(selectButton);
    ButtonGroup mode = new ButtonGroup();
    mode.add(selectButton);
    mode.add(drawButton);
    JButton undo = new JButton(new ImageIcon(Editor.class.getResource("undo.png")));
    undo.setToolTipText("Undo last action");
    undo.addActionListener(toolBarListener);
    undo.setActionCommand("undo");
    editBar.add(undo);
    Editor.addToolBar(layerBar);
    Editor.addToolBar(zoomBar);
    Editor.addToolBar(editBar);
  }

  public static boolean isVisible(Instance r) {
    if (r instanceof IRegion && Editor.tShow.isSelected()) {
      return true;
    } else if (r instanceof IObject && Editor.oShow.isSelected()) {
      return true;
    } else {
      return false;
    }
  }

  private static short createNewUID(DataStore store) {
    short uid = (short) (Math.random() * Short.MAX_VALUE);
    while (store.getMapUIDs().containsValue(uid)) {
      uid++;
    }
    return uid;
  }

  public static boolean drawMode() {
    return drawButton.isSelected();
  }

  public static void deleteMap(String id, DataStore store) {
    // TODO: activeMaps is <RMap>, not <String>!
    store.getActiveMaps().remove(id);
    store.getResourceManager().removeResource(id);
  }

  public static void makeMap(MapDialog.Properties props, JTree mapTreeParam, DataStore store) {
    if (!props.cancelled()) {
      // editableMap maken
      short uid = createNewUID(store);
      RMap map = new RMap(uid, store.getActive().get("id"), props);
      store.getActiveMaps().add(map);
      // en node maken
      DefaultTreeModel model = (DefaultTreeModel) mapTreeParam.getModel();
      DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
      MapTreeNode node = new MapTreeNode(map);
      if (!map.isDungeon()) {
        node.add(new ZoneTreeNode(0, map.getZone(0)));
      }
      model.insertNodeInto(node, root, root.getChildCount());
      mapTreeParam.expandPath(new TreePath(root));
      store.getResourceManager().addResource(map, "maps");
    }
  }

  public void loadMaps(Collection<RMap> maps, String path, JTree mapTree1, DataStore store) {
    DefaultTreeModel model = (DefaultTreeModel) mapTree1.getModel();
    MutableTreeNode root = MapEditor.loadMapsHeadless(maps,model,store);
    mapTree1.expandPath(new TreePath(root));
    mapTree1.setVisible(true);
  }
  public static MutableTreeNode loadMapsHeadless(Collection<RMap> maps, DefaultTreeModel model, DataStore store) {
    MutableTreeNode root = (MutableTreeNode) model.getRoot();
    for (RMap map : maps) {
      store.getMapUIDs().put(map.id, map.uid);
      MapTreeNode node = new MapTreeNode(map);
      if (map.isDungeon()) {
        for (Map.Entry<Integer, RZone> zone : map.zones.entrySet()) {
          node.add(new ZoneTreeNode(zone.getKey(), zone.getValue()));
        }
      } else {
        node.add(new ZoneTreeNode(0, map.getZone(0)));
      }
      model.insertNodeInto(node, root, root.getChildCount());
    }
    return root;

  }

  public void setTerrain(String type) {
    terrain = type;
  }

  public static String getSelectedTerrain() {
    return terrain;
  }

  private class ToolBarListener implements ActionListener, ChangeListener {
    public void stateChanged(ChangeEvent e) {
      EditablePane mapPane = (EditablePane) tabs.getSelectedComponent();
      if (mapPane != null) {
        mapPane.setLayer((Integer) levelSpinner.getValue());
        reload(mapPane);
      }
    }

    public void actionPerformed(ActionEvent e) {
      EditablePane mapPane = (EditablePane) tabs.getSelectedComponent();
      if ("layer".equals(e.getActionCommand())) {
        reload(mapPane);
      } else if ("undo".equals(e.getActionCommand())) {
        if (undoAction != null) {
          undoAction.undo();
        }
        mapPane.repaint();
        System.out.println("undo");
      } else {
        if ("-".equals(e.getActionCommand())) {
          mapPane.setZoom(mapPane.getZoom() / 2);
        } else if ("+".equals(e.getActionCommand())) {
          mapPane.setZoom(mapPane.getZoom() * 2);
        }
      }
    }

    private void reload(EditablePane pane) {
      if (levelBox.isSelected()) {
        pane.setLayer((Integer) levelSpinner.getValue());
        pane.toggleView(false);
      } else {
        pane.toggleView(true);
      }
      pane.repaint();
    }
  }
}
