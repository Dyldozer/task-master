package com.taskmaster;

import lombok.Data;

@Data
public class TaskUpdate {
    String type;
    private String username;
    private String objective;
    private int update;

    public TaskUpdate(String type, String username, String objective, int update)
    {
        this.type = type;
        this.username = username;
        this.objective = objective;
        this.update = update;
    }
}
