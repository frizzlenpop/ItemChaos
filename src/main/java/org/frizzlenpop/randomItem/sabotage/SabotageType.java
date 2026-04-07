package org.frizzlenpop.randomItem.sabotage;

import org.bukkit.Material;

public enum SabotageType {

    INVENTORY_SHUFFLE("Inventory Shuffle", Material.HOPPER, 300, 0,
            "Randomly rearranges the target's inventory"),

    BUTTER_FINGERS("Butter Fingers", Material.ICE, 500, 45,
            "Target randomly drops their held item every 3s"),

    DRUNK_VISION("Drunk Vision", Material.POTION, 250, 60,
            "Nausea and slowness - everything goes wobbly"),

    GRAVITY_FLIP("Gravity Flip", Material.FEATHER, 400, 30,
            "Target gets launched skyward repeatedly"),

    TNT_RAIN("TNT Rain", Material.TNT, 800, 30,
            "TNT falls from the sky around the target"),

    CHICKEN_SWARM("Chicken Swarm", Material.EGG, 350, 45,
            "A flock of chickens surrounds the target"),

    PHANTOM_MENACE("Phantom Menace", Material.PHANTOM_MEMBRANE, 600, 60,
            "Phantoms spawn and attack the target"),

    FAKE_DEATH("Fake Death", Material.SKELETON_SKULL, 200, 0,
            "Fakes the target's death with dramatic effects");

    private final String displayName;
    private final Material icon;
    private final int cost;
    private final int durationSeconds;
    private final String description;

    SabotageType(String displayName, Material icon, int cost, int durationSeconds, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.cost = cost;
        this.durationSeconds = durationSeconds;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getCost() {
        return cost;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public String getDescription() {
        return description;
    }
}
