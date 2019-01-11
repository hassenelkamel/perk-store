package org.exoplatform.addon.perkstore.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class UserOrders implements Serializable, Cloneable {
  private static final long serialVersionUID = -7144496703478026420L;

  private double            purchasedInCurrentPeriod;

  private double            totalPuchased;

  @SuppressWarnings("all")
  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException("Error while cloning object");
    }
  }
}