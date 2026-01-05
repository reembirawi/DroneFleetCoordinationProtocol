package org.example.util;

import org.example.ClientDroneThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Properties;

public class SendPacket {
    private static final Logger logger = LoggerFactory.getLogger(ClientDroneThread.class);
    Properties properties = new Properties();
    ObjectConverter objectConverter = new ObjectConverter();

    public void sendData(Data sendData, InetAddress destination, int port, DatagramSocket skt){
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
