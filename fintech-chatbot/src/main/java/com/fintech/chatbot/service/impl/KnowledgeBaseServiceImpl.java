package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.ai.AiSettings;
import com.fintech.chatbot.ai.client.EmbeddingClient;
import com.fintech.chatbot.ai.rag.Retriever;
import com.fintech.chatbot.ai.rag.TextChunker;
import com.fintech.chatbot.ai.rag.TextExtractor;
import com.fintech.chatbot.ai.rag.TextSection;
import com.fintech.chatbot.ai.rag.UrlFetcher;
import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dao.ChunkDao;
import com.fintech.chatbot.dao.ChunkEmbeddingDao;
import com.fintech.chatbot.dao.DocumentDao;
import com.fintech.chatbot.dto.request.DocumentMetadataRequest;
import com.fintech.chatbot.dto.response.KnowledgeResponses;
import com.fintech.chatbot.entity.Chunk;
import com.fintech.chatbot.entity.Document;
import com.fintech.chatbot.entity.enums.DocumentStatus;
import com.fintech.chatbot.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    /**
     * Vilao tính phí embedding theo request (không theo token) nên gom nhiều đoạn trong một lần gọi.
     * Lô 64 đoạn mất gần 60 giây, sát read timeout của client, nên tài liệu dài bị lỗi 502 khi duyệt.
     */
    private static final int EMBED_BATCH = 24;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_HEADING = 300;

    private final DocumentDao documentDao;
    private final ChunkDao chunkDao;
    private final ChunkEmbeddingDao embeddingDao;
    private final TextExtractor extractor;
    private final TextChunker chunker;
    private final UrlFetcher urlFetcher;
    private final EmbeddingClient embeddingClient;
    private final Retriever retriever;
    private final AiSettings aiSettings;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<KnowledgeResponses.DocumentSummary> list(DocumentStatus status, String keyword, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        var items = documentDao.search(status, keyword, safePage, safeSize).stream().map(this::summary).toList();
        return PageResponse.of(items, safePage, safeSize, documentDao.countSearch(status, keyword));
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeResponses.DocumentDetail get(Long documentId) {
        return detail(load(documentId));
    }

    @Override
    @Transactional
    public KnowledgeResponses.DocumentDetail upload(Long adminId, DocumentMetadataRequest metadata, byte[] content,
                                                    String filename) {
        List<TextSection> sections = extractor.extract(content, filename);
        Document doc = new Document();
        doc.setUploadedBy(adminId);
        applyMetadata(doc, metadata);
        return createWithContent(doc, sections);
    }

    @Override
    @Transactional
    public KnowledgeResponses.DocumentDetail importUrl(Long adminId, DocumentMetadataRequest metadata) {
        if (metadata.url() == null || metadata.url().isBlank()) {
            throw new BusinessException(ErrorCode.DOCUMENT_URL_INVALID, "Vui lòng nhập URL");
        }
        UrlFetcher.Fetched fetched = urlFetcher.fetch(metadata.url());
        return upload(adminId, metadata, fetched.content(), fetched.filename());
    }

    @Override
    @Transactional
    public KnowledgeResponses.DocumentSummary updateMetadata(Long documentId, DocumentMetadataRequest metadata) {
        Document doc = load(documentId);
        applyMetadata(doc, metadata);
        return summary(doc);
    }

    @Override
    @Transactional
    public KnowledgeResponses.DocumentSummary approve(Long documentId) {
        Document doc = load(documentId);
        if (isBlank(doc.getLicense()) || isBlank(doc.getSourceId())) {
            throw new BusinessException(ErrorCode.DOCUMENT_METADATA_REQUIRED);
        }
        embedDocument(doc);
        doc.setStatus(DocumentStatus.APPROVED);
        return summary(doc);
    }

    @Override
    @Transactional
    public KnowledgeResponses.DocumentSummary disable(Long documentId) {
        Document doc = load(documentId);
        doc.setStatus(DocumentStatus.DISABLED);
        return summary(doc);
    }

    @Override
    @Transactional
    public KnowledgeResponses.DocumentDetail replaceContent(Long documentId, byte[] content, String filename) {
        Document doc = load(documentId);
        List<TextSection> sections = extractor.extract(content, filename);
        String hash = hash(sections);
        documentDao.findByContentHash(hash)
                .filter(other -> !other.getId().equals(documentId))
                .ifPresent(other -> {
                    throw new BusinessException(ErrorCode.DOCUMENT_DUPLICATE);
                });
        embeddingDao.deleteByDocumentId(documentId);
        chunkDao.deleteByDocumentId(documentId);
        doc.setContentHash(hash);
        doc.setStatus(DocumentStatus.DRAFT);
        saveChunks(doc, sections);
        return detail(doc);
    }

    @Override
    @Transactional
    public void delete(Long documentId) {
        Document doc = load(documentId);
        embeddingDao.deleteByDocumentId(documentId);
        chunkDao.deleteByDocumentId(documentId);
        documentDao.delete(doc);
    }

    @Override
    @Transactional
    public KnowledgeResponses.ReindexResult reindexAll() {
        int chunks = 0;
        List<Document> approved = documentDao.findByStatus(DocumentStatus.APPROVED);
        for (Document doc : approved) {
            chunks += embedDocument(doc);
        }
        return new KnowledgeResponses.ReindexResult(approved.size(), chunks);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeResponses.RetrievalTest testQuery(String question) {
        Retriever.Result result = retriever.retrieve(question, aiSettings.snapshot());
        Set<Long> used = result.chunks().stream().map(Retriever.RetrievedChunk::chunkId).collect(Collectors.toSet());
        var hits = result.candidates().stream()
                .map(c -> new KnowledgeResponses.RetrievalHit(c.chunkId(), c.documentId(), c.title(), c.heading(),
                        c.pageNumber(), c.score(), used.contains(c.chunkId()), c.content()))
                .toList();
        return new KnowledgeResponses.RetrievalTest(question, result.threshold(), result.sufficient(), hits);
    }

    // ---------- nội bộ ----------

    private KnowledgeResponses.DocumentDetail createWithContent(Document doc, List<TextSection> sections) {
        String hash = hash(sections);
        if (documentDao.findByContentHash(hash).isPresent()) {
            throw new BusinessException(ErrorCode.DOCUMENT_DUPLICATE);
        }
        doc.setContentHash(hash);
        doc.setStatus(DocumentStatus.DRAFT);
        documentDao.save(doc);
        saveChunks(doc, sections);
        return detail(doc);
    }

    private void saveChunks(Document doc, List<TextSection> sections) {
        int index = 0;
        for (TextSection piece : chunker.chunk(sections)) {
            Chunk c = new Chunk();
            c.assignDocument(doc);
            c.setChunkIndex(index++);
            c.setContent(piece.text());
            c.setHeading(truncate(piece.heading()));
            c.setPageNumber(piece.pageNumber());
            chunkDao.save(c);
        }
        chunkDao.flush();
    }

    /** Embed theo lô; xóa vector cũ của tài liệu trước. @return số đoạn đã embed. */
    private int embedDocument(Document doc) {
        if (!embeddingClient.embeddingAvailable()) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED, "Chưa cấu hình model embedding, không thể index");
        }
        List<Chunk> chunks = chunkDao.findByDocumentId(doc.getId());
        embeddingDao.deleteByDocumentId(doc.getId());
        for (int from = 0; from < chunks.size(); from += EMBED_BATCH) {
            List<Chunk> batch = chunks.subList(from, Math.min(chunks.size(), from + EMBED_BATCH));
            List<String> texts = batch.stream().map(KnowledgeBaseServiceImpl::embeddingText).toList();
            List<float[]> vectors = embeddingClient.embed(texts);
            for (int i = 0; i < batch.size(); i++) {
                embeddingDao.save(batch.get(i).getId(), embeddingClient.modelName(), vectors.get(i));
            }
        }
        return chunks.size();
    }

    /** Ghép tiêu đề tài liệu và đề mục vào văn bản được embed để tăng độ liên quan khi tìm kiếm. */
    private static String embeddingText(Chunk c) {
        String title = c.getDocument() == null ? "" : c.getDocument().getTitle();
        String heading = c.getHeading() == null ? "" : " — " + c.getHeading();
        return title + heading + "\n" + c.getContent();
    }

    private void applyMetadata(Document doc, DocumentMetadataRequest m) {
        doc.setTitle(m.title().trim());
        doc.setSourceId(m.sourceId());
        doc.setUrl(isBlank(m.url()) ? null : m.url().trim());
        doc.setLicense(m.license().trim());
        doc.setLanguage(m.language());
        doc.setAccessedAt(m.accessedAt() == null ? LocalDate.now(clock) : m.accessedAt());
    }

    private KnowledgeResponses.DocumentDetail detail(Document doc) {
        var chunks = chunkDao.findByDocumentId(doc.getId()).stream()
                .map(c -> new KnowledgeResponses.ChunkPreview(c.getId(), c.getChunkIndex(), c.getHeading(),
                        c.getPageNumber(), c.getContent().length(), c.getContent()))
                .toList();
        return new KnowledgeResponses.DocumentDetail(summary(doc), chunks);
    }

    private KnowledgeResponses.DocumentSummary summary(Document d) {
        return new KnowledgeResponses.DocumentSummary(d.getId(), d.getTitle(), d.getSourceId(), d.getUrl(),
                d.getLicense(), d.getLanguage(), d.getAccessedAt(), d.getStatus(),
                chunkDao.countByDocumentId(d.getId()), embeddingDao.countByDocumentId(d.getId()), d.getUpdatedAt());
    }

    private Document load(Long id) {
        return documentDao.findById(id).orElseThrow(() -> BusinessException.notFound("tài liệu"));
    }

    /** SHA-256 của nội dung đã làm sạch, phát hiện tài liệu trùng (EX-3). */
    private static String hash(List<TextSection> sections) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (TextSection s : sections) {
                md.update(s.text().getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String truncate(String heading) {
        if (heading == null) {
            return null;
        }
        return heading.length() > MAX_HEADING ? heading.substring(0, MAX_HEADING) : heading;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
