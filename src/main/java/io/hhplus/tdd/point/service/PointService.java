package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.exception.PointException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    // 동시성 제어
    private final Lock lock = new ReentrantLock();

    public UserPoint selectPointById(long id) {

        PointValidator.validateId(id);

        UserPoint userPoint = userPointTable.selectById(id);

        if (userPoint == null) {
            throw new PointException("입력한 ID가 존재하지 않습니다.");
        }

        return userPoint;
    }

    public UserPoint charge(long id, long amount) {
        // 동기화 시작
        lock.lock();
        try {
            PointValidator.validateId(id);
            PointValidator.validateAmount(amount);

            UserPoint userPoint = selectPointById(id);
            long updateAmount = userPoint.point() + amount;

            // 포인트 충전
            UserPoint updateUserPoint = userPointTable.insertOrUpdate(id, updateAmount);

            // 포인트 충전 내역 추가
            pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return updateUserPoint;
        } finally {
            lock.unlock();
        }

    }

    public UserPoint use(long id, long amount) {
        lock.lock();
        try {
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
        } finally {
            lock.unlock();
        }
    }

    public List<PointHistory> getPointHistory(long id) {

        selectPointById(id);

        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(id);
        return histories.isEmpty() ? Collections.emptyList() : histories;
    }
}
