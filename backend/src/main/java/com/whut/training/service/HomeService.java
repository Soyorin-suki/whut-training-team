package com.whut.training.service;

import com.whut.training.domain.dto.HomeOverview;

public interface HomeService {
    HomeOverview getOverview(int topLimit);
}
