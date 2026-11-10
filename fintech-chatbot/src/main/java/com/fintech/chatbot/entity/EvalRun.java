package com.fintech.chatbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một lần chạy bộ đánh giá chatbot (UC-C03 AF-1), lưu để so sánh giữa các cấu hình.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "eval_runs")
public class EvalRun extends BaseEntity {

    @Column(name = "dataset_name", nullable = false, length = 200)
    private String datasetName;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    /** Hit@k: tỷ lệ câu có tài liệu đúng nằm trong các đoạn truy xuất. */
    @Column(name = "hit_rate", nullable = false)
    private double hitRate;

    /** Tỷ lệ câu trả lời có trích dẫn nguồn. */
    @Column(name = "citation_rate", nullable = false)
    private double citationRate;

    /** Tỷ lệ câu trả lời đúng so với đáp án mẫu. */
    @Column(name = "accuracy", nullable = false)
    private double accuracy;

    /** Cấu hình AI lúc chạy (JSON). */
    @Column(name = "config_snapshot", nullable = false)
    private String configSnapshot;

    /** Kết quả từng câu (JSON). */
    @Column(name = "details", nullable = false)
    private String details;

    @Column(name = "run_by")
    private Long runBy;
}
