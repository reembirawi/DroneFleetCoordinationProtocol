package org.example;

import org.example.util.GeoLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.Constants.DronesConstants.ALIVE;
import static org.example.Constants.TaskConstants.PENDING;
import static org.example.MainServer.socket;
import static org.example.config.AppConfig.getInt;
import static org.example.config.ConfigKeys.SERVER_PORT;

public class MainClient {
    private static final ConcurrentHashMap<String, GeoLocation> tasks = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> droneState;
    private static ConcurrentHashMap<String, String> taskStatus;
    private final static Integer serverPort = getInt(SERVER_PORT);
    private final static InetAddress ip;

    static {
        try {
            ip = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {

        GeoLocation geoLocation1 = new GeoLocation(31.962600, 36.912450);
        GeoLocation geoLocation2 = new GeoLocation(31.962600, 35.913000);
        GeoLocation geoLocation3 = new GeoLocation(31.962600, 35.913550);
        GeoLocation geoLocation4 = new GeoLocation(31.962600, 35.914100);
        GeoLocation geoLocation5 = new GeoLocation(31.962600, 35.914650);


        System.out.println("Drone Fleet Coordination Protocol Project");
        tasks.put("TS-1",new GeoLocation(31.962600, 35.909700));
        tasks.put("TS-2",new GeoLocation(31.962600, 35.910250));
        tasks.put("TS-3",new GeoLocation(31.962600, 35.910800));
        tasks.put("TS-4",new GeoLocation(31.962600, 35.910800));
        tasks.put("TS-5",new GeoLocation( 31.962600, 35.911900));

        Map<String,GeoLocation> idToGeoLocation = new HashMap<>();
        idToGeoLocation.put("DR-10",geoLocation1);
        idToGeoLocation.put("MQ-90",geoLocation2);
        idToGeoLocation.put("RQ-40",geoLocation3);
        idToGeoLocation.put("DR-11",geoLocation4);
        idToGeoLocation.put("DR-12",geoLocation5);


        try {

            System.out.println("Enter drone id");
            Scanner scan = new Scanner(System.in);
            String droneId = scan.nextLine();

            DatagramSocket skt = new DatagramSocket();
            ClientDroneThread clientDroneThread = new ClientDroneThread(droneId,ip ,serverPort,idToGeoLocation.get(droneId),skt,tasks);
            clientDroneThread.run();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
