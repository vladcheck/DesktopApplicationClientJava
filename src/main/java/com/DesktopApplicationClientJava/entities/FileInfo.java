package com.DesktopApplicationClientJava.entities;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
