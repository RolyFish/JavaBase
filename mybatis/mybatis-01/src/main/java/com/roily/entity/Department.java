package com.roily.entity;


import lombok.*;
import org.apache.ibatis.type.Alias;

import java.util.Date;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
//@Alias("ddd")
public class Department {

  private long deptId;
  private String deptName;
  private long delete;
  private Date createTime;
  private Date modifyTime;

}
