package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Integer> {

    /**
     * 根据PromptType和PromptName查找Prompt
     * 
     * @param promptType 提示类型
     * @param promptName 提示名称
     * @return 匹配的PromptTemplate对象
     */
    PromptTemplate findByPromptTypeAndPromptName(String promptType, String promptName);

    /**
     * 查找所有激活状态的Prompt模板
     * @param isActive 是否激活
     * @return 激活的Prompt模板列表
     */
    List<PromptTemplate> findByIsActive(Boolean isActive);

    /**
     * 根据激活状态和Prompt类型查找Prompt模板
     * 
     * @param isActive 是否激活
     * @param promptType 提示类型
     * @return 匹配的Prompt模板列表
     */
    List<PromptTemplate> findByIsActiveAndPromptType(Boolean isActive, String promptType);
}
