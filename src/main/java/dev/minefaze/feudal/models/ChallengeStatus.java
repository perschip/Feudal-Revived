package dev.minefaze.feudal.models;

public enum ChallengeStatus {
    PENDING("Pending", "Challenge has been issued but not yet responded to"),
    ACCEPTED("Accepted", "Challenge has been accepted and is waiting for both players to be online"),
    IN_PROGRESS("In Progress", "Challenge is currently active with both players fighting"),
    COMPLETED("Completed", "Challenge has finished with a winner"),
    CANCELLED("Cancelled", "Challenge was cancelled by one of the participants"),
    EXPIRED("Expired", "Challenge expired without being accepted"),
    DECLINED("Declined", "Challenge was declined by the target player");
    
    private final String displayName;
    private final String description;
    
    ChallengeStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    
    public static ChallengeStatus fromString(String name) {
        for (ChallengeStatus status : values()) {
            if (status.name().equalsIgnoreCase(name) || 
                status.displayName.equalsIgnoreCase(name)) {
                return status;
            }
        }
        return null;
    }
}
