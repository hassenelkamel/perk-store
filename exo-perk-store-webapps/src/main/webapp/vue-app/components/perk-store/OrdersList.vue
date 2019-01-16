<template>
  <v-layout row class="border-box-sizing mr-0 ml-0">
    <orders-filter
      ref="productOrdersFilter"
      :filter="ordersFilter"
      @search="searchOrders" />
    <v-container
      class="productOrdersParent border-box-sizing mt-0"
      fluid
      grid-list-md>
      <v-data-iterator
        :items="filteredOrders"
        content-tag="v-layout"
        no-data-text="No orders"
        hide-actions
        row
        wrap>
        <v-flex
          slot="item"
          slot-scope="props"
          class="border-box-sizing"
          xs12
          sm6
          md4
          lg3
          xl2>
          <order-detail
            :order="props.item"
            :product="product"
            :symbol="symbol"
            @init-wallet="$emit('init-wallet')"
            @display-product="$emit('display-product', $event)"
            @changed="updateOrder(props.item, $event)"
            @loading="$emit('loading', $event)"
            @error="$emit('error', $event)" />
        </v-flex>
        <v-flex
          v-if="displayLoadMoreButton"
          slot="footer"
          class="mt-2 text-xs-center"
          dense
          flat>
          <v-btn
            class="primary--text"
            flat
            @click="loadMore">
            Load more
          </v-btn>
        </v-flex>
      </v-data-iterator>
    </v-container>
  </v-layout>
</template>

<script>
import OrderDetail from './OrderDetail.vue';
import OrdersFilter from './OrdersFilter.vue';

import {getOrderList} from '../../js/PerkStoreProductOrder.js';

export default {
  components: {
    OrderDetail,
    OrdersFilter,
  },
  props: {
    product: {
      type: Object,
      default: function() {
        return {};
      },
    },
    ordersFilter: {
      type: Object,
      default: function() {
        return {};
      },
    },
    selectedOrderId: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    symbol: {
      type: String,
      default: function() {
        return "";
      },
    },
  },
  data() {
    return {
      pageSize: 12,
      limit: 12,
      limitReached: false,
      orders: [],
    };
  },
  computed: {
    filteredOrders() {
      const order = this.selectedOrderId && this.orders.find(order => order && order.id === this.selectedOrderId);
      if (order) {
        return [order];
      } else {
        return this.orders;
      }
    },
    displayLoadMoreButton() {
      return !this.limitReached && this.orders.length && this.orders.length % this.pageSize === 0;
    }
  },
  watch: {
    product(value) {
      if(value) {
        this.init();
      } else {
        this.orders = [];
      }
    }
  },
  created() {
    document.addEventListener('exo.addons.perkstore.order.createOrModify', this.updateOrderFromWS);
  },
  methods: {
    init() {
      this.$emit('error', null);
      this.$emit('loading', true);
      const initialOrdersLength = this.orders.length;
      return getOrderList(this.product && this.product.id, this.ordersFilter, this.limit)
        .then((orders) => {
          this.orders = orders || [];
          this.orders.forEach(order => {
            if(order.transactionHash) {
              if((order.receiver.type === 'user' && order.receiver.id === eXo.env.portal.userName) || (order.sender.type === 'user' && order.sender.id === eXo.env.portal.userName)) {
                order.transactionLink = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet?hash=${order.transactionHash}&principal=true`;
              } else if (order.receiver.type === 'space') {
                order.transactionLink = `${eXo.env.portal.context}/g/:spaces:${order.receiver.spaceURLId}/${order.receiver.id}/EthereumSpaceWallet?hash=${order.transactionHash}&principal=true`;
              }
            }
            if(order.refundTransactionHash) {
              if((order.receiver.type === 'user' && order.receiver.id === eXo.env.portal.userName) || (order.sender.type === 'user' && order.sender.id === eXo.env.portal.userName)) {
                order.refundTransactionLink = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet?hash=${order.refundTransactionHash}&principal=true`;
              } else if (order.receiver.type === 'space') {
                order.refundTransactionLink = `${eXo.env.portal.context}/g/:spaces:${order.receiver.spaceURLId}/${order.receiver.id}/EthereumSpaceWallet?hash=${order.refundTransactionHash}&principal=true`;
              }
            }
          })
          this.limitReached = this.orders.length <= initialOrdersLength || this.orders.length < this.limit;
        })
        .catch(e => {
          console.debug("Error while listing orders", e);
          this.$emit('error', e && e.message ? e.message : String(e));
        }).finally(() => this.$emit('loading', false));
    },
    searchOrders() {
      return this.init();
    },
    showFilters() {
      this.$refs.productOrdersFilter.showFilters();
    },
    updateOrder(order, newOrder) {
      Object.assign(order, newOrder);
      if(order.transactionHash) {
        if((order.receiver.type === 'user' && order.receiver.id === eXo.env.portal.userName) || (order.sender.type === 'user' && order.sender.id === eXo.env.portal.userName)) {
          order.transactionLink = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet?hash=${order.transactionHash}&principal=true`;
        } else if (order.receiver.type === 'space') {
          order.transactionLink = `${eXo.env.portal.context}/g/:spaces:${order.receiver.spaceURLId}/${order.receiver.id}/EthereumSpaceWallet?hash=${order.transactionHash}&principal=true`;
        }
      }
      if(order.refundTransactionHash) {
        if((order.receiver.type === 'user' && order.receiver.id === eXo.env.portal.userName) || (order.sender.type === 'user' && order.sender.id === eXo.env.portal.userName)) {
          order.refundTransactionLink = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet?hash=${order.refundTransactionHash}&principal=true`;
        } else if (order.receiver.type === 'space') {
          order.refundTransactionLink = `${eXo.env.portal.context}/g/:spaces:${order.receiver.spaceURLId}/${order.receiver.id}/EthereumSpaceWallet?hash=${order.refundTransactionHash}&principal=true`;
        }
      }
    },
    loadMore() {
      this.limit += this.pageSize;
      return this.init();
    },
    updateOrderFromWS(event) {
      const wsMessage = event.detail;
      if(this.orders && this.orders.length && wsMessage.productorder && wsMessage.productorder.id) {
        const order = this.orders.find(order => order && order.id === wsMessage.productorder.id);
        if(order) {
          this.updateOrder(order, wsMessage.productorder);
        } else {
          // New order: do nothing for now
        }
      }
    },
  },
}
</script>