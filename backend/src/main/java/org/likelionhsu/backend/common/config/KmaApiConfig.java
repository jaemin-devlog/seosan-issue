package org.likelionhsu.backend.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
@ConfigurationProperties(prefix = "kma.api")
@Getter
@Setter
public class KmaApiConfig {

    private String serviceKey;
    private String baseUrl;
    private List<RegionCoordinate> gridCoords;

    // ★ 추가: 도시 → 지역 리스트
    private Map<String, List<String>> cityGroups;

    @Getter @Setter
    public static class RegionCoordinate {
        private String name;
        private int nx;
        private int ny;
    }

    // ★ 추가: 지역명으로 좌표 찾기
    public Optional<RegionCoordinate> findByName(String name) {
        if (gridCoords == null) return Optional.empty();
        return gridCoords.stream().filter(rc -> rc.getName().equals(name)).findFirst();
    }

    // ★ 추가: 도시의 지역 리스트 반환
    public List<String> regionsOfCity(String city) {
        return cityGroups == null ? List.of() : cityGroups.getOrDefault(city, List.of());
    }
}