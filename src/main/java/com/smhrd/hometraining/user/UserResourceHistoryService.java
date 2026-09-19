package com.smhrd.hometraining.user;

import org.springframework.stereotype.Service;

import com.smhrd.hometraining.user.entity.User;
import com.smhrd.hometraining.user.entity.UserResourceHistory;
import com.smhrd.hometraining.user.entity.UserResourceHistory.Reason;
import com.smhrd.hometraining.user.entity.UserResourceHistory.ResourceType;
import com.smhrd.hometraining.user.repository.UserResourceHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserResourceHistoryService {

    private final UserResourceHistoryRepository repository;

    public void record(
            User user,
            ResourceType resourceType,
            long changeAmount,
            long balanceBefore,
            long balanceAfter,
            Reason reason,
            Object sourceId
    ) {

        if (changeAmount == 0) {
            return;
        }

        repository.save(
                UserResourceHistory.create(
                        user,
                        resourceType,
                        changeAmount,
                        balanceBefore,
                        balanceAfter,
                        reason,
                        sourceId == null
                                ? null
                                : sourceId.toString()
                )
        );
    }
}
