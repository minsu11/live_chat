package com.chat_server.error.handler;

import com.chat_server.common.propertis.CustomProperties;
import com.chat_server.error.dto.ErrorResponse;
import com.chat_server.error.enumulation.ErrorCode;
import com.chat_server.error.exception.BusinessException;
import com.chat_server.file.exception.FileValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api