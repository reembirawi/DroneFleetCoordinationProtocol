package org.example.util;

import java.io.Serializable;

public class Data implements Serializable {
    private final String type;
    private final String content;

    public Data(String type, String content){
        this.type = type;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public String getContent(){
        return content;
    }
}
