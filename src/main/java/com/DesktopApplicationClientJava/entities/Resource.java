package com.DesktopApplicationClientJava.entities;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Resource {
  private UUID uuid;
  private String title;
  private String description;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<FileInfo> files;
}
