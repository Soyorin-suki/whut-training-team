package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.domain.entity.UserRank;
import com.whut.training.service.RankService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/rank")
public class RankController {
    private final RankService rankService;
    public RankController(RankService rankService){
        this.rankService = rankService;
    }
    public ApiResponse<List<UserRank>> queryAllRank(@PathVariable Integer page, @PathVariable Integer pageSize){
        return ApiResponse.ok(rankService.queryAllRank(page, pageSize));
    }
    public void queryRank(){

    }
    public void updateRank(){

    }
}
