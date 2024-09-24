package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.exception.PointException;

public class PointValidator {
    public static void validateId(long id) {
        if (id <= 0) {
            throw new PointException("ID는 0 이상의 숫자여야 합니다.");
        }
    }

    public static void validateAmount(long amount) {
        if (amount <= 0) {
            throw new PointException("충전 금액은 0보다 커야 합니다.");
        }
    }

    public static void validateUseAmount(long currentPoint, long useAmount) {
        if (useAmount <= 0) {
            throw new PointException("사용할 포인트는 0보다 커야 합니다.");
        }

        if (currentPoint <= 0) {
            throw new PointException("잔여 포인트가 부족합니다.");
        }
    }
}
