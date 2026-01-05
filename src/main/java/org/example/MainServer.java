package org.example;

import org.example.util.GeoLocation;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.Constants.DronesConstants.ALIVE;
import static org.example.Constants.TaskConstants.PENDING;
import static org.example.config.AppConfig.getInt;
import static org.example.config.ConfigKeys.SERVER_PORT;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class MainServer {

    private ConcurrentHashMap<String, GeoLocation> tasks = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> droneState;
    private static ConcurrentHashMap<String, String> taskStatus;
    private final static Integer serverPort = getInt(SERVER_PORT);
    public static DatagramSocket socket = null;

    public static void main(String[] args) {

        System.out.println("Drone Fleet Coordination Protocol Project");
/*
        droneState.put("DR-10",ALIVE);
        droneState.put("MQ-90",ALIVE);
        droneState.put("RQ-40",ALIVE);
        droneState.put("DR-11",ALIVE);
        droneState.put("DR-12",ALIVE);*/

        taskStatus.put("TS-1",PENDING);
        taskStatus.put("TS-2",PENDING);
        taskStatus.put("TS-3",PENDING);
        taskStatus.put("TS-4",PENDING);
        taskStatus.put("TS-5",PENDING);

        try {
            socket = new DatagramSocket(serverPort);
            MissionLeaderServer missionLeaderServer = new MissionLeaderServer(socket,droneState,taskStatus);

            missionLeaderServer.start();

        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }
}
