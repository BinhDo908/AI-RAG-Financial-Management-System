package com.fintech.chatbot.ai.rag;

import com.fintech.chatbot.common.exception.BusinessException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Chống SSRF khi Admin nhập tài liệu từ URL (UC-C01).
 */
class UrlFetcherTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:8080/admin",
            "http://127.0.0.1/",
            "http://10.0.0.5/secret",
            "http://192.168.1.1/",
            "http://169.254.169.254/latest/meta-data/",
            "http://[::1]/",
            "file:///etc/passwd",
            "ftp://example.com/a.pdf",
            "not a url"
    })
    void rejectsInternalOrNonHttpUrls(String url) {
        assertThatThrownBy(() -> UrlFetcher.validate(url)).isInstanceOf(BusinessException.class);
    }
}
