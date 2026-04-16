package com.chat_server.chatattachment.service.impl;

import com.chat_server.chatattachment.dto.response.ChatAttachmentDownloadResult;
import com.chat_server.chatattachment.dto.response.ChatAttachmentUploadResponse;
import com.chat_server.chatattachment.entity.ChatAttachment;
import com.chat_server.chatattachment.repository.ChatAttachmentRepository;
import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.chatroommember.repository.ChatRoomMemberRepository;
import com.chat_server.common.propertis.CustomProperties;
import com.chat_server.error.enumulation.ErrorCode;
import com.chat_server.file.config.FileUploadProperties;
import com.chat_server.file.exception.FileValidationException;
import com.chat_server.user.entity.User;
import com.chat_server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatAttachmentServiceImplTest {

    private ChatAttachmentRepository chatAttachmentRepository;
    private ChatRoomRepository chatRoomRepository;
    private ChatRoomMemberRepository chatRoomMemberRepository;
    private UserRepository userRepository;
    private FileUploadProperties fileUploadProperties;
    private CustomProperties customProperties;

    private ChatAttachmentServiceImpl chatAttachmentService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        chatAttachmentRepository = mock(ChatAttachmentRepository.class);
        chatRoomRepository = mock(ChatRoomRepository.class);
        chatRoomMemberRepository = mock(ChatRoomMemberRepository.class);
        userRepository = mock(UserRepository.class);
        customProperties = mock(CustomProperties.class);

        fileUploadProperties = new FileUploadProperties();
        fileUploadProperties.setChatFileDir(tempDir.toString());
        fileUploadProperties.setChatFileMaxSize(DataSize.ofMegabytes(5));
        fileUploadProperties.setChatFileAllowedExtensions(Set.of("pdf", "txt", "png", "jpg", "jpeg"));

        chatAttachmentService = new ChatAttachmentServiceImpl(
                chatAttachmentRepository,
                chatRoomRepository,
                chatRoomMemberRepository,
                userRepository,
                fileUploadProperties,
                customProperties
        );
    }

    @Test
    @DisplayName("업로드 성공 시 파일을 저장하고 응답을 반환한다")
    void uploadShouldStoreFileAndReturnResponse() throws Exception {
        // given
        Long roomId = 1L;
        Long userId = 10L;

        when(chatRoomMemberRepository.existsActiveMember(roomId, userId)).thenReturn(true);
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mock(ChatRoom.class)));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));

        when(chatAttachmentRepository.save(any(ChatAttachment.class)))
                .thenAnswer(invocation -> {
                    ChatAttachment saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 100L);
                    return saved;
                });

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                "text/plain",
                "hello world".getBytes()
        );

        ArgumentCaptor<ChatAttachment> captor = ArgumentCaptor.forClass(ChatAttachment.class);

        // when
        ChatAttachmentUploadResponse response = chatAttachmentService.upload(roomId, userId, file);

        // then
        verify(chatAttachmentRepository).save(captor.capture());
        ChatAttachment savedAttachment = captor.getValue();

        assertThat(response.attachmentId()).isEqualTo(100L);
        assertThat(response.fileName()).isEqualTo("hello.txt");
        assertThat(response.contentType()).isEqualTo("text/plain");
        assertThat(response.fileSize()).isEqualTo((long) file.getSize());

        Path savedPath = tempDir.resolve(savedAttachment.getFileUrl());
        assertThat(Files.exists(savedPath)).isTrue();
        assertThat(savedAttachment.getOriginalFileName()).isEqualTo("hello.txt");
        assertThat(savedAttachment.getStoredFileName()).endsWith(".txt");
    }

    @Test
    @DisplayName("빈 파일 업로드 시 예외가 발생한다")
    void uploadShouldThrowWhenFileIsEmpty() {
        // given
        Long roomId = 1L;
        Long userId = 10L;
        when(chatRoomMemberRepository.existsActiveMember(roomId, userId)).thenReturn(true);

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        // when & then
        assertThatThrownBy(() -> chatAttachmentService.upload(roomId, userId, emptyFile))
                .isInstanceOf(FileValidationException.class)
                .isInstanceOfSatisfying(FileValidationException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                    assertThat(ex.getMessage()).isEqualTo("빈 파일은 전송할 수 없습니다.");
                });
    }

    @Test
    @DisplayName("허용되지 않은 확장자 업로드 시 예외가 발생한다")
    void uploadShouldThrowWhenExtensionIsNotAllowed() {
        // given
        Long roomId = 1L;
        Long userId = 10L;
        when(chatRoomMemberRepository.existsActiveMember(roomId, userId)).thenReturn(true);

        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "virus.exe",
                "application/octet-stream",
                "malicious".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> chatAttachmentService.upload(roomId, userId, exeFile))
                .isInstanceOf(FileValidationException.class)
                .isInstanceOfSatisfying(FileValidationException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                    assertThat(ex.getMessage()).isEqualTo("허용되지 않는 파일 형식입니다.");
                });
    }

    @Test
    @DisplayName("같은 방, 같은 업로더의 attachment는 메시지와 연결된다")
    void connectMessageShouldLinkAttachmentWhenRoomAndUploaderMatch() {
        // given
        Long attachmentId = 1L;
        Long roomId = 10L;
        Long userId = 20L;

        ChatAttachment attachment = createAttachment(roomId, userId, "2026/04/17/test.txt", null);
        ChatMessage chatMessage = createMessage(roomId, userId);

        when(chatAttachmentRepository.findByIdAndUploader_IdAndChatMessageIsNull(attachmentId, userId))
                .thenReturn(Optional.of(attachment));

        // when
        chatAttachmentService.connectMessage(attachmentId, chatMessage, userId);

        // then
        assertThat(attachment.getChatMessage()).isEqualTo(chatMessage);
    }

    @Test
    @DisplayName("다른 방의 attachment를 연결하려 하면 예외가 발생한다")
    void connectMessageShouldThrowWhenRoomDoesNotMatch() {
        // given
        Long attachmentId = 1L;
        Long userId = 20L;

        ChatAttachment attachment = createAttachment(10L, userId, "2026/04/17/test.txt", null);
        ChatMessage chatMessage = createMessage(99L, userId);

        when(chatAttachmentRepository.findByIdAndUploader_IdAndChatMessageIsNull(attachmentId, userId))
                .thenReturn(Optional.of(attachment));

        // when & then
        assertThatThrownBy(() -> chatAttachmentService.connectMessage(attachmentId, chatMessage, userId))
                .isInstanceOf(ResponseStatusException.class)
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode().value()).isEqualTo(403);
                    assertThat(ex.getReason()).isEqualTo("다른 채팅방의 첨부파일은 연결할 수 없습니다.");
                });
    }

    @Test
    @DisplayName("다른 사용자가 업로드한 attachment를 연결하려 하면 예외가 발생한다")
    void connectMessageShouldThrowWhenUploaderDoesNotMatch() {
        // given
        Long attachmentId = 1L;
        Long userId = 20L;

        ChatAttachment attachment = createAttachment(10L, userId, "2026/04/17/test.txt", null);
        ChatMessage chatMessage = createMessage(10L, 999L);

        when(chatAttachmentRepository.findByIdAndUploader_IdAndChatMessageIsNull(attachmentId, userId))
                .thenReturn(Optional.of(attachment));

        // when & then
        assertThatThrownBy(() -> chatAttachmentService.connectMessage(attachmentId, chatMessage, userId))
                .isInstanceOf(ResponseStatusException.class)
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode().value()).isEqualTo(403);
                    assertThat(ex.getReason()).isEqualTo("본인이 업로드한 첨부파일만 연결할 수 있습니다.");
                });
    }

    @Test
    @DisplayName("다운로드 권한이 있으면 파일 리소스를 반환한다")
    void downloadShouldReturnResourceWhenUserIsRoomMember() throws Exception {
        // given
        Long attachmentId = 1L;
        Long userId = 20L;
        Long roomId = 10L;

        String relativePath = "2026/04/17/file.txt";
        Path targetPath = tempDir.resolve(relativePath);
        Files.createDirectories(targetPath.getParent());
        Files.writeString(targetPath, "download-content");

        ChatAttachment attachment = createAttachment(
                roomId,
                userId,
                relativePath,
                mock(ChatMessage.class)
        );

        when(chatAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(chatRoomMemberRepository.existsActiveMember(roomId, userId)).thenReturn(true);

        // when
        ChatAttachmentDownloadResult result = chatAttachmentService.download(attachmentId, userId);

        // then
        assertThat(result.fileName()).isEqualTo("original-name.txt");
        assertThat(result.contentLength()).isEqualTo(Files.size(targetPath));
        assertThat(result.resource().exists()).isTrue();
    }

    @Test
    @DisplayName("다운로드 권한이 없으면 예외가 발생한다")
    void downloadShouldThrowWhenUserIsNotRoomMember() {
        // given
        Long attachmentId = 1L;
        Long userId = 20L;
        Long roomId = 10L;

        ChatAttachment attachment = createAttachment(
                roomId,
                userId,
                "2026/04/17/file.txt",
                mock(ChatMessage.class)
        );

        when(chatAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(chatRoomMemberRepository.existsActiveMember(roomId, userId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> chatAttachmentService.download(attachmentId, userId))
                .isInstanceOf(ResponseStatusException.class)
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode().value()).isEqualTo(403);
                    assertThat(ex.getReason()).isEqualTo("채팅방 멤버만 파일에 접근할 수 있습니다.");
                });
    }

    private ChatAttachment createAttachment(Long roomId, Long uploaderId, String fileUrl, ChatMessage linkedMessage) {
        ChatRoom room = mock(ChatRoom.class);
        when(room.getId()).thenReturn(roomId);

        User uploader = mock(User.class);
        when(uploader.getId()).thenReturn(uploaderId);

        ChatAttachment attachment = ChatAttachment.builder()
                .chatMessage(linkedMessage)
                .room(room)
                .uploader(uploader)
                .fileUrl(fileUrl)
                .originalFileName("original-name.txt")
                .storedFileName("stored-name.txt")
                .contentType("text/plain")
                .fileSize(123L)
                .createdAt(LocalDateTime.now())
                .build();

        ReflectionTestUtils.setField(attachment, "id", 1L);
        return attachment;
    }

    @Test
    @DisplayName("메시지에 연결되지 않은 attachment는 다운로드할 수 없다")
    void downloadShouldThrowWhenAttachmentIsNotLinkedToMessage() {
        // given
        Long attachmentId = 1L;
        Long userId = 20L;
        Long roomId = 10L;

        ChatAttachment attachment = createAttachment(
                roomId,
                userId,
                "2026/04/17/file.txt",
                null
        );

        when(chatAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(chatRoomMemberRepository.existsActiveMember(roomId, userId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> chatAttachmentService.download(attachmentId, userId))
                .isInstanceOf(ResponseStatusException.class)
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode().value()).isEqualTo(400);
                    assertThat(ex.getReason()).isEqualTo("아직 메시지에 연결되지 않은 파일입니다.");
                });
    }

    private ChatMessage createMessage(Long roomId, Long senderId) {
        ChatRoom room = mock(ChatRoom.class);
        when(room.getId()).thenReturn(roomId);

        User sender = mock(User.class);
        when(sender.getId()).thenReturn(senderId);

        ChatMessage chatMessage = mock(ChatMessage.class);
        when(chatMessage.getChatRoom()).thenReturn(room);
        when(chatMessage.getSender()).thenReturn(sender);

        return chatMessage;
    }
}