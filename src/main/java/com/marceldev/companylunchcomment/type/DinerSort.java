package com.marceldev.companylunchcomment.type;

import lombok.Getter;

@Getter
public enum DinerSort {
  DINER_NAME_ASC("name", "ASC"),
  DINER_NAME_DESC("name", "DESC");

  private final String field;
  private final String direction;

  DinerSort(String field, String direction) {
    this.field = field;
    this.direction = direction;
  }
}
