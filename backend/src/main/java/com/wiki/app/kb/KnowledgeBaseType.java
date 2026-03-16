package com.wiki.app.kb;

public enum KnowledgeBaseType {
    COMPANY,    // 公司公开：全公司所有员工可见、可编辑
    DEPARTMENT, // 部门：部门内成员可见、可编辑，部门部长有管理权
    PRIVATE     // 私有：仅创建者可见、可编辑
}
