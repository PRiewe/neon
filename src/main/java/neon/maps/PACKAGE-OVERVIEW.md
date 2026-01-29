# neon.maps Package - Comprehensive Technical Overview

## Package Purpose

The `neon.maps` package is the core map management system for the Neon roguelike engine. It handles:
- Hierarchical world/dungeon/zone structure
- Procedural dungeon and wilderness generation
- Map loading from XML and disk-backed caching with H2 MVStore
- Zone transitions and activation
- Dependency injection for testability

## Package Structure

```
neon.maps/
├── Core Map Classes (World, Dungeon, Zone, Region, Map)
├── Map Management (Atlas, MapLoader, ZoneFactory, ZoneActivator)
├── Utilities (MapUtils, Decomposer)
├── generators/          # Procedural generation algorithms
│   ├── DungeonGenerator
│   ├── DungeonTileGenerator
│   ├── WildernessGenerator
│   ├── MazeGenerator
│   ├── CaveGenerator
│   ├── RoomGenerator
│   ├── ComplexGenerator
│   ├── TownGenerator
│   ├── FeatureGenerator
│   ├── BlocksGenerator
│   └── WildernessTerrainGenerator
├── services/           # Dependency injection interfaces
│   ├── EntityStore
│   ├── ResourceProvider
│   ├── PhysicsManager
│   ├── QuestProvider
│   ├── MapAtlas
│   ├── GameContextEntityStore (impl)
│   └── GameContextResourceProvider (impl)
└── mvstore/           # H2 MVStore serialization
    ├── MvStoreFactory
    ├── MVUtils
    ├── ZoneType
    ├── RegionDataType
    ├── WorldDataType
    ├── MapDataType
    └── IntegerDataType
```

## Architecture

### Hierarchical Map Structure

The maps system uses a 4-level hierarchy:

```
Atlas (Map Cache/Manager)
  └─ Map (interface)
      ├─ World (single-zone outdoor map)
      │   └─ Zone (playable area)
      │       └─ Region (terrain rectangles)
      └─ Dungeon (multi-zone indoor map)
          └─ Zone[] (multiple connected zones)
              └─ Region (terrain rectangles)
```

### Core Classes

#### Atlas
**Purpose**: Central map cache and manager
**Key Responsibilities**:
- Disk-backed map caching using H2 MVStore
- Current map/zone tracking
- Zone transition handling
- Map loading/saving coordination

**Key Fields**:
- `atlasMapStore`: MVStore instance for persistent storage
- `maps`: In-memory map cache
- `currentMap`, `currentZone`: Active map and zone references
- `mapLoader`: Handles XML map loading
- `zoneFactory`: Creates/serializes zones
- `zoneActivator`: Activates zones (registers entities with physics)

**Key Methods**:
- `getCurrentMap()`, `getCurrentZone()`: Access active map/zone
- `setCurrentZone(int)`: Switch zones, triggers activation
- `enterZone(Door)`: Handle door transitions between zones
- `putMapIfNeeded(String)`: Load map if not cached
- `close()`: Persist all data and close MVStore

#### Map (interface)
**Purpose**: Common interface for World and Dungeon
**Methods**: `getName()`, `setName()`, `getUID()`, `getZone(int)`, `getZones()`

#### World
**Purpose**: Single-zone outdoor map (e.g., wilderness areas)
**Key Fields**:
- `name`: Map name
- `uid`: Unique identifier
- `zone`: Single zone containing the playable area

**Usage**: Typically used for town exteriors, wilderness regions, overworld

#### Dungeon
**Purpose**: Multi-zone indoor map with connections
**Key Fields**:
- `name`: Dungeon name
- `uid`: Unique identifier
- `zones`: List of zones (dungeon levels)
- `zoneFactory`: For zone creation/serialization

**Key Methods**:
- `addZone(Zone)`: Add a new level
- `addConnection(int, int, Door)`: Link zones via doors
- `getConnections(int)`: Get doors connecting to a zone

**Usage**: Multi-level dungeons, caves, buildings with multiple floors

#### Zone
**Purpose**: Playable area containing terrain, creatures, and items
**Key Fields**:
- `name`: Zone name (e.g., "Dungeon Level 1")
- `map`: Parent map reference
- `index`: Zone number within dungeon
- `theme`: Optional RZoneTheme for procedural generation
- `regions`: Set of terrain regions
- `creatures`: Spatial index of creatures
- `items`: Spatial index of items
- `lights`: Spatial index of light sources
- `top`: Top rendering layer elements

**Key Methods**:
- `getRenderables(Rectangle)`: Get all renderable objects in area
- `getCreatures(Rectangle)`, `getItems(Rectangle)`: Spatial queries
- `addCreature(Creature)`, `addItem(Item)`: Add entities
- `removeCreature(long)`, `removeItem(long)`: Remove entities
- `isRandom()`: Check if procedurally generated
- `fix()`: Finalize zone after generation

**Spatial Indexing**: Uses R-tree spatial indexing for efficient entity queries

#### Region
**Purpose**: Rectangular terrain area with properties
**Key Fields**:
- `id`: Terrain ID (e.g., "grass", "stone_wall")
- `label`: Display name
- `x, y, z`: Position and layer
- `width, height`: Dimensions
- `terrain`: Associated RTerrain resource
- `scripts`: List of scripts to execute
- `theme`: Optional RRegionTheme for randomization

**Key Methods**:
- `getBounds()`: Get rectangular bounds
- `getColor()`: Terrain color
- `getMovMod()`: Movement modifier
- `isFixed()`: Check if randomization is locked
- `paint(Graphics2D, int, int, int)`: Render the region
- `addScript(String)`, `removeScript(String)`: Manage scripts

**Nested Class**:
- `RegionBuilder`: Builder pattern for region construction

### Map Loading and Management

#### MapLoader
**Purpose**: Load maps from XML files
**Key Fields**:
- `entityStore`: For entity creation and UID management
- `resourceProvider`: For resource lookup
- `entityFactory`: Creates entities from resources

**Key Methods**:
- `loadMap(String)`: Load map by name (determines World vs Dungeon)
- `loadWorld(Element)`: Parse World XML
- `loadDungeon(Element)`: Parse Dungeon XML
- `loadThemedDungeon(RDungeonTheme)`: Create themed dungeon structure
- `loadZone(Element, Map, int)`: Parse zone XML
- `loadRegion(Element)`: Parse region XML
- `loadDoor(Element, Zone)`, `loadContainer(Element, Zone)`: Load entities

**XML Format**:
```xml
<!-- World (single zone) -->
<world name="Town" uid="123">
  <zone>...</zone>
</world>

<!-- Dungeon (multi-zone) -->
<dungeon name="Cave" uid="456">
  <level>...</level>
  <level>...</level>
</dungeon>

<!-- Themed dungeon (procedural) -->
<dungeon name="RandomCave" theme="cave_theme" uid="789"/>
```

#### ZoneFactory
**Purpose**: Zone serialization/deserialization with MVStore
**Key Fields**:
- `cache`: MVStore map for zones
- `uidStore`: For UID generation
- `resourceManager`: For resource lookup
- `regionDataType`: Custom serializer for regions

**Key Methods**:
- `createZone(String, Map, int)`: Create empty zone
- `createZoneWithTheme(String, Map, int, RZoneTheme)`: Create themed zone
- `readZoneFromExternal(int)`: Load zone from disk
- `writeZoneToExternal(Zone, WriteBuffer)`: Save zone to disk
- `close()`: Persist and close cache

**Serialization**: Uses H2 MVStore custom DataType implementations for efficient binary serialization

#### ZoneActivator
**Purpose**: Activate a zone for gameplay
**Key Fields**:
- `physicsManager`: Physics system interface
- `gameStore`: Game context entity store

**Key Methods**:
- `activateZone(Zone)`: Clear physics, register all zone entities

**Activation Process**:
1. Clear all physics bodies
2. Register all creatures in zone
3. Register all items in zone
4. Ready for gameplay

### Utilities

#### MapUtils
**Purpose**: Random generation utilities
**Key Fields**:
- `randomSource`: Random number generator (supports seeding)

**Key Methods**:
- `withSeed(long)`: Create seeded instance
- `randomRectangle(Rectangle, int, int)`: Random rectangle within bounds
- `randomSquare(Rectangle, int)`: Random square
- `randomPolygon(Rectangle, int)`: Random polygon
- `randomPoint(Rectangle)`: Random point
- `randomRibbon(Rectangle, int)`: Random ribbon shape
- `random()`: Get random double
- `amount(int, int)`: Random amount between min/max
- `average(int...)`: Average of values
- `reverse(int[][])`: Flip tile array

**Usage**: Extensively used by all generators for random placement and shapes

#### Decomposer
**Purpose**: Binary Space Partitioning (BSP) for dungeon generation
**Key Methods**:
- `split(Rectangle, int, int)`: Recursively split rectangle

**Nested Classes**:
- `Tree`: BSP tree structure
- `Node`: Tree node (leaf or parent)

**Usage**: Used by ComplexGenerator for BSP dungeon layouts

### Tile Constants (MapUtils)

Generators use integer constants for tile types:
- `WALL = 0`: Solid wall
- `FLOOR = 1`: Walkable floor
- `DOOR = 2`: Open door
- `DOOR_CLOSED = 3`: Closed door
- `DOOR_LOCKED = 4`: Locked door
- `CORRIDOR = 5`: Corridor tile
- `WALL_ROOM = 6`: Room perimeter wall
- `ENTRY = 7`: Entry point
- `CORNER = 8`: Corner marker
- `TEMP = 9`: Temporary marker

These are converted to terrain string IDs (e.g., "stone_floor", "stone_wall") by generators.

## Procedural Generation System

### Generation Pipeline

The procedural generation system follows a consistent pipeline:

1. **Tile Generation** (`generateTiles()`): Create integer tile array
2. **Terrain Conversion** (`makeTerrain()`): Convert tiles to terrain IDs
3. **Feature Generation** (`generateFeatures()`): Add lakes, rivers, patches
4. **Engine Content** (`generateEngineContent()`): Create Region/Entity objects
5. **Finalization** (`generate()`): Place doors, connections, quest objects

### Generators

#### DungeonGenerator
**Purpose**: Main coordinator for dungeon zone generation
**Key Fields**:
- `theme`: RZoneTheme defining generation parameters
- `zone`: Target zone to populate
- `dungeonTileGenerator`: Delegates tile generation
- `tiles`: Generated tile array
- `terrain`: Converted terrain string array

**Key Methods**:
- `generate(Point)`: Full generation pipeline with entry point
- `generateEngineContent()`: Convert terrain/tiles to regions and entities
- `addDoor(Point)`, `addCreature(Point, LCreature)`, `addItem(Point, LItem)`: Entity placement

**Generation Flow**:
```
generate(Point)
  ├─> dungeonTileGenerator.generateTiles() → int[][]
  ├─> dungeonTileGenerator.makeTerrain() → String[][]
  ├─> dungeonTileGenerator.generateFeatures()
  ├─> generateEngineContent() → creates Regions + Entities
  └─> addDoor(entry), placeQuestObjects()
```

#### DungeonTileGenerator
**Purpose**: Generate base tile layout using various algorithms
**Key Fields**:
- `theme`: Generation parameters
- `blocksGenerator`, `complexGenerator`, `caveGenerator`, `mazeGenerator`: Algorithm delegates
- `featureGenerator`: Adds natural features

**Key Methods**:
- `generateTiles()`: Main entry point, delegates to algorithm
- `generateBaseTiles()`: Apply specific algorithm based on theme
- `generateFeatures()`: Add lakes, rivers, patches
- `makeTerrain(int[][])`: Convert tiles to terrain IDs
- `exposed(int, int)`: Check if tile is exposed (adjacent to floor)

**Algorithms** (based on `RZoneTheme.type`):
- `MAZE`: Standard maze (MazeGenerator)
- `SQUASHED`: Squashed maze for caves (MazeGenerator)
- `OPEN_CAVE`: Cellular automata cave (CaveGenerator)
- `SPARSE`: Sparse rooms with corridors (ComplexGenerator)
- `BSP`: Binary space partitioned (ComplexGenerator)
- `PACKED`: Densely packed rooms (ComplexGenerator)

**Nested Class**:
- `DungeonLayout`: Holds tiles, doors, creatures, items

#### MazeGenerator
**Purpose**: Generate maze and squashed maze patterns
**Key Fields**:
- `dice`: Random number generator

**Key Methods**:
- `generateMaze(int, int)`: Standard recursive backtracker maze
- `generateSquashedMaze(int, int, int)`: Maze squashed vertically for caves
- `generateBlock(int, int)`: Create maze block

**Algorithm**: Recursive backtracking maze generation

**Nested Class**:
- `Block`: Individual maze cell

#### CaveGenerator
**Purpose**: Generate open cave layouts using cellular automata
**Key Fields**:
- `mazeGenerator`: For fallback maze generation

**Key Methods**:
- `generateOpenCave(int, int)`: Cellular automata cave
- `makeTiles(int[][])`: Apply cave generation rules

**Algorithm**:
1. Random initial state (45% walls)
2. Cellular automata rules (4-5 rule)
3. Multiple iterations for smoothing
4. Flood fill to ensure connectivity

#### RoomGenerator
**Purpose**: Generate individual room shapes
**Key Fields**:
- `mapUtils`: Random utilities

**Key Methods**:
- `makeRoom(Rectangle, Dice)`: Rectangular room
- `makePolyRoom(Rectangle, MapUtils)`: Polygonal room
- `makeCaveRoom(Rectangle, int[][])`: Irregular cave-like room
- `applyFloor(int[][], Room)`: Place room in tile array
- `isCorner(int[][])`, `exposed(int[][])`: Helper methods

**Room Types**:
- Rectangular: Simple rectangular rooms
- Polygonal: Irregular polygon rooms
- Cave: Organic cave-like rooms using cellular automata

**Nested Class**:
- `Room`: Holds floor tiles, corner markers, perimeter

#### ComplexGenerator
**Purpose**: Generate complex multi-room layouts
**Key Fields**:
- `mapUtils`: Random utilities
- `blocksGenerator`: For block placement
- `roomGenerator`: For individual rooms

**Key Methods**:
- `generateBSPDungeon(int, int, MapUtils)`: Binary space partitioned
- `generateSparseDungeon(int, int, MapUtils)`: Sparse rooms with corridors
- `generatePackedDungeon(int, int, MapUtils)`: Densely packed rooms
- `connect(int[][], Set<Room>)`: Connect rooms with corridors
- `floodFill(int[][], int, int)`: Ensure connectivity

**Algorithms**:
- **BSP**: Recursively split space, room in each leaf, connect siblings
- **Sparse**: Place rooms randomly, connect with corridors
- **Packed**: Place rooms densely, connect adjacent rooms

#### TownGenerator
**Purpose**: Generate town layouts (not fully implemented in standard pipeline)
**Key Fields**:
- `zone`: Target zone
- `entityStore`, `resourceProvider`: Services
- `itemFactory`: For item creation

**Key Methods**:
- `generate(RZoneTheme)`: Generate town from theme
- `makeDoor(Region, Region)`: Connect regions with door

**Usage**: Specialized generator for town zones

#### WildernessGenerator
**Purpose**: Generate outdoor wilderness zones
**Key Fields**:
- `zone`: Target zone
- `terrain`: Terrain array
- `wildernessTerrainGenerator`: Delegates terrain generation
- `blocksGenerator`, `caveGenerator`: For feature placement

**Key Methods**:
- `generate(Point)`: Full generation with entry point
- `generateEngineContent()`: Create regions and entities
- `decompose(Rectangle)`: BSP decomposition for region variety
- `divide(Tree, int, MapUtils)`: Subdivide for varied terrain
- `addCreatures()`: Place creatures by terrain type

**Generation Process**:
1. Generate base terrain grid
2. BSP decompose for region variety
3. Apply region themes for randomization
4. Add creatures based on terrain
5. Create Region objects

#### WildernessTerrainGenerator
**Purpose**: Generate wilderness terrain patterns
**Key Methods**: Various terrain generation algorithms (implementation details in source)

#### FeatureGenerator
**Purpose**: Generate natural features (lakes, rivers)
**Key Fields**:
- `mapUtils`: Random utilities

**Key Methods**:
- `generateLake(int[][], Rectangle, int, String)`: Place lake feature
- `generateRiver(int[][], Rectangle, String)`: Place river feature
- `generateRiverPolygon(int[][])`: Create river polygon shape

**Usage**: Called by DungeonTileGenerator to add variety

#### BlocksGenerator
**Purpose**: Place rectangular block features
**Key Methods**: Block placement utilities (implementation details in source)

**Usage**: Used by ComplexGenerator and other generators for block operations

## Service Layer (Dependency Injection)

### Purpose

The `services` subpackage provides abstractions over Engine subsystems to:
- Reduce coupling between maps and Engine singleton
- Enable unit testing with mock implementations
- Allow future refactoring without breaking maps code

### Service Interfaces

#### EntityStore
**Purpose**: Entity storage and UID management
**Methods**:
- `getEntity(long)`: Retrieve entity by UID
- `addEntity(Entity)`: Store entity
- `createNewEntityUID()`: Generate new entity UID
- `createNewMapUID()`: Generate new map UID
- `getMapPath()`: Get map file path

**Implementation**: `GameContextEntityStore` delegates to `GameContext` (Engine wrapper)

#### ResourceProvider
**Purpose**: Resource lookup
**Methods**:
- `getResource(String, Class<T>)`: Get resource by ID and type
- `getResource(String)`: Get resource by ID (any type)
- `getResources(Class<T>)`: Get all resources of type

**Implementation**: `GameContextResourceProvider` delegates to `ResourceManager`

#### PhysicsManager
**Purpose**: Physics system management
**Methods**:
- `clear()`: Clear all physics bodies
- `register(Creature)`: Register creature with physics
- `register(Item)`: Register item with physics

**Implementation**: Delegates to `PhysicsSystem`

#### QuestProvider
**Purpose**: Quest tracking
**Methods**:
- `getNextRequestedObject(String)`: Get next quest object for resource ID

**Implementation**: Delegates to `QuestTracker`

#### MapAtlas
**Purpose**: Map lookup (for cross-references)
**Methods**:
- `getMap(long)`: Get map by UID

**Implementation**: Delegates to `Atlas`

### Dependency Injection Pattern

**Old Pattern** (Engine singleton):
```java
public DungeonGenerator(RZoneTheme theme, Zone zone) {
    this.theme = theme;
    this.zone = zone;
    // Direct Engine singleton access
    Engine engine = Engine.getInstance();
    this.entities = engine.getEntities();
}
```

**New Pattern** (Constructor injection):
```java
public DungeonGenerator(
    RZoneTheme theme,
    Zone zone,
    EntityStore entityStore,
    ResourceProvider resourceProvider,
    QuestProvider questProvider,
    GameContext gameContext
) {
    this.theme = theme;
    this.zone = zone;
    this.entityStore = entityStore;
    this.resourceProvider = resourceProvider;
    this.questProvider = questProvider;
    this.gameContext = gameContext;
}
```

**Compatibility**: Most classes maintain deprecated constructors using Engine singleton for backward compatibility during migration.

## MVStore Serialization System

### Purpose

The `mvstore` subpackage provides custom H2 MVStore DataType implementations for efficient binary serialization of map objects.

### H2 MVStore Overview

H2 MVStore is a persistent, disk-backed key-value store supporting:
- ACID transactions
- Custom serialization via DataType interface
- Memory-mapped files for performance
- Automatic compaction

### Custom DataType Classes

#### MvStoreFactory
**Purpose**: Factory for creating MVStore instances
**Usage**: Centralized MVStore creation and configuration

#### MVUtils
**Purpose**: Common serialization utilities
**Methods**:
- `writeString(WriteBuffer, String)`: Write string with length prefix
- `readString(ByteBuffer)`: Read length-prefixed string

#### ZoneType (implements DataType<Zone>)
**Purpose**: Serialize/deserialize Zone objects
**Key Fields**:
- `zoneFactory`: For zone creation

**Key Methods**:
- `write(WriteBuffer, Zone)`: Serialize zone to buffer
- `read(ByteBuffer)`: Deserialize zone from buffer
- `getMemory(Zone)`: Estimate memory usage
- `compare(Zone, Zone)`: Compare zones (for ordering)

**Serialization Format**: Delegates to `ZoneFactory.writeZoneToWriteBuffer()`

#### RegionDataType (implements DataType<Region>)
**Purpose**: Serialize/deserialize Region objects
**Key Fields**:
- `resourceProvider`: For terrain lookup

**Serialization Format**:
- Terrain ID (String)
- Position (x, y, z)
- Dimensions (width, height)
- Scripts (String[])
- Theme ID (String, optional)

#### WorldDataType (implements DataType<World>)
**Purpose**: Serialize/deserialize World objects
**Key Fields**:
- `zoneFactory`: For zone serialization
- `zoneType`: For zone DataType

**Serialization Format**:
- Name (String)
- UID (long)
- Zone (serialized Zone)

#### MapDataType (implements DataType<Map>)
**Purpose**: Polymorphic serialization for Map interface
**Key Fields**:
- `worldDataType`, `dungeonDataType`: Concrete type serializers
- `WORLDTYPE = 1`, `DUNGEONTYPE = 2`: Type discriminators

**Serialization Format**:
- Type byte (1 = World, 2 = Dungeon)
- Concrete type data

**Polymorphism**: Switches on type byte to delegate to correct DataType

#### IntegerDataType (implements DataType<Integer>)
**Purpose**: Serialize Integer objects (for map indices)

#### Dungeon.DungeonDataType (inner class)
**Purpose**: Serialize/deserialize Dungeon objects

**Serialization Format**:
- Name (String)
- UID (long)
- Zone count (int)
- Zones (serialized Zone[])
- Connections (Map<Integer, Set<Door>>)

### MVStore Usage in Atlas

```java
// Open/create MVStore
MVStore store = MVStore.open(fileName);

// Create map with custom DataType
MVMap<Integer, Zone> zoneCache = store.openMap(
    "zones",
    new MVMap.Builder<Integer, Zone>()
        .keyType(new IntegerDataType())
        .valueType(new ZoneType(zoneFactory))
);

// Use as normal map
zoneCache.put(0, zone);
Zone loaded = zoneCache.get(0);

// Changes auto-persist
store.commit();

// Close and flush
store.close();
```

### Memory Estimation

DataType implementations provide `getMemory()` for cache management:
- Helps MVStore estimate memory usage
- Enables automatic eviction policies
- Important for large zones with many entities

**Zone.getEstimatedMemory()**:
```java
long getEstimatedMemory() {
    long memory = 1000; // Base overhead
    memory += regions.size() * 500;
    memory += creatures.size() * 1000;
    memory += items.size() * 800;
    return memory;
}
```

## Key Workflows

### Loading a Map

```
User requests map "dungeon_cave"
  ↓
Atlas.putMapIfNeeded("dungeon_cave")
  ↓
Check MVStore cache
  ├─ Cache hit → return cached map
  └─ Cache miss ↓
      MapLoader.loadMap("dungeon_cave")
        ↓
      Read XML from darkness/maps/dungeon_cave.xml
        ↓
      Determine type (World or Dungeon)
        ↓
      MapLoader.loadDungeon(Element) or loadWorld(Element)
        ↓
      Create Map, Zones, Regions, Entities
        ↓
      Store in MVStore cache
        ↓
      Return Map
```

### Generating a Themed Dungeon Zone

```
Atlas.enterZone(Door) → new themed zone
  ↓
ZoneFactory.createZoneWithTheme(RZoneTheme)
  ↓
DungeonGenerator(theme, zone, services)
  ↓
DungeonGenerator.generate(entryPoint)
  ├─> DungeonTileGenerator.generateTiles()
  │     ├─> Select algorithm (MAZE/BSP/CAVE/etc)
  │     ├─> Generate int[][] tile array
  │     └─> Return tiles
  ├─> DungeonTileGenerator.makeTerrain(tiles)
  │     └─> Convert int[][] → String[][] terrain IDs
  ├─> DungeonTileGenerator.generateFeatures()
  │     └─> Add lakes, rivers, patches
  ├─> DungeonGenerator.generateEngineContent()
  │     ├─> Create Region objects from terrain
  │     ├─> Place creatures from theme
  │     ├─> Place items from theme
  │     └─> Add to zone
  └─> DungeonGenerator.generate() finalization
        ├─> Add entry door
        ├─> Place quest objects
        └─> Fix zone (finalize)
```

### Zone Activation

```
Atlas.setCurrentZone(3)
  ↓
ZoneActivator.activateZone(zone)
  ↓
PhysicsManager.clear()
  ↓
For each creature in zone:
  PhysicsManager.register(creature)
  ↓
For each item in zone:
  PhysicsManager.register(item)
  ↓
Zone ready for gameplay
```

### Saving Maps

```
Game save triggered
  ↓
Atlas persists all maps to MVStore
  ├─> For each modified map
  │     ├─> MapDataType.write(map)
  │     │     ├─> Write type discriminator
  │     │     └─> Delegate to WorldDataType or DungeonDataType
  │     ├─> DungeonDataType.write(dungeon)
  │     │     ├─> Write name, UID
  │     │     ├─> For each zone:
  │     │     │     └─> ZoneType.write(zone)
  │     │     │           └─> ZoneFactory.writeZoneToWriteBuffer()
  │     │     └─> Write connections
  │     └─> Store in MVStore map
  └─> MVStore.commit()
        └─> Flush to disk
```

## Design Patterns

### Builder Pattern
- `Region.RegionBuilder`: Fluent API for region construction

### Factory Pattern
- `ZoneFactory`: Creates zones with proper initialization
- `MvStoreFactory`: Creates MVStore instances

### Strategy Pattern
- Generator algorithms: Interchangeable generation strategies
- DataType implementations: Pluggable serialization strategies

### Dependency Injection
- Service interfaces: Decouple from Engine singleton
- Constructor injection: Explicit dependencies

### Template Method
- `DungeonGenerator.generate()`: Template for generation pipeline
- Subclasses override specific steps

### Spatial Indexing
- R-tree for entities: Efficient spatial queries
- Enables fast rendering and collision detection

## Testing Considerations

### Unit Testing

**With Service Interfaces**:
```java
@Test
void testDungeonGeneration() {
    EntityStore mockStore = mock(EntityStore.class);
    ResourceProvider mockResources = mock(ResourceProvider.class);
    QuestProvider mockQuests = mock(QuestProvider.class);
    GameContext mockContext = mock(GameContext.class);

    Zone zone = new Zone("Test", null, 0);
    RZoneTheme theme = createTestTheme();

    DungeonGenerator generator = new DungeonGenerator(
        theme, zone, mockStore, mockResources, mockQuests, mockContext
    );

    generator.generate(new Point(5, 5));

    assertFalse(zone.getRegions().isEmpty());
    verify(mockStore).createNewEntityUID();
}
```

**Seed-based Testing**:
```java
@Test
void testDeterministicGeneration() {
    MapUtils utils = MapUtils.withSeed(12345L);

    MazeGenerator gen1 = new MazeGenerator(utils);
    int[][] maze1 = gen1.generateMaze(20, 20);

    MapUtils utils2 = MapUtils.withSeed(12345L);
    MazeGenerator gen2 = new MazeGenerator(utils2);
    int[][] maze2 = gen2.generateMaze(20, 20);

    assertArrayEquals(maze1, maze2); // Same seed = same maze
}
```

### Integration Testing

**MVStore Persistence**:
```java
@Test
void testZonePersistence() {
    // Create and save zone
    Zone original = createTestZone();
    ZoneFactory factory = new ZoneFactory(uidStore, resourceManager);
    factory.writeZoneToExternal(original, writeBuffer);

    // Load zone
    Zone loaded = factory.readZoneFromExternal(0);

    assertEquals(original.getName(), loaded.getName());
    assertEquals(original.getRegions().size(), loaded.getRegions().size());
}
```

## Performance Considerations

### Memory Management
- MVStore provides disk-backed caching (reduces memory for large worlds)
- Spatial indices (R-tree) enable efficient queries
- Zone activation clears inactive zone entities from physics
- `getEstimatedMemory()` helps MVStore manage cache

### Lazy Loading
- Maps loaded on-demand via `Atlas.putMapIfNeeded()`
- Themed dungeon zones generated on first entry
- MVStore pages loaded on access

### Generation Performance
- BSP algorithms: O(n log n)
- Maze generation: O(width × height)
- Cellular automata: O(iterations × width × height)
- Spatial queries: O(log n) with R-tree

### Caching
- Atlas maintains in-memory map cache
- MVStore provides persistent cache across sessions
- Zone cache in ZoneFactory reduces serialization overhead

## Common Tasks

### Adding a New Generator Algorithm

1. Create new generator class in `neon.maps.generators`
2. Implement generation method returning `int[][]` tiles
3. Add algorithm to `DungeonTileGenerator.generateBaseTiles()`
4. Add algorithm type to `RZoneTheme.Type` enum
5. Test with seed-based deterministic tests

### Adding a New Map Type

1. Implement `Map` interface
2. Create corresponding `DataType` implementation
3. Add to `MapDataType` with new type discriminator
4. Update `MapLoader` to handle new type
5. Add serialization tests

### Adding a New Service Interface

1. Create interface in `neon.maps.services`
2. Add methods needed by generators/loaders
3. Create implementation delegating to Engine/subsystem
4. Update relevant classes to accept interface in constructor
5. Maintain deprecated constructor for backward compatibility

### Debugging Generation Issues

1. Use seeded `MapUtils` for reproducible generation
2. Add logging to generation pipeline steps
3. Visualize tile arrays as ASCII art
4. Check tile constant usage (WALL, FLOOR, etc.)
5. Verify terrain ID conversion in `makeTerrain()`
6. Validate region bounds and positions

## Related Packages

- `neon.entities`: Entity classes (Creature, Item) placed in zones
- `neon.resources`: Resource definitions (RZoneTheme, RDungeonTheme, RTerrain)
- `neon.systems.physics`: Physics system (PhysicsSystem) for collision detection
- `neon.narrative`: Quest system (QuestTracker) for quest object placement
- `neon.util.spatial`: Spatial indexing (RTree, QuadTree) for entity queries
- `neon.editor.editors`: Map editor components

## Migration Notes

### Java 21 Migration
- MVStore replaces MapDB/JDBM for persistence
- Custom DataType implementations required for serialization
- Updated to use modern Java constructs

### Engine Singleton Removal (In Progress)
- Service interfaces introduced for dependency injection
- Deprecated constructors maintained for backward compatibility
- New code should use service-based constructors

## Future Improvements

Potential areas for enhancement:
- More generator algorithms (cellular automata variations, noise-based)
- Dynamic region themes (regions change over time)
- Multi-threaded generation for large zones
- Improved connectivity algorithms
- Region scripting system expansion
- Enhanced spatial partitioning for massive zones
- Procedural quest integration in generation

## References

- Main CLAUDE.md: `/home/coder/neon/CLAUDE.md`
- Package CLAUDE.md: `/home/coder/neon/src/main/java/neon/maps/CLAUDE.md`
- H2 MVStore Documentation: https://www.h2database.com/html/mvstore.html
- BSP Dungeon Generation: https://www.roguebasin.com/index.php/Basic_BSP_Dungeon_generation
- Cellular Automata Caves: https://www.roguebasin.com/index.php/Cellular_Automata_Method_for_Generating_Random_Cave-Like_Levels
