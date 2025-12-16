package com.seibel.cpss.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestCompanyUpdate extends BaseRequest {

    @Size(max = 8, message = "The code must be at most 8 characters.")
    private String code;

    @Size(max = 64, message = "The name must be at most 64 characters.")
    private String name;

    @Size(max = 128, message = "The description must be at most 128 characters.")
    private String description;
}
