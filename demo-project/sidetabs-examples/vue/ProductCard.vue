<template>
  <article class="product-card">
    <header>
      <h2>{{ name }}</h2>
      <span>{{ price }} €</span>
    </header>
    <p>{{ description }}</p>
  </article>
</template>

<script>
export default {
  name: 'ProductCard',
  props: {
    name: { type: String, default: 'Mug' },
    price: { type: Number, default: 12 },
    description: { type: String, default: 'Ceramic demo mug' },
  },
};
</script>

<style>
.product-card {
  border: 1px solid #d0d7de;
  padding: 16px;
}
</style>
