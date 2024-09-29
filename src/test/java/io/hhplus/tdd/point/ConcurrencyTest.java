package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.exception.PointException;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
public class ConcurrencyTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;

    @Test
    @DisplayName("동시에 포인트 충전 및 사용 요청이 들어온 경우")
    void pointChargeAndUse() throws PointException {

        // given : 사용자 및 초기 포인트
        long id = 2L;
        userPointTable.insertOrUpdate(id, 0L);

        // when : 포인트 충전, 사용 요청
        // CompletableFuture.allOf().join() : 동시에 실행된 여러 비동기 작업들이 끝날 때까지 기다리고 완료 상태를 확인할 때 사용
        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> pointService.charge(id, 200L)),
                CompletableFuture.runAsync(() -> pointService.use(id, 100L)),
                CompletableFuture.runAsync(() -> pointService.charge(id, 10L)),
                CompletableFuture.runAsync(() -> pointService.charge(id, 40L))
        ).join();

        // then : 올바르게 계산되었는지 확인
        UserPoint userPoint = pointService.selectPointById(id);
        assertThat(userPoint.point()).isEqualTo(150L);
    }

    @Test
    @DisplayName("동시에 여러 사용자가 포인트 충전 및 사용 요청이 들어온 경우")
    void pointChargeAndUseDifferentUsers() {
        // given : 여러 사용자 설정
        long user1 = 2L;
        long user2 = 3L;
        userPointTable.insertOrUpdate(user1, 0L);
        userPointTable.insertOrUpdate(user2, 500L);

        // when : 두 사용자가 동시에 포인트 충전 및 사용
        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> pointService.charge(user1, 200L)),
                CompletableFuture.runAsync(() -> pointService.use(user1, 100L)),
                CompletableFuture.runAsync(() -> pointService.charge(user2, 50L)),
                CompletableFuture.runAsync(() -> pointService.use(user2, 30L))
        ).join();

        // then : 각 사용자의 포인트 상태 확인
        UserPoint userPoint1 = pointService.selectPointById(user1);
        UserPoint userPoint2 = pointService.selectPointById(user2);
        assertThat(userPoint1.point()).isEqualTo(100L);
        assertThat(userPoint2.point()).isEqualTo(520L);
    }

    @Test
    @DisplayName("포인트 사용 시 잔여 포인트보다 더 많은 포인트를 사용하려고 하는 경우")
    void useMorePointsThanHave() {

        // given : 사용자 및 초기 포인트
        long id = 2L;
        userPointTable.insertOrUpdate(id, 100L);

        // when : 잔여 포인트보다 더 많은 포인트를 사용하려고 하는 경우
        // CompletableFuture<Void> : 비동기 작업 중 예외가 발생하거나 처리 결과가 중요할 때 사용
        CompletableFuture<Void> completableFuture = CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> pointService.use(id, 50L)),
                CompletableFuture.runAsync(() -> pointService.use(id, 30L)),
                CompletableFuture.runAsync(() -> pointService.use(id, 40L))
        );

        // then : 예외처리
        // completableFuture의 작업이 끝날 대까지 기다렸다가 결과를 반환하거나 예외를 던짐
        Throwable throwable = catchThrowable(completableFuture::join);
        // 비동기식 예외(CompletionException)에 PointException이 포함되는지 확인
        assertThat(throwable).isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(PointException.class);
    }
}
