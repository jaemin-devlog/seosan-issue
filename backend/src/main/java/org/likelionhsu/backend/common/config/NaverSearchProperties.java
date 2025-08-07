package org.likelionhsu.backend.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "naver.search")
@Getter
@Setter
public class NaverSearchProperties {
    private List<String> queries;
    private List<String> regions;
}
