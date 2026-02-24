package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.DepartmentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DepartmentGroupRepository extends JpaRepository<DepartmentGroup, Integer> {
    List<DepartmentGroup> findByDepartmentId(int departmentId);
}
