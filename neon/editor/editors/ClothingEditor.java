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

package neon.editor.editors;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;
import javax.swing.border.*;

import neon.editor.*;
import neon.editor.help.HelpLabels;
import neon.entities.property.Slot;
import neon.resources.RClothing;
import neon.resources.RSpell;
import neon.util.ColorFactory;

public class ClothingEditor extends ObjectEditor {
	private JTextField nameField;
	private JFormattedTextField costField, weightField, charField;
	private JComboBox<String> colorBox, spellBox;
	private JComboBox<Slot> slotBox;
	private RClothing data;
	
	public ClothingEditor(JFrame parent, RClothing data) {
		super(parent, "Clothing Editor: " + data.id);
		this.data = data;
				
		JPanel itemProps = new JPanel();
		GroupLayout layout = new GroupLayout(itemProps);
		itemProps.setLayout(layout);
		layout.setAutoCreateGaps(true);

		JLabel nameLabel = new JLabel("Name: ");
		JLabel costLabel = new JLabel("Cost: ");
		JLabel colorLabel = new JLabel("Color: ");
		JLabel charLabel = new JLabel("Character: ");
		JLabel weightLabel = new JLabel("Weight: ");
		JLabel slotLabel = new JLabel("Slot: ");
		JLabel spellLabel = new JLabel("Enchantment: ");
		nameField = new JTextField(15);
		costField = new JFormattedTextField(NeonFormat.getIntegerInstance());
		colorBox = new JComboBox<String>(ColorFactory.getColorNames());
		colorBox.setBackground(Color.black);
		colorBox.setRenderer(new ColorCellRenderer());
		colorBox.addActionListener(new ColorListener(colorBox));
		charField = new JFormattedTextField(getMaskFormatter("*", 'X'));
		weightField = new JFormattedTextField(NeonFormat.getFloatInstance());
		slotBox = new JComboBox<Slot>(loadSlots());
		spellBox = new JComboBox<String>(loadSpells());
		JLabel nameHelpLabel = HelpLabels.getNameHelpLabel();
		JLabel costHelpLabel = HelpLabels.getCostHelpLabel();
		JLabel colorHelpLabel = HelpLabels.getColorHelpLabel();
		JLabel charHelpLabel = HelpLabels.getCharHelpLabel();
		JLabel weightHelpLabel = HelpLabels.getWeightHelpLabel();
		JLabel slotHelpLabel = HelpLabels.getClothingSlotHelpLabel();
		JLabel spellHelpLabel = HelpLabels.getClothingEnchantmentHelpLabel();
		layout.setVerticalGroup(
				layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(nameLabel).addComponent(nameField).addComponent(nameHelpLabel))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(costLabel).addComponent(costField).addComponent(costHelpLabel))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(colorLabel).addComponent(colorBox).addComponent(colorHelpLabel))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(charLabel).addComponent(charField).addComponent(charHelpLabel))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(weightLabel).addComponent(weightField).addComponent(weightHelpLabel))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(slotLabel).addComponent(slotBox).addComponent(slotHelpLabel))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(spellLabel).addComponent(spellBox).addComponent(spellHelpLabel)));
		layout.setHorizontalGroup(
				layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(nameLabel).addComponent(costLabel).addComponent(colorLabel)
						.addComponent(charLabel).addComponent(weightLabel).addComponent(slotLabel).addComponent(spellLabel))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
						.addComponent(nameField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(costField).addComponent(colorBox).addComponent(charField).addComponent(weightField)
						.addComponent(slotBox).addComponent(spellBox))
				.addGap(10)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
						.addComponent(nameHelpLabel).addComponent(costHelpLabel).addComponent(colorHelpLabel)
						.addComponent(charHelpLabel).addComponent(weightHelpLabel)
						.addComponent(slotHelpLabel).addComponent(spellHelpLabel)));
		JScrollPane propScroller = new JScrollPane(itemProps);
		propScroller.setBorder(new TitledBorder("Properties"));
		frame.add(propScroller, BorderLayout.CENTER);
	}
	
	protected void load() {
		nameField.setText(data.name);
		costField.setValue(data.cost);
		colorBox.setSelectedItem(data.color);
		charField.setValue(data.text);
		weightField.setValue(data.weight);
		slotBox.setSelectedItem(data.slot);
		spellBox.setSelectedItem(data.spell);
	}
	
	protected void save() {
		data.name = nameField.getText();
		data.cost = Integer.parseInt(costField.getText());
		data.color = colorBox.getSelectedItem().toString();
		data.text = charField.getText();
		data.weight = Float.parseFloat(weightField.getText());
		data.slot  = slotBox.getItemAt(slotBox.getSelectedIndex());
		if(spellBox.getSelectedItem() != null) {
			data.spell = spellBox.getSelectedItem().toString();
		}

		data.setPath(Editor.getStore().getActive().get("id"));
	}
	
	private Vector<String> loadSpells() {
		Vector<String> spells = new Vector<String>();
		spells.add(null);
		for(RSpell.Enchantment spell : Editor.resources.getResources(RSpell.Enchantment.class)) {
			if(spell.item.equals("clothing")) {
				spells.add(spell.id);
			}
		}
		return spells;
	}

	private Vector<Slot> loadSlots() {
		Vector<Slot> slots = new Vector<Slot>();
		slots.add(Slot.AMULET);
		slots.add(Slot.RING);
		slots.add(Slot.BELT);
		slots.add(Slot.SHIRT);
		slots.add(Slot.PANTS);
		slots.add(Slot.CLOAK);
		slots.add(Slot.SHOES);
		slots.add(Slot.GLOVES);
		return slots;
	}
}
