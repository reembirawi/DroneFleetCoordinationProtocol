package org.example;

import org.example.config.AppConfig;
import org.example.config.ConfigKeys;
import org.example.service.DroneManager;
import org.example.util.Data;
import org.example.util.ObjectConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.Constants.DronesConstants.*;

public class MissionLeaderServer extends Thread {
    private final DatagramSocket socket;
    ObjectConverter objectConverter = new ObjectConverter();
    private ConcurrentHashMap<String, String> droneState;
    private ConcurrentHashMap<String, String> taskStatus;
    private static final Logger logger = LoggerFactory.getLogger(MissionLeaderServer.class);
    private final ConcurrentHashMap<String, DroneManager> droneThreads = new ConcurrentHashMap<>();

    MissionLeaderServer(
            DatagramSocket socket,
            ConcurrentHashMap<String, String>droneState,
            ConcurrentHashMap<String, String>taskStatus
    ) {
        this.socket  = socket;
        this.droneState = droneState;
        this.taskStatus = taskStatus;
    }

    @Override
    public void run() {
        final int SERVER_PORT = AppConfig.getInt(ConfigKeys.SERVER_PORT);
        logger.info("Mission Leader Server started on port {}", SERVER_PORT);

        while(!socket.isClosed()){
            try {
                byte[] buffer = new byte[100];

                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                socket.receive(receivePacket);
                InetSocketAddress clientAddress = new InetSocketAddress(
                        receivePacket.getAddress(), receivePacket.getPort()
                );

                Data data = (Data) objectConverter.byteToObject(receivePacket.getData());
                route(data, clientAddress);
            } catch(SocketException e) {
                logger.info("Socket closed, stopping Mission Leader");
                break;
            } catch(ClassNotFoundException e) {
                logger.error(e.getMessage());
                break;
            } catch (IOException e) {
                logger.error("Error processing packet", e);
                break;
            }
        }

    }

    private void infoLogging(String id, String command) {
        logger.info("Drone {} send a {} command", id, command);
    }

    private String getDroneId(String content) {
        return content.split("-")[0];
    }
    private void routeRegister(Data data, InetSocketAddress clientAddress) {
        String id = data.getContent();
        infoLogging(id, REGISTER);
        if(droneThreads.get(id) == null) {
            droneThreads.put(id, new DroneManager(id, clientAddress, droneState, taskStatus));
        } else {
            logger.warn("Drone {} try to register again", id);
        }
    }

    private void routeRequestTask(Data data) {
        String id = data.getContent();

        DroneManager manager = droneThreads.get(id);
        if (manager == null) {
            logger.warn("Request Task from unregistered drone {}", id);
            return;
        }

        infoLogging(id, REQUEST_TASK);
        manager.sendMessage(data);
    }

    private void routeSubmitResult(Data data) {
        String id = getDroneId(data.getContent());

        DroneManager manager = droneThreads.get(id);
        if (manager == null) {
            logger.warn("Result from unregistered drone {}", id);
            return;
        }

        infoLogging(id, SUBMIT_RESULT);
        manager.sendMessage(data);
    }

    private void routeHeartbeat(Data data) {
        String id = getDroneId(data.getContent());

        DroneManager manager = droneThreads.get(id);
        if (manager == null) {
            logger.warn("Heartbeat from unregistered drone {}", id);
            return;
        }

        infoLogging(id, HEARTBEAT);
        manager.sendMessage(data);
    }


    private void route(@org.jetbrains.annotations.NotNull Data data, InetSocketAddress clientAddress) {
        switch (data.getType()) {
            case REGISTER -> routeRegister(data, clientAddress);
            case REQUEST_TASK -> routeRequestTask(data);
            case SUBMIT_RESULT -> routeSubmitResult(data);
            case HEARTBEAT -> routeHeartbeat(data);
            default -> logger.warn("Unknown command {}", data.getType());
        }
    }
}
