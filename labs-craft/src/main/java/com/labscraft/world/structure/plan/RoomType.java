package com.labscraft.world.structure.plan;

/** The kinds of spaces in the Googleplex office. */
public enum RoomType {
    LOBBY("Lobby"),
    CORRIDOR("Corridor"),
    MEETING_ROOM("Meeting Room"),
    MICRO_KITCHEN("Micro-Kitchen"),
    DESK_AREA("Desk Pods"),
    LAB("Flow Lab"),
    /** Virtual room representing the world outside the building (entrance side). */
    OUTSIDE("Outside");

    private final String displayName;

    RoomType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
