package com.platform.dto.role;

import lombok.Data;
import java.util.List;

@Data
public class UpdateRoleRequest {
    private String name;
    private String description;
    private List<Long> permissionIds;
}
