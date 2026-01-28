/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2012 - Maarten Driesen
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

package neon.ui.states;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import neon.core.GameContext;
import neon.core.handlers.CombatUtils;
import neon.core.handlers.InventoryHandler;
import neon.entities.Player;
import neon.entities.components.HealthComponent;
import neon.entities.components.Stats;
import neon.entities.property.*;
import neon.resources.RSpell;
import neon.ui.UserInterface;
import neon.util.fsm.State;
import neon.util.fsm.TransitionEvent;
import net.engio.mbassy.bus.MBassador;

public class JournalState extends State implements FocusListener {
  private static final UIDefaults defaults = UIManager.getLookAndFeelDefaults();
  private static final Color line = defaults.getColor("List.foreground");

  private final JPanel quests;
  private final CardLayout layout;
  private final JPanel cards;
  private final JPanel main;
  private final JLabel instructions;
  private final MBassador<EventObject> bus;
  private final UserInterface ui;
  private final GameContext context;

  // character sheet panel
  private final JPanel stats;
  private final JPanel stuff;
  private final JPanel skills;
  private final JPanel feats;
  private final JPanel traits;
  private final JPanel abilities;
  private final JScrollPane skillScroller;
  private final JScrollPane featScroller;
  private final JScrollPane traitScroller;
  private final JScrollPane abilityScroller;
  private final CombatUtils combatUtils;
  // spells panel
  private final JList<RSpell> sList;

  public JournalState(
      State parent, MBassador<EventObject> bus, UserInterface ui, GameContext context) {
    super(parent);
    this.bus = bus;
    this.ui = ui;
    this.context = context;
    main = new JPanel(new BorderLayout());
    this.combatUtils = new CombatUtils(context.getStore());
    // cardlayout om verschillende panels weer te geven.
    layout = new CardLayout();
    cards = new JPanel(layout);

    // stats
    stats = new JPanel(new GridLayout(0, 3));
    stats.setBorder(new TitledBorder("Character sheet"));
    stuff = new JPanel(new GridLayout(0, 1));
    JScrollPane stuffScroller = new JScrollPane(stuff);
    stuffScroller.setBorder(new TitledBorder("Stats"));
    skills = new JPanel(new GridLayout(0, 1));
    skillScroller = new JScrollPane(skills);
    skillScroller.addFocusListener(this);
    skillScroller.setBorder(new TitledBorder("Skills"));
    JPanel advantages = new JPanel(new GridLayout(3, 0));
    feats = new JPanel(new GridLayout(0, 1));
    featScroller = new JScrollPane(feats);
    featScroller.addFocusListener(this);
    featScroller.setBorder(new TitledBorder("Feats"));
    advantages.add(featScroller);
    traits = new JPanel(new GridLayout(0, 1));
    traitScroller = new JScrollPane(traits);
    traitScroller.addFocusListener(this);
    traitScroller.setBorder(new TitledBorder("Traits"));
    advantages.add(traitScroller);
    abilities = new JPanel(new GridLayout(0, 1));
    abilityScroller = new JScrollPane(abilities);
    abilityScroller.addFocusListener(this);
    abilityScroller.setBorder(new TitledBorder("Abilities"));
    advantages.add(abilityScroller);
    stats.add(stuffScroller);
    stats.add(skillScroller);
    stats.add(advantages);
    cards.add(stats, "stats");

    // quests
    quests = new JPanel();
    quests.setBorder(new TitledBorder("Quests"));
    quests.setLayout(new GridLayout(0, 1));
    cards.add(quests, "quests");

    // spells
    JPanel spells = new JPanel(new BorderLayout());
    spells.setBorder(new TitledBorder("Spells"));
    cards.add(spells, "spells");

    sList = new JList<RSpell>();
    JScrollPane sScroll = new JScrollPane(sList);
    sList.setCellRenderer(new SpellCellRenderer());
    sList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    spells.add(sScroll, BorderLayout.CENTER);

    // instructies
    instructions = new JLabel();
    instructions.setBorder(
        new CompoundBorder(new TitledBorder("Instructions"), new EmptyBorder(0, 5, 10, 5)));

    // dinges toevoegen
    main.add(cards, java.awt.BorderLayout.CENTER);
    main.add(instructions, java.awt.BorderLayout.PAGE_END);
    initKeys();
  }

  @Override
  public void enter(TransitionEvent t) {
    initJournal();
    initSpells();
    initStats();
    layout.show(cards, "stats");
    instructions.setText(
        "<html>Press J or esc to quit, Q to see quests, "
            + "S to view spells and C for your character sheet. Use arrow keys "
            + "to scroll through lists, shift + arrow keys to change list.</html>");
    ui.showPanel(main);
    skillScroller.requestFocus();
  }

  private void initKeys() {
    // keybindings
    InputMap map = main.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    map.put(KeyStroke.getKeyStroke('q'), "quests");
    main.getActionMap().put("quests", new KeyAction("quests"));
    map.put(KeyStroke.getKeyStroke('s'), "spells");
    main.getActionMap().put("spells", new KeyAction("spells"));
    map.put(KeyStroke.getKeyStroke('c'), "stats");
    main.getActionMap().put("stats", new KeyAction("stats"));
    sList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "equip");
    sList.getActionMap().put("equip", new KeyAction("equip"));
    map.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK), "left");
    main.getActionMap().put("left", new KeyAction("left"));
    map.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK), "right");
    main.getActionMap().put("right", new KeyAction("right"));
    map.put(KeyStroke.getKeyStroke("ESCAPE"), "esc");
    map.put(KeyStroke.getKeyStroke('j'), "esc");
    main.getActionMap().put("esc", new KeyAction("esc"));
  }

  private void initJournal() {
    quests.removeAll();
    HashMap<String, Integer> questList = context.getPlayer().getJournal().getQuests();
    HashMap<String, String> questDescriptions = context.getPlayer().getJournal().getSubjects();
    for (Map.Entry<String, Integer> entry : questList.entrySet()) {
      quests.add(
          new JLabel(
              "<html><b>"
                  + entry.getKey()
                  + ": "
                  + (entry.getValue() == 100 ? "finished" : "active")
                  + "</b><br />"
                  + questDescriptions.get(entry.getKey())
                  + "</html>"));
    }
  }

  private void initSpells() {
    ArrayList<RSpell> formulae = new ArrayList<RSpell>();
    formulae.addAll(context.getPlayer().getMagicComponent().getSpells());
    formulae.addAll(context.getPlayer().getMagicComponent().getPowers());
    sList.setListData(formulae.toArray(new RSpell[0]));
  }

  private void initStats() {
    Player player = context.getPlayer();
    HealthComponent health = player.getHealthComponent();

    stuff.removeAll();
    skills.removeAll();
    feats.removeAll();
    traits.removeAll();
    abilities.removeAll();

    Stats stats = player.getStatsComponent();
    stuff.add(new JLabel("Name: " + player.getName()));
    stuff.add(new JLabel("Race: " + player.species.id));
    stuff.add(new JLabel("Specialisation: " + player.getSpecialisation()));
    stuff.add(new JLabel("Strength: " + stats.getStr()));
    stuff.add(new JLabel("Constitution: " + stats.getCon()));
    stuff.add(new JLabel("Dexterity: " + stats.getDex()));
    stuff.add(new JLabel("Intelligence: " + stats.getInt()));
    stuff.add(new JLabel("Charisma: " + stats.getCha()));
    stuff.add(new JLabel("Wisdom: " + stats.getWis()));
    stuff.add(new JLabel("Health: " + health.getHealth() + "/" + health.getBaseHealth()));
    stuff.add(
        new JLabel(
            "Mana: "
                + player.getMagicComponent().getMana()
                + "/"
                + player.species.mana * player.species.iq));
    stuff.add(new JLabel("Size: " + player.species.size));
    stuff.add(new JLabel("Gender: " + (player.getGender().toString().toLowerCase())));
    int light = stats.getStr() * 3;
    int medium = stats.getStr() * 6;
    int heavy = stats.getStr() * 9;
    stuff.add(
        new JLabel(
            "Encumbrance: "
                + InventoryHandler.getWeight(player)
                + " (of "
                + light
                + "/"
                + medium
                + "/"
                + heavy
                + ") kg"));
    stuff.add(new JLabel("Defense value: " + combatUtils.getDV(player)));
    stuff.add(new JLabel("Attack value: " + player.getAVString()));

    for (Skill skill : Skill.values()) {
      skills.add(new JLabel(skill.toString().toLowerCase() + ": " + player.getSkill(skill)));
    }

    for (Feat feat : player.getCharacteristicsComponent().getFeats()) {
      feats.add(new JLabel(feat.text));
    }
    for (Trait trait : player.getCharacteristicsComponent().getTraits()) {
      traits.add(new JLabel(trait.text));
    }
    for (Ability ability : player.getCharacteristicsComponent().getAbilities()) {
      abilities.add(
          new JLabel(
              ability.text + ": " + player.getCharacteristicsComponent().getAbility(ability)));
    }
  }

  public void focusGained(FocusEvent fe) {
    if (fe.getComponent().equals(featScroller)) {
      featScroller.setBorder(new TitledBorder(new LineBorder(line.brighter(), 2), "Feats"));
    } else if (fe.getComponent().equals(skillScroller)) {
      skillScroller.setBorder(new TitledBorder(new LineBorder(line.brighter(), 2), "Skills"));
    } else if (fe.getComponent().equals(traitScroller)) {
      traitScroller.setBorder(new TitledBorder(new LineBorder(line.brighter(), 2), "Traits"));
    } else if (fe.getComponent().equals(abilityScroller)) {
      abilityScroller.setBorder(new TitledBorder(new LineBorder(line.brighter(), 2), "Abilities"));
    }
  }

  public void focusLost(FocusEvent fe) {
    if (fe.getComponent().equals(featScroller)) {
      featScroller.setBorder(new TitledBorder("Feats"));
    } else if (fe.getComponent().equals(skillScroller)) {
      skillScroller.setBorder(new TitledBorder("Skills"));
    } else if (fe.getComponent().equals(traitScroller)) {
      traitScroller.setBorder(new TitledBorder("Traits"));
    } else if (fe.getComponent().equals(abilityScroller)) {
      abilityScroller.setBorder(new TitledBorder("Abilities"));
    }
  }

  @SuppressWarnings("serial")
  private class KeyAction extends AbstractAction {
    private final String command;

    public KeyAction(String command) {
      this.command = command;
    }

    public void actionPerformed(ActionEvent ae) {
      switch (command) {
        case "esc":
          bus.publishAsync(new TransitionEvent("cancel"));
          break;
        case "right":
          if (skillScroller.hasFocus()) {
            featScroller.requestFocus();
          } else if (featScroller.hasFocus()) {
            traitScroller.requestFocus();
          } else if (traitScroller.hasFocus()) {
            abilityScroller.requestFocus();
          } else if (abilityScroller.hasFocus()) {
            skillScroller.requestFocus();
          }
          break;
        case "left":
          if (skillScroller.hasFocus()) {
            abilityScroller.requestFocus();
          } else if (featScroller.hasFocus()) {
            skillScroller.requestFocus();
          } else if (traitScroller.hasFocus()) {
            featScroller.requestFocus();
          } else if (abilityScroller.hasFocus()) {
            traitScroller.requestFocus();
          }
          break;
        case "equip":
          context.getPlayer().getMagicComponent().equipSpell(sList.getSelectedValue());
          initSpells();
          break;
        case "quests":
          layout.show(cards, "quests");
          instructions.setText(
              "<html>Press J or esc to quit, Q to see quests, "
                  + "S to view spells and C for your character sheet.</html>");
          main.repaint();
          break;
        case "spells":
          layout.show(cards, "spells");
          sList.setSelectedIndex(0);
          sList.requestFocus();
          instructions.setText(
              "<html>Press J or esc to quit, Q to see quests, "
                  + "S to view spells and C for your character sheet. Press enter "
                  + "to equip a spell.</html>");
          main.repaint();
          break;
        case "stats":
          layout.show(cards, "stats");
          skillScroller.requestFocus();
          instructions.setText(
              "<html>Press J or esc to quit, Q to see quests, "
                  + "S to view spells and C for your character sheet. Use arrow keys "
                  + "to scroll through lists, shift + arrow keys to change list.</html>");
          main.repaint();
          break;
      }
    }
  }

  @SuppressWarnings("serial")
  private class SpellCellRenderer extends JLabel implements ListCellRenderer<RSpell> {
    private final Font font;

    /** Initializes this renderer. */
    public SpellCellRenderer() {
      font = getFont();
    }

    /** If the player has equiped the spell in the cell, the text is rendered in bold. */
    public Component getListCellRendererComponent(
        JList<? extends RSpell> list,
        RSpell spell,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {
      setText(spell.name != null ? spell.name : spell.id);
      if (context.getPlayer().getMagicComponent().getSpell() == spell) {
        setFont(new Font(getFont().getName(), Font.BOLD, getFont().getSize()));
      } else {
        setFont(font);
      }

      if (isSelected) {
        setBackground(defaults.getColor("List.selectionBackground"));
        setForeground(defaults.getColor("List.selectionForeground"));
      } else {
        setForeground(defaults.getColor("List.foreground"));
      }
      setOpaque(isSelected);
      return this;
    }
  }
}
