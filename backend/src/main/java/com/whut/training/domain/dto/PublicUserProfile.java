package com.whut.training.domain.dto;

import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.MemberType;

/**
 * 可向其他已登录成员公开的用户资料。
 *
 * <p>这里有意不包含登录账号、邮箱、角色和密码等账户字段。
 */
public record PublicUserProfile(
        Long id,
        String displayName,
        String avatarUrl,
        String bio,
        MemberType memberType,
        String codeforcesHandle,
        Integer codeforcesRating,
        Integer maxRating,
        Integer totalPoints
) {
    public static PublicUserProfile from(User user) {
        String publicName = user.getDisplayName();
        if (publicName == null || publicName.isBlank()) {
            publicName = user.getCodeforcesHandle();
        }
        if (publicName == null || publicName.isBlank()) {
            publicName = "成员 #" + user.getId();
        }
        return new PublicUserProfile(
                user.getId(),
                publicName,
                user.getAvatarUrl(),
                user.getBio(),
                user.getMemberType(),
                user.getCodeforcesHandle(),
                user.getCodeforcesRating(),
                user.getMaxRating(),
                user.getTotalPoints()
        );
    }
}
