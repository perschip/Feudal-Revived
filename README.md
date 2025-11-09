# Feudal RPG Plugin

A comprehensive Minecraft RPG plugin featuring kingdoms, professions, attributes, and honor-based PvP combat system.

## Features

### 🏰 Kingdom System
- Create and manage kingdoms with multiple members
- Claim and defend territories
- Kingdom capitals and treasury system
- Territory-based building protection

### ⚔️ Unique Challenge System
- **No Offline Raiding**: Players must be online to fight
- Challenge system prevents cowardly offline attacks
- Multiple challenge types: Land Conquest, Honor Duels, Resource Raids
- 24-hour challenge expiration system
- Both players must be online for combat to begin

### 📈 Profession & Attribute System
- **8 Professions**: Warrior, Miner, Farmer, Builder, Merchant, Scholar, Hunter, Blacksmith
- **6 Attributes**: Strength, Defense, Agility, Intelligence, Endurance, Luck
- Experience-based leveling system
- Profession bonuses affect combat and activities

### ⚡ Combat System
- Attribute-based damage scaling
- Profession bonuses in combat
- Anti-friendly fire within kingdoms
- Respawn at kingdom capitals
- Combat timeout system

## Commands

### Main Commands
- `/feudal help` - Show all available commands
- `/feudal stats [player]` - View player statistics

### Kingdom Management
- `/feudal kingdom create <name>` - Create a new kingdom
- `/feudal kingdom join <name>` - Join an existing kingdom
- `/feudal kingdom leave` - Leave your current kingdom
- `/feudal kingdom info` - View kingdom information

### Challenge System
- `/feudal challenge <player> <type>` - Challenge another player
  - Types: `land_conquest`, `honor_duel`, `resource_raid`
- `/feudal accept <challengeId>` - Accept a challenge
- `/feudal decline <challengeId>` - Decline a challenge
- `/feudal cancel <challengeId>` - Cancel your challenge

### Territory Management
- `/feudal territory claim` - Claim territory for your kingdom (leaders only)
- `/feudal territory info` - View territory information

### Character Development
- `/feudal profession [profession]` - View profession information
- `/feudal attribute [attribute]` - View attribute information

## Professions

1. **Warrior** - Masters of combat and warfare
2. **Miner** - Experts at extracting resources from the earth
3. **Farmer** - Skilled in agriculture and food production
4. **Builder** - Architects and construction specialists
5. **Merchant** - Traders and economic experts
6. **Scholar** - Researchers and knowledge seekers
7. **Hunter** - Skilled in tracking and survival
8. **Blacksmith** - Masters of metalworking and crafting

## Attributes

1. **Strength** - Increases damage dealt in combat
2. **Defense** - Reduces damage taken in combat
3. **Agility** - Affects movement speed and dodge chance
4. **Intelligence** - Improves experience gain and skill learning
5. **Endurance** - Increases health and stamina
6. **Luck** - Improves chances of finding rare items and resources

## Challenge Types

### Land Conquest
- Challenge to claim enemy territory
- Must be initiated while target is in their territory
- Winner gains control of the territory
- Requires both players to be online for combat

### Honor Duel
- Personal combat for honor and experience
- Can be initiated anywhere
- Winner gains experience and attribute points
- Great for settling disputes

### Resource Raid
- Attack to steal resources from enemy territory
- Target must be in their kingdom's territory
- Winner gains resources from the loser

## Installation

1. Download the latest release
2. Place the `Feudal.jar` file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/Feudal/config.yml`

## Requirements

- **Minecraft Version**: 1.21+
- **Server Software**: Paper, Spigot, or compatible
- **Java Version**: 21+

## Configuration

The plugin creates a `config.yml` file with various settings:

- Kingdom limits and costs
- Challenge timeouts and cooldowns
- Combat settings and damage scaling
- Experience rates and bonuses
- Territory protection settings

## Data Storage

Player data, kingdoms, territories, and challenges are stored in YAML files in the `plugins/Feudal/data/` directory:

- `players/` - Individual player data
- `kingdoms/` - Kingdom information
- `territories/` - Territory claims
- `challenges/` - Active and completed challenges

## Permissions

- `feudal.use` - Basic permission to use Feudal commands (default: true)
- `feudal.admin` - Administrative permissions (default: op)
- `feudal.kingdom.create` - Permission to create kingdoms (default: true)
- `feudal.kingdom.leader` - Permission to use kingdom leader commands (default: true)
- `feudal.challenge` - Permission to challenge other players (default: true)
- `feudal.territory.claim` - Permission to claim territory (default: true)

## Building from Source

1. Clone the repository
2. Ensure you have Java 21+ and Maven installed
3. Run `mvn clean package`
4. The compiled JAR will be in the `target/` directory

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

Fedual Is a private non-open sourced project. Feudal must be purchased in order to use.

## Support

For support, bug reports, or feature requests, please open an issue on the GitHub repository.

---

**Feudal RPG** - Creating better multiplayer PvP experiences through honor-based combat!
