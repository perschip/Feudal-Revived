package dev.minefaze.feudal.models;

public enum Profession {
    WARRIOR("Warrior", "Masters of combat and warfare"),
    MINER("Miner", "Experts at extracting resources from the earth"),
    FARMER("Farmer", "Skilled in agriculture and food production"),
    BUILDER("Builder", "Architects and construction specialists"),
    MERCHANT("Merchant", "Traders and economic experts"),
    SCHOLAR("Scholar", "Researchers and knowledge seekers"),
    HUNTER("Hunter", "Skilled in tracking and survival"),
    BLACKSMITH("Blacksmith", "Masters of metalworking and crafting");
    
    private final String displayName;
    private final String description;
    
    Profession(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    
    public static Profession fromString(String name) {
        for (Profession profession : values()) {
            if (profession.name().equalsIgnoreCase(name) || 
                profession.displayName.equalsIgnoreCase(name)) {
                return profession;
            }
        }
        return null;
    }
}
