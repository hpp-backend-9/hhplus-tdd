package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.exception.PointException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PointValidator {
    // 최대로 충전할 수 있는 금액
    private static final long MAX_CHARGE_AMOUNT = 10000;

    public static void validateUseAmount(long currentPoint, long useAmount) {
        if (currentPoint < useAmount) {
            throw new PointException("잔여 포인트가 부족합니다.");
        }
    }

    public static void validateAmount(long amount) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "금액은 0보다 커야 합니다.");
        }
        if (amount > MAX_CHARGE_AMOUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최대 충전 금액은 " + MAX_CHARGE_AMOUNT + "원입니다.");
        }
    }
}
