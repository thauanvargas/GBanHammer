import entities.Player;
import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.misc.Cacher;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.packethandler.shockwave.packets.ShockPacketIncoming;
import gearth.protocol.packethandler.shockwave.packets.ShockPacketOutgoing;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.json.JSONArray;
import org.json.JSONObject;
import parsers.OHEntity;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

@ExtensionInfo(
        Title = "Ban Hammer",
        Description = "While not implemented, ban trolls!",
        Version = "1.0",
        Author = "Thauan"
)

public class BanHammer extends ExtensionForm implements Initializable {
    public static BanHammer RUNNING_INSTANCE;
    public Button buttonUnban;
    public ListView<String> playerListView;
    public Label labelInfo;
    public Button buttonClearList;
    public Label labelRoomName;
    protected List<Player> playerList = new ArrayList<>();
    public static String habboUserName;
    public String roomName;
    public String roomId;

    @Override
    protected void onStartConnection() {
        System.out.println("BanHammer started!");
    }

    @Override
    protected void onShow() {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCache();
    }

    @Override
    protected void initExtension() {
        RUNNING_INSTANCE = this;

        onConnect((host, port, APIVersion, versionClient, client) -> {
            if (!Objects.equals(versionClient, "SHOCKWAVE")) {
                System.exit(0);
            }
        });

        intercept(HMessage.Direction.TOCLIENT, "USERS", this::onUsersOrigin);
        intercept(HMessage.Direction.TOCLIENT, "USER_OBJ", this::onUserObject);
        intercept(HMessage.Direction.TOCLIENT, "LOGOUT", this::onUserRemove);
        intercept(HMessage.Direction.TOSERVER, "CHAT", this::onChat);
        intercept(HMessage.Direction.TOSERVER, "SHOUT", this::onChat);
        intercept(HMessage.Direction.TOCLIENT, "FLATINFO", this::onFlatInfo);

    }

    private void onFlatInfo(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();
        hPacket.readBoolean();
        roomId = hPacket.readString();
        hPacket.readString();
        roomName = hPacket.readString();
        String sanitizedRoomId = roomId.replaceAll("[^a-zA-Z]", "");

        playerList.clear();
        Platform.runLater(() -> {
            labelRoomName.setText(roomName + " BAN LIST:");
            playerListView.getItems().clear();
        });

        new Thread(() -> {
            loadRoomCache(sanitizedRoomId);
        }).start();
    }

    private void onChat(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();
        String message = hPacket.readString(StandardCharsets.ISO_8859_1);

        if (message.contains(":ban") && message.split(" ").length > 0) {
            if(roomId == null) {
                sendToServer(new ShockPacketOutgoing("{out:WHISPER}{s:\" Please reload the room to ban a player!\"}"));
                hMessage.setBlocked(true);
                return;
            }
            String playerName = message.split(" ")[1].trim();
            System.out.println("Banning " + playerName + "...");
            sendToServer(new ShockPacketOutgoing("{out:KICKUSER}{s:\"" + playerName + "\"}"));

            Platform.runLater(() -> {
                playerListView.getItems().add(playerName);
            });
            new Thread(this::updateRoomCache).start();
            hMessage.setBlocked(true);
        }
    }

    private void onUserRemove(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();

        final byte[] dataRemainder = hPacket.readBytes(hPacket.getBytesLength() - hPacket.getReadIndex());
        final String data = new String(dataRemainder, StandardCharsets.ISO_8859_1);
        int index = Integer.parseInt(data);

        Player player = findPlayerByIndex(index);

        if (player != null) {
            playerList.remove(player);
        }
    }

    private void onUserObject(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();
        final byte[] dataRemainder = hPacket.readBytes(hPacket.length() - hPacket.getReadIndex());
        final String data = new String(dataRemainder, StandardCharsets.ISO_8859_1);

        String[] pairs = data.split("\r");

        String nameValue = null;

        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals("name")) {
                nameValue = keyValue[1];
                break;
            }
        }

        habboUserName = nameValue;
    }


    private void onUsersOrigin(HMessage hMessage) {
        new Thread(() -> {
            try {
                HPacket hPacket = hMessage.getPacket();
                OHEntity[] roomUsersList = OHEntity.parse(hPacket);

                for (OHEntity hEntity : roomUsersList) {
                    String playerName = hEntity.getName();
                    if (playerName.equals(habboUserName)) {
                        continue;
                    }

                    if(playerListView.getItems().contains(playerName)) {
                        sendToServer(new ShockPacketOutgoing("{out:KICKUSER}{s:\"" + playerName + "\"}"));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    protected Player findPlayerByIndex(int index) {
        return playerList.stream().filter(player -> player.getIndex() == index).findFirst().orElse(null);
    }

    protected Player findPlayerByUserName(String userName) {
        return playerList.stream().filter(player -> Objects.equals(player.getName(), userName)).findFirst().orElse(null);
    }

    public void unbanPlayer(ActionEvent actionEvent) {
        String playerName = playerListView.getSelectionModel().getSelectedItem();
        if (playerName == null) {
            Platform.runLater(() -> {
                labelInfo.setText("Select a player to unban!");
            });
            return;
        }

        Platform.runLater(() -> {
            playerListView.getItems().remove(playerName);
            updateRoomCache();
        });
    }


    private void loadRoomCache(String sanitizedRoomId) {
        JSONObject cache = Cacher.getCacheContents();

        if(cache.has(sanitizedRoomId)) {
            JSONArray jsonArray = (JSONArray) Cacher.get(sanitizedRoomId);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject bannedPlayer = jsonArray.getJSONObject(i);
                Platform.runLater(() -> {
                    playerListView.getItems().add(bannedPlayer.getString("name"));
                });
            }
        }
    }

    private void setupCache() {
        File extDir = null;
        try {
            extDir = (new File(BanHammer.class.getProtectionDomain().getCodeSource().getLocation().toURI())).getParentFile();
            if (extDir.getName().equals("Extensions")) {
                extDir = extDir.getParentFile();
            }
        } catch (URISyntaxException ignored) {
        }

        Cacher.setCacheDir(extDir + File.separator + "Cache");
    }

    public void updateRoomCache() {
        JSONArray jsonBannedList = new JSONArray();
        for (String name : playerListView.getItems()) {
            JSONObject jsonPlayer = new JSONObject();
            jsonPlayer.put("name", name);
            jsonBannedList.put(jsonPlayer);
        }
        Cacher.put(roomId, jsonBannedList);
    }

    public void clearList(ActionEvent actionEvent) {
        Cacher.put(roomId, new JSONArray());
        Platform.runLater(() -> {
            playerListView.getItems().clear();
        });
    }
}
