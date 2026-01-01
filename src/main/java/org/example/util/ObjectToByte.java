package org.example.util;

import java.io.*;

public class ObjectToByte {

    public Object byteToObject (byte[] input) throws IOException, ClassNotFoundException {
        ByteArrayInputStream inputByte = new ByteArrayInputStream(input);
        ObjectInputStream inputObject = new ObjectInputStream(inputByte);

        return inputObject.readObject();
    }


    public  byte[] objectToBytes(Object obj) throws IOException {
        ByteArrayOutputStream outputByte = new ByteArrayOutputStream();
        ObjectOutputStream outputObject = new ObjectOutputStream(outputByte);
        outputObject.writeObject(obj);
        outputObject.flush();
        return outputByte.toByteArray();
    }

}
