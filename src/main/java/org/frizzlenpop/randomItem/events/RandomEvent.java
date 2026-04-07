package org.frizzlenpop.randomItem.events;

public class RandomEvent {

    private final String name;
    private final String description;
    private final int durationSeconds;
    private final int weight;
    private final String type;

    public RandomEvent(String name, String description, int durationSeconds, int weight, String type) {
        this.name = name;
        this.description = description;
        this.durationSeconds = durationSeconds;
        this.weight = weight;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getWeight() {
        return weight;
    }

    public String getType() {
        return type;
    }
}
