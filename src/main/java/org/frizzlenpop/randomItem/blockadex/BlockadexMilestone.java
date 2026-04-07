package org.frizzlenpop.randomItem.blockadex;

public class BlockadexMilestone {

    private final int percentage;
    private final String name;
    private final String effectName;
    private final int amplifier;
    private final String description;

    public BlockadexMilestone(int percentage, String name, String effectName, int amplifier, String description) {
        this.percentage = percentage;
        this.name = name;
        this.effectName = effectName;
        this.amplifier = amplifier;
        this.description = description;
    }

    public int getPercentage() { return percentage; }
    public String getName() { return name; }
    public String getEffectName() { return effectName; }
    public int getAmplifier() { return amplifier; }
    public String getDescription() { return description; }
}
