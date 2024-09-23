package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private UserPointTable userPointTable;

    @Test
    @DisplayName("존재하지 않는 ID인 경우")
    void notExsistId() {
        // given : 입력한 ID가 존재하지 않다면
        when(userPointTable.selectById(999999999999L)).thenReturn(null);

        // when, then : 예외 발생
        assertThatThrownBy(() -> pointService.selectPointById(999999999999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("입력한 ID가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("유효하지 않은 패턴의 ID가 입력된 경우")
    void invalidIdFormat() {
        // then : 예외 발생
        assertThatThrownBy(() -> pointService.selectPointById(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID는 0 이상의 숫자여야 합니다.");
    }

    @Test
    @DisplayName("기존 포인트에 0을 충전하는 경우")
    void chargeZeroPoint() {
        // when, then : 0원을 충전하려고 한다면 예외 발생
        assertThatThrownBy(() -> pointService.charge(1L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("충전 금액은 0보다 커야합니다.");
    }

    @Test
    @DisplayName("기존 포인트에 1000을 충전하는 경우")
    void chargeThousandPoint() {
        // given : 사용자 ID가 1L이고 기존 포인트가 2000인 상태에서 1000 충전
        long id = 1L;
        int currentPoint = 3000;
        int chargePoint = 1000;
        int totalPoint = currentPoint + chargePoint;

        // when : 이용자의 현재 포인트 조회 후 1000원 충전
        UserPoint currentUserPoint = new UserPoint(id, currentPoint, System.currentTimeMillis());
        when(userPointTable.selectById(id)).thenReturn(currentUserPoint);
        pointService.charge(id, chargePoint);

        // then : 2000 + 1000 = 3000이 맞는지 확인
        // 메서드 호출 검증(verify)
        verify(userPointTable).insertOrUpdate(id, totalPoint);
        UserPoint updateUserPoint = new UserPoint(id, totalPoint, System.currentTimeMillis());
        assertThat(updateUserPoint.point()).isEqualTo(totalPoint);
    }
}
