package com.recruitment.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.getInstance();
            }

            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-adminsdk.json");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setStorageBucket("recruitment-7e017.firebasestorage.app")
                    .build();

            return FirebaseApp.initializeApp(options);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi cấu hình Firebase: " + e.getMessage());
        }
    }
}
