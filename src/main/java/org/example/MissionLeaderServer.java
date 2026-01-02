package org.example;

import org.example.config.AppConfig;
import org.example.config.ConfigKeys;
import org.example.util.Data;
import org.example.util.GeoLocation;
import org.example.util.ObjectConverter;
import org.jetbrains.annotations.NotNull;
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
    private final static ConcurrentHashMap<String, DroneManager> droneThreads = new ConcurrentHashMap<>();

    MissionLeaderServer(
            DatagramSocket socket,
            ConcurrentHashMap<String, String> droneState,
            ConcurrentHashMap<String, String> taskStatus
    ) {
        this.socket = socket;
        this.droneState = droneState;
        this.taskStatus = taskStatus;
    }

    @Override
    public void run() {
        final int SERVER_PORT = AppConfig.getInt(ConfigKeys.SERVER_PORT);
        logger.info("Mission Leader Server started on port {}", SERVER_PORT);

        while (!socket.isClosed()) {
            try {
                byte[] buffer = new byte[100];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(receivePacket);

                InetSocketAddress clientAddress = new InetSocketAddress(
                        receivePacket.getAddress(), receivePacket.getPort()
                );

                Data data = (Data) objectConverter.byteToObject(receivePacket.getData());
                route(data, clientAddress);

            } catch (SocketException e) {
                logger.info("Socket closed, stopping Mission Leader");
                break;
            } catch (ClassNotFoundException e) {
                logger.error(e.getMessage());
                break;
            } catch (IOException e) {
                logger.error("Error while routing the packet", e);
                break;
            }
        }
    }

    private void infoLogging(String id, String command) {
        logger.info("Drone {} send a {} command", id, command);
    }

    private void routeRegister(Data data, InetSocketAddress clientAddress) throws IOException{
        String droneId = data.getId();
        if (droneId == null || droneId.length() != 10) {
            logger.error("Invalid droneId: {}", droneId);
            return;
        }

        infoLogging(droneId, REGISTER);

        if (droneThreads.get(droneId) == null) {
            DroneManager manager = new DroneManager(
                    droneId,
                    clientAddress,
                    socket,
                    droneState,
                    taskStatus,
                    this::unregisterDrone
            );
            droneThreads.put(droneId, manager);
            Data ack = new Data(OK);
            byte []buffer = objectConverter.objectToBytes(ack);
            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length, clientAddress.getAddress(), clientAddress.getPort()
            );
            socket.send(packet);
            manager.start();
        } else {
            logger.warn("Drone {} tried to register again", droneId);
        }
    }

    private void routeRequestTask(Data data) {
        String droneId = data.getId();

        DroneManager manager = droneThreads.get(droneId);
        if (manager == null) {
            logger.warn("Request Task from unregistered drone {}", droneId);
            return;
        }

        infoLogging(droneId, REQUEST_TASK);
        manager.sendTaskMessage(data);
    }

    private void routeSubmitResult(Data data) {
        String droneId = data.getId();

        DroneManager manager = droneThreads.get(droneId);
        if (manager == null) {
            logger.warn("Result from unregistered drone {}", droneId);
            return;
        }

        infoLogging(droneId, SUBMIT_RESULT);
        manager.sendTaskMessage(data);
    }

    private void routeHeartbeat(Data data) {
        String droneId = data.getId();

        DroneManager manager = droneThreads.get(droneId);
        if (manager == null) {
            logger.warn("Heartbeat from unregistered drone {}", droneId);
            return;
        }

        infoLogging(droneId, HEARTBEAT);
        manager.sendHeartBeatMessage(data);
    }

    private void route(@NotNull Data data, InetSocketAddress clientAddress) throws IOException{
        switch (data.getType()) {
            case REGISTER -> routeRegister(data, clientAddress);
            case REQUEST_TASK -> routeRequestTask(data);
            case SUBMIT_RESULT -> routeSubmitResult(data);
            case HEARTBEAT -> routeHeartbeat(data);
            default -> logger.warn("Unknown command {}", data.getType());
        }
    }
    public void unregisterDrone(String droneId) {
        droneThreads.remove(droneId);
        logger.info("Drone {} unregistered and marked as LOST", droneId);
    }
}
