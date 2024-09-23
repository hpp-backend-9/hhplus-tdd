package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;

    public UserPoint selectPointById(long id) {

        if (id <= 0) {
            throw new IllegalArgumentException("ID는 0 이상의 숫자여야 합니다.");
        }

        UserPoint userPoint = userPointTable.selectById(id);

        if (userPoint == null) {
            throw new IllegalArgumentException("입력한 ID가 존재하지 않습니다.");
        }

        return userPoint;
    }

    public UserPoint charge(long id, long amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야합니다.");
        }

        UserPoint userPoint = selectPointById(id);
        long updateAmount = userPoint.point() + amount;

        return userPointTable.insertOrUpdate(id, updateAmount);
    }
}
