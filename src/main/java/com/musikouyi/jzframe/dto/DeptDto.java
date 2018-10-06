package com.musikouyi.jzframe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeptDto {

    private int id;
    private String fullName;
    private String parent_dept;
}
