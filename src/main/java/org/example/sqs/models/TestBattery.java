package org.example.sqs.models;

public class TestBattery {
    public String deviceId;
    public int powerRating;
    public String queueUrl;
    
    public TestBattery(String deviceId, int powerRating, String queueUrl) {
        this.deviceId = deviceId;
        this.powerRating = powerRating;
        this.queueUrl = queueUrl;
    }
}
