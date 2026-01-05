package org.example;

import org.example.config.AppConfig;
import org.example.config.ConfigKeys;
import org.example.util.Data;
import org.example.util.ObjectConverter;
import org.example.util.SendPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.example.Constants.DronesConstants.*;
import static org.example.Constants.TaskConstants.*;

public class DroneManager extends Thread {
    final private String droneId;
    InetSocketAddress clientAddress;
    DatagramSocket socket;
    private final ConcurrentHashMap<String, String> droneState;
    private final ConcurrentHashMap<String, String> taskStatus;
    private final int HEARTBEAT_TIMEOUT_SECONDS;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Queue<Data> taskQueue = new LinkedList<>();
    private final Queue<Data> heartbeatQueue = new LinkedList<>();
    private static final Logger logger = LoggerFactory.getLogger(DroneManager.class);
    private LocalTime lastHeartbeat;
    private volatile boolean running = true;
    private static final ObjectConverter objectConverter = new ObjectConverter();
    private final Consumer<String> unregisterCallback;
    private final SendPacket sendPacket = new SendPacket();



    public DroneManager(
            String droneId,
            InetSocketAddress clientAddress,
            DatagramSocket socket,
            ConcurrentHashMap<String, String> droneState,
            ConcurrentHashMap<String, String> taskStatus,
            Consumer<String> unregisterCallback
    ) {
        this.droneId = droneId;
        this.clientAddress = clientAddress;
        this.socket = socket;
        this.droneState = droneState;
        this.taskStatus = taskStatus;
        this.unregisterCallback = unregisterCallback;
        this.HEARTBEAT_TIMEOUT_SECONDS = AppConfig.getInt(ConfigKeys.HEARTBEAT_TIMEOUT_SECONDS);
    }

    public void sendHeartBeatMessage(Data message) {
        synchronized (heartbeatQueue) {
            heartbeatQueue.add(message);
            heartbeatQueue.notify();
        }
    }

    public void sendTaskMessage(Data message) {
        synchronized (taskQueue) {
            taskQueue.add(message);
            taskQueue.notify();
        }
    }

    @Override
    public void run() {
        try {
            lastHeartbeat = LocalTime.now();
            droneState.put(droneId, ALIVE);

            while (running) {
                LocalTime currentHeartbeat = LocalTime.now();
                long diffSeconds = Duration.between(lastHeartbeat, currentHeartbeat).toSeconds();
                if (diffSeconds > HEARTBEAT_TIMEOUT_SECONDS) {
                    droneState.put(droneId, LOST);
                    logger.warn("Drone {} get {}", droneId, LOST);
                    unregisterCallback.accept(droneId);
                    shutdown();
                    return;
                }

                Data heartbeatMessage = null;
                try {
                    synchronized (heartbeatQueue) {
                        if (!heartbeatQueue.isEmpty()) {
                            heartbeatMessage = heartbeatQueue.poll();
                        }
                    }
                    if (heartbeatMessage != null) route(heartbeatMessage);

                } catch (Exception e) {
                    logger.error("Error while receiving heartbeat message from drone: {}, error message: {}", droneId, e.getMessage());
                }

                Data taskMessage = null;
                try {
                    synchronized (taskQueue) {
                        if (!taskQueue.isEmpty()) {
                            taskMessage = taskQueue.poll();
                        }
                    }
                    if (taskMessage != null) route(taskMessage);

                } catch (Exception e) {
                    logger.error("Error while receiving task message from drone: {}, error message: {}", droneId, e.getMessage());
                }
            }

        } catch(Exception e) {
            logger.error("Unexpected error in DroneManager thread for drone: {}, error message: {}", droneId, e.getMessage());
        }
    }

    private void routeRequestTask() {
        Data data = null;
        for (Map.Entry<String, String> task : taskStatus.entrySet()) {
            String taskId = task.getKey();
            String status = task.getValue();
            if(status.equals(PENDING)) {
                data = new Data(TASK, taskId);
                taskStatus.put(taskId, IN_PROGRESS);
                logger.info("Drone {} got assigned to task {} and set task {} to {}", droneId, taskId, taskId, IN_PROGRESS);
                break;
            }
        }

        if(data == null) {
            logger.info("No pending tasks available for drone {}", droneId);
            data = new Data(NO_TASK_AVAILABLE);
        }

        sendPacket.sendData(data,clientAddress.getAddress(),clientAddress.getPort(),socket);

    }

    private void routeSubmitResult(Data messageReceive) {
        taskStatus.put(messageReceive.getId(), COMPLETED);
        logger.info("Drone {} complete task {} with result of {}", droneId, messageReceive.getId(), messageReceive.getContent());
    }

    private void routeHeartbeat(Data messageReceive) {
        if(messageReceive.getContent() != null){
            String messageReceiveTime = messageReceive.getContent().split("-")[1];
            lastHeartbeat = LocalTime.parse(messageReceiveTime, TIME_FORMATTER);
            logger.info("Drone {} still {}", droneId, ALIVE);
        }
    }

    private void route(@org.jetbrains.annotations.NotNull Data data) {
        switch (data.getType()) {
            case REQUEST_TASK -> routeRequestTask();
            case SUBMIT_RESULT -> routeSubmitResult(data);
            case HEARTBEAT -> routeHeartbeat(data);
            default -> logger.warn("Unknown command {}", data.getType());
        }
    }

    private void shutdown() {
        running = false;
        synchronized (heartbeatQueue) {
            heartbeatQueue.notifyAll();
        }
        synchronized (taskQueue) {
            taskQueue.notifyAll();
        }
    }
}
