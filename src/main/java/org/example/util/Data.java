package org.example.util;

import java.io.Serializable;

public class Data implements Serializable {
    private final String type;
    private final String id;
    private final String content;

    public Data(String type, String id, String content){
        this.type = type;
        this.id = id;
        this.content = content;
    }
    public Data(String type, String id) {
        this(type, id, null);
    }
    public Data(String type) {
        this(type, null, null);
    }

    public String getType() {
        return type;
    }

    public String getId(){
        return id;
    }

    public String getContent(){
        return content;
    }
}
