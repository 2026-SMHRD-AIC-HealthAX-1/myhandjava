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

/**
 * [담당] 크루대전 진행 중 실시간 렙(반복) 판정 결과를 서버에 반영하고 양팀에 브로드캐스트.
 * [프론트 연동] ounhome-f/js/exercise.js가 크루대전 모드일 때 렙 판정마다 STOMP publish
 *              /app/crew-battles/reps → 응답은 /topic/crew-battles/{battleId}로 양팀에 전송.
 * [DB] CrewBattleService → crew_battle_participants(점수 누적), crew_battles(팀 합계) 갱신.
 * [주의] ⚠️ 운동 중 프레임마다 호출될 수 있는 고빈도 경로라, 여기서 무거운 쿼리를 추가하면
 *        실시간성이 바로 체감될 만큼 느려진다. 동시성 처리(버전/락)를 CrewBattleService가 맡고 있음.
 */
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