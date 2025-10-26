package dev.minefaze.feudal.models;

public enum Attribute {
    STRENGTH("Strength", "Increases damage dealt in combat"),
    DEFENSE("Defense", "Reduces damage taken in combat"),
    AGILITY("Agility", "Affects movement speed and dodge chance"),
    INTELLIGENCE("Intelligence", "Improves experience gain and skill learning"),
    ENDURANCE("Endurance", "Increases health and stamina"),
    LUCK("Luck", "Improves chances of finding rare items and resources");
    
    private final String displayName;
    private final String description;
    
    Attribute(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    
    public static Attribute fromString(String name) {
        for (Attribute attribute : values()) {
            if (attribute.name().equalsIgnoreCase(name) || 
                attribute.displayName.equalsIgnoreCase(name)) {
                return attribute;
            }
        }
        return null;
    }
}
