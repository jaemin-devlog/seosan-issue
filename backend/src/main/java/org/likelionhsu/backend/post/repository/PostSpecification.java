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
            // 특정 지역(예: "음암면")과 "서산시 전체"를 함께 조회
            return criteriaBuilder.or(
                criteriaBuilder.equal(root.get("region"), region),
                criteriaBuilder.equal(root.get("region"), "서산시 전체")
            );
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

    public static Specification<Post> containsKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String like = "%" + keyword + "%";
            return cb.or(
                    cb.like(root.get("title"), like),
                    cb.like(root.get("content"), like)
            );
        };
    }
}
