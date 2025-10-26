package dev.minefaze.feudal.models;

public enum TerritoryType {
    CAPITAL("Capital", "The heart of the kingdom", 50),
    FORTRESS("Fortress", "A heavily defended military outpost", 30),
    TOWN("Town", "A populated settlement", 20),
    FARM("Farm", "Agricultural land for food production", 10),
    MINE("Mine", "Resource extraction site", 15),
    OUTPOST("Outpost", "A basic territorial claim", 5);
    
    private final String displayName;
    private final String description;
    private final int defenseBonus;
    
    TerritoryType(String displayName, String description, int defenseBonus) {
        this.displayName = displayName;
        this.description = description;
        this.defenseBonus = defenseBonus;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getDefenseBonus() { return defenseBonus; }
    
    public static TerritoryType fromString(String name) {
        for (TerritoryType type : values()) {
            if (type.name().equalsIgnoreCase(name) || 
                type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
