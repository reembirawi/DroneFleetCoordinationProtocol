package org.example;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;

import static org.example.Constants.TaskConstants.PENDING;
import static org.example.config.AppConfig.getInt;
import static org.example.config.ConfigKeys.SERVER_PORT;

public class MainServer {

    private static final ConcurrentHashMap<String, String> droneState = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> taskStatus = new ConcurrentHashMap<>();
    private final static Integer serverPort = getInt(SERVER_PORT);
    public static DatagramSocket socket = null;
    private static LinkedList<String> acceptableDrones = new LinkedList<>();


    public static void main(String[] args) {

        System.out.println("Drone Fleet Coordination Protocol Project");

        taskStatus.put("TS-1",PENDING);
        taskStatus.put("TS-2",PENDING);
        taskStatus.put("TS-3",PENDING);
        taskStatus.put("TS-4",PENDING);
        taskStatus.put("TS-5",PENDING);

        acceptableDrones.add("DR-10");
        acceptableDrones.add("DR-13");
        acceptableDrones.add("DR-14");
        acceptableDrones.add("DR-11");
        acceptableDrones.add("DR-12");

        try {
            socket = new DatagramSocket(serverPort);
            MissionLeaderServer missionLeaderServer = new MissionLeaderServer(socket,droneState,taskStatus, acceptableDrones);

            missionLeaderServer.start();

        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }
}
