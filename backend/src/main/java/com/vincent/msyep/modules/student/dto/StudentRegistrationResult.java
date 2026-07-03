package com.vincent.msyep.modules.student.dto;

import com.vincent.msyep.modules.student.Student;

/** Returned after a student is registered. */
public record StudentRegistrationResult(
        Student student,
        String registerNo,
        String batchCode,
        String loginId,
        String note
) {}
