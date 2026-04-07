package org.frizzlenpop.randomItem.teams;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.frizzlenpop.randomItem.RandomItem;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class TeamManager {

    private static final int MAX_TEAMS = 4;
    private static final Type MAP_TYPE = new TypeToken<Map<String, List<String>>>() {}.getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File file;
    private final Map<String, Team> teams = new LinkedHashMap<>();

    public TeamManager(RandomItem plugin) {
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "teams.json");
        load();

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::save, 6000L, 6000L);
    }

    public boolean createTeam(String name) {
        if (teams.size() >= MAX_TEAMS) return false;
        String key = name.toLowerCase();
        if (teams.containsKey(key)) return false;
        teams.put(key, new Team(name));
        return true;
    }

    public boolean disbandTeam(String name) {
        return teams.remove(name.toLowerCase()) != null;
    }

    public boolean addPlayer(String teamName, UUID player) {
        Team team = teams.get(teamName.toLowerCase());
        if (team == null) return false;
        if (getPlayerTeam(player) != null) return false;
        team.addMember(player);
        return true;
    }

    public boolean removePlayer(UUID player) {
        for (Team team : teams.values()) {
            if (team.hasMember(player)) {
                team.removeMember(player);
                return true;
            }
        }
        return false;
    }

    public Team getTeam(String name) {
        return teams.get(name.toLowerCase());
    }

    public Team getPlayerTeam(UUID player) {
        for (Team team : teams.values()) {
            if (team.hasMember(player)) return team;
        }
        return null;
    }

    public Collection<Team> getAllTeams() {
        return teams.values();
    }

    public List<Player> getOnlineTeamMembers(String teamName) {
        Team team = teams.get(teamName.toLowerCase());
        if (team == null) return Collections.emptyList();
        List<Player> online = new ArrayList<>();
        for (UUID uuid : team.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) online.add(p);
        }
        return online;
    }

    public void save() {
        Map<String, List<String>> serializable = new LinkedHashMap<>();
        for (Map.Entry<String, Team> entry : teams.entrySet()) {
            List<String> memberStrings = new ArrayList<>();
            for (UUID uuid : entry.getValue().getMembers()) {
                memberStrings.add(uuid.toString());
            }
            serializable.put(entry.getValue().getName(), memberStrings);
        }
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(serializable, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            Map<String, List<String>> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw != null) {
                for (Map.Entry<String, List<String>> entry : raw.entrySet()) {
                    Team team = new Team(entry.getKey());
                    for (String uuidStr : entry.getValue()) {
                        team.addMember(UUID.fromString(uuidStr));
                    }
                    teams.put(entry.getKey().toLowerCase(), team);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
