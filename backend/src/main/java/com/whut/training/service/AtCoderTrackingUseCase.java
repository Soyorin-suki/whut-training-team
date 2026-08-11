package com.whut.training.service;

import com.whut.training.domain.dto.AtCoderAbcDashboard;
import com.whut.training.domain.dto.AtCoderExemptionRequest;
import com.whut.training.domain.dto.AtCoderTrackingSettingRequest;

/** 管理端每周 ABC 跟踪用例。 */
public interface AtCoderTrackingUseCase {
    AtCoderAbcDashboard getDashboard(String contestId);

    AtCoderAbcDashboard refreshAndGet(String contestId);

    AtCoderAbcDashboard updateSetting(AtCoderTrackingSettingRequest request, Long administratorId, String contestId);

    AtCoderAbcDashboard setExemption(String contestId, Long userId, AtCoderExemptionRequest request);
}
