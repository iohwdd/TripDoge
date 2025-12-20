package com.tripdog.common.enums;

import lombok.Getter;

public enum DocParseStatus {
    PARSING(0, "解析中"),
    SUCCESS(1,"解析成功"),
    FAIL(2,"解析失败"),
    ;

    @Getter
    private int status;
    private String desc;
    DocParseStatus(int status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public static DocParseStatus getDocParseStatus(int status) {
        for (DocParseStatus docParseStatus : DocParseStatus.values()) {
            if(docParseStatus.status == status){
                return docParseStatus;
            }
        }
        return null;
    }

}
