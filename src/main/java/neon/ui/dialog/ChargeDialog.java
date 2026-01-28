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

package neon.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.*;
import neon.core.GameContext;
import neon.entities.Item;
import neon.entities.Player;
import neon.entities.components.Enchantment;
import neon.ui.UserInterface;

public class ChargeDialog implements KeyListener {
  private Player player;
  private final JList<Item> items;
  private final JDialog frame;
  private final JScrollPane scroller;
  private final UserInterface ui;
  private final GameContext context;

  public ChargeDialog(UserInterface ui, GameContext context) {
    this.ui = ui;
    this.context = context;
    JFrame parent = ui.getWindow();
    frame = new JDialog(parent, true);
    frame.setPreferredSize(new Dimension(parent.getWidth() - 100, parent.getHeight() - 100));
    frame.setUndecorated(true);

    JPanel contents = new JPanel(new BorderLayout());
    contents.setBorder(
        new CompoundBorder(new EtchedBorder(EtchedBorder.RAISED), new EmptyBorder(10, 10, 10, 10)));
    frame.setContentPane(contents);

    // list of items
    items = new JList<Item>();
    items.setFocusable(false);
    scroller = new JScrollPane(items);
    items.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    scroller.setBorder(new TitledBorder("Magic items"));
    contents.add(scroller, BorderLayout.CENTER);

    JLabel instructions =
        new JLabel(
            "<html>Use arrow keys to select item, press enter to charge, esc to exit.</html>");
    instructions.setBorder(
        new CompoundBorder(new TitledBorder("Instructions"), new EmptyBorder(0, 5, 10, 5)));
    contents.add(instructions, BorderLayout.PAGE_END);

    frame.addKeyListener(this);
    try {
      frame.setOpacity(0.9f);
    } catch (UnsupportedOperationException e) {
      System.out.println("setOpacity() not supported.");
    }
  }

  public void show(Player player) {
    this.player = player;
    initItems();

    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  public void keyReleased(KeyEvent e) {}

  public void keyTyped(KeyEvent e) {}

  public void keyPressed(KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_ESCAPE:
        frame.dispose();
        break;
      case KeyEvent.VK_UP:
        if (items.getSelectedIndex() > 0) {
          items.setSelectedIndex(items.getSelectedIndex() - 1);
        }
        break;
      case KeyEvent.VK_DOWN:
        items.setSelectedIndex(items.getSelectedIndex() + 1);
        break;
      case KeyEvent.VK_ENTER:
        try {
          Item item = items.getSelectedValue();
          item.getMagicComponent().setModifier(0);
          ui.showMessage("Item charged.", 2);
        } catch (ArrayIndexOutOfBoundsException f) {
          ui.showMessage("No item selected.", 2);
        }
        break;
    }
  }

  private void initItems() {
    Vector<Item> listData = new Vector<Item>();
    for (long uid : player.getInventoryComponent()) {
      Item item = (Item) context.getStore().getEntity(uid);
      Enchantment enchantment = item.getMagicComponent();
      if (enchantment != null && enchantment.getMana() < enchantment.getBaseMana()) {
        listData.add(item);
      }
    }
    items.setListData(listData);
    items.setSelectedIndex(0);
  }
}
