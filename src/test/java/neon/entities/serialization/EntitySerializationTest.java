package neon.entities.serialization;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.stream.Stream;
import neon.core.DefaultUIEngineContext;
import neon.editor.DataStore;
import neon.entities.*;
import neon.entities.property.Slot;
import neon.resources.*;
import neon.systems.files.FileSystem;
import neon.test.MapDbTestHelper;
import neon.test.TestEngineContext;
import neon.util.mapstorage.MapStore;
import org.h2.mvstore.WriteBuffer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Comprehensive test for entity serialization and deserialization. Tests all resources from
 * sampleMod1 (88+ items, 136+ creatures) by: 1. Creating entity instances from resources 2.
 * Serializing using EntitySerializerFactory 3. Deserializing and verifying complete fidelity
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EntitySerializationTest {

  // Test infrastructure
  private MapStore testDb;
  private DefaultUIEngineContext testContext;
  private EntitySerializerFactory serializerFactory;
  private EntityFactory entityFactory;
  private DataStore dataStore;

  // Resource collections
  private Map<String, RItem> itemResources;
  private Map<String, RCreature> creatureResources;

  @BeforeAll
  public void setUp() throws Exception {
    // Initialize MapStore database
    testDb = MapDbTestHelper.createTempFileDb();

    // Initialize TestEngineContext
    TestEngineContext.initialize(testDb);
    testContext = TestEngineContext.getTestUiEngineContext();

    // Mount file system and load resources
    FileSystem fileSystem = testContext.getFileSystem();
    fileSystem.mount("src/test/resources/");

    dataStore = new DataStore(testContext.getResourceManageer(), fileSystem);
    dataStore.loadData("sampleMod1", true, false);

    // Create factories
    serializerFactory = new EntitySerializerFactory(testContext);
    entityFactory = new EntityFactory(testContext);

    // Build resource maps
    itemResources = buildItemResourceMap(testContext.getResourceManageer());
    creatureResources = buildCreatureResourceMap(testContext.getResourceManageer());

    System.out.println("Loaded " + itemResources.size() + " item resources");
    System.out.println("Loaded " + creatureResources.size() + " creature resources");
  }

  @AfterAll
  public void tearDown() {
    TestEngineContext.reset();
    MapDbTestHelper.cleanup(testDb);
  }

  // ==================== TIER 1: PARAMETERIZED BULK TESTS ====================

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("provideAllItemResources")
  public void testItemSerializationRoundTrip(ItemTestCase testCase) {
    // Create entity
    long uid = testContext.getStore().createNewEntityUID();
    Item original = entityFactory.getItem(testCase.resourceId, 10, 20, uid);
    assertNotNull(original, "Failed to create item: " + testCase.resourceId);

    // Serialize and deserialize
    Item deserialized = serializeAndDeserialize(original);

    // Verify
    assertItemEquals(original, deserialized, testCase.itemType);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("provideAllCreatureResources")
  public void testCreatureSerializationRoundTrip(CreatureTestCase testCase) {
    // Create entity
    long uid = testContext.getStore().createNewEntityUID();
    Creature original = entityFactory.getCreature(testCase.resourceId, 10, 20, uid);
    assertNotNull(original, "Failed to create creature: " + testCase.resourceId);

    // Serialize and deserialize
    Creature deserialized = serializeAndDeserialize(original);

    // Verify
    assertCreatureEquals(original, deserialized, testCase.type);
  }

  // ==================== TIER 2: TYPE-SPECIFIC TESTS ====================

  @Test
  public void testWeaponSerialization() {
    long uid = testContext.getStore().createNewEntityUID();
    Weapon weapon = (Weapon) entityFactory.getItem("dagger", 5, 10, uid);
    weapon.setState(50); // Set durability

    Weapon deserialized = serializeAndDeserialize(weapon);

    assertItemEquals(weapon, deserialized, ItemType.WEAPON);
    assertWeaponEquals(weapon, deserialized);
  }

  @Test
  public void testArmorSerialization() {
    long uid = testContext.getStore().createNewEntityUID();
    Armor armor = (Armor) entityFactory.getItem("leather chausses", 5, 10, uid);
    armor.setState(75); // Set durability

    Armor deserialized = serializeAndDeserialize(armor);

    assertItemEquals(armor, deserialized, ItemType.ARMOR);
    assertArmorEquals(armor, deserialized);
  }

  @Test
  public void testDoorSerialization() {
    long uid = testContext.getStore().createNewEntityUID();
    Door door = (Door) entityFactory.getItem("door", 5, 10, uid);

    // Set rich state
    door.setSign("Tavern");
    door.lock.setLockDC(15);
    door.lock.lock();
    door.trap.setTrapDC(20);
    door.portal.setDestMap(123);
    door.portal.setDestZone(1);

    Door deserialized = serializeAndDeserialize(door);

    assertItemEquals(door, deserialized, ItemType.DOOR);
    assertDoorEquals(door, deserialized);
  }

  @Test
  public void testContainerSerialization() {
    long uid = testContext.getStore().createNewEntityUID();
    Container container = (Container) entityFactory.getItem("chest", 5, 10, uid);

    // Add items to container
    long item1Uid = testContext.getStore().createNewEntityUID();
    long item2Uid = testContext.getStore().createNewEntityUID();
    container.addItem(item1Uid);
    container.addItem(item2Uid);

    // Set lock and trap
    container.lock.setLockDC(10);
    container.lock.unlock();
    container.trap.setTrapDC(15);

    Container deserialized = serializeAndDeserialize(container);

    assertItemEquals(container, deserialized, ItemType.CONTAINER);
    assertContainerEquals(container, deserialized);
  }

  @Test
  public void testHominidSerialization() {
    long uid = testContext.getStore().createNewEntityUID();
    Creature hominid = entityFactory.getCreature("tengri2", 5, 10, uid);

    assertTrue(hominid instanceof Hominid, "Expected Hominid creature");

    // Set health and money
    hominid.getHealthComponent().heal(10);
    hominid.getInventoryComponent().addMoney(100);

    Creature deserialized = serializeAndDeserialize(hominid);

    assertCreatureEquals(hominid, deserialized, CreatureType.HOMINID);
    assertTrue(deserialized instanceof Hominid);
  }

  @Test
  public void testConstructSerialization() {
    long uid = testContext.getStore().createNewEntityUID();
    Creature construct = entityFactory.getCreature("gargoyle", 5, 10, uid);

    assertTrue(construct instanceof Construct, "Expected Construct creature");

    Creature deserialized = serializeAndDeserialize(construct);

    assertCreatureEquals(construct, deserialized, CreatureType.CONSTRUCT);
    assertTrue(deserialized instanceof Construct);
  }

  @Test
  public void testDragonSerialization() {
    long uid = testContext.getStore().createNewEntityUID();
    Creature dragon = entityFactory.getCreature("red dragon", 5, 10, uid);

    assertTrue(dragon instanceof Dragon, "Expected Dragon creature");

    Creature deserialized = serializeAndDeserialize(dragon);

    assertCreatureEquals(dragon, deserialized, CreatureType.DRAGON);
    assertTrue(deserialized instanceof Dragon);
  }

  @Test
  public void testDaemonSerialization() {
    long uid = testContext.getStore().createNewEntityUID();
    Creature daemon = entityFactory.getCreature("frost daemon", 5, 10, uid);

    assertTrue(daemon instanceof Daemon, "Expected Daemon creature");

    Creature deserialized = serializeAndDeserialize(daemon);

    assertCreatureEquals(daemon, deserialized, CreatureType.DAEMON);
    assertTrue(deserialized instanceof Daemon);
  }

  // ==================== TIER 3: EDGE CASES ====================

  @Test
  public void testCreatureWithInventory() {
    long uid = testContext.getStore().createNewEntityUID();
    Creature creature = entityFactory.getCreature("tengri2", 5, 10, uid);

    // Add items to inventory
    long item1Uid = testContext.getStore().createNewEntityUID();
    long item2Uid = testContext.getStore().createNewEntityUID();
    Item weapon = entityFactory.getItem("dagger", 0, 0, item1Uid);
    Item armor = entityFactory.getItem("leather chausses", 0, 0, item2Uid);

    creature.getInventoryComponent().addItem(weapon.getUID());
    creature.getInventoryComponent().addItem(armor.getUID());
    creature.getInventoryComponent().put(Slot.WEAPON, weapon.getUID());
    creature.getInventoryComponent().put(Slot.CUIRASS, armor.getUID());
    creature.getInventoryComponent().addMoney(50);

    Creature deserialized = serializeAndDeserialize(creature);

    assertCreatureEquals(creature, deserialized, CreatureType.HOMINID);
    assertEquals(
        creature.getInventoryComponent().getItems().size(),
        deserialized.getInventoryComponent().getItems().size());
    assertEquals(
        creature.getInventoryComponent().get(Slot.WEAPON),
        deserialized.getInventoryComponent().get(Slot.WEAPON));
    assertEquals(
        creature.getInventoryComponent().get(Slot.CUIRASS),
        deserialized.getInventoryComponent().get(Slot.CUIRASS));
  }

  @Test
  public void testCreatureWithScripts() {
    long uid = testContext.getStore().createNewEntityUID();
    Creature creature = entityFactory.getCreature("tengri2", 5, 10, uid);

    // Add scripts
    creature.getScriptComponent().addScript("death_script.js");
    creature.getScriptComponent().addScript("attack_script.js");

    Creature deserialized = serializeAndDeserialize(creature);

    assertCreatureEquals(creature, deserialized, CreatureType.HOMINID);
    assertEquals(
        creature.getScriptComponent().getScripts(), deserialized.getScriptComponent().getScripts());
  }

  @Test
  public void testItemWithOwner() {
    long ownerUid = testContext.getStore().createNewEntityUID();
    long itemUid = testContext.getStore().createNewEntityUID();
    Item item = entityFactory.getItem("dagger", 5, 10, itemUid);

    item.setOwner(ownerUid);

    Item deserialized = serializeAndDeserialize(item);

    assertItemEquals(item, deserialized, ItemType.WEAPON);
    assertEquals(ownerUid, deserialized.getOwner());
  }

  @Test
  public void testEmptyContainer() {
    long uid = testContext.getStore().createNewEntityUID();
    Container container = (Container) entityFactory.getItem("chest", 5, 10, uid);

    // Don't add any items
    assertTrue(container.getItems().isEmpty());

    Container deserialized = serializeAndDeserialize(container);

    assertItemEquals(container, deserialized, ItemType.CONTAINER);
    assertTrue(deserialized.getItems().isEmpty());
  }

  @Test
  public void testBulkSerializationPerformance() {
    long startTime = System.currentTimeMillis();

    // Serialize 200 entities
    for (int i = 0; i < 200; i++) {
      long uid = testContext.getStore().createNewEntityUID();
      if (i % 2 == 0) {
        // Create item
        String resourceId =
            itemResources.keySet().stream()
                .skip(i % itemResources.size())
                .findFirst()
                .orElse("dagger");
        Item item = entityFactory.getItem(resourceId, 0, 0, uid);
        serializeAndDeserialize(item);
      } else {
        // Create creature
        String resourceId =
            creatureResources.keySet().stream()
                .skip(i % creatureResources.size())
                .findFirst()
                .orElse("bandit");
        Creature creature = entityFactory.getCreature(resourceId, 0, 0, uid);
        serializeAndDeserialize(creature);
      }
    }

    long duration = System.currentTimeMillis() - startTime;
    System.out.println("Bulk serialization of 200 entities took: " + duration + "ms");

    assertTrue(duration < 2000, "Bulk serialization took too long: " + duration + "ms");
  }

  @Test
  public void testResourceCoverage() {
    // Verify we have the expected number of resources
    assertTrue(
        itemResources.size() >= 80,
        "Expected at least 80 item resources, got " + itemResources.size());
    assertTrue(
        creatureResources.size() >= 120,
        "Expected at least 120 creature resources, got " + creatureResources.size());
  }

  // ==================== HELPER METHODS ====================

  private <T extends Entity> T serializeAndDeserialize(T original) {
    WriteBuffer writeBuffer = new WriteBuffer();
    serializerFactory.writeEntityToWriteBuffer(writeBuffer, original);

    byte[] serialized = writeBuffer.getBuffer().array();
    ByteBuffer readBuffer = ByteBuffer.wrap(serialized);

    @SuppressWarnings("unchecked")
    T result = (T) serializerFactory.readEntityFromByteBuffer(readBuffer);
    return result;
  }

  private Map<String, RItem> buildItemResourceMap(ResourceManager rm) {
    return rm.getResources(RItem.class).stream()
        .filter(r -> !(r instanceof LItem))
        .collect(java.util.stream.Collectors.toMap(r -> r.id, r -> r));
  }

  private Map<String, RCreature> buildCreatureResourceMap(ResourceManager rm) {
    return rm.getResources(RCreature.class).stream()
        .filter(r -> !(r instanceof LCreature))
        .collect(java.util.stream.Collectors.toMap(r -> r.id, r -> r));
  }

  private ItemType determineItemType(RItem resource) {
    RItem.Type type = resource.type;
    return switch (type) {
      case weapon -> ItemType.WEAPON;
      case armor -> ItemType.ARMOR;
      case clothing -> ItemType.CLOTHING;
      case door -> ItemType.DOOR;
      case container -> ItemType.CONTAINER;
      case food -> ItemType.FOOD;
      case aid -> ItemType.AID;
      case light -> ItemType.LIGHT;
      case potion -> ItemType.POTION;
      case scroll -> ItemType.SCROLL;
      case book -> ItemType.BOOK;
      case coin -> ItemType.COIN;
      default -> ItemType.GENERIC;
    };
  }

  private CreatureType determineCreatureType(RCreature resource) {
    RCreature.Type type = resource.type;
    return switch (type) {
      case humanoid -> CreatureType.HOMINID;
      case construct -> CreatureType.CONSTRUCT;
      case dragon -> CreatureType.DRAGON;
      case daemon -> CreatureType.DAEMON;
      default -> CreatureType.GENERIC;
    };
  }

  // ==================== VERIFICATION METHODS ====================

  private void assertEntityBaseEquals(Entity expected, Entity actual) {
    assertNotSame(expected, actual, "Should be different instances");
    assertEquals(expected.getUID(), actual.getUID(), "UID should match");
    assertEquals(
        expected.getShapeComponent().getX(),
        actual.getShapeComponent().getX(),
        "X coordinate should match");
    assertEquals(
        expected.getShapeComponent().getY(),
        actual.getShapeComponent().getY(),
        "Y coordinate should match");
  }

  private void assertItemEquals(Item expected, Item actual, ItemType type) {
    assertEntityBaseEquals(expected, actual);
    assertEquals(expected.getOwner(), actual.getOwner(), "Owner should match");
    assertEquals(expected.resource.id, actual.resource.id, "Resource ID should match");

    // Type-specific checks
    switch (type) {
      case DOOR -> {
        if (expected instanceof Door expectedDoor && actual instanceof Door actualDoor) {
          assertDoorEquals(expectedDoor, actualDoor);
        }
      }
      case CONTAINER -> {
        if (expected instanceof Container expectedContainer
            && actual instanceof Container actualContainer) {
          assertContainerEquals(expectedContainer, actualContainer);
        }
      }
      case ARMOR -> {
        if (expected instanceof Armor expectedArmor && actual instanceof Armor actualArmor) {
          assertArmorEquals(expectedArmor, actualArmor);
        }
      }
      case WEAPON -> {
        if (expected instanceof Weapon expectedWeapon && actual instanceof Weapon actualWeapon) {
          assertWeaponEquals(expectedWeapon, actualWeapon);
        }
      }
    }
  }

  private void assertDoorEquals(Door expected, Door actual) {
    assertEquals(expected.hasSign(), actual.hasSign(), "Door sign presence should match");
    if (expected.hasSign()) {
      assertEquals(expected.toString(), actual.toString(), "Door sign should match");
    }
    assertEquals(expected.lock.isLocked(), actual.lock.isLocked(), "Lock state should match");
    assertEquals(expected.lock.getLockDC(), actual.lock.getLockDC(), "Lock DC should match");
    assertEquals(expected.trap.getTrapDC(), actual.trap.getTrapDC(), "Trap DC should match");
    assertEquals(
        expected.portal.getDestMap(),
        actual.portal.getDestMap(),
        "Portal destination map should match");
    assertEquals(
        expected.portal.getDestZone(),
        actual.portal.getDestZone(),
        "Portal destination zone should match");
  }

  private void assertContainerEquals(Container expected, Container actual) {
    assertEquals(
        expected.getItems().size(), actual.getItems().size(), "Container items count should match");
    assertEquals(expected.lock.isLocked(), actual.lock.isLocked(), "Lock state should match");
    assertEquals(expected.lock.getLockDC(), actual.lock.getLockDC(), "Lock DC should match");
    assertEquals(expected.trap.getTrapDC(), actual.trap.getTrapDC(), "Trap DC should match");

    // Verify all item UIDs match
    for (int i = 0; i < expected.getItems().size(); i++) {
      assertEquals(
          expected.getItems().get(i),
          actual.getItems().get(i),
          "Container item UID at index " + i + " should match");
    }
  }

  private void assertArmorEquals(Armor expected, Armor actual) {
    assertEquals(expected.getState(), actual.getState(), "Armor state should match");
  }

  private void assertWeaponEquals(Weapon expected, Weapon actual) {
    assertEquals(expected.getState(), actual.getState(), "Weapon state should match");
  }

  private void assertCreatureEquals(Creature expected, Creature actual, CreatureType type) {
    assertEntityBaseEquals(expected, actual);
    assertEquals(expected.getName(), actual.getName(), "Name should match");
    assertEquals(expected.species.id, actual.species.id, "Species ID should match");

    // Health component
    assertEquals(
        expected.getHealthComponent().getBaseHealth(),
        actual.getHealthComponent().getBaseHealth(),
        "Base health should match");
    assertEquals(
        expected.getHealthComponent().getHealth(),
        actual.getHealthComponent().getHealth(),
        "Current health should match");

    // Inventory
    assertEquals(
        expected.getInventoryComponent().getMoney(),
        actual.getInventoryComponent().getMoney(),
        "Money should match");
    assertEquals(
        expected.getInventoryComponent().getItems().size(),
        actual.getInventoryComponent().getItems().size(),
        "Inventory size should match");

    // Equipment slots
    for (Slot slot : Slot.values()) {
      assertEquals(
          expected.getInventoryComponent().get(slot),
          actual.getInventoryComponent().get(slot),
          "Equipment slot " + slot + " should match");
    }

    // Scripts
    assertEquals(
        expected.getScriptComponent().getScripts(),
        actual.getScriptComponent().getScripts(),
        "Scripts should match");

    // Type verification
    Class<?> expectedClass = getExpectedCreatureClass(type);
    assertTrue(
        expectedClass.isInstance(actual),
        "Creature should be of type " + expectedClass.getSimpleName());
  }

  private Class<?> getExpectedCreatureClass(CreatureType type) {
    return switch (type) {
      case HOMINID -> Hominid.class;
      case CONSTRUCT -> Construct.class;
      case DRAGON -> Dragon.class;
      case DAEMON -> Daemon.class;
      case GENERIC -> Creature.class;
    };
  }

  // ==================== METHOD SOURCES ====================

  private Stream<ItemTestCase> provideAllItemResources() throws Exception {
    // This will be populated during test execution

    return itemResources.entrySet().stream()
        .map(
            entry ->
                new ItemTestCase(
                    entry.getKey(), entry.getValue(), determineItemType(entry.getValue())));
  }

  private Stream<CreatureTestCase> provideAllCreatureResources() throws Exception {
    // This will be populated during test execution

    return creatureResources.entrySet().stream()
        .map(
            entry ->
                new CreatureTestCase(
                    entry.getKey(), entry.getValue(), determineCreatureType(entry.getValue())));
  }

  // ==================== TEST CASE RECORDS ====================

  record ItemTestCase(String resourceId, RItem resource, ItemType itemType) {
    @Override
    public String toString() {
      return resourceId + " (" + itemType + ")";
    }
  }

  record CreatureTestCase(String resourceId, RCreature resource, CreatureType type) {
    @Override
    public String toString() {
      return resourceId + " (" + type + ")";
    }
  }

  // ==================== ENUMS ====================

  enum ItemType {
    WEAPON,
    ARMOR,
    CLOTHING,
    DOOR,
    CONTAINER,
    FOOD,
    AID,
    LIGHT,
    POTION,
    SCROLL,
    BOOK,
    COIN,
    GENERIC
  }

  enum CreatureType {
    HOMINID,
    CONSTRUCT,
    DRAGON,
    DAEMON,
    GENERIC
  }
}
