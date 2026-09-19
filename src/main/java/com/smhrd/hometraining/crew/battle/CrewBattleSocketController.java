package com.smhrd.hometraining.crew.battle;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.crew.battle.dto.CrewBattleEventResponse;
import com.smhrd.hometraining.crew.battle.dto.CrewBattleRepRequest;
import com.smhrd.hometraining.security.CustomUserPrincipal;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CrewBattleSocketController {

    private final CrewBattleService crewBattleService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 크루대전 참가자의 운동 판정 결과를 등록합니다.
     *
     * 실제 참가자 여부와 ACTIVE 상태 여부는
     * CrewBattleService에서 다시 검증합니다.
     */
    @MessageMapping("/crew-battles/reps")
    public void registerRep(
            CrewBattleRepRequest request,
            Principal principal
    ) {

        if (!(principal instanceof Authentication authentication)
                || !(authentication.getPrincipal()
                        instanceof CustomUserPrincipal userPrincipal)) {

            throw new BusinessException(
                    "로그인이 필요합니다."
            );
        }

        CrewBattleEventResponse response =
                crewBattleService.registerRep(
                        userPrincipal.getUserId(),
                        request
                );

        /*
         * 같은 대전에 접속한 양쪽 크루 참가자에게
         * 최신 운동 판정과 크루 점수를 전송합니다.
         */
        messagingTemplate.convertAndSend(
                "/topic/crew-battles/"
                        + response.battleId(),
                response
        );
    }
}