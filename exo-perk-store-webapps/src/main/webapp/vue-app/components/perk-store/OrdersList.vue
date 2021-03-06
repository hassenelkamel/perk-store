<template>
  <v-layout
    row
    class="border-box-sizing mr-0 ml-0">
    <div
      v-show="barcodeReader"
      id="interactive"
      class="viewport"
      style="width: 100%;"></div>
    <orders-filter
      ref="productOrdersFilter"
      :filter="ordersFilter"
      @search="searchOrders" />
    <v-container
      v-show="!barcodeReader"
      class="productOrdersParent border-box-sizing mt-0"
      fluid
      grid-list-md>
      <v-card
        v-if="!selectedOrderId && displayFilterDetails && filterDescriptionLabels && filterDescriptionLabels.length"
        class="transparent mb-0 mt-0 pt-0"
        flat>
        <v-card-title class="mt-0 pt-0">
          <v-spacer />
          <div class="no-wrap ellipsis">
            Selected filter:
            <v-chip v-for="filterDescription in filterDescriptionLabels" :key="filterDescription">
              {{ filterDescription }}
            </v-chip>
          </div>
          <v-spacer />
        </v-card-title>
      </v-card>
      <a id="downloadOrders" class="hidden">download</a>
      <v-data-iterator
        :items="filteredOrders"
        :no-data-text="loading ? '': 'No orders'"
        content-tag="v-layout"
        content-class="mt-0 mb-0"
        hide-actions
        row
        wrap>
        <v-flex
          slot="item"
          slot-scope="props"
          class="border-box-sizing pt-0"
          xs12
          sm6
          md4
          lg3
          xl2>
          <order-detail
            :ref="`orderDetail${props.item.id}`"
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
          v-if="loading || displayLoadMoreButton"
          slot="footer"
          class="mt-2 text-xs-center"
          dense
          flat>
          <v-btn
            v-if="displayLoadMoreButton"
            class="primary--text"
            flat
            :loading="loading"
            :disabled="loading"
            @click="loadMore">
            Load more
          </v-btn>
          <v-progress-circular
            v-else-if="loading"
            color="primary"
            class="mb-2"
            indeterminate />
        </v-flex>
      </v-data-iterator>
      <order-notification
        :orders="newAddedOrders"
        @refresh-list="addNewOrdersToList" />
    </v-container>
  </v-layout>
</template>

<script>
import OrderDetail from './OrderDetail.vue';
import OrdersFilter from './OrdersFilter.vue';
import OrderNotification from './OrderNotification.vue';

import {getOrderList} from '../../js/PerkStoreProductOrder.js';
import {getDefaultOrderFilter, formatDate, formatDateTime} from '../../js/PerkStoreSettings.js';

export default {
  components: {
    OrderDetail,
    OrdersFilter,
    OrderNotification,
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
      loading: false,
      barcodeReader: false,
      pageSize: 12,
      limit: 12,
      limitReached: false,
      filterDescriptionLabels: [],
      displayFilterDetails: false,
      orders: [],
      newAddedOrders: [],
    };
  },
  computed: {
    selectedOrdersFilter() {
      return (!this.selectedOrderId && this.ordersFilter) || getDefaultOrderFilter();
    },
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
    product(value, oldValue) {
      if(value && (!oldValue || value.id !== oldValue.id)) {
        this.limit = 12;
        this.limitReached = false;
        this.newAddedOrders = [];
        this.init();
      }
    },
    barcodeReader() {
      this.initBarcodeReader();
    }
  },
  created() {
    document.addEventListener('exo.addons.perkstore.order.createOrModify', this.updateOrderFromWS);
  },
  methods: {
    init() {
      this.$emit('error', null);

      this.computeDisplayFilterDetails();
      this.computeDescriptionLabels();

      const initialOrdersLength = this.orders.length;

      this.loading = true;

      return getOrderList(this.product && this.product.id, this.selectedOrdersFilter, this.selectedOrderId, this.limit)
        .then((orders) => {
          this.orders = orders || [];
          this.limitReached = this.orders.length <= initialOrdersLength || this.orders.length < this.limit;
        })
        .catch(e => {
          console.debug("Error while listing orders", e);
          this.$emit('error', e && e.message ? e.message : String(e));
        }).finally(() => {
          this.loading = false;
        });
    },
    searchOrders() {
      return this.init();
    },
    computeDisplayFilterDetails() {
      this.displayFilterDetails = false;

      if(!this.selectedOrdersFilter) {
        this.displayFilterDetails = false;
        return;
      }
      if(this.selectedOrdersFilter.notProcessed) {
        this.displayFilterDetails = true;
        return;
      }
      if(this.selectedOrdersFilter.searchInDates && this.selectedOrdersFilter.selectedDate) {
        this.displayFilterDetails = true;
        return;
      }

      const selectedOrdersFilter = {
        ordered: this.selectedOrdersFilter.ordered,
        canceled: this.selectedOrdersFilter.canceled,
        paid: this.selectedOrdersFilter.paid,
        partial: this.selectedOrdersFilter.partial,
        delivered: this.selectedOrdersFilter.delivered,
        refunded: this.selectedOrdersFilter.refunded,
      };

      // Check if all details are checked by default
      this.displayFilterDetails = !Object.values(selectedOrdersFilter).every(value => value);
    },
    computeDescriptionLabels() {
      this.filterDescriptionLabels = [];
      if(this.selectedOrdersFilter.searchInDates && this.selectedOrdersFilter.selectedDate) {
        const dateString = formatDate(this.selectedOrdersFilter.selectedDate);
        this.filterDescriptionLabels.push(`DATE: ${dateString}`);
      }
      if(this.selectedOrdersFilter.notProcessed) {
        this.filterDescriptionLabels.push("NOT PROCESSED");
      } else {
        if (this.selectedOrdersFilter.ordered) {
          this.filterDescriptionLabels.push("ORDERED");
        }
        if (this.selectedOrdersFilter.canceled) {
          this.filterDescriptionLabels.push("CANCELED");
        }
        if (this.selectedOrdersFilter.error) {
          this.filterDescriptionLabels.push("ERROR");
        }
        if (this.selectedOrdersFilter.paid) {
          this.filterDescriptionLabels.push("PAID");
        }
        if (this.selectedOrdersFilter.partial) {
          this.filterDescriptionLabels.push("PARTIAL");
        }
        if (this.selectedOrdersFilter.delivered) {
          this.filterDescriptionLabels.push("DELIVERED");
        }
        if (this.selectedOrdersFilter.refunded) {
          this.filterDescriptionLabels.push("REFUNDED");
        }
      }
    },
    showFilters() {
      this.$refs.productOrdersFilter.showFilters();
    },
    updateOrder(order, newOrder, orders) {
      if (!orders) {
        orders = this.orders;
      }
      Object.assign(order, newOrder);
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
          this.updateOrder(order, wsMessage.productorder, this.newAddedOrders);
        } else if(this.product && wsMessage.productorder.productId === this.product.id) {
          if (this.product.userData && this.product.userData.canEdit) {
            if(wsMessage.productorder.status.toUpperCase() === 'ORDERED' && !this.newAddedOrders.find(order => order.id === wsMessage.productorder.id)) {
              this.newAddedOrders.unshift(wsMessage.productorder);
            }
          } else {
            this.orders.unshift(wsMessage.productorder);
          }
        }
      }
    },
    addNewOrdersToList() {
      this.newAddedOrders.reverse().forEach(order => {
        if (!this.orders.find(existingOrder => existingOrder.id === order.id)) {
          this.orders.unshift(order);
        }
      });
      this.newAddedOrders.splice(0, this.newAddedOrders.length);
    },
    exportOrders() {
      return getOrderList(this.product && this.product.id, this.selectedOrdersFilter, this.selectedOrderId, 0)
        .then((ordersToExport) => {
          if(!ordersToExport || !ordersToExport.length) {
            return;
          }
          ordersToExport.reverse();
          const csvHeader = {
            id: 'Id',
            sender: {
              displayName: 'Buyer',
            },
            receiver: {
              displayName: 'Seller',
            },
            status: 'Status',
            quantity: 'Quantity',
            deliveredQuantity: 'Delivered quantity',
            refundedQuantity: 'Refunded quantity',
            amount: 'Amount',
            refundedAmount: 'Refunded amount',
            createdDate: 'Order date',
            deliveredDate: 'Delivered date',
            refundedDate: 'Refunded date',
          };

          ordersToExport.unshift(csvHeader);
          ordersToExport = ordersToExport.map(order =>
            [
              order.id,
              order.sender && order.sender.displayName,
              order.receiver && order.receiver.displayName,
              order.status,
              order.quantity,
              order.deliveredQuantity,
              order.refundedQuantity,
              `${order.amount} ${this.symbol}`,
              `${order.refundedAmount} ${this.symbol}`,
              order.createdDate && order.createdDate !== csvHeader.createdDate ? formatDateTime(order.createdDate) : (order.createdDate || '-'),
              order.deliveredDate && order.deliveredDate !== csvHeader.deliveredDate ? formatDateTime(order.deliveredDate) : (order.deliveredDate || '-'),
              order.refundedDate && order.refundedDate !== csvHeader.refundedDate ? formatDateTime(order.refundedDate) : (order.refundedDate || '-'),
            ]).map(e=>e.join(",")).join("\n");
          const csvContent = `data:text/csv;charset=utf-8,${ordersToExport}`;
          const downloadLink = document.getElementById('downloadOrders');
          downloadLink.setAttribute("href", csvContent);
          downloadLink.setAttribute("download", `orders.csv`);
          downloadLink.click();
        })
    },
    openBarcodeReader() {
      this.barcodeReader = true;
      this.$emit('reader-opened');
    },
    closeBarcodeReader() {
      this.barcodeReader = false;
      this.$emit('reader-closed');
    },
    initBarcodeReader() {
      const thiss = this;
      if (this.barcodeReader) {
        Quagga.init({
          inputStream: {
            type : "LiveStream",
            constraints: {
                facingMode: "environment"
            }
          },
          locator: {
              patchSize: "medium",
              halfSample: true
          },
          numOfWorkers: 4,
          frequency: 5,
          decoder: {
              readers : [{
                  format: "code_128_reader",
                  config: {}
              }]
          },
          locate: true
        }, function(err) {
            if (err) {
                console.log(err);
                return
            }
            Quagga.start();
            Quagga.onDetected((data) => {
              if (data && data.codeResult && data.codeResult.code) {
                const barcodeText = data.codeResult.code;
                const barcodeTextParts = barcodeText.split('@');
                if (barcodeTextParts.length === 5) {
                  const productId = barcodeTextParts[1];
                  const orderId = barcodeTextParts[2];
                  const userId = barcodeTextParts[3];

                  if (thiss.$refs && thiss.$refs[`orderDetail${orderId}`]) {
                    thiss.$refs[`orderDetail${orderId}`].openDeliverWindow(productId, orderId, userId);
                  }
                }
              }
            });
        });
      } else {
        Quagga.stop();
        $('#interactive').html('');
      }
    },
  },
}
</script>