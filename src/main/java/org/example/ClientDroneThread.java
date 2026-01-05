package org.example;

import org.example.util.Data;
import org.example.util.SendPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.example.util.GeoLocation;
import org.example.util.ObjectConverter;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


import static org.example.Constants.DronesConstants.*;
import static org.example.config.AppConfig.getInt;
import static org.example.config.ConfigKeys.*;

public class ClientDroneThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ClientDroneThread.class);
    private final ObjectConverter objectConverter = new ObjectConverter();
    private final SendPacket sendPacket = new SendPacket();
    private final String id;
    private final Integer speed = getInt(DRONE_SPEED);
    private final Integer maxSurvivors = getInt(MAX_NUMBER_OF_SURVIVORS);
    private final int destinationPort;
    private final InetAddress destination;
    private final DatagramSocket skt;
    private GeoLocation currentLocation;
    private Data sendData;
    private Data recivedData;
    private final ConcurrentHashMap<String, GeoLocation> tasks;
    private String status = AVAILABLE;


    public ClientDroneThread(String Id, InetAddress destination,
                             int destinationPort, GeoLocation currentLocation, DatagramSocket skt, ConcurrentHashMap<String, GeoLocation> tasks)throws IOException {
        this.destinationPort = destinationPort;
        this.destination = destination;
        this.id = Id;
        this.skt = skt;
        this.currentLocation = currentLocation;
        this.tasks = tasks;
    }

    public void run() {
        try {

            byte[] reply = new byte[1024];

            //Drone register on the server (leader mission)
            sendData = new Data(REGISTER,id);
            sendPacket.sendData(sendData,destination,destinationPort,skt);
            logger.info("Drone {} try to register in {}", id, destination);

            //Receive
            skt.setSoTimeout(5000);
            DatagramPacket receiveData = new DatagramPacket(reply, reply.length);
            skt.receive(receiveData);
            logger.info("Drone {} RECEIVE OK {}", id, destination);
            String receivedReplay = new String(receiveData.getData(),receiveData.getOffset(),receiveData.getLength());
            HeartBeatDrone heartBeat = new HeartBeatDrone(id, destinationPort,destination,skt);
            heartBeat.start();

            while(receivedReplay.contains(OK)&&status.equals(AVAILABLE)){
                //send request
                sendData = new Data(REQUEST_TASK,id);
                sendPacket.sendData(sendData, destination, destinationPort,skt);

                //receive GeoLocation
                DatagramPacket receivePacket = new DatagramPacket(reply, reply.length);
                skt.receive(receivePacket);

                Data processData = (Data) objectConverter.byteToObject(receivePacket.getData());

                if(processData.getType().equals(NO_TASK_AVAILABLE)){
                        Thread.sleep(100000);
                        continue;
                }

                status = OCCUPIED;
                processGeoLocation(processData);
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
    private int scanningArea(GeoLocation destinationLocation,String taskId) throws InterruptedException {

        double currentLatitude = currentLocation.getLatitude();
        double currentLongitude = currentLocation.getLongitude();
        double destinationLatitude = destinationLocation.getLatitude();
        double destinationLongitude = destinationLocation.getLongitude();
        double distanceInDegrees = Math.sqrt((Math.pow((destinationLatitude -  currentLatitude),2) + Math.pow((destinationLongitude - currentLongitude),2)));
        double distanceInKm = distanceInDegrees * 111.0;
        double timeToScan = (distanceInKm / speed) * 3600 * 1000;
        double timeInSec = timeToScan/1000;


        logger.info("Start scanning {} , {} , task id: {}  total wait time : {} in sec, and {} in min",destinationLatitude, destinationLongitude,taskId,timeInSec,timeInSec/60);
        Thread.sleep((long)timeToScan);
        currentLocation = destinationLocation;
        return numberOfSurvivors();

    }

    private int numberOfSurvivors (){
        Random r= new Random();
        return r.nextInt(maxSurvivors) + 1;
    }


    private void processGeoLocation(Data processData) throws InterruptedException {

        String taskId = processData.getId();
        GeoLocation geoLocationReply = tasks.get(taskId);
        String numberOfSurvivors = String.valueOf(scanningArea(geoLocationReply,taskId));

        logger.info("drone is trying to submit {}",taskId);
        sendData = new Data(SUBMIT_RESULT,id,numberOfSurvivors,taskId);
        sendPacket.sendData(sendData,destination, destinationPort,skt);
    }

}
