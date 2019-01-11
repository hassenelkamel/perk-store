/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.addon.perkstore.rest;

import static org.exoplatform.addon.perkstore.service.utils.Utils.getCurrentUserId;
import static org.exoplatform.addon.perkstore.service.utils.Utils.getErrorJSONFormat;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.addon.perkstore.exception.PerkStoreException;
import org.exoplatform.addon.perkstore.model.OrderFilter;
import org.exoplatform.addon.perkstore.model.ProductOrder;
import org.exoplatform.addon.perkstore.service.PerkStoreService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to retrieve detailed information about
 * perk store order
 */
@Path("/perkstore/api/order")
@RolesAllowed("users")
public class PerkStoreOrderREST implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(PerkStoreOrderREST.class);

  private PerkStoreService perkStoreService;

  public PerkStoreOrderREST(PerkStoreService perkStoreService) {
    this.perkStoreService = perkStoreService;
  }

  /**
   * List orders of a product
   * 
   * @param filter
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("list")
  @RolesAllowed("users")
  public Response listOrders(OrderFilter filter) {
    if (filter == null) {
      LOG.warn("Bad request sent to server with empty filter");
      return Response.status(400).build();
    }
    if (filter.getProductId() == 0) {
      LOG.warn("Bad request sent to server with empty filter product id");
      return Response.status(400).build();
    }
    try {
      List<ProductOrder> orders = perkStoreService.getOrders(getCurrentUserId(), filter);
      return Response.ok(orders).build();
    } catch (Exception e) {
      LOG.warn("Error listing orders", e);
      return Response.serverError().build();
    }
  }

  /**
   * Save order
   * 
   * @param order
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("save")
  @RolesAllowed("users")
  public Response saveOrder(ProductOrder order) {
    if (order == null) {
      LOG.warn("Bad request sent to server with empty order");
      return Response.status(400).build();
    }
    try {
      perkStoreService.createOrder(getCurrentUserId(), order);
      return Response.ok().build();
    } catch (PerkStoreException e) {
      LOG.warn("Error saving new order", e);
      return Response.serverError()
                     .entity(getErrorJSONFormat(e))
                     .build();
    } catch (Exception e) {
      LOG.warn("Error saving new order", e);
      return Response.serverError().build();
    }
  }

  /**
   * Save order status
   * 
   * @param order
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("saveStatus")
  @RolesAllowed("users")
  public Response saveOrderStatus(ProductOrder order) {
    if (order == null) {
      LOG.warn("Bad request sent to server with empty order");
      return Response.status(400).build();
    }
    try {
      perkStoreService.saveOrderStatus(getCurrentUserId(), order);
      return Response.ok(perkStoreService.getOrderById(order.getId())).build();
    } catch (PerkStoreException e) {
      LOG.warn("Error saving order status", e);
      return Response.serverError()
                     .entity(getErrorJSONFormat(e))
                     .build();
    } catch (Exception e) {
      LOG.warn("Error saving order status", e);
      return Response.serverError().build();
    }
  }

}