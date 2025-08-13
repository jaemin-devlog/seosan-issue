package org.likelionhsu.backend.post.repository;

import org.likelionhsu.backend.post.domain.Category;
import org.likelionhsu.backend.post.domain.Post;
import org.springframework.data.jpa.domain.Specification;

public class PostSpecification {

    public static Specification<Post> hasRegion(String region) {
        return (root, query, criteriaBuilder) -> {
            if (region == null || region.trim().isEmpty()) {
                return null; // 조건이 없으면 이 Specification은 무시됨
            }
            if ("서산시 전체".equals(region)) {
                // "서산시 전체"가 요청되면, 지역 필터링을 적용하지 않아 모든 게시물이 보이도록 함
                return null;
            } else {
                // 특정 지역(예: "음암면")이 요청되면, 해당 지역 또는 "서산시 전체" 게시물을 함께 조회
                return criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("region"), region),
                    criteriaBuilder.equal(root.get("region"), "서산시 전체")
                );
            }
        };
    }

    public static Specification<Post> hasCategory(Category category) {
        return (root, query, criteriaBuilder) -> {
            if (category == null) {
                return null; // 조건이 없으면 이 Specification은 무시됨
            }
            return criteriaBuilder.equal(root.get("category"), category);
        };
    }
}
