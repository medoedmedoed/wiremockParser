package org.example;

import java.io.*;

public class ForInflux {
    private String nameFolder;
    private String method;
    private String urlPattern;
    private int count;
    private int port;

    public ForInflux(String nameFolder){
        this.nameFolder = nameFolder;
        this.method = "";
        this.urlPattern = "";
        this.count = 0;
        this.port = 0;
    }

    public String getNameFolder() {
        return nameFolder;
    }

    public void setNameFolder(String nameFolder) {
        this.nameFolder = nameFolder;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

   public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString(){
        return "nameFolder=" + this.getNameFolder() + ",port=" + this.getPort() + ",method=" + this.getMethod() + ",urlPattern=" + this.getUrlPattern() + " value=" + this.getCount();
    }

}
