package com.smhrd.hometraining.exercise.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 운동 탭 종목선택 화면에 노출되는 종목 카탈로그 (script.js의 EXS). */
@Entity
@Table(name = "exercise_definitions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExerciseDefinition {

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 30)
    private String target;

    @Column(nullable = false, length = 10)
    private String level;

    public static ExerciseDefinition of(String code, String name, String target, String level) {
        ExerciseDefinition e = new ExerciseDefinition();
        e.code = code;
        e.name = name;
        e.target = target;
        e.level = level;
        return e;
    }
}
