package gov.nist.asbestos.client.events;


import gov.nist.asbestos.client.log.SimStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An Event is a request (the trigger) and any number of taskFiles undertaken
 * to satisfy that request.
 */
public class Event {
    private SimStore simStore;
    private File eventDir;
    private List<File> taskFiles = new ArrayList<>();
    private List<Task> tasks = new ArrayList<>();

    public Event(SimStore simStore, File eventDir) {
        this.simStore = simStore;
        this.eventDir = eventDir;

        // load task references
        int i = 0;
        while (true) {
            File taskFile = getTaskFile(i);
            if (taskFile.exists()) {
                if (taskFiles.size() <= i) {
                    taskFiles.add(taskFile);
                    tasks.add(new Task(i, this));
                } else {
                    taskFiles.set(i, taskFile);
                    tasks.set(i, new Task(i, this));
                }
            }
            else
                break;
            i++;
        }
        if (taskFiles.size() == 0) {
            newTask();  // initialize request
        }
    }

    public Task getClientTask() {
        return tasks.get(0);
    }

    public String toString() {
        return "Event: " + tasks.size() + " tasks";
    }

    public Task newTask() {
        int i = taskFiles.size();
        File taskFile = getTaskFile(i);
        taskFile.mkdirs();
        taskFiles.add(taskFile);
        Task task = new Task(i, this);
        tasks.add(task);
        return task;
    }

    private File getTaskFile(int i) {
        return new File(eventDir, "task" + i);
    }


    public int getTaskCount() {
        return taskFiles.size();
    }

    public File getEventDir() {
        return eventDir;
    }

    public File getRequestHeaderFile(int i) { return new File(taskFiles.get(i), "request_header.txt"); }
     File getRequestBodyFile(int i) { return new File(taskFiles.get(i), "request_body.bin"); }
     File getRequestBodyStringFile(int i) {  return new File(taskFiles.get(i), "request_body.txt"); }
     public File getResponseHeaderFile(int i) {  return new File(taskFiles.get(i), "response_header.txt"); }
     public File getResponseBodyFile(int i) {  return new File(taskFiles.get(i), "response_body.bin"); }
     public File getResponseBodyStringFile(int i) {  return new File(taskFiles.get(i), "response_body.txt"); }
     File getResponseBodyHTMLFile(int i) {  return new File(taskFiles.get(i), "response_body.html"); }
     File getRequestBodyHTMLFile(int i) {  return new File(taskFiles.get(i), "request_body.html"); }
     File getDescriptionFile(int i) { return new File(taskFiles.get(i), "description.txt"); }


}