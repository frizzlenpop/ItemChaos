package org.frizzlenpop.randomItem.teams;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Team {

    private final String name;
    private final Set<UUID> members = new HashSet<>();

    public Team(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean hasMember(UUID uuid) {
        return members.contains(uuid);
    }
}
