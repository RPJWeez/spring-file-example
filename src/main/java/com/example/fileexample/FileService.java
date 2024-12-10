package com.example.fileexample;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
public class FileService {
    public void handleFile(Message message) {
        if (message.getPayload() instanceof String) {
            System.out.println(message.getHeaders());
            System.out.println(message.getPayload());
        } else {
            throw new RuntimeException("Unexpected message type");
        }
    }
}
