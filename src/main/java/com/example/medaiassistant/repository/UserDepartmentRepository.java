package com.example.medaiassistant.repository;

import com.example.medaiassistant.dto.DepartmentDTO;
import com.example.medaiassistant.model.UserDepartment;
import com.example.medaiassistant.model.UserDepartmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDepartmentRepository extends JpaRepository<UserDepartment, UserDepartmentId> {

    @Query("SELECT new com.example.medaiassistant.dto.DepartmentDTO(" +
            "d.departmentName, ud.isPrimary, d.departmentId) " +
            "FROM UserDepartment ud " +
            "JOIN Department d ON ud.departmentId = d.departmentId " +
            "WHERE ud.userId = :userId " +
            "ORDER BY ud.isPrimary DESC, d.departmentName ASC")
    List<DepartmentDTO> findDepartmentsByUserId(@Param("userId") String userId);
}
