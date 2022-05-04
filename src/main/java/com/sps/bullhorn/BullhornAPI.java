package com.sps.bullhorn;

import com.bullhornsdk.data.api.BullhornData;
import com.bullhornsdk.data.api.BullhornRestCredentials;
import com.bullhornsdk.data.api.StandardBullhornData;
import com.bullhornsdk.data.model.entity.core.standard.Candidate;
import com.bullhornsdk.data.model.entity.core.standard.Placement;
import com.bullhornsdk.data.model.entity.core.type.BullhornEntity;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

public class BullhornAPI {
    private Properties bullhornProps;
    private Logger logger;
    private Path filePath;

    private int lastParsed;

    private BullhornRestCredentials creds;
    private BullhornData bullhornData;
    public BullhornAPI() {
        System.out.println("Connecting to Bullhorn API");
        bullhornProps = new Properties();

        logger = LogManager.getLogger(BullhornAPI.class);//.getRootLogger();
        logger.trace("Configuration File :: " + System.getProperty("log4j.configurationFile"));

        filePath = Path.of(System.getProperty("user.home")).resolve("placementFile.dat");

        try {
            bullhornProps.load(new FileInputStream("./src/main/resources/bullhorn.properties"));
            System.out.println(bullhornProps.getProperty("username"));

            if (!Files.notExists(filePath)) {
                Path path = Files.createFile(filePath);
                String latestID = JOptionPane.showInputDialog("");
                Files.writeString(path, latestID);
                logger.warn("File " + filePath + " was created. Began with Placement ID #" + latestID);
            }
        } catch (IOException e) {
            logger.error("File not created. \n" + e);
        }

        connectToServer();
    }

    public void connectToServer() {
        creds = new BullhornRestCredentials();
        creds.setUsername(bullhornProps.getProperty("username"));
        creds.setPassword(bullhornProps.getProperty("password"));
        logger.info("Credentials verified. Authorizing URL. . .");
        creds.setRestAuthorizeUrl(bullhornProps.getProperty("authUrl"));
        logger.info("URL authorized. Verifying client ID & secret. . .");
        creds.setRestClientId(bullhornProps.getProperty("clientID"));
        creds.setRestClientSecret(bullhornProps.getProperty("clientSecret"));
        logger.info("Client verified. Logging in. . .");
        creds.setRestLoginUrl(bullhornProps.getProperty("loginUrl"));
        logger.info("Logged in. Obtaining REST token. . .");
        creds.setRestSessionMinutesToLive(bullhornProps.getProperty("minutesToLive"));
        creds.setRestTokenUrl(bullhornProps.getProperty("tokenUrl"));
        bullhornData = new StandardBullhornData(creds);
        logger.info("Successfully connected.");
    }

    public int getLastParsed() {
        return lastParsed;
    }

    public void setLastParsed(int lastParsed) {
        this.lastParsed = lastParsed;
    }

    public Candidate getCandidate(int id, Set<String> fieldSet) {
        return bullhornData.findEntity(Candidate.class, id, fieldSet);
    }

    public Placement getPlacement(int id, Set<String> fieldSet) {
        return bullhornData.findEntity(Placement.class, id, fieldSet);
    }

    public <T extends BullhornEntity> T get(Class<T> entity, int id, Set<String> fieldSet) {
        return bullhornData.findEntity(entity, id, fieldSet);
    }
}

