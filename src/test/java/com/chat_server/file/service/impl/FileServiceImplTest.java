package com.chat_server.file.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class FileServiceImplTest {
    @Test
    @DisplayName("FileServiceImpl는 Service 구현체 계약을 유지한다")
    void serviceImplementationShouldKeepSpringServiceContract() {
        assertThat(FileServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(FileServiceImpl.class.getInterfaces()).isNotEmpty();
        assertThat(FileServiceImpl.class.getSimpleName()).endsWith("Impl");
    }
}
