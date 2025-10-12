package com.chat_server.file.impl;

import com.chat_server.file.FileService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static String profileUrl = "/app/uploads/profile";

    @Override
    public String saveProfileImage(Long userId, MultipartFile file) {

        try{
            Files.createDirectories(Paths.get(profileUrl));
            String ext = getExtension(Objects.requireNonNull(file.getOriginalFilename()));
            String filename = "u" + userId + "_" + System.currentTimeMillis() + ext;
            Path path = Paths.get(profileUrl, filename);
            file.transferTo(path);
            log.info("파일 저장 완료");
            return "/uploads/profile/" + filename;
        }catch (IOException e){
            log.error(e.getMessage());
        }
        return null;
    }

    private String getExtension(String fileName){
        int idx = fileName.lastIndexOf(".");
        return idx > 0 ? fileName.substring(idx) : "";
    }
}
