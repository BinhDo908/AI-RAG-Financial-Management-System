package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.dao.FinancialProfileDao;
import com.fintech.chatbot.dto.request.FinancialProfileRequest;
import com.fintech.chatbot.dto.request.RiskAnswersRequest;
import com.fintech.chatbot.dto.response.FinancialProfileResponse;
import com.fintech.chatbot.dto.response.RiskQuestionResponse;
import com.fintech.chatbot.entity.FinancialProfile;
import com.fintech.chatbot.entity.enums.FinancialGoal;
import com.fintech.chatbot.entity.enums.RiskLevel;
import com.fintech.chatbot.service.FinancialProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialProfileServiceImpl implements FinancialProfileService {

    /** Lựa chọn thứ k (1–3) được k điểm: càng về sau càng chấp nhận rủi ro cao. */
    private static final List<RiskQuestionResponse> QUESTIONS = List.of(
            new RiskQuestionResponse(1, "Nếu khoản đầu tư của bạn giảm 20% trong một tháng, bạn sẽ làm gì?",
                    List.of("Bán hết để tránh lỗ thêm", "Giữ nguyên và chờ hồi phục", "Mua thêm vì giá đang rẻ")),
            new RiskQuestionResponse(2, "Bạn dự định để tiền đầu tư trong bao lâu?",
                    List.of("Dưới 1 năm", "Từ 1 đến 5 năm", "Trên 5 năm")),
            new RiskQuestionResponse(3, "Quỹ dự phòng hiện có đủ trang trải bao lâu chi tiêu?",
                    List.of("Dưới 3 tháng", "Từ 3 đến 6 tháng", "Trên 6 tháng")),
            new RiskQuestionResponse(4, "Mục tiêu chính khi đầu tư của bạn là gì?",
                    List.of("Bảo toàn vốn", "Cân bằng giữa an toàn và tăng trưởng", "Tăng trưởng cao, chấp nhận biến động")),
            new RiskQuestionResponse(5, "Kinh nghiệm đầu tư (chứng khoán, quỹ, vàng…) của bạn?",
                    List.of("Chưa có", "Có một ít", "Nhiều kinh nghiệm"))
    );

    private final FinancialProfileDao profileDao;

    @Override
    @Transactional(readOnly = true)
    public FinancialProfileResponse get(Long userId) {
        return profileDao.findByUserId(userId).map(FinancialProfileServiceImpl::toResponse)
                .orElseGet(() -> new FinancialProfileResponse(null, 0, List.of(), null, null, null, null));
    }

    @Override
    @Transactional
    public FinancialProfileResponse update(Long userId, FinancialProfileRequest request) {
        FinancialProfile p = loadOrCreate(userId);
        p.setMonthlyIncome(request.monthlyIncome());
        p.setDependents(request.dependents());
        p.setGoals(request.goals() == null || request.goals().isEmpty() ? null
                : request.goals().stream().distinct().map(Enum::name).collect(Collectors.joining(",")));
        profileDao.save(p);
        return toResponse(p);
    }

    @Override
    public List<RiskQuestionResponse> riskQuestions() {
        return QUESTIONS;
    }

    @Override
    @Transactional
    public FinancialProfileResponse submitRiskAnswers(Long userId, RiskAnswersRequest request) {
        List<Integer> answers = request.answers();
        if (answers == null || answers.size() != QUESTIONS.size()
                || answers.stream().anyMatch(a -> a == null || a < 1 || a > 3)) {
            throw new BusinessException(ErrorCode.RISK_ANSWERS_INVALID);
        }
        int score = answers.stream().mapToInt(Integer::intValue).sum();
        FinancialProfile p = loadOrCreate(userId);
        p.setRiskScore(score);
        p.setRiskLevel(levelOf(score));
        profileDao.save(p);
        return toResponse(p);
    }

    static RiskLevel levelOf(int score) {
        if (score <= 8) {
            return RiskLevel.LOW;
        }
        return score <= 12 ? RiskLevel.MEDIUM : RiskLevel.HIGH;
    }

    /** BR-01: chỉ mô tả thái độ với rủi ro, không gợi ý sản phẩm cụ thể. */
    private static String describe(RiskLevel level) {
        if (level == null) {
            return null;
        }
        return switch (level) {
            case LOW -> "Thận trọng: ưu tiên an toàn vốn và thanh khoản, chấp nhận lợi nhuận thấp.";
            case MEDIUM -> "Cân bằng: chấp nhận biến động vừa phải để có tăng trưởng ổn định.";
            case HIGH -> "Chấp nhận rủi ro cao: sẵn sàng chịu biến động lớn để tìm tăng trưởng dài hạn.";
        };
    }

    private FinancialProfile loadOrCreate(Long userId) {
        return profileDao.findByUserId(userId).orElseGet(() -> {
            FinancialProfile p = new FinancialProfile();
            p.setUserId(userId);
            return p;
        });
    }

    private static FinancialProfileResponse toResponse(FinancialProfile p) {
        List<FinancialGoal> goals = p.getGoals() == null ? List.of()
                : Arrays.stream(p.getGoals().split(",")).map(FinancialGoal::valueOf).toList();
        return new FinancialProfileResponse(p.getMonthlyIncome(), p.getDependents(), goals, p.getRiskScore(),
                p.getRiskLevel(), describe(p.getRiskLevel()), p.getUpdatedAt());
    }
}
