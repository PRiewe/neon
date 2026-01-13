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

package neon.editor;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.jar.JarFile;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.*;
import lombok.Getter;
import neon.editor.help.HelpLabels;
import neon.editor.maps.*;
import neon.editor.resources.*;
import neon.resources.*;
import neon.resources.quest.RQuest;
import neon.systems.files.*;
import neon.ui.HelpWindow;

// TODO: use mbassador for events
public class Editor implements Runnable, ActionListener {
  public static JCheckBoxMenuItem tShow, tEdit, oShow, oEdit;
  @Getter private final FileSystem files;
  @Getter private final ResourceManager resources;
  @Getter private static JFrame frame;
  @Getter private final DataStore store;
  private static JPanel toolPanel;
  private static StatusBar status;

  protected MapEditor mapEditor;
  private JTabbedPane mapTabbedPane;
  private JMenuBar menuBar;
  private JMenuItem pack, unpack, newMain, newExt, load, save, export, calculate;
  private JMenu make, edit, tools;
  private JPanel terrainPanel, objectPanel, resourcePanel;
  private JTree objectTree, resourceTree;
  private ModFiler filer;
  private JList<RTerrain> terrainList;
  private DefaultListModel<RTerrain> terrainListModel;
  private InfoEditor infoEditor;
  private CCEditor ccEditor;
  private ScriptEditor scriptEditor;
  private EventEditor eventEditor;

  public static void main(String[] args) throws IOException {
    try { // set directly here to avoid problems
      javax.swing.UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException
        | InstantiationException
        | IllegalAccessException
        | UnsupportedLookAndFeelException e) {
      e.printStackTrace();
    }
    Editor editor = new Editor();
    javax.swing.SwingUtilities.invokeLater(editor);
  }

  public Editor() throws IOException {
    // main window
    frame = new JFrame("Neon Editor");
    frame.getContentPane().setLayout(new BorderLayout());
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setPreferredSize(new Dimension(1280, 800));

    // stuff
    files = new FileSystem();
    resources = new ResourceManager();
    store = new DataStore(resources, files);

    // menu bar
    menuBar = new JMenuBar();
    JMenu file = new JMenu("File");
    make = new JMenu("New");
    newMain = new JMenuItem("Master module...");
    newExt = new JMenuItem("Extension module...");
    load = new JMenuItem("Load...");
    save = new JMenuItem("Save");
    JMenuItem quit = new JMenuItem("Exit");
    newMain.setActionCommand("newMain");
    newExt.setActionCommand("newExt");
    save.setActionCommand("save");
    load.setActionCommand("load");
    quit.setActionCommand("quit");
    newMain.addActionListener(this);
    newExt.addActionListener(this);
    quit.addActionListener(this);
    filer = new ModFiler(frame, files, store, this);
    save.addActionListener(this);
    load.addActionListener(this);
    save.setEnabled(false);
    make.add(newMain);
    make.add(newExt);
    file.add(make);
    file.add(load);
    file.addSeparator();
    file.add(save);
    file.addSeparator();
    file.add(quit);
    edit = new JMenu("Edit");
    edit.setEnabled(false);
    JMenuItem script = new JMenuItem("Scripts...");
    JMenuItem events = new JMenuItem("Events...");
    JMenuItem cc = new JMenuItem("Character creation...");
    JMenuItem game = new JMenuItem("Game info...");
    script.setActionCommand("script");
    cc.setActionCommand("cc");
    game.setActionCommand("game");
    events.setActionCommand("events");
    script.addActionListener(this);
    cc.addActionListener(this);
    game.addActionListener(this);
    events.addActionListener(this);
    edit.add(script);
    edit.add(events);
    edit.add(cc);
    edit.add(game);
    JMenu view = new JMenu("View");
    tShow = new JCheckBoxMenuItem("Show terrain");
    oShow = new JCheckBoxMenuItem("Show objects");
    tEdit = new JCheckBoxMenuItem("Edit terrain");
    oEdit = new JCheckBoxMenuItem("Edit objects");
    tShow.setSelected(true);
    oShow.setSelected(true);
    tEdit.setSelected(true);
    oEdit.setSelected(true);
    view.add(tShow);
    view.add(oShow);
    view.addSeparator();
    view.add(tEdit);
    view.add(oEdit);
    tools = new JMenu("Tools");
    tools.setEnabled(false);
    unpack = new JMenuItem("Unpack mod...");
    unpack.setActionCommand("unpack");
    unpack.addActionListener(this);
    pack = new JMenuItem("Pack mod...");
    pack.setActionCommand("pack");
    pack.addActionListener(this);
    tools.add(unpack);
    tools.add(pack);
    tools.addSeparator();
    export = new JMenuItem("Export SVG...");
    export.setActionCommand("svg");
    export.addActionListener(this);
    calculate = new JMenuItem("Challenge calculator...");
    calculate.setActionCommand("calculate");
    calculate.addActionListener(this);
    tools.add(export);
    tools.add(calculate);
    JMenu help = new JMenu("Help");
    JMenuItem introGuide = new JMenuItem("Getting started...");
    introGuide.setActionCommand("intro");
    introGuide.addActionListener(this);
    help.add(introGuide);
    JMenuItem scriptGuide = new JMenuItem("Scripting guide...");
    scriptGuide.setActionCommand("scripting");
    scriptGuide.addActionListener(this);
    help.add(scriptGuide);
    JMenuItem mapGuide = new JMenuItem("Map editing...");
    mapGuide.setActionCommand("mapping");
    mapGuide.addActionListener(this);
    help.add(mapGuide);
    JMenuItem resGuide = new JMenuItem("Resource editing...");
    resGuide.setActionCommand("resources");
    resGuide.addActionListener(this);
    help.add(resGuide);
    menuBar.add(file);
    menuBar.add(edit);
    menuBar.add(view);
    menuBar.add(tools);
    menuBar.add(help);
    frame.setJMenuBar(menuBar);

    toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    frame.add(toolPanel, BorderLayout.PAGE_START);

    // objects
    objectTree = new JTree();
    objectTree.setRootVisible(false);
    objectTree.setShowsRootHandles(true);
    resourceTree = new JTree();
    resourceTree.setRootVisible(false);
    resourceTree.setShowsRootHandles(true);

    // panels with maps
    mapTabbedPane = new JTabbedPane();
    JPanel mapPanel = new JPanel(new BorderLayout());
    mapEditor = new MapEditor(mapTabbedPane, mapPanel, store);

    // panel with objects and terrain
    JTabbedPane editPanel = new JTabbedPane();
    objectPanel = new JPanel(new BorderLayout());
    objectPanel.setBorder(new TitledBorder("Objects"));
    editPanel.add(objectPanel, "Objects");
    terrainPanel = new JPanel(new BorderLayout());
    terrainPanel.setBorder(new TitledBorder("Terrain"));
    editPanel.add(terrainPanel, "Terrain");
    resourcePanel = new JPanel(new BorderLayout());
    resourcePanel.setBorder(new TitledBorder("Resources"));
    editPanel.add(resourcePanel, "Resources");

    // fiddling with JSplitPanes to get three columns
    JSplitPane bigSplitPane = new JSplitPane();
    JSplitPane smallSplitPane = new JSplitPane();
    smallSplitPane.setLeftComponent(mapPanel);
    smallSplitPane.setRightComponent(mapTabbedPane);
    bigSplitPane.setLeftComponent(smallSplitPane);
    bigSplitPane.setRightComponent(editPanel);

    // this could be better
    smallSplitPane.setDividerLocation(200);
    bigSplitPane.setDividerLocation(1000);
    frame.add(bigSplitPane, BorderLayout.CENTER);

    // status bar
    status = new StatusBar();
    frame.add(status, BorderLayout.SOUTH);
  }

  public void run() {
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private void createMain() {
    JFileChooser chooser = new JFileChooser(new File("neon.ini.xml"));
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setDialogTitle("Choose module directory");
    if (chooser.showDialog(frame, "Choose") == JFileChooser.APPROVE_OPTION) {
      // create mod
      createMod(chooser.getSelectedFile());
      // enable editing
      enableEditing(true);
      frame.pack();
    }
  }

  private void createMod(File file) {
    // path here is the name of the dir, only files knows the full path
    String path = file.getName(); // this also becomes the mod id
    try {
      files.mount(file.getPath());
      // generate directories
      File objects = new File(file, "objects");
      objects.mkdir();
      File maps = new File(file, "maps");
      maps.mkdir();
      File quests = new File(file, "quests");
      quests.mkdir();
      File themes = new File(file, "themes");
      themes.mkdir();

      // load ensures that all resources etc. are initialized
      store.loadData(path, true, false);
      mapEditor.loadMaps(resources.getResources(RMap.class), path);
    } catch (IOException e) {
      JOptionPane.showMessageDialog(frame, "Invalid mod directory: " + file + ".");
    }
  }

  protected void enableEditing(boolean unpacked) {
    // enable menu items
    pack.setEnabled(unpacked);
    unpack.setEnabled(!unpacked);
    make.setEnabled(false);
    tools.setEnabled(true);
    edit.setEnabled(true);
    load.setEnabled(false);
    save.setEnabled(true);

    // prepare lists
    initObjects();
    initResources();
    initTerrain();
  }

  private void createExtension() {
    JFileChooser chooser = new JFileChooser(new File("neon.ini.xml"));
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setDialogTitle("Choose master module");
    if (chooser.showDialog(frame, "Master") == JFileChooser.APPROVE_OPTION) {
      File master = chooser.getSelectedFile();
      chooser.setDialogTitle("Choose extension directory");
      if (chooser.showDialog(frame, "Extension") == JFileChooser.APPROVE_OPTION) {
        // load master
        filer.load(master, false);
        // create extension
        createMod(chooser.getSelectedFile());
        enableEditing(true);
        frame.pack();
      }
    }
  }

  public static void addToolBar(JToolBar bar) {
    toolPanel.add(bar);
  }

  public static StatusBar getStatusBar() {
    return status;
  }

  private void initTerrain() {
    terrainListModel = new DefaultListModel<RTerrain>();
    for (RTerrain rt : resources.getResources(RTerrain.class)) {
      terrainListModel.addElement(rt);
    }
    terrainList = new JList<RTerrain>(terrainListModel);
    terrainList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    TerrainListener listener = new TerrainListener(mapEditor, terrainList, store);
    terrainList.addListSelectionListener(listener);
    terrainList.addMouseListener(listener);
    terrainPanel.add(new JScrollPane(terrainList));
  }

  private void initResources() {
    DefaultMutableTreeNode top = new DefaultMutableTreeNode("Resources");

    // crafts
    ResourceNode craftNode = new ResourceNode("Crafting", ResourceNode.ResourceType.CRAFT, store);
    for (RCraft craft : resources.getResources(RCraft.class)) {
      craftNode.add(new ResourceNode(craft, ResourceNode.ResourceType.CRAFT, store));
    }
    top.add(craftNode);

    // factions
    ResourceNode factionNode =
        new ResourceNode("Factions", ResourceNode.ResourceType.FACTION, store);
    for (RFaction faction : resources.getResources(RFaction.class)) {
      factionNode.add(new ResourceNode(faction, ResourceNode.ResourceType.FACTION, store));
    }
    top.add(factionNode);

    // region themes
    ResourceNode regionNode =
        new ResourceNode("Region themes", ResourceNode.ResourceType.REGION, store);
    for (RRegionTheme region : resources.getResources(RRegionTheme.class)) {
      regionNode.add(new ResourceNode(region, ResourceNode.ResourceType.REGION, store));
    }
    top.add(regionNode);

    // zone themes
    ResourceNode zoneNode = new ResourceNode("Zone themes", ResourceNode.ResourceType.ZONE, store);
    for (RZoneTheme zone : resources.getResources(RZoneTheme.class)) {
      zoneNode.add(new ResourceNode(zone, ResourceNode.ResourceType.ZONE, store));
    }
    top.add(zoneNode);

    // dungeon themes
    ResourceNode dungeonNode =
        new ResourceNode("Dungeon themes", ResourceNode.ResourceType.DUNGEON, store);
    for (RDungeonTheme dungeon : resources.getResources(RDungeonTheme.class)) {
      dungeonNode.add(new ResourceNode(dungeon, ResourceNode.ResourceType.DUNGEON, store));
    }
    top.add(dungeonNode);

    // quests
    ResourceNode questNode = new ResourceNode("Quests", ResourceNode.ResourceType.QUEST, store);
    for (RQuest quest : resources.getResources(RQuest.class)) {
      questNode.add(new ResourceNode(quest, ResourceNode.ResourceType.QUEST, store));
    }
    top.add(questNode);

    // alchemy
    ResourceNode alchemyNode = new ResourceNode("Alchemy", ResourceNode.ResourceType.RECIPE, store);
    for (RRecipe rr : resources.getResources(RRecipe.class)) {
      alchemyNode.add(new ResourceNode(rr, ResourceNode.ResourceType.RECIPE, store));
    }
    top.add(alchemyNode);

    // tattoos
    ResourceNode tattooNode = new ResourceNode("Tattoos", ResourceNode.ResourceType.TATTOO, store);
    for (RTattoo rt : resources.getResources(RTattoo.class)) {
      tattooNode.add(new ResourceNode(rt, ResourceNode.ResourceType.TATTOO, store));
    }
    top.add(tattooNode);

    // spells
    ResourceNode spellNode = new ResourceNode("Spells", ResourceNode.ResourceType.SPELL, store);
    ResourceNode powerNode = new ResourceNode("Powers", ResourceNode.ResourceType.POWER, store);
    ResourceNode curseNode = new ResourceNode("Curses", ResourceNode.ResourceType.CURSE, store);
    ResourceNode poisonNode = new ResourceNode("Poison", ResourceNode.ResourceType.POISON, store);
    ResourceNode enchantNode =
        new ResourceNode("Enchantments", ResourceNode.ResourceType.ENCHANTMENT, store);
    ResourceNode diseaseNode =
        new ResourceNode("Diseases", ResourceNode.ResourceType.DISEASE, store);
    ResourceNode levelNode =
        new ResourceNode("Leveled spells", ResourceNode.ResourceType.LEVEL_SPELL, store);
    for (RSpell rs : resources.getResources(RSpell.class)) {
      if (rs instanceof LSpell) {
        levelNode.add(new ResourceNode(rs, ResourceNode.ResourceType.LEVEL_SPELL, store));
      } else {
        switch (rs.type) {
          case CURSE:
            curseNode.add(new ResourceNode(rs, ResourceNode.ResourceType.CURSE, store));
            break;
          case DISEASE:
            diseaseNode.add(new ResourceNode(rs, ResourceNode.ResourceType.DISEASE, store));
            break;
          case ENCHANT:
            enchantNode.add(new ResourceNode(rs, ResourceNode.ResourceType.ENCHANTMENT, store));
            break;
          case POISON:
            poisonNode.add(new ResourceNode(rs, ResourceNode.ResourceType.POISON, store));
            break;
          case POWER:
            powerNode.add(new ResourceNode(rs, ResourceNode.ResourceType.POWER, store));
            break;
          case SPELL:
            spellNode.add(new ResourceNode(rs, ResourceNode.ResourceType.SPELL, store));
            break;
        }
      }
    }
    top.add(spellNode);
    top.add(powerNode);
    top.add(curseNode);
    top.add(poisonNode);
    top.add(enchantNode);
    top.add(diseaseNode);
    top.add(levelNode);

    // signs
    ResourceNode signNode = new ResourceNode("Birth signs", ResourceNode.ResourceType.SIGN, store);
    for (RSign rs : resources.getResources(RSign.class)) {
      signNode.add(new ResourceNode(rs, ResourceNode.ResourceType.SIGN, store));
    }
    top.add(signNode);

    resourceTree.setModel(new DefaultTreeModel(top));
    resourceTree.addMouseListener(new ResourceTreeListener(resourceTree, frame, store));
    resourcePanel.add(new JScrollPane(resourceTree));
  }

  private void initObjects() {
    DefaultMutableTreeNode top = new DefaultMutableTreeNode("Objects");

    // creatures;
    ObjectNode creatureNode = new ObjectNode("Creatures", ObjectNode.ObjectType.CREATURE, store);
    ObjectNode levelCreatureNode =
        new ObjectNode("Leveled creatures", ObjectNode.ObjectType.LEVEL_CREATURE, store);
    for (RCreature rc : resources.getResources(RCreature.class)) {
      if (rc instanceof LCreature) {
        levelCreatureNode.add(new ObjectNode(rc, ObjectNode.ObjectType.LEVEL_CREATURE, store));
      } else {
        creatureNode.add(new ObjectNode(rc, ObjectNode.ObjectType.CREATURE, store));
      }
    }
    top.add(creatureNode);
    top.add(levelCreatureNode);

    // NPCs
    ObjectNode npcNode = new ObjectNode("NPCs", ObjectNode.ObjectType.NPC, store);
    for (RPerson rp : resources.getResources(RPerson.class)) {
      npcNode.add(new ObjectNode(rp, ObjectNode.ObjectType.NPC, store));
    }
    top.add(npcNode);

    // items
    ObjectNode itemNode = new ObjectNode("Items", ObjectNode.ObjectType.ITEM, store);
    ObjectNode weaponNode = new ObjectNode("Weapons", ObjectNode.ObjectType.WEAPON, store);
    ObjectNode clothingNode = new ObjectNode("Clothing", ObjectNode.ObjectType.CLOTHING, store);
    ObjectNode armorNode = new ObjectNode("Armor", ObjectNode.ObjectType.ARMOR, store);
    ObjectNode lightNode = new ObjectNode("Light", ObjectNode.ObjectType.LIGHT, store);
    ObjectNode doorNode = new ObjectNode("Doors", ObjectNode.ObjectType.DOOR, store);
    ObjectNode containerNode = new ObjectNode("Containers", ObjectNode.ObjectType.CONTAINER, store);
    ObjectNode potionNode = new ObjectNode("Potions", ObjectNode.ObjectType.POTION, store);
    ObjectNode scrollNode = new ObjectNode("Scrolls", ObjectNode.ObjectType.SCROLL, store);
    ObjectNode bookNode = new ObjectNode("Books", ObjectNode.ObjectType.BOOK, store);
    ObjectNode coinNode = new ObjectNode("Money", ObjectNode.ObjectType.MONEY, store);
    ObjectNode foodNode = new ObjectNode("Food", ObjectNode.ObjectType.FOOD, store);
    ObjectNode levelItemNode =
        new ObjectNode("Leveled items", ObjectNode.ObjectType.LEVEL_ITEM, store);
    for (RItem ri : resources.getResources(RItem.class)) {
      if (ri instanceof LItem) {
        levelItemNode.add(new ObjectNode(ri, ObjectNode.ObjectType.LEVEL_ITEM, store));
      } else {
        switch (ri.type) {
          case armor:
            armorNode.add(new ObjectNode(ri, ObjectNode.ObjectType.ARMOR, store));
            break;
          case book:
            bookNode.add(new ObjectNode(ri, ObjectNode.ObjectType.BOOK, store));
            break;
          case clothing:
            clothingNode.add(new ObjectNode(ri, ObjectNode.ObjectType.CLOTHING, store));
            break;
          case coin:
            coinNode.add(new ObjectNode(ri, ObjectNode.ObjectType.MONEY, store));
            break;
          case container:
            containerNode.add(new ObjectNode(ri, ObjectNode.ObjectType.CONTAINER, store));
            break;
          case door:
            doorNode.add(new ObjectNode(ri, ObjectNode.ObjectType.DOOR, store));
            break;
          case food:
            foodNode.add(new ObjectNode(ri, ObjectNode.ObjectType.FOOD, store));
            break;
          case light:
            lightNode.add(new ObjectNode(ri, ObjectNode.ObjectType.LIGHT, store));
            break;
          case potion:
            potionNode.add(new ObjectNode(ri, ObjectNode.ObjectType.POTION, store));
            break;
          case scroll:
            scrollNode.add(new ObjectNode(ri, ObjectNode.ObjectType.SCROLL, store));
            break;
          case weapon:
            weaponNode.add(new ObjectNode(ri, ObjectNode.ObjectType.WEAPON, store));
            break;
          default:
            itemNode.add(new ObjectNode(ri, ObjectNode.ObjectType.ITEM, store));
        }
      }
    }
    top.add(itemNode);
    top.add(weaponNode);
    top.add(clothingNode);
    top.add(armorNode);
    top.add(lightNode);
    top.add(doorNode);
    top.add(containerNode);
    top.add(potionNode);
    top.add(scrollNode);
    top.add(bookNode);
    top.add(coinNode);
    top.add(foodNode);
    top.add(levelItemNode);

    objectTree.setModel(new DefaultTreeModel(top));
    objectTree.addMouseListener(new ObjectTreeListener(objectTree, frame, store));
    objectPanel.add(new JScrollPane(objectTree));
    objectTree.setDragEnabled(true);
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals("save")) {
      filer.save();
    } else if (e.getActionCommand().equals("load")) {
      filer.loadMod();
    } else if (e.getActionCommand().equals("quit")) {
      System.exit(0);
    } else if (e.getActionCommand().equals("newMain")) {
      createMain();
    } else if (e.getActionCommand().equals("newExt")) {
      createExtension();
    } else if (e.getActionCommand().equals("script")) {
      if (scriptEditor == null) {
        scriptEditor = new ScriptEditor(frame, store);
      }
      scriptEditor.show();
    } else if (e.getActionCommand().equals("cc")) {
      if (ccEditor == null) {
        ccEditor = new CCEditor(frame, store);
      }
      ccEditor.show();
    } else if (e.getActionCommand().equals("game")) {
      if (infoEditor == null) {
        infoEditor = new InfoEditor(frame, store);
      }
      infoEditor.show();
    } else if (e.getActionCommand().equals("events")) {
      if (eventEditor == null) {
        eventEditor = new EventEditor(frame, store);
      }
      eventEditor.show();
    } else if (e.getActionCommand().equals("pack")) {
      if (JOptionPane.showConfirmDialog(
              frame,
              "Do you wish to save the current data and pack it?",
              "Pack mod",
              JOptionPane.YES_NO_OPTION)
          == 0) {
        pack();
      }
    } else if (e.getActionCommand().equals("unpack")) {
      unpack();
    } else if (e.getActionCommand().equals("svg")) {
      if ((EditablePane) mapTabbedPane.getSelectedComponent() != null) {
        ZoneTreeNode node = ((EditablePane) mapTabbedPane.getSelectedComponent()).getNode();
        SVGExporter.exportToSVG(node, files, store);
      }
    } else if (e.getActionCommand().equals("calculate")) {
      new ChallengeCalculator().show();
    } else if (e.getActionCommand().equals("scripting")) {
      showHelp("scripting.html", "Scripting guide");
    } else if (e.getActionCommand().equals("intro")) {
      showHelp("intro.html", "Getting started");
    } else if (e.getActionCommand().equals("mapping")) {
      showHelp("maps.html", "Map editing");
    } else if (e.getActionCommand().equals("resources")) {
      showHelp("resources.html", "Resource editing");
    }
  }

  private void showHelp(String file, String title) {
    InputStream input = HelpLabels.class.getResourceAsStream(file);
    Scanner scanner = new Scanner(input, "UTF-8");
    String text = scanner.useDelimiter("\\A").next();
    scanner.close();
    new HelpWindow(frame).show(title, text);
  }

  private void pack() {
    filer.save();
    try {
      JarFile jar = FileUtils.pack(store.getActive().getPath()[0], store.getActive().get("id"));
      System.out.println("attributes: " + jar.getManifest().getMainAttributes());
    } catch (IOException e) {
      JOptionPane.showMessageDialog(frame, "Packing failed");
    }
  }

  private void unpack() {
    FileUtils.unpack(store.getActive().getPath()[0]);
    JOptionPane.showMessageDialog(
        frame,
        "Please restart the editor and load the unpacked mod.",
        "Unpack mod",
        JOptionPane.INFORMATION_MESSAGE);
  }
}
