package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.exception.PointException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public UserPoint selectPointById(long id) {

        PointValidator.validateId(id);

        UserPoint userPoint = userPointTable.selectById(id);

        if (userPoint == null) {
            throw new PointException("입력한 ID가 존재하지 않습니다.");
        }

        return userPoint;
    }

    public UserPoint charge(long id, long amount) {

        PointValidator.validateId(id);
        PointValidator.validateAmount(amount);

        UserPoint userPoint = selectPointById(id);
        long updateAmount = userPoint.point() + amount;

        // 포인트 충전
        UserPoint updateUserPoint = userPointTable.insertOrUpdate(id, updateAmount);

        // 포인트 충전 내역 추가
        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return updateUserPoint;
    }

    public UserPoint use(long id, long amount) {

        PointValidator.validateId(id);

        UserPoint userPoint = selectPointById(id);
        PointValidator.validateUseAmount(userPoint.point(), amount);

        if (userPoint.point() < amount) {
            throw new PointException("잔여 포인트가 부족합니다.");
        }

        long updateAmount = userPoint.point() - amount;
        UserPoint updateUserPoint = userPointTable.insertOrUpdate(id, updateAmount);

        // 포인트 사용 내역
        pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());
        return updateUserPoint;
    }
}
