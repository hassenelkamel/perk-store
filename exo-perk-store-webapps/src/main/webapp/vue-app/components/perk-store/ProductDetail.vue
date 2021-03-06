<template>
  <v-flex class="productDetailContent">
    <v-hover>
      <v-card
        slot-scope="{ hover }"
        :class="`elevation-${hover ? 9 : 3}`"
        color="grey lighten-4"
        max-width="600">
        <v-carousel
          hide-controls
          hide-delimiters
          class="carousselParent">
          <template v-if="product.imageFiles">
            <v-carousel-item
              v-for="(imageFile,i) in product.imageFiles"
              :key="i"
              :src="imageFile.src"
              max="300"
              class="carousselImage" />
          </template>
          <v-expand-transition>
            <div
              v-if="hover || !product || !product.enabled || !product.imageFiles || !product.imageFiles.length || !available || maxOrdersReached"
              class="d-flex transition-fast-in-fast-out darken-2 v-card--reveal white--text productDetailHover"
              style="height: 100%;">
              <product-detail-content
                :product="product"
                :symbol="symbol"
                :max-orders-reached="maxOrdersReached"
                :hover="hover || !product.imageFiles || !product.imageFiles.length"
                :available="available" />
            </div>
          </v-expand-transition>
        </v-carousel>
        <v-card-text
          :class="product.unlimited && 'mt-2'"
          class="pt-2"
          style="position: relative;">
          <v-btn
            :class="ordersListBtnClass"
            :title="userData.canEdit ? 'Orders list' : 'My orders'"
            absolute
            color="secondary"
            class="white--text orderListButton"
            fab
            right
            top
            @click="$emit('orders-list', product)">
            <v-badge
              :color="userData.canEdit ? 'red' : 'orange'"
              right
              overlap>
              <span
                v-if="product.notProcessedOrders"
                slot="badge"
                class="orderListBadge">
                {{ product.notProcessedOrders }}
              </span>
              <v-icon v-if="userData.canEdit">fa-list-ul</v-icon>
              <v-icon v-else>fa-file-invoice-dollar</v-icon>
            </v-badge>
          </v-btn>
          <v-btn
            v-if="userData.canEdit"
            :class="editBtnClass"
            title="Edit product"
            absolute
            color="secondary"
            class="white--text editButton"
            fab
            right
            top
            @click="$emit('edit', product)">
            <v-icon>fa-pen</v-icon>
          </v-btn>
          <v-btn
            v-if="displayBuyButton"
            title="Buy"
            absolute
            class="white--text primary"
            :disabled="disabledBuy || !walletEnabled || walletLoading"
            :loading="!disabledBuy && walletLoading"
            fab
            right
            top
            @click="displayBuyModal">
            <v-icon>fa-shopping-cart</v-icon>
          </v-btn>
        </v-card-text>
        <v-tooltip bottom>
          <v-card-title slot="activator" class="ellipsis no-wrap pt-0 pb-0">
            <h3 :title="product.title" class="mb-2 primary--text ellipsis">
              <a
                :href="productLink"
                class="ellipsis"
                @click="openProductDetail">
                {{ product.title }}
              </a>
            </h3>
            <v-spacer />
            <h3 class="mb-2">
              {{ product.price }} {{ symbol }}
            </h3>
          </v-card-title>
          <strong v-if="!product.unlimited">{{ purchasedPercentageLabel }} articles sold</strong>
          <strong v-else>Unlimited supply</strong>
        </v-tooltip>
        <v-tooltip v-if="!product.unlimited" bottom>
          <v-progress-linear
            slot="activator"
            v-model="purchasedPercentage"
            :open-delay="0"
            color="red"
            class="mb-0 mt-0" />
          <strong>{{ purchasedPercentageLabel }} articles sold</strong>
        </v-tooltip>
        <v-card-text class="productCardFooter">
          <div
            :title="product.description"
            class="font-weight-light title mb-2 truncate8 productCardFooterDescription"
            v-text="product.description && product.description.trim()">
          </div>
        </v-card-text>
      </v-card>
    </v-hover>
  </v-flex>
</template>

<script>
import ProductDetailContent from './ProductDetailContent.vue';

export default {
  components: {
    ProductDetailContent
  },
  props: {
    product: {
      type: Object,
      default: function() {
        return {};
      },
    },
    symbol: {
      type: String,
      default: function() {
        return '';
      },
    },
    walletLoading: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    walletEnabled: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  computed: {
    productLink() {
      return (this.product && `${eXo.env.portal.context}/${eXo.env.portal.portalName}/perkstore?productId=${this.product.id}`) || '#';
    },
    userData() {
      return (this.product && this.product.userData) || {};
    },
    ordersListBtnClass() {
      if(!this.product) {
        return '';
      }
      let paddingIndex = 0;
      if(this.userData.canEdit) {
        paddingIndex++;
      }
      if(this.displayBuyButton) {
        paddingIndex++;
      }
      return `left-pa${paddingIndex}`;
    },
    editBtnClass() {
      if(this.displayBuyButton) {
        return 'left-pa1';
      }
      return '';
    },
    displayBuyButton() {
      return this.product && this.product.enabled && this.userData.canOrder && this.product.receiverMarchand && this.product.receiverMarchand.type && this.product.receiverMarchand.id && (this.product.receiverMarchand.type !== 'user' || this.product.receiverMarchand.id !== eXo.env.portal.userName);
    },
    disabledBuy() {
      return (!this.product.unlimited && this.available <= 0) || this.maxOrdersReached;
    },
    purchasedPercentageLabel() {
      return `${Number(this.purchasedPercentage).toFixed(0)}%`;
    },
    purchasedPercentage() {
      return !this.product.unlimited ? this.product.totalSupply ? ((this.product.purchased * 100) /this.product.totalSupply) : 100 : 0;
    },
    maxOrdersCurrentPeriodReached() {
      return this.userData && this.product.orderPeriodicity && this.userData.purchasedInCurrentPeriod && this.userData.purchasedInCurrentPeriod >= this.product.maxOrdersPerUser;
    },
    maxOrdersAllTimeReached() {
      return this.userData && !this.product.orderPeriodicity && this.userData.totalPurchased && this.userData.totalPurchased >= this.product.maxOrdersPerUser;
    },
    maxOrdersReached() {
      return this.product.maxOrdersPerUser && this.userData && (this.maxOrdersCurrentPeriodReached || this.maxOrdersAllTimeReached);
    },
    available() {
      if(this.product.unlimited) {
        return 10000;
      } else {
        const available = this.product.totalSupply - this.product.purchased;
        return available > 0 ? available : 0;
      }
    },
  },
  methods: {
    openProductDetail(event) {
      event.stopPropagation();
      event.preventDefault();

      this.$emit('product-details', this.product);
    },
    displayBuyModal() {
      if (!this.disabledBuy && this.walletEnabled) {
        this.$emit('buy', this.product);
      }
    }
  }
}
</script>