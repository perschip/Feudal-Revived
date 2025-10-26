package dev.minefaze.feudal.models;

public enum ChallengeType {
    LAND_CONQUEST("Land Conquest", "Challenge to claim enemy territory"),
    HONOR_DUEL("Honor Duel", "Personal combat for honor and experience"),
    KINGDOM_WAR("Kingdom War", "Large scale conflict between kingdoms"),
    RESOURCE_RAID("Resource Raid", "Attack to steal resources from enemy territory");
    
    private final String displayName;
    private final String description;
    
    ChallengeType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    
    public static ChallengeType fromString(String name) {
        for (ChallengeType type : values()) {
            if (type.name().equalsIgnoreCase(name) || 
                type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
