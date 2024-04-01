package com.thecodinglab.imdbclone.entity;

import com.thecodinglab.imdbclone.enums.RoleNameEnum;
import jakarta.persistence.*;

@Entity
public class Role {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RoleNameEnum name;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public RoleNameEnum getName() {
    return name;
  }

  public void setName(RoleNameEnum name) {
    this.name = name;
  }
}
