package com.whut.training.service;

import com.whut.training.domain.dto.ProblemListDetail;
import com.whut.training.domain.dto.ProblemListItemRequest;
import com.whut.training.domain.dto.ProblemListItemView;
import com.whut.training.domain.dto.ProblemListSaveRequest;
import com.whut.training.domain.dto.ProblemListSummary;
import com.whut.training.domain.entity.User;

import java.util.List;

/** 题单应用用例，避免 Web 层依赖具体业务实现。 */
public interface ProblemListUseCase {
    List<ProblemListSummary> listVisible(User currentUser);

    ProblemListDetail get(User currentUser, Long listId);

    ProblemListDetail create(User currentUser, ProblemListSaveRequest request);

    ProblemListDetail update(User currentUser, Long listId, ProblemListSaveRequest request);

    void delete(User currentUser, Long listId);

    ProblemListItemView addItem(User currentUser, Long listId, ProblemListItemRequest request);

    ProblemListItemView updateItem(User currentUser, Long listId, Long itemId, ProblemListItemRequest request);

    void deleteItem(User currentUser, Long listId, Long itemId);
}
