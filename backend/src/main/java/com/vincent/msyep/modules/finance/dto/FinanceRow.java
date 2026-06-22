package com.vincent.msyep.modules.finance.dto;

/** One row in the finance documents table. */
public record FinanceRow(
        int serialNo,
        String studentId,
        String studentName,
        String district,
        String taluk,
        String gramPanchayat,
        String centerId,
        String centerName,
        String gramPanchayatEmail,
        int documentCount
) {}
