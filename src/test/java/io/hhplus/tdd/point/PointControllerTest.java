package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PointControllerTest {
    /**
     * 컨트롤러 테스트 = 입력 유효성 검사, HTTP 응답 테스트
     * MockMvc를 사용하여 실제 HTTP 요청을 시뮬레이션하고, 컨트롤러에서 입력 유효성 테스트 검증.
     * 잘못된 입력에 대해서 사용자에게 피드백 제공. (사용자와의 인터페이스 검증 중점)
     * 외부에서 들어오는 요청을 받고, 입력 값의 유효성 검사 후 서비스 레이어 호출.
     */
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("유효하지 않은 패턴의 ID가 입력된 경우 예외 발생")
    void invalidIdFormat() throws Exception {
        // given : 유효하지 않은 ID 설정
        long invalidId = -1L;

        // when, then : 잘못된 ID로 요청 시 예외 발생
        mockMvc.perform(get("/point/{id}", invalidId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())  // 이제 400 상태 코드가 반환됩니다.
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(ResponseStatusException.class)
                        .hasMessageContaining("ID는 0 이상의 숫자여야 합니다."));
    }

    @Test
    @DisplayName("0을 충전하려는 경우 예외 발생")
    void chargeZeroPoint() throws Exception {
        // given : 유효한 ID 설정
        long validId = 1L;

        mockMvc.perform(patch("/point/{id}/charge", validId)
                        .contentType(MediaType.APPLICATION_JSON).content("0")) // JSON 문자열로 값 전달
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertThat(result.getResolvedException())
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("금액은 0보다 커야 합니다."));
    }

    @Test
    @DisplayName("음수를 충전하려는 경우 예외 발생")
    void chargeNegativePoint() throws Exception {
        // given : 유효한 ID 설정
        long validId = 1L;

        mockMvc.perform(patch("/point/{id}/charge", validId)
                        .contentType(MediaType.APPLICATION_JSON).content("-100"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(ResponseStatusException.class)
                        .hasMessageContaining("금액은 0보다 커야 합니다."));
    }

    @Test
    @DisplayName("최대 충전 가능한 금액을 초과하는 경우 예외 발생")
    void chargeOverMaxPoint() throws Exception {
        // given : 유효한 ID 설정 및 최대 금액 초과 설정
        long validId = 1L;
        long overMaxPoint = 20000;

        mockMvc.perform(patch("/point/{id}/charge", validId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(overMaxPoint)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(ResponseStatusException.class)
                        .hasMessageContaining("최대 충전 금액은 10000원입니다."));
    }
}
