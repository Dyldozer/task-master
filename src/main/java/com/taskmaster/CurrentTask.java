package com.taskmaster;

import lombok.Data;

@Data
public class CurrentTask {
    private String task;
    private String objective;
    private int start;
    private int current;
    private int goal;
    public CurrentTask(String task, String objective, int start, int current, int goal)
    {
        this.task = task;
        this.objective = objective;
        this.start = start;
        this.current = current;
        this.goal =goal;
    }
}
