package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.ai.AiSettings;
import com.fintech.chatbot.ai.client.EmbeddingClient;
import com.fintech.chatbot.ai.client.LlmClient;
import com.fintech.chatbot.ai.client.SpeechToTextClient;
import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.config.properties.AiProperties;
import com.fintech.chatbot.dao.AiConfigDao;
import com.fintech.chatbot.dao.AiConfigHistoryDao;
import com.fintech.chatbot.dto.response.AiConfigResponse;
import com.fintech.chatbot.entity.AiConfigEntry;
import com.fintech.chatbot.entity.AiConfigHistory;
import com.fintech.chatbot.service.AiConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl implements AiConfigService {

    private static final int HISTORY_LIMIT = 50;

    private final AiConfigDao configDao;
    private final AiConfigHistoryDao historyDao;
    private final AiSettings settings;
    private final AiProperties properties;
    private final LlmClient llmClient;
    private final EmbeddingClient embeddingClient;
    private final SpeechToTextClient sttClient;

    @Override
    @Transactional(readOnly = true)
    public AiConfigResponse get() {
        AiSettings.Snapshot snap = settings.snapshot();
        var history = historyDao.findRecent(HISTORY_LIMIT).stream()
                .map(h -> new AiConfigResponse.HistoryItem(h.getConfigKey(), h.getOldValue(), h.getNewValue(),
                        h.getChangedBy(), h.getCreatedAt()))
                .toList();
        var status = new AiConfigResponse.ProviderStatus(properties.chat().baseUrl(),
                llmClient.available() && snap.model() != null && !snap.model().isBlank(),
                embeddingClient.modelName(), embeddingClient.embeddingAvailable(), sttClient.sttAvailable());
        return new AiConfigResponse(snap.raw(), AiSettings.DEFAULTS, snap.model(), snap.taskModels(), status, history);
    }

    @Override
    @Transactional
    public AiConfigResponse update(Long adminId, Map<String, String> values) {
        Map<String, String> validated = new LinkedHashMap<>();
        values.forEach((k, v) -> validated.put(k, AiSettings.validate(k, v)));

        int topK = Integer.parseInt(validated.getOrDefault(AiSettings.RAG_TOP_K, settings.snapshot().raw().get(AiSettings.RAG_TOP_K)));
        int topN = Integer.parseInt(validated.getOrDefault(AiSettings.RAG_TOP_N, settings.snapshot().raw().get(AiSettings.RAG_TOP_N)));
        if (topN > topK) {
            throw new BusinessException(ErrorCode.AI_CONFIG_INVALID, "rag.top_n không được lớn hơn rag.top_k");
        }
        validated.forEach((k, v) -> save(adminId, k, v));
        return get();
    }

    @Override
    @Transactional
    public AiConfigResponse resetDefaults(Long adminId) {
        AiSettings.DEFAULTS.forEach((k, v) -> save(adminId, k, v));
        return get();
    }

    private void save(Long adminId, String key, String value) {
        AiConfigEntry entry = configDao.findByKey(key).orElseGet(() -> {
            AiConfigEntry e = new AiConfigEntry();
            e.setConfigKey(key);
            return e;
        });
        String old = entry.getConfigValue();
        if (Objects.equals(old, value)) {
            return;
        }
        entry.setConfigValue(value);
        entry.setUpdatedBy(adminId);
        configDao.save(entry);

        AiConfigHistory h = new AiConfigHistory();
        h.setConfigKey(key);
        h.setOldValue(old);
        h.setNewValue(value);
        h.setChangedBy(adminId);
        historyDao.save(h);
    }
}
