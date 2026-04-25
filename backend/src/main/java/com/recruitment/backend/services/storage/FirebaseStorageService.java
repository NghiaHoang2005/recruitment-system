package com.recruitment.backend.services.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FirebaseStorageService {

    public String uploadCv(MultipartFile file, String folder) throws IOException {
        Bucket bucket = StorageClient.getInstance().bucket();
        String fileName = folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        bucket.create(fileName, file.getBytes(), file.getContentType());

        return fileName;
    }

    public String getPresignedUrl(String fileName) {
        Bucket bucket = StorageClient.getInstance().bucket();
        Blob blob = bucket.get(fileName);

        if (blob == null) {
            throw new AppException(ErrorCode.URL_NOT_FOUND);
        }

        return blob.signUrl(2, TimeUnit.HOURS).toString();
    }

    public void deleteFile(String fileName) {
        Bucket bucket = StorageClient.getInstance().bucket();
        Blob blob = bucket.get(fileName);

        if (blob == null) {
            return;
        }

        blob.delete();
    }
}
