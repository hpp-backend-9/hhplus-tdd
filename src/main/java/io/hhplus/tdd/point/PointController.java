package io.hhplus.tdd.point;

import io.hhplus.tdd.point.service.PointService;
import io.hhplus.tdd.point.service.PointValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final PointService pointService;

    /**
     * 특정 유저의 포인트 조회
     *
     * @param id 조회할 유저의 ID
     * @return 유저의 포인트 정보
     */
    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable long id
    ) {
        validateId(id);
        return pointService.selectPointById(id);
    }

    /**
     * 특정 유저의 포인트 충전/이용 내역 조회
     *
     * @param id 조회할 유저의 ID
     * @return 유저의 포인트 충전 및 이용 내역 리스트
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable long id
    ) {
        validateId(id);
        return pointService.history(id);
    }

    /**
     * 특정 유저의 포인트 충전 기능
     *
     * @param id     조회할 유저의 ID
     * @param amount 충전할 포인트 금액
     * @return 충전된 이후의 유저 포인트 정보
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        validateId(id);
        PointValidator.validateAmount(amount);
        return pointService.charge(id, amount);
    }

    /**
     * 특정 유저의 포인트 사용
     *
     * @param id     조회할 유저의 ID
     * @param amount 사용할 포인트 금액
     * @return 사용 이후의 유저 포인트 정보
     */
    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        validateId(id);
        PointValidator.validateAmount(amount);
        return pointService.use(id, amount);
    }

    /**
     * 유효한 ID인지 확인하는 메서드
     *
     * @param id 조회할 유저의 ID
     */
    private void validateId(long id) {
        if (id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID는 0 이상의 숫자여야 합니다.");
        }
    }

}
