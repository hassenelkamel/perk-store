package org.exoplatform.addon.perkstore.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class GlobalSettings implements Serializable, Cloneable {

  private static final long      serialVersionUID = 6313043752170656574L;

  private transient List<String> productCreationPermissions;

  private transient List<String> accessPermissions;

  private boolean                isAdministrator;

  private boolean                canAddProduct;

  private String                 symbol;

  @SuppressWarnings("all")
  public GlobalSettings clone() {
    try {
      return (GlobalSettings) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException("Error while cloning object");
    }
  }
}