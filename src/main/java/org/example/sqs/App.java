package org.example.sqs;

import java.io.*;
import org.example.sqs.models.*;
import java.util.ArrayList;
import java.io.Serializable;
import java.net.URI;
import java.io.UnsupportedEncodingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.core.builder.CloudEventBuilder;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import com.amazonaws.regions.Regions;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import java.util.UUID;

public class App 
{
    public static void clearTopic(String queueUrl) {
        // Should match the values entered in the localstack configuration step
        String awsAccessKeyId = "123";
        String awsSecretAccessKey = "123";

        BasicAWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);

        AmazonSQS sqs = AmazonSQSClient.builder()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(queueUrl, "ap-southeast-2"))
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();


        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl)
                    .withAttributeNames("All")
                    .withMaxNumberOfMessages(10)
                    .withMessageAttributeNames("All")
                    .withWaitTimeSeconds(1); 

            ReceiveMessageResult receiveMessageResult = sqs.receiveMessage(receiveMessageRequest);

            for (Message message : receiveMessageResult.getMessages()) {
                System.out.println("Received message: " + message.getBody());
                sqs.deleteMessage(queueUrl, message.getReceiptHandle());
            }
   }

public static void connectAndSendCommand(String queueUrl, String payload) {
    String accessKey = "123";
    String secretKey = "123";

    BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);

    // Create an SQS client
    AmazonSQS sqs = AmazonSQSClient.builder()
    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(queueUrl, "ap-southeast-2"))
    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
    .build();
    
     System.out.println("Connecting to queue: " + queueUrl);

    // Create a message
    SendMessageRequest sendMessageRequest = new SendMessageRequest()
        .withQueueUrl(queueUrl)
        .withMessageBody(payload);

    // Send the message to the SQS queue
    SendMessageResult sendMessageResult = sqs.sendMessage(sendMessageRequest);

    // Print the message ID to confirm that the message was sent
    System.out.println("Message sent with ID: " + sendMessageResult.getMessageId());
}


public static String getExampleCommand(String commandType, String deviceId, int powerW) {
    Command command = new Command();
    command.deviceId = deviceId;
    command.durationSeconds = 600;

    Instant currentUtcTime = Instant.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);
    command.startTime = formatter.format(currentUtcTime);

    Map<String, Object> hashMap = new HashMap<>();
    Map<String, Object> emptyMap = new HashMap<>();
    Map<String, Object> powerMap = new HashMap<>();
    powerMap.put("powerW", powerW);

    // Use if-else instead of switch for String values
    if ("selfConsumptionCommand".equals(commandType)) {
        hashMap.put("selfConsumptionCommand", emptyMap);
    } else if ("chargeOnlySelfConsumptionCommand".equals(commandType)) {
        hashMap.put("chargeOnlySelfConsumptionCommand", emptyMap);
    } else if ("dischargeCommand".equals(commandType)) {
        hashMap.put("dischargeCommand", powerMap);
    } else if ("chargeCommand".equals(commandType)) {
        hashMap.put("chargeCommand", powerMap);
    } else {
        throw new IllegalArgumentException("Invalid commandType: " + commandType);
    }

    command.realMode = hashMap;

    String jsonString = "";
    try {
        ObjectMapper objectMapper = new ObjectMapper();
        jsonString = objectMapper.writeValueAsString(command);
    } catch (Exception e) {
        e.printStackTrace();
    }

    return jsonString;
}

   public static String getExampleCloudEvent(String payload) {
        UUID uuid = UUID.randomUUID();

        // Convert UUID to String
        String uuidString = uuid.toString();

        CloudEvent event = CloudEventBuilder.v1()
                .withId(uuidString)
                .withType("com.evergen.energy.battery-inverter.command.v1")
                .withSource(URI.create("https://evergen.energy"))
                .withDataContentType("application/json")
                .withData(payload.getBytes())
                .build();

        byte[]serialized = EventFormatProvider
            .getInstance()
            .resolveFormat("application/cloudevents+json")
            .serialize(event);

        String serialisedCloudEvent = "";
        try {
            serialisedCloudEvent = new String(serialized, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.out.println("CloudEvent: " + serialisedCloudEvent);
        
        return serialisedCloudEvent;
   }

   public static void sleepXseconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void test_1_1_idle(ArrayList<TestBattery> testBatteries) {
        System.out.println("Running Test 1.1. Idle");
        for (TestBattery battery : testBatteries) {
            connectAndSendCommand(battery.queueUrl, getExampleCloudEvent(getExampleCommand("chargeCommand", battery.deviceId, 0)));
        }
        sleepXseconds(600);
    }

    public static void test_1_2_slowDischarge(ArrayList<TestBattery> testBatteries) {
        System.out.println("Running Test 1.2. Slow Discharge");
        for (TestBattery battery : testBatteries) {
            connectAndSendCommand(battery.queueUrl, getExampleCloudEvent(getExampleCommand("dischargeCommand", battery.deviceId, (int) (battery.powerRating / 2))));
        }
        sleepXseconds(600);
    }

    public static void test_1_3_slowCharge(ArrayList<TestBattery> testBatteries) {
        System.out.println("Running Test 1.3. Slow Charge");
        for (TestBattery battery : testBatteries) {
            connectAndSendCommand(battery.queueUrl, getExampleCloudEvent(getExampleCommand("chargeCommand", battery.deviceId, (int) (battery.powerRating / 2))));
        }
        sleepXseconds(600);
    }

    public static void test_1_4_fastDischarge(ArrayList<TestBattery> testBatteries) {
        System.out.println("Running Test 1.4. Fast Discharge");
        for (TestBattery battery : testBatteries) {
            connectAndSendCommand(battery.queueUrl, getExampleCloudEvent(getExampleCommand("dischargeCommand", battery.deviceId, battery.powerRating)));
        }
        sleepXseconds(600);
    }

    public static void test_1_5_fastCharge(ArrayList<TestBattery> testBatteries) {
        System.out.println("Running Test 1.5. Fast Charge");
        for (TestBattery battery : testBatteries) {
            connectAndSendCommand(battery.queueUrl, getExampleCloudEvent(getExampleCommand("chargeCommand", battery.deviceId, battery.powerRating)));
        }
        sleepXseconds(600);
    }

    public static void test_2_1_selfConsumptionCharge(ArrayList<TestBattery> testBatteries) {
        System.out.println("Running Test 2.1. Self-consumption; charge");
        for (TestBattery battery : testBatteries) {
            connectAndSendCommand(battery.queueUrl, getExampleCloudEvent(getExampleCommand("selfConsumptionCommand", battery.deviceId, 0)));
        }
        sleepXseconds(600);
    }

    public static void test_2_2_selfConsumptionDischarge(ArrayList<TestBattery> testBatteries) {
        System.out.println("Running Test 2.2. Self-consumption; discharge");
        for (TestBattery battery : testBatteries) {
            connectAndSendCommand(battery.queueUrl, getExampleCloudEvent(getExampleCommand("selfConsumptionCommand", battery.deviceId, 0)));
        }
        sleepXseconds(600);
    }

    public static void test_2_3_selfConsumptionChargeOnly(ArrayList<TestBattery> testBatteries) {
        System.out.println("Running Test 2.3. Self-consumption charge only; charge");
        for (TestBattery battery : testBatteries) {
            connectAndSendCommand(battery.queueUrl, getExampleCloudEvent(getExampleCommand("chargeOnlySelfConsumptionCommand", battery.deviceId, 0)));
        }
        sleepXseconds(600);
    }

    public static void test_2_4_selfConsumptionChargeOnlyIdle(ArrayList<TestBattery> testBatteries) {
        System.out.println("Running Test 2.4. Self-consumption charge only; idle");
        for (TestBattery battery : testBatteries) {
            connectAndSendCommand(battery.queueUrl, getExampleCloudEvent(getExampleCommand("chargeOnlySelfConsumptionCommand", battery.deviceId, 0)));
        }
        sleepXseconds(600);
    }

    public static void test_3_1_commandInterruption(ArrayList<TestBattery> testBatteries) {
        System.out.println("Running 3.1. Command interruption test");
        for (TestBattery battery : testBatteries) {
            connectAndSendCommand(battery.queueUrl, getExampleCloudEvent(getExampleCommand("chargeCommand", battery.deviceId, (int) (battery.powerRating / 2))));
        }
        sleepXseconds(300);

        for (TestBattery battery : testBatteries) {
            connectAndSendCommand(battery.queueUrl, getExampleCloudEvent(getExampleCommand("selfConsumptionCommand", battery.deviceId, 0)));
        }
        sleepXseconds(600);
    }

    public static void test_3_2_commandExpiry(ArrayList<TestBattery> testBatteries) {
        System.out.println("Running 3.2. Command expiry test");
        for (TestBattery battery : testBatteries) {
            connectAndSendCommand(battery.queueUrl, getExampleCloudEvent(getExampleCommand("dischargeCommand", battery.deviceId, (int) (battery.powerRating / 2))));
        }
        sleepXseconds(900);
    }

    public static void main(String[] args) {
        ArrayList<TestBattery> testBatteries = new ArrayList<>();

        String queueUrl = "http://localhost:4566/000000000000/example-queue";

        // Add TestBattery objects to the ArrayList
        TestBattery testBattery = new TestBattery("testDeviceID", 5000, queueUrl);
        testBatteries.add(testBattery);


        // Uncomment which commands you would like to test.
        test_1_1_idle(testBatteries);
        // test_1_2_slowDischarge(testBatteries);
        // test_1_3_slowCharge(testBatteries);
        // test_1_4_fastDischarge(testBatteries);
        // test_1_5_fastCharge(testBatteries);
        // test_2_1_selfConsumptionCharge(testBatteries);
        // test_2_2_selfConsumptionDischarge(testBatteries);
        // test_2_3_selfConsumptionChargeOnly(testBatteries);
        // test_2_4_selfConsumptionChargeOnlyIdle(testBatteries);
        // test_3_1_commandInterruption(testBatteries);
        // test_3_2_commandExpiry(testBatteries);

        clearTopic(queueUrl);

        System.exit(0);
    }
}