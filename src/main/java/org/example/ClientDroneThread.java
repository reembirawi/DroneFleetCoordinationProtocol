package org.example;

import org.example.util.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.example.util.GeoLocation;
import org.example.util.ObjectConverter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Properties;
import java.util.Random;


import static java.lang.Math.abs;
import static org.example.Constants.DronesConstants.*;
import static org.example.config.AppConfig.getInt;
import static org.example.config.ConfigKeys.*;

public class ClientDroneThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ClientDroneThread.class);
    Properties properties = new Properties();
    ObjectConverter objectConverter = new ObjectConverter();

    private final String id;
    private final Integer speed = getInt(DRONE_SPEED);
    private final Integer maxServivors = getInt(MAX_NUMBER_OF_SURVIVORS);
    private final int destinationPort;
    private final InetAddress destination;
    private final DatagramSocket skt;
    private GeoLocation curruentLocation;
    private Data sendData;
    private Data recivedData;
    private String status = AVAILABLE;

    public ClientDroneThread(String Id, int localPort, String destination,
                             int destinationPort, GeoLocation curruentLocation) throws IOException {
        this.destinationPort = destinationPort;
        this.destination = InetAddress.getByName(destination);
        this.id = Id;
        this.skt = new DatagramSocket(localPort);
        this.curruentLocation = curruentLocation;
    }

    public void run() {
        try {

            DatagramPacket sendPacket;

            //Drone register on the server (leader mission)
            sendData = new Data(REGISTER,id);
            sendData(sendData,destination,destinationPort);
            logger.info("Drone {} try to register in {}", id, destination);

            //Receive ok
            byte[] reply = new byte[100];
            DatagramPacket receiveData = new DatagramPacket(reply, reply.length);
            skt.receive(receiveData);
            String receivedReplay = new String(receiveData.getData(),receiveData.getOffset(),receiveData.getLength());


            while(receivedReplay.equals(OK) && status.equals(AVAILABLE)){
                //send request
                sendData = new Data(REQUEST_TASK,id);
                sendData(sendData, destination, destinationPort);

                //receive GeoLocation
                byte[] data = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(data, data.length);
                skt.receive(receivePacket);

                //in case we cant now the real size
                byte[] receivedData = java.util.Arrays.copyOfRange(
                        data, 0, receivePacket.getLength()
                );


                Object receivedObject = objectConverter.byteToObject(receivedData);
                GeoLocation geoLocationReply = (GeoLocation) receivedObject;

                int numberOfSurvivors = scanningArea(geoLocationReply);

                //send request
                sendData = new Data(SUBMIT_RESULT,id+":"+numberOfSurvivors);
                sendData(sendData,destination, destinationPort);
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

        double currentLatitude = curruentLocation.getLatitude();
        double currentLongitude = curruentLocation.getLongitude();
        double destinationLatitude = destinationLocation.getLatitude();
        double destinationLongitude = destinationLocation.getLongitude();
        long area = (long) (abs(destinationLatitude -  currentLatitude) * abs(destinationLongitude - currentLongitude));
        long timeToScan = area / speed * 1000;

        logger.info("Start scanning {} , {}  total wait time : {}",destinationLatitude, destinationLongitude, timeToScan);
        Thread.sleep(timeToScan);
        curruentLocation = destinationLocation;
        return numberOfSurvivors();

    }

    private int numberOfSurvivors (){
        Random r= new Random();
        return r.nextInt(maxServivors) + 1;
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

    //private Data reciveData(){}
}


