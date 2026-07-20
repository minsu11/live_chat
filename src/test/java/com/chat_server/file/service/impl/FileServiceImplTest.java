package com.chat_server.file.service.impl;

import com.chat_server.file.config.FileUploadProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileServiceImplTest {

    @TempDir
    Path tempDir;

    private FileUploadProperties properties;
    private FileServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new FileUploadProperties();
        properties.setProfileDir(tempDir.resolve("profile").toString());
        properties.setChatImageDir(tempDir.resolve("chat-images").toString());
        properties.setChatFileDir(tempDir.resolve("chat-files").toString());
        service = new FileServiceImpl(properties);
    }

    /**
     * 정상 이미지 파일을 프로필 저장 경로에 기록하고 외부 접근 URL을 반환하는지 검증한다.
     * 생성된 파일명에는 사용자 ID가 포함되고 실제 파일도 디스크에 존재해야 한다.
     */
    @Test
    @DisplayName("프로필 이미지 저장 성공 - 이미지 파일을 전용 디렉터리에 저장하고 접근 URL을 반환한다")
    void saveProfileImageShouldStoreFileAndReturnProfileUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "image-data".getBytes());

        String url = service.saveProfileImage(7L, file);

        assertThat(url).startsWith("/uploads/profile/u7_").endsWith(".png");
        String savedName = url.substring("/uploads/profile/".length());
        assertThat(Files.readString(tempDir.resolve("profile").resolve(savedName)))
                .isEqualTo("image-data");
    }

    /**
     * 채팅 이미지 저장이 프로필 이미지와 다른 디렉터리 및 URL prefix를 사용하는지 검증한다.
     */
    @Test
    @DisplayName("채팅 이미지 저장 성공 - 채팅 이미지 전용 경로와 URL prefix를 사용한다")
    void saveChatImageShouldUseChatImageDirectory() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "photo".getBytes());

        String url = service.saveChatImage(10L, file);

        assertThat(url).startsWith("/uploads/chat/images/u10_").endsWith(".jpg");
        String savedName = url.substring("/uploads/chat/images/".length());
        assertThat(tempDir.resolve("chat-images").resolve(savedName)).exists();
    }

    /**
     * 허용된 일반 파일 확장자가 정상 저장되는지 검증한다.
     * 대문자 확장자도 소문자로 정규화해 validation을 통과해야 한다.
     */
    @Test
    @DisplayName("채팅 파일 저장 성공 - 허용 확장자는 대소문자와 관계없이 저장한다")
    void saveChatFileShouldAcceptAllowedExtensionCaseInsensitively() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "REPORT.PDF", "application/pdf", "pdf".getBytes());

        String url = service.saveChatFile(3L, file);

        assertThat(url).startsWith("/uploads/chat/files/u3_").endsWith(".PDF");
        String savedName = url.substring("/uploads/chat/files/".length());
        assertThat(tempDir.resolve("chat-files").resolve(savedName)).exists();
    }

    /**
     * 확장자가 없는 이미지도 content-type이 이미지라면 저장할 수 있는 현재 정책의 경계값을 검증한다.
     */
    @Test
    @DisplayName("이미지 저장 경계값 - 확장자가 없어도 image content-type이면 확장자 없이 저장한다")
    void saveProfileImageShouldAllowImageWithoutExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar", "image/png", "image".getBytes());

        String url = service.saveProfileImage(1L, file);

        assertThat(url).startsWith("/uploads/profile/u1_");
        String savedName = url.substring("/uploads/profile/".length());
        assertThat(savedName).doesNotContain(".");
        assertThat(tempDir.resolve("profile").resolve(savedName)).exists();
    }

    /**
     * null 파일 요청을 초기에 차단하는지 검증한다.
     * 저장 디렉터리 생성 등 파일 시스템 작업이 수행되기 전에 validation 예외가 발생해야 한다.
     */
    @Test
    @DisplayName("파일 validation 실패 - null 파일이면 업로드 요청을 거부한다")
    void saveProfileImageShouldRejectNullFile() {
        assertThatThrownBy(() -> service.saveProfileImage(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("업로드할 파일이 없습니다.");

        assertThat(tempDir.resolve("profile")).doesNotExist();
    }

    /**
     * 크기가 0인 MultipartFile을 빈 파일로 판단해 거부하는지 검증한다.
     */
    @Test
    @DisplayName("파일 validation 실패 - 빈 MultipartFile이면 저장하지 않는다")
    void saveChatImageShouldRejectEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.saveChatImage(1L, empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("업로드할 파일이 없습니다.");
    }

    /**
     * 프로필 이미지 API에 일반 문서 content-type이 전달되면 거부하는지 검증한다.
     */
    @Test
    @DisplayName("이미지 validation 실패 - image content-type이 아니면 업로드를 거부한다")
    void saveProfileImageShouldRejectNonImageContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "application/octet-stream", "data".getBytes());

        assertThatThrownBy(() -> service.saveProfileImage(1L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미지 파일만 업로드할 수 있습니다.");
    }

    /**
     * content-type이 null인 경우에도 이미지로 신뢰하지 않고 거부하는지 검증한다.
     */
    @Test
    @DisplayName("이미지 validation 실패 - content-type이 null이면 이미지 업로드를 거부한다")
    void saveChatImageShouldRejectNullContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", null, "data".getBytes());

        assertThatThrownBy(() -> service.saveChatImage(1L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미지 파일만 업로드할 수 있습니다.");
    }

    /**
     * 허용 목록에 없는 실행 파일 확장자를 거부하는지 검증한다.
     */
    @Test
    @DisplayName("채팅 파일 validation 실패 - 지원하지 않는 확장자는 저장하지 않는다")
    void saveChatFileShouldRejectUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "data".getBytes());

        assertThatThrownBy(() -> service.saveChatFile(1L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지원하지 않는 파일 형식입니다.");
    }

    /**
     * 파일명이 null인 일반 파일 요청을 빈 확장자로 처리해 거부하는지 검증한다.
     */
    @Test
    @DisplayName("채팅 파일 validation 실패 - 원본 파일명이 null이면 지원하지 않는 형식으로 거부한다")
    void saveChatFileShouldRejectNullOriginalFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file", null, "application/pdf", "data".getBytes());

        assertThatThrownBy(() -> service.saveChatFile(1L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지원하지 않는 파일 형식입니다.");
    }

    /**
     * 최대 허용 크기인 20MB는 초과가 아니므로 validation을 통과하는 임계치를 검증한다.
     */
    @Test
    @DisplayName("채팅 파일 크기 임계치 - 정확히 20MB인 파일은 허용한다")
    void saveChatFileShouldAllowExactlyTwentyMegabytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "limit.zip", "application/zip", new byte[20 * 1024 * 1024]);

        String url = service.saveChatFile(1L, file);

        assertThat(url).startsWith("/uploads/chat/files/u1_").endsWith(".zip");
    }

    /**
     * 최대 허용 크기보다 1byte 큰 파일을 거부하는 경계값을 검증한다.
     */
    @Test
    @DisplayName("채팅 파일 크기 한계 초과 - 20MB보다 1byte 큰 파일은 거부한다")
    void saveChatFileShouldRejectFileOneByteOverLimit() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "too-large.zip", "application/zip", new byte[20 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> service.saveChatFile(1L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일 최대 크기를 초과했습니다.");
    }

    /**
     * MultipartFile.transferTo에서 IOException이 발생하면 내부 예외를 노출하지 않고
     * 서비스 공통 RuntimeException으로 변환하는지 검증한다.
     */
    @Test
    @DisplayName("파일 저장 예외 처리 - 실제 파일 기록에 실패하면 공통 저장 실패 예외로 변환한다")
    void saveProfileImageShouldWrapTransferFailure() {
        MockMultipartFile failingFile = new MockMultipartFile(
                "file", "avatar.png", "image/png", "data".getBytes()) {
            @Override
            public void transferTo(Path dest) throws java.io.IOException {
                throw new java.io.IOException("disk full");
            }
        };

        assertThatThrownBy(() -> service.saveProfileImage(1L, failingFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("파일 저장에 실패했습니다.");
    }
}