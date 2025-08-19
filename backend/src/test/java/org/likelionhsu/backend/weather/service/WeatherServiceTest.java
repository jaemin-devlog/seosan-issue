package org.likelionhsu.backend.weather.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.backend.common.config.KmaApiConfig;
import org.likelionhsu.backend.weather.dto.WeatherCardDto;
import org.likelionhsu.backend.weather.dto.WeatherCardsResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private KmaApiConfig kmaApiConfig;

    // 실제 ObjectMapper를 사용하여 JSON 파싱 로직을 테스트
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        // 오류 해결: new RegionCoordinate() 생성자가 없으므로, 기본 생성자와 setter를 사용하도록 수정
        KmaApiConfig.RegionCoordinate haemi = new KmaApiConfig.RegionCoordinate();
        haemi.setName("해미면");
        haemi.setNx(68);
        haemi.setNy(99);

        KmaApiConfig.RegionCoordinate seongyeon = new KmaApiConfig.RegionCoordinate();
        seongyeon.setName("성연면");
        seongyeon.setNx(69);
        seongyeon.setNy(100);

        List<KmaApiConfig.RegionCoordinate> coords = List.of(haemi, seongyeon);

        // Mock 설정
        given(kmaApiConfig.getGridCoords()).willReturn(coords);
        given(kmaApiConfig.getBaseUrl()).willReturn("https://api.test");
        given(kmaApiConfig.getServiceKey()).willReturn("DUMMY_KEY");

        // RestTemplate의 동작을 모의(Mocking)
        // invocation을 사용하여 들어오는 요청 URI에 따라 다른 응답을 반환하도록 설정
        given(restTemplate.exchange(any(URI.class), eq(org.springframework.http.HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .willAnswer(invocation -> {
                    URI uri = invocation.getArgument(0);
                    String path = uri.getPath();
                    String query = uri.getQuery();
                    String body;

                    // 실황(ncst) API 호출에 대한 모의 응답
                    if (path.contains("getUltraSrtNcst")) {
                        if (query.contains("nx=68")) { // 해미면
                            body = "{\"response\":{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":{\"item\":[{\"category\":\"T1H\",\"obsrValue\":\"29.6\"},{\"category\":\"REH\",\"obsrValue\":\"72\"},{\"category\":\"WSD\",\"obsrValue\":\"1.1\"},{\"category\":\"VEC\",\"obsrValue\":\"220\"}]}}}}";
                        } else { // 성연면
                            body = "{\"response\":{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":{\"item\":[{\"category\":\"T1H\",\"obsrValue\":\"28.8\"},{\"category\":\"REH\",\"obsrValue\":\"65\"},{\"category\":\"WSD\",\"obsrValue\":\"0.8\"},{\"category\":\"VEC\",\"obsrValue\":\"270\"}]}}}}";
                        }
                        return new ResponseEntity<>(body, HttpStatus.OK);
                    }

                    // 예보(fcst) API 호출에 대한 모의 응답
                    if (path.contains("getUltraSrtFcst")) {
                        if (query.contains("nx=68")) { // 해미면
                            body = "{\"response\":{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":{\"item\":[{\"category\":\"SKY\",\"fcstDate\":\"20990101\",\"fcstTime\":\"1100\",\"fcstValue\":\"1\"},{\"category\":\"PTY\",\"fcstDate\":\"20990101\",\"fcstTime\":\"1100\",\"fcstValue\":\"0\"}]}}}}";
                        } else { // 성연면
                            body = "{\"response\":{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":{\"item\":[{\"category\":\"SKY\",\"fcstDate\":\"20990101\",\"fcstTime\":\"1100\",\"fcstValue\":\"3\"},{\"category\":\"PTY\",\"fcstDate\":\"20990101\",\"fcstTime\":\"1100\",\"fcstValue\":\"0\"}]}}}}";
                        }
                        return new ResponseEntity<>(body, HttpStatus.OK);
                    }

                    return new ResponseEntity<>("{\"response\":{\"header\":{\"resultCode\":\"99\"}}}", HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("gridCoords 2곳에 대해 실황+예보를 결합하여 카드 2개 생성")
    void getCardsByCity_buildsTwoCardsSuccessfully() {
        // When
        WeatherCardsResponse res = weatherService.getCardsByCity("서산시 전체");

        // Then
        assertThat(res.getCity()).isEqualTo("서산시 전체");
        assertThat(res.getCards()).hasSize(2);

        // 첫 번째 카드(해미면) 검증
        WeatherCardDto haemiCard = res.getCards().stream()
                .filter(c -> "해미면".equals(c.getRegion()))
                .findFirst().orElse(null);

        assertThat(haemiCard).isNotNull();
        assertThat(haemiCard.getTemperature()).isEqualTo(29.6);
        assertThat(haemiCard.getWindDirection()).isEqualTo("남서풍");
        assertThat(haemiCard.getCondition()).isEqualTo("맑음"); // SKY=1, PTY=0 -> 맑음

        // 두 번째 카드(성연면) 검증
        WeatherCardDto seongyeonCard = res.getCards().stream()
                .filter(c -> "성연면".equals(c.getRegion()))
                .findFirst().orElse(null);

        assertThat(seongyeonCard).isNotNull();
        assertThat(seongyeonCard.getTemperature()).isEqualTo(28.8);
        assertThat(seongyeonCard.getWindDirection()).isEqualTo("서풍");
        assertThat(seongyeonCard.getCondition()).isEqualTo("흐림"); // SKY=3, PTY=0 -> 흐림 (코드 로직상 구름많음도 흐림으로 처리됨)
    }
}
