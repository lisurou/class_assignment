package org.example.classAssignment.pojo;

import lombok.Data;

import java.util.List;

@Data
public class MaterialResult {
    private Boolean success;
    private String message;
    private List<MaterialFolder> folders;
    private List<MaterialAttachment> attachments;
    private List<MaterialLink> links;
    private MaterialFolder folder;
    private MaterialAttachment attachment;
    private MaterialLink link;
}
