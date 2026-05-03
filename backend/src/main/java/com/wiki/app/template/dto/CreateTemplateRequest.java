package com.wiki.app.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTemplateRequest {
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称不能超过128个字符")
    private String name;

    @Size(max = 512, message = "模板描述不能超过512个字符")
    private String description;

    private Long kbId;

    private String markdownContent;

    private Boolean isPublic = false;

    @Size(max = 64, message = "分类名称不能超过64个字符")
    private String category;

    private String coverUrl;
}
