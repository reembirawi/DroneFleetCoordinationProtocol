package org.example.util;

import java.io.Serializable;

public class Data implements Serializable {
    private final String type;
    private final String id;
    private final String content;
    private final String taskId;

    public Data(String type, String id, String content,String taskId){
        this.type = type;
        this.id = id;
        this.content = content;
        this.taskId = taskId;
    }

    public Data(String type, String id, String content){
        this(type,id,content,null);
    }

    public Data(String type, String id) {
        this(type, id, null, null);
    }
    public Data(String type) {
        this(type, null, null, null);
    }

    public String getType() {
        return type;
    }

    public String getId(){
        return id;
    }

    public String getTaskId (){
        return taskId;
    }

    public String getContent(){
        return content;
    }
}
