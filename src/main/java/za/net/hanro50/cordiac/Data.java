package za.net.hanro50.cordiac;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;

public class Data {
    static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
    @Expose
    Map<UUID, Long> linkedPlayers;
    @Expose
    Long Server;
    @Expose
    Long logChannel;
    @Expose
    Long linkedChannel;

    File dataFile;

    public Data() {
        linkedPlayers = new HashMap<>();
    }

    public Data(File dataFile) {
        this();
        this.dataFile = dataFile;
        if (dataFile.exists()) {
            try (Scanner myReader = new Scanner(dataFile)) {
                String json = "";
                while (myReader.hasNextLine()) {
                    json += myReader.nextLine();

                }
                Data data = gson.fromJson(json, Data.class);
                myReader.close();

                this.linkedPlayers.putAll(data.linkedPlayers);
                this.Server = data.Server;
                this.logChannel = data.logChannel;
                this.linkedChannel = data.linkedChannel;
            } catch (JsonSyntaxException | FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void Save() throws IOException {
        if (dataFile == null) {
            App.log.warning("Cannot save data file");
            return;
        }
        if (!dataFile.exists())
            dataFile.createNewFile();
        FileWriter myWriter = new FileWriter(dataFile);
        myWriter.write(gson.toJson(this));
        myWriter.close();
    }
}
