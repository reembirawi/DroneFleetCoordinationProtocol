package org.example;

import org.example.util.Data;
import org.example.util.GeoLocation;
import org.example.util.ObjectConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.example.Constants.DronesConstants.*;
import static org.example.config.AppConfig.getInt;
import static org.example.config.ConfigKeys.HEARTBEAT_RESEND_SECONDS;

public class HeartBeatDrone extends Thread{

    private final String id;
    private final int destinationPort;
    private final InetAddress destination;
    private final DatagramSocket skt;
    private Data sendData;
    ObjectConverter objectConverter = new ObjectConverter();
    private static final Logger logger = LoggerFactory.getLogger(HeartBeatDrone.class);
    private final Integer sleepTime = getInt(HEARTBEAT_RESEND_SECONDS);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");


    public HeartBeatDrone(String id, int destinationPort, InetAddress destination, DatagramSocket skt) {
        this.id = id;
        this.destinationPort = destinationPort;
        this.destination = destination;
        this.skt = skt;
    }

    public void run() {
            while(true){
                String heartBeatTime = LocalTime.now().format(TIME_FORMATTER);
                sendData = new Data(HEARTBEAT,id,heartBeatTime);
                sendData(sendData,destination,destinationPort);
                logger.info("HeartBeat from {} to register {} in {} ", id, destination,heartBeatTime);

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    logger.error("InterruptedException in HeartBeat for drone {} at {}: message : {} ", id, heartBeatTime, e);
                }
            }
    }

    private void sendData(Data sendData, InetAddress destination, int port){
        try {
            byte [] sendBytes = objectConverter.objectToBytes(sendData);
            DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length, destination , port);
            skt.send(sendPacket);
        }
        catch (IOException e){
            logger.error("Cant send data to : {} , port {} ",destination, port);
        }
    }

}
