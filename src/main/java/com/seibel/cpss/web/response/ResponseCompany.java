package com.seibel.cpss.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseCompany {
    private String extid;
    private String code;
    private String name;
    private String description;
}
