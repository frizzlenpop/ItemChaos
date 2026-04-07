package org.frizzlenpop.randomItem.voting;

public class VoteOption {

    private final String type;
    private final String target;
    private final String description;

    public VoteOption(String type, String target, String description) {
        this.type = type;
        this.target = target;
        this.description = description;
    }

    public String getType() { return type; }
    public String getTarget() { return target; }
    public String getDescription() { return description; }
}
