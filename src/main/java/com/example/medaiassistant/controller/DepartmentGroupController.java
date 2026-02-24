package com.example.medaiassistant.controller;

import com.example.medaiassistant.model.DepartmentGroup;
import com.example.medaiassistant.repository.DepartmentGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/department-groups")
public class DepartmentGroupController {

    @Autowired
    private DepartmentGroupRepository departmentGroupRepository;

    /**
     * 根据科室ID获取专业组列表
     * 
     * @param departmentId 科室ID
     * @return 专业组列表
     */
    @GetMapping("/{departmentId}")
    public List<DepartmentGroup> getGroupsByDepartmentId(@PathVariable int departmentId) {
        return departmentGroupRepository.findByDepartmentId(departmentId);
    }
}
