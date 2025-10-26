# Feudal RPG Schematics

This folder contains schematic files for town halls and other structures in the Feudal RPG plugin.

## Folder Structure

```
schematics/
├── townhalls/
│   ├── medieval_castle/
│   │   ├── level_1.schem
│   │   ├── level_2.schem
│   │   └── ... (up to level_10.schem)
│   ├── mystical_tower/
│   │   ├── level_1.schem
│   │   ├── level_2.schem
│   │   └── ... (up to level_10.schem)
│   └── modern_fortress/
│       ├── level_1.schem
│       ├── level_2.schem
│       └── ... (up to level_10.schem)
├── nexus/
│   ├── medieval_nexus.schem
│   ├── mystical_nexus.schem
│   └── modern_nexus.schem
└── decorations/
    ├── flags/
    ├── walls/
    └── gates/
```

## Schematic Requirements

### File Format
- Use `.schem` format (WorldEdit 7+ format)
- Alternatively, `.schematic` format (legacy WorldEdit format) is supported

### Town Hall Schematics
- **Naming Convention**: `level_X.schem` where X is the town hall level (1-10)
- **Size Recommendations**:
  - Level 1-3: Small structures (10x10x10 or smaller)
  - Level 4-6: Medium structures (15x15x15)
  - Level 7-10: Large structures (20x20x20 or larger)
- **Origin Point**: The schematic should be designed with the origin at the base center
- **Materials**: Use materials appropriate for the theme (stone, wood, etc.)

### Nexus Schematics
- **Naming Convention**: `{type}_nexus.schem`
- **Size**: Recommended 5x5x5 to 10x10x10
- **Special Blocks**: The nexus core should be marked with a beacon or similar distinctive block

## How to Add Custom Schematics

1. **Create your structure** in-game using WorldEdit
2. **Select the structure** with WorldEdit wand (`//wand`)
3. **Copy the structure** (`//copy`)
4. **Save as schematic** (`//schem save filename`)
5. **Move the file** to the appropriate folder in this directory
6. **Restart the server** or use `/feudal reload` to load new schematics

## Schematic Building Behavior

- **Auto-Build**: If enabled in config, structures build automatically when town hall is created/upgraded
- **Manual Build**: Server owners can trigger builds with `/feudal townhall build`
- **Replacement**: Higher level schematics replace lower level ones during upgrades
- **Fallback**: If a schematic is missing, the plugin will use a default structure or skip building

## Tips for Creating Schematics

1. **Test in Creative**: Build and test your structures in creative mode first
2. **Consider Terrain**: Design structures that work on various terrain types
3. **Use Relative Positioning**: Design with the assumption that the structure will be placed at ground level
4. **Include Interior**: Add furniture and details to make structures feel alive
5. **Progressive Scaling**: Make higher levels visibly more impressive than lower levels

## Troubleshooting

- **Schematic Not Loading**: Check file permissions and naming convention
- **Structure Not Building**: Verify the schematic file is valid and not corrupted
- **Wrong Position**: Ensure the origin point is set correctly in the schematic
- **Missing Blocks**: Some modded blocks may not transfer correctly between servers

For support, check the plugin logs for schematic loading errors.
