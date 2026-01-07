/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2026 - Peter Riewe
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

package neon.resources;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import neon.systems.files.JacksonMapper;
import org.junit.jupiter.api.Test;

/** Test Jackson XML parsing for RRecipe resources. */
public class RRecipeJacksonTest {

  @Test
  public void testSimpleRecipeParsing() throws IOException {
    String xml =
        "<recipe id=\"bread\" cost=\"5\">"
            + "<out>bread</out>"
            + "<in>flour</in>"
            + "<in>water</in>"
            + "<in>yeast</in>"
            + "</recipe>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RRecipe recipe = mapper.fromXml(input, RRecipe.class);

    assertNotNull(recipe);
    assertEquals("bread", recipe.id);
    assertEquals("bread", recipe.name);
    assertEquals(5, recipe.cost);
    assertEquals(3, recipe.ingredients.size());
    assertEquals("flour", recipe.ingredients.get(0));
    assertEquals("water", recipe.ingredients.get(1));
    assertEquals("yeast", recipe.ingredients.get(2));
  }

  @Test
  public void testRecipeWithDefaultCost() throws IOException {
    // Cost defaults to 10 if not specified
    String xml = "<recipe id=\"soup\"><out>vegetable_soup</out><in>vegetables</in></recipe>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RRecipe recipe = mapper.fromXml(input, RRecipe.class);

    assertNotNull(recipe);
    assertEquals("soup", recipe.id);
    assertEquals("vegetable_soup", recipe.name);
    assertEquals(10, recipe.cost); // Default value
    assertEquals(1, recipe.ingredients.size());
    assertEquals("vegetables", recipe.ingredients.get(0));
  }

  @Test
  public void testRecipeWithMultipleIngredients() throws IOException {
    String xml =
        "<recipe id=\"potion\" cost=\"25\">"
            + "<out>healing_potion</out>"
            + "<in>red_herbs</in>"
            + "<in>blue_herbs</in>"
            + "<in>water</in>"
            + "<in>bottle</in>"
            + "<in>magic_essence</in>"
            + "</recipe>";
    JacksonMapper mapper = new JacksonMapper();
    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

    RRecipe recipe = mapper.fromXml(input, RRecipe.class);

    assertNotNull(recipe);
    assertEquals(5, recipe.ingredients.size());
    assertTrue(recipe.ingredients.contains("red_herbs"));
    assertTrue(recipe.ingredients.contains("blue_herbs"));
    assertTrue(recipe.ingredients.contains("water"));
    assertTrue(recipe.ingredients.contains("bottle"));
    assertTrue(recipe.ingredients.contains("magic_essence"));
  }

  @Test
  public void testToElementUsesJackson() {
    RRecipe recipe = new RRecipe();
    recipe.name = "iron_sword";
    recipe.cost = 20;
    recipe.ingredients.add("iron_ingot");
    recipe.ingredients.add("leather");
    recipe.ingredients.add("wood");

    // Call toElement() which uses Jackson internally
    org.jdom2.Element element = recipe.toElement();

    // Verify JDOM Element
    assertEquals("recipe", element.getName());
    assertNotNull(element.getAttributeValue("id"));
    assertEquals("20", element.getAttributeValue("cost"));
    assertEquals("iron_sword", element.getChild("out").getText().trim());

    // Check ingredients
    var inElements = element.getChildren("in");
    assertEquals(3, inElements.size());
    assertEquals("iron_ingot", inElements.get(0).getText().trim());
    assertEquals("leather", inElements.get(1).getText().trim());
    assertEquals("wood", inElements.get(2).getText().trim());
  }

  @Test
  public void testRoundTrip() throws IOException {
    // Create recipe, serialize, deserialize, compare
    RRecipe original = new RRecipe();
    original.name = "enchanted_armor";
    original.cost = 100;
    original.ingredients.add("steel_plate");
    original.ingredients.add("magic_gem");
    original.ingredients.add("dragon_scale");

    JacksonMapper mapper = new JacksonMapper();
    String xml = mapper.toXml(original).toString();

    InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    RRecipe deserialized = mapper.fromXml(input, RRecipe.class);

    assertEquals(original.id, deserialized.id);
    assertEquals(original.name, deserialized.name);
    assertEquals(original.cost, deserialized.cost);
    assertEquals(original.ingredients.size(), deserialized.ingredients.size());
    for (int i = 0; i < original.ingredients.size(); i++) {
      assertEquals(original.ingredients.get(i), deserialized.ingredients.get(i));
    }
  }
}
