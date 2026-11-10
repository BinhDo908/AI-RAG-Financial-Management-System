package com.fintech.chatbot.ai.rag;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

/**
 * UC-C01 bước 2 — Tải trang / file công khai từ URL do Admin dán vào.
 *
 * <p>Chống SSRF: chỉ http(s), không theo redirect, từ chối địa chỉ nội bộ (loopback, mạng LAN, link-local,
 * metadata cloud), giới hạn 10 MB.
 */
@Component
public class UrlFetcher {

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public record Fetched(byte[] content, String filename) {
    }

    public Fetched fetch(String url) {
        URI uri = validate(url);
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "FintechChatbot-KnowledgeBase/1.0")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new BusinessException(ErrorCode.DOCUMENT_URL_INVALID,
                        "Không tải được URL (HTTP %d)".formatted(response.statusCode()));
            }
            byte[] body;
            try (InputStream in = response.body()) {
                body = in.readNBytes((int) MAX_BYTES + 1);
            }
            if (body.length > MAX_BYTES) {
                throw new BusinessException(ErrorCode.DOCUMENT_URL_INVALID, "Tài liệu vượt quá 10 MB");
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            String filename = contentType.contains("pdf") || path.endsWith(".pdf") ? "document.pdf"
                    : path.endsWith(".docx") ? "document.docx"
                    : contentType.contains("text/plain") ? "document.txt"
                    : "document.html";
            return new Fetched(body, filename);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.DOCUMENT_URL_INVALID, "Không tải được URL");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.DOCUMENT_URL_INVALID, "Không tải được URL");
        }
    }

    static URI validate(String url) {
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.DOCUMENT_URL_INVALID);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https") || uri.getHost() == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_URL_INVALID);
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                        || address.isAnyLocalAddress() || address.isMulticastAddress()
                        || isUniqueLocalIpv6(address)) {
                    throw new BusinessException(ErrorCode.DOCUMENT_URL_INVALID, "Không được tải từ địa chỉ nội bộ");
                }
            }
        } catch (UnknownHostException e) {
            throw new BusinessException(ErrorCode.DOCUMENT_URL_INVALID, "Không phân giải được tên miền");
        }
        return uri;
    }

    /** fc00::/7 */
    private static boolean isUniqueLocalIpv6(InetAddress address) {
        byte[] b = address.getAddress();
        return b.length == 16 && (b[0] & 0xFE) == 0xFC;
    }
}
