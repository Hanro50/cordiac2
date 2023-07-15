package za.net.hanro50.cordiac.players;

import java.util.UUID;

public class BasePlayer {
    public String name;
    public UUID uuid;

    public BasePlayer(String name, UUID uuid) {
        this.name = name;
        this.uuid = uuid;
    }
}
