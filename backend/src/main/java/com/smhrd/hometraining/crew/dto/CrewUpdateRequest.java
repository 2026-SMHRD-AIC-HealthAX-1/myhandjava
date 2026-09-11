package com.smhrd.hometraining.crew.dto;

/** 크루 소개/컨셉 수정. null인 필드는 그대로 둔다(UserService.updateProfile()과 같은 부분수정 패턴). */
public record CrewUpdateRequest(String description, String concept) {}
