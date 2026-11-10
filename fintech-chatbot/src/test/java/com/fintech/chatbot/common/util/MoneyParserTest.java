package com.fintech.chatbot.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyParserTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "50000          | 50000",
            "50k            | 50000",
            "50 k           | 50000",
            "50K            | 50000",
            "80 nghìn       | 80000",
            "80 ngàn        | 80000",
            "15.000.000     | 15000000",
            "15,000,000     | 15000000",
            "50.000         | 50000",
            "50.000đ        | 50000",
            "50000 vnd      | 50000",
            "2 triệu        | 2000000",
            "2tr            | 2000000",
            "3 củ           | 3000000",
            "1.5tr          | 1500000",
            "1,5 triệu      | 1500000",
            "1tr2           | 1200000",
            "1tr25          | 1250000",
            "2k5            | 2500",
            "1 tỷ           | 1000000000",
            "2.5 tỉ         | 2500000000"
    })
    void parsesVietnameseAmounts(String input, long expected) {
        assertThat(MoneyParser.parse(input)).contains(BigDecimal.valueOf(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "abc", "0", "0k", "50 2", "k", "1.2.3tr", "-50k", "1.5tr2"})
    void rejectsInvalidInput(String input) {
        assertThat(MoneyParser.parse(input)).isEmpty();
    }

    @Test
    void rejectsNull() {
        assertThat(MoneyParser.parse(null)).isEmpty();
    }
}
