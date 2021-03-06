<template>
  <v-hover v-if="order" class="orderDetailParent">
    <v-card slot-scope="{ hover }" :class="`elevation-${hover ? 9 : 3}`">
      <v-card-title v-if="order.sender" class="pt-1 pb-1">
        <h4>
          <a
            :href="orderLink"
            rel="nofollow"
            target="_blank">
            <strong>#{{ order.id }}</strong>
          </a>
        </h4>
        <v-spacer />
        <template v-if="userData && userData.canEdit">
          <select
            v-model="order.status"
            :disabled="true"
            class="orderStatusSelectBox small mt-1 mb-1 mr-2"
            @change="changeStatus('STATUS')">
            <option v-for="option in statusList" :key="option">
              {{ option }}
            </option>
          </select>
          <div
            v-if="order.remainingQuantityToProcess"
            :title="`${order.remainingQuantityToProcess} to deliver`"
            class="orderQuantityBadgeParent">
            <div class="orderQuantityBadge red">
              {{ order.remainingQuantityToProcess }}
            </div>
          </div>
        </template>
        <span v-else>{{ statusLabel }}</span>
      </v-card-title>
  
      <v-divider />
  
      <v-list dense>
        <v-list-tile>
          <v-list-tile-content>Buyer:</v-list-tile-content>
          <v-list-tile-content class="align-end">
            <div class="ellipsis orderDetailText">
              <profile-link
                :id="order.sender.id"
                :space-id="order.sender.spaceId"
                :url-id="order.sender.spaceURLId"
                :type="order.sender.type"
                :display-name="order.sender.displayName"
                display-avatar />
            </div>
          </v-list-tile-content>
        </v-list-tile>
        <v-list-tile>
          <v-list-tile-content>Date:</v-list-tile-content>
          <v-list-tile-content class="align-end">
            <div :title="createdDateLabel" class="orderCreatedDate ellipsis">
              {{ createdDateLabel }}
            </div>
          </v-list-tile-content>
        </v-list-tile>
        <v-list-tile>
          <v-list-tile-content>Items:</v-list-tile-content>
          <v-list-tile-content class="align-end">
            <div class="ellipsis orderDetailText">
              {{ order.quantity }} x 
              <a
                :href="productLink"
                :title="productTitle"
                @click="openProductDetail">
                {{ productTitle }}
              </a>
            </div>
          </v-list-tile-content>
        </v-list-tile>
        <v-list-tile>
          <v-list-tile-content>
            <v-dialog
              v-if="order.transactionHash"
              v-model="barcodeModal"
              content-class="barcodeModal"
              width="500px"
              flat>
              <v-btn
                slot="activator"
                class="blue-grey white--text mr-0 mb-0 mr-0 mt-0"
                small
                @click="displayBarcode">
                Payment
                <v-icon
                  right
                  dark
                  size="20">
                  fa-barcode
                </v-icon>
              </v-btn>
              <v-card-title class="barcodeParent white pl-0 pr-0 pt-0" @click="barcodeModal = false">
                <v-spacer />
                <div class="barcodeContent">
                  <svg :id="barcodeContainerId"></svg>
                </div>
                <v-spacer />
              </v-card-title>
            </v-dialog>
          </v-list-tile-content>
          <v-list-tile-content class="align-end">
            <div class="no-wrap ellipsis orderDetailText">
              <v-icon
                v-if="order.transactionStatus === 'SUCCESS'"
                title="Transaction succeeded"
                class="green--text"
                size="16">
                fa-check-circle
              </v-icon>
              <v-icon
                v-if="order.transactionStatus === 'FAILED'"
                title="Transaction failed"
                class="red--text"
                size="16">
                fa-exclamation-circle
              </v-icon>
              <v-icon
                v-if="order.transactionStatus === 'PENDING'"
                title="Transaction in progress"
                class="orange--text"
                size="16">
                far fa-clock
              </v-icon>
              <a
                v-if="transactionLink"
                :href="transactionLink"
                rel="nofollow"
                target="_blank">
                {{ order.amount }} {{ symbol }}
              </a>
              <template v-else>
                {{ order.amount }} {{ symbol }}
              </template>
              to
              <profile-link
                :id="order.receiver.id"
                :space-id="order.receiver.spaceId"
                :url-id="order.receiver.spaceURLId"
                :type="order.receiver.type"
                :display-name="order.receiver.displayName" />
            </div>
          </v-list-tile-content>
        </v-list-tile>
      </v-list>
  
      <v-divider />
  
      <v-list dense class="orderProcessingDetails">
        <v-list-tile class="orderProcessingContent">
          <v-list-tile-content>Processing:</v-list-tile-content>
          <v-list-tile-content class="align-end orderProcessingActions">
            <div>
              <add-to-delivered-modal
                ref="addToDeliveredModal"
                :product="product"
                :order="order" />

              <div v-if="!refunding && (!order.remainingQuantityToProcess || isError)">
                <v-icon class="green--text mr-1" size="16px">fa-check-circle</v-icon>DONE
              </div>
              <template v-else-if="userData && userData.canEdit">
                <deliver-modal
                  v-if="order.remainingQuantityToProcess && (isPaid || isPartial)"
                  ref="deliverModal"
                  :product="product"
                  :order="order" />
                <refund-modal
                  v-if="refunding || (order.remainingQuantityToProcess && (isPaid || isPartial))"
                  :product="product"
                  :order="order"
                  :symbol="symbol"
                  @refunding="refunding = true"
                  @refunded="refunded"
                  @closed="refundDialogClosed" />
                <button
                  v-if="isOrdered"
                  class="btn orderProcessingBtn"
                  @click="cancelOrder">
                  Cancel
                </button>
              </template>
              <div v-else-if="isCanceled">
                CANCELED
              </div>
              <div v-else>
                <v-icon class="orange--text mr-1" size="16px">far fa-clock</v-icon>PENDING
              </div>
            </div>
          </v-list-tile-content>
        </v-list-tile>
        <v-list-tile class="mt-1">
          <v-list-tile-content class="orderDeliveredLabel">
            <div class="no-wrap">
              Delivered:
              <v-progress-circular
                :rotate="360"
                :size="40"
                :width="5"
                :value="deliveredPercentage"
                color="teal"
                class="ml-2">
                <span class="no-wrap">
                  {{ order.deliveredQuantity }}/{{ order.quantity }}
                </span>
              </v-progress-circular>
            </div>
          </v-list-tile-content>
          <v-list-tile-content v-if="order.deliveredDate" class="align-end">
            <div :title="deliveredDateLabel" class="orderDeliveredDate ellipsis">
              {{ deliveredDateLabel }}
            </div>
          </v-list-tile-content>
        </v-list-tile>
        <v-list-tile v-if="order.refundedAmount && order.refundTransactionHash" class="mt-1">
          <v-list-tile-content class="orderRefundedLabel">
            <div class="no-wrap">
              Refunded:
              <v-icon
                v-if="order.refundTransactionStatus === 'SUCCESS'"
                title="Transaction succeeded"
                class="green--text"
                size="16">
                fa-check-circle
              </v-icon>
              <v-icon
                v-if="order.refundTransactionStatus === 'FAILED'"
                title="Transaction failed"
                class="red--text"
                size="16">
                fa-exclamation-circle
              </v-icon>
              <v-icon
                v-if="order.refundTransactionStatus === 'PENDING'"
                title="Transaction in progress"
                class="orange--text"
                size="16">
                far fa-clock
              </v-icon>
              <a
                v-if="refundTransactionLink"
                :href="refundTransactionLink"
                rel="nofollow"
                target="_blank">
                {{ order.refundedAmount }} {{ symbol }}
              </a>
              <template v-else>
                {{ order.refundedAmount }} {{ symbol }}
              </template>
            </div>
          </v-list-tile-content>
          <v-list-tile-content v-if="order.refundedDate" class="align-end">
            <div :title="refundedDateLabel" class="orderRefundedDate ellipsis">
              {{ refundedDateLabel }}
            </div>
          </v-list-tile-content>
        </v-list-tile>
      </v-list>
    </v-card>
  </v-hover>
  <span v-else class="hidden"></span>
</template>

<script>
import RefundModal from './RefundModal.vue';
import DeliverModal from './DeliverModal.vue';
import AddToDeliveredModal from './AddToDeliveredModal.vue';
import ProfileLink from '../ProfileLink.vue';

import {saveOrderStatus} from '../../js/PerkStoreProductOrder.js';
import {formatDateTime} from '../../js/PerkStoreSettings.js';
import {generateBarCode} from '../../js/QRCode.js';

export default {
  components: {
    DeliverModal,
    RefundModal,
    ProfileLink,
    AddToDeliveredModal,
  },
  props: {
    product: {
      type: Object,
      default: function() {
        return {};
      },
    },
    order: {
      type: Object,
      default: function() {
        return {};
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
      // Attention: Used to close modal only when refunding process
      // is finished
      barcodeModal: false,
      refunding: false,
      statusList: [
        'ORDERED',
        'CANCELED',
        'ERROR',
        'PAID',
        'PARTIAL',
        'DELIVERED',
        'REFUNDED'
      ],
    };
  },
  computed: {
    barcodeContainerId() {
      return `barcode${this.order.id}`;
    },
    transactionLink() {
      if(this.order.transactionHash) {
        if((this.order.receiver.type === 'user' && this.order.receiver.id === eXo.env.portal.userName) || (this.order.sender.type === 'user' && this.order.sender.id === eXo.env.portal.userName)) {
          return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet?hash=${this.order.transactionHash}&principal=true`;
        } else if (this.order.receiver.type === 'space') {
          return `${eXo.env.portal.context}/g/:spaces:${this.order.receiver.spaceURLId}/${this.order.receiver.id}/EthereumSpaceWallet?hash=${this.order.transactionHash}&principal=true`;
        }
      }
      return null;
    },
    refundTransactionLink() {
      if(this.order.refundTransactionHash) {
        if((this.order.receiver.type === 'user' && this.order.receiver.id === eXo.env.portal.userName) || (this.order.sender.type === 'user' && this.order.sender.id === eXo.env.portal.userName)) {
          return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet?hash=${this.order.refundTransactionHash}&principal=true`;
        } else if (this.order.receiver.type === 'space') {
          return `${eXo.env.portal.context}/g/:spaces:${this.order.receiver.spaceURLId}/${this.order.receiver.id}/EthereumSpaceWallet?hash=${this.order.refundTransactionHash}&principal=true`;
        }
      }
      return null;
    },
    orderLink() {
      return (this.order && `${eXo.env.portal.context}/${eXo.env.portal.portalName}/perkstore?productId=${this.order.productId}&orderId=${this.order.id}`) || '#';
    },
    productLink() {
      return (this.order && `${eXo.env.portal.context}/${eXo.env.portal.portalName}/perkstore?productId=${this.order.productId}`) || '#';
    },
    productTitle() {
      return (this.product && this.product.title) || (this.order && this.order.productTitle) || '';
    },
    userData() {
      return (this.product && this.product.userData) || {};
    },
    createdDateLabel() {
      return formatDateTime(this.order.createdDate);
    },
    deliveredDateLabel() {
      return formatDateTime(this.order.deliveredDate);
    },
    refundedDateLabel() {
      return formatDateTime(this.order.refundedDate);
    },
    statusLabel() {
      return this.order.status;
    },
    deliveredPercentage() {
      return parseInt(((this.order.deliveredQuantity + this.order.refundedQuantity) * 100) / this.order.quantity);
    },
    statusLowerCase() {
      return this.order.status && this.order.status.toLowerCase();
    },
    isError() {
      return this.statusLowerCase === 'error';
    },
    isPaid() {
      return this.statusLowerCase === 'paid';
    },
    isDelivered() {
      return this.statusLowerCase === 'delivered';
    },
    isRefunded() {
      return this.statusLowerCase === 'refunded';
    },
    isCanceled() {
      return this.statusLowerCase === 'canceled';
    },
    isOrdered() {
      return this.statusLowerCase === 'ordered';
    },
    isPartial() {
      return this.statusLowerCase === 'partial';
    },
  },
  methods: {
    openProductDetail(event) {
      event.stopPropagation();
      event.preventDefault();

      this.$emit('display-product', this.order.productId);
    },
    cancelOrder() {
      this.order.status= 'CANCELED';
      return this.changeStatus('STATUS');
    },
    refunded(order) {
      // Nothing to do, the update will be made by Websocket
    },
    refundDialogClosed() {
      // We have to re-init wallet settings to the current user instead of wallet of Order receiver
      this.$emit('init-wallet');
      this.refunding = false;
    },
    changeStatus(modificationType) {
      this.$emit('loading', true);
      return saveOrderStatus(this.order, modificationType)
        .then(order => {
          this.$emit('changed', order);
          this.$forceUpdate();
        })
        .catch(e => {
          console.debug("Error saving status", e);
          this.$emit('error', e && e.message ? e.message : String(e));
        }).finally(() => this.$emit('loading', false));
    },
    displayBarcode() {
      generateBarCode(this.barcodeContainerId, this.product.id, this.order.id, eXo.env.portal.userName);
    },
    openDeliverWindow(productId, orderId, userId) {
      console.debug('Detected barcode', productId, orderId, userId, this.$refs);
      if (this.$refs && this.$refs.addToDeliveredModal) {
        this.$refs.addToDeliveredModal.open(productId, orderId, userId);
      }
    },
  }
}
</script>