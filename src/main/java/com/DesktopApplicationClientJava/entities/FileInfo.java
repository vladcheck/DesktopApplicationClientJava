package com.DesktopApplicationClientJava.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    private UUID uuid;
    private String name;
    private String contentType;
    private Long size;
}