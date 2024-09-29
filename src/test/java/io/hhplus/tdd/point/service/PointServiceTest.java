package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.exception.PointException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {
    /**
     * 서비스 테스트 = 비즈니스 로직 검증 (단위 테스트)
     * 서비스 내부의 동작을 Mock 객체와 함께 독립적으로 테스트
     * 서비스 내부 로직의 작동 확인, 데이터의 일관성 및 규칙 준수 여부 검증
     */

    @InjectMocks
    private PointService pointService;

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    // 포인트 조회
    private void pointInquiry(long id, long currentPoint) {
        UserPoint userPoint = new UserPoint(id, currentPoint, System.currentTimeMillis());
        when(userPointTable.selectById(id)).thenReturn(userPoint);
    }

    // 사용자가 존재하지 않음
    private void userNotExist(long id) {
        when(userPointTable.selectById(id)).thenReturn(null);
    }

    @Test
    @DisplayName("존재하지 않는 ID인 경우")
    void notExsistId() {
        // given : 입력한 ID가 존재하지 않다면
        long nonExistingId = 999999999999L;
        userNotExist(nonExistingId);

        // when, then : 예외 발생
        assertThatThrownBy(() -> pointService.selectPointById(999999999999L))
                .isInstanceOf(PointException.class)
                .hasMessageContaining("입력한 ID가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("포인트 사용하려고 하는데 아이디가 없는 경우")
    void cannotUsePointsIfIdDoesNotExist() {
        // given : 존재하지 않는 않는 ID로 포인트를 사용하려고 하는 경우
        long notExistId = 99999999L;
        int usePoint = 1000;

        userNotExist(notExistId);

        //when, then : 예외 발생
        assertThatThrownBy(() -> pointService.use(notExistId, usePoint))
                .isInstanceOf(PointException.class)
                .hasMessageContaining("입력한 ID가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("잔여 포인트보다 더 많이 사용하고자 하는 경우")
    void exceededPoint() {
        // given : 사용자의 포인트가 1000인데 10000을 사용하고자 하는 경우
        long id = 1L;
        int currentPoint = 1000;
        int wantUsePoint = 10000;

        pointInquiry(id, currentPoint);

        // When, then : 예외 발생
        assertThatThrownBy(() -> pointService.use(id, wantUsePoint))
                .isInstanceOf(PointException.class)
                .hasMessageContaining("잔여 포인트가 부족합니다.");
    }

    @Test
    @DisplayName("정상적으로 포인트를 사용한 경우")
    void usePointSuccessfully() {
        // given : 기존 포인트가 2000일 때 500 사용
        long id = 1L;
        int currentPoint = 2000;
        int usePoint = 500;
        int remainPoint = currentPoint - usePoint;

        pointInquiry(id, currentPoint);

        // when : 포인트 사용
        pointService.use(id, usePoint);

        // then : 포인트 차감 확인
        verify(userPointTable).insertOrUpdate(id, remainPoint);
    }

    @Test
    @DisplayName("포인트 사용 시 포인트 내역에 기록되는지 확인")
    void useAndRecordPointHistory() {
        // given : 500 포인트 사용
        long id = 1L;
        long currentPoint = 2000;
        long usePoint = 500;
        pointInquiry(id, currentPoint);

        // when : 포인트 사용
        pointService.use(id, usePoint);

        // then : 포인트 내역에 사용 기록이 추가되는지 확인
        verify(userPointTable).insertOrUpdate(eq(id), eq(currentPoint - usePoint));
        verify(pointHistoryTable).insert(eq(id), eq(usePoint), eq(TransactionType.USE), anyLong());
    }

    @Test
    @DisplayName("최대 충전 금액을 초과하는 경우 예외 발생")
    void chargeOverMaxAmount() {
        // given : 유효한 사용자 ID와 현재 포인트 설정
        long id = 1L;
        long currentPoint = 1000L;
        long overMaxAmount = 20000L;

        pointInquiry(id, currentPoint);

        // when, then : 포인트 충전 시 예외 발생
        assertThatThrownBy(() -> pointService.charge(id, overMaxAmount))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("최대 충전 금액은 10000원입니다.");
    }

    @Test
    @DisplayName("정상적으로 포인트를 충전하는 경우")
    void chargePointSuccessfully() {
        // given : 정상적인 충전 포인트가 입력되었을 때
        long id = 1L;
        long currentPoint = 1000;
        long chargePoint = 200;
        long totalPoint = currentPoint + chargePoint;

        pointInquiry(id, currentPoint);

        // when : 포인트 충전
        pointService.charge(id, chargePoint);

        // then : 포인트가 정상적으로 충전되었는지 확인
        verify(userPointTable).insertOrUpdate(id, totalPoint);
    }

    @Test
    @DisplayName("포인트 충전 시 포인트 내역이 기록되는지 확인")
    void chargePointRecordHistory() {
        long id = 1L;
        long currentPoint = 3000;
        long chargePoint = 1000;
        pointInquiry(id, currentPoint);

        pointService.charge(id, chargePoint);

        // then : 포인트 내역에 충전 기록이 추가 되는지 확인
        verify(userPointTable).insertOrUpdate(eq(id), eq(currentPoint + chargePoint));
        verify(pointHistoryTable).insert(eq(id), eq(chargePoint), eq(TransactionType.CHARGE), anyLong());
    }
}
