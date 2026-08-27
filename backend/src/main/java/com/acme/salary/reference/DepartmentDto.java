package com.acme.salary.reference;

public record DepartmentDto(Long id, String name) {
    public static DepartmentDto from(Department department) {
        return new DepartmentDto(department.getId(), department.getName());
    }
}
