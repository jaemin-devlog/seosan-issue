package org.likelionhsu.backend.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "kma.api")
@Component
@Getter
@Setter
public class KmaApiProperties {

    private String serviceKey;
    private String baseUrl;
    private List<RegionCoordinate> gridCoords;

    @Getter
    @Setter
    public static class RegionCoordinate {
        private String name;
        private int nx;
        private int ny;
    }
}

