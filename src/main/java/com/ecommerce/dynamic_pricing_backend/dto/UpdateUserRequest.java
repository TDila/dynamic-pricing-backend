package com.ecommerce.dynamic_pricing_backend.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
}
