package org.example;

import org.example.util.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.example.util.GeoLocation;
import org.example.util.ObjectConverter;
import org.example.Main;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Properties;
import java.util.Random;


import static java.lang.Math.abs;
import static org.example.Constants.DronesConstants.*;
import static org.example.Main.tasks;
import static org.example.config.AppConfig.getInt;
import static org.example.config.ConfigKeys.*;

public class ClientDroneThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ClientDroneThread.class);
    Properties properties = new Properties();
    ObjectConverter objectConverter = new ObjectConverter();

    private final String id;
    private final Integer speed = getInt(DRONE_SPEED);
    private final Integer maxSurvivors = getInt(MAX_NUMBER_OF_SURVIVORS);
    private final int destinationPort;
    private final InetAddress destination;
    private final DatagramSocket skt;
    private GeoLocation currentLocation;
    private Data sendData;
    private Data recivedData;
    private String status = AVAILABLE;


    public ClientDroneThread(String Id, int localPort, String destination,
                             int destinationPort, GeoLocation currentLocation, DatagramSocket skt)throws IOException {
        this.destinationPort = destinationPort;
        this.destination = InetAddress.getByName(destination);
        this.id = Id;
        this.skt = skt;
        this.currentLocation = currentLocation;
    }

    public void run() {
        try {

            HeartBeatDrone heartBeat = new HeartBeatDrone(id, destinationPort,destination,skt);
            byte[] reply = new byte[1024];

            DatagramPacket sendPacket;
            //Drone register on the server (leader mission)
            sendData = new Data(REGISTER,id);
            sendData(sendData,destination,destinationPort);
            logger.info("Drone {} try to register in {}", id, destination);

            //Receive ok
            DatagramPacket receiveData = new DatagramPacket(reply, reply.length);
            skt.receive(receiveData);
            String receivedReplay = new String(receiveData.getData(),receiveData.getOffset(),receiveData.getLength());


            while(receivedReplay.equals(OK) && status.equals(AVAILABLE)){
                //send request
                sendData = new Data(REQUEST_TASK,id);
                sendData(sendData, destination, destinationPort);

                //receive GeoLocation
                DatagramPacket receivePacket = new DatagramPacket(reply, reply.length);
                skt.receive(receivePacket);

                Data processData = (Data) objectConverter.byteToObject(receivePacket.getData());

                switch (processData.getType()){
                    case SUBMIT_RESULT -> {
                        status = OCCUPIED;
                        processGeoLocation(processData);
                    }
                    case NO_TASK_AVAILABLE -> {
                        Thread.sleep(100);
                        continue;
                    }
                }

                status = AVAILABLE;
            }

        }catch (InterruptedException e) {
            logger.error("InterruptedException with drone {} , error massage : {}, ",id ,e);
        }catch (ClassNotFoundException e) {
            logger.error("ClassNotFoundException with drone {} , error massage : {}, ",id ,e);
        }catch(IOException e){
            logger.error("IOException with drone {} , error massage : {}, ",id ,e);
        }
    }

    //This function just to simulate scanning time
    private int scanningArea(GeoLocation destinationLocation) throws InterruptedException {

        double currentLatitude = currentLocation.getLatitude();
        double currentLongitude = currentLocation.getLongitude();
        double destinationLatitude = destinationLocation.getLatitude();
        double destinationLongitude = destinationLocation.getLongitude();
        long area = (long) (abs(destinationLatitude -  currentLatitude) * abs(destinationLongitude - currentLongitude));
        long timeToScan = area / speed * 1000;

        logger.info("Start scanning {} , {}  total wait time : {}",destinationLatitude, destinationLongitude, timeToScan);
        Thread.sleep(timeToScan);
        currentLocation = destinationLocation;
        return numberOfSurvivors();

    }

    private int numberOfSurvivors (){
        Random r= new Random();
        return r.nextInt(maxSurvivors) + 1;
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

    private void processGeoLocation(Data processData) throws InterruptedException {

        String taskId = processData.getId();
        GeoLocation geoLocationReply = tasks.get(taskId);
        String numberOfSurvivors = String.valueOf(scanningArea(geoLocationReply));

        sendData = new Data(SUBMIT_RESULT,taskId,numberOfSurvivors);
        sendData(sendData,destination, destinationPort);
    }

}
