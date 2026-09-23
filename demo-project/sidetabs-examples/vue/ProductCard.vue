<template>
  <article class="product-card">
    <header>
      <p class="eyebrow">Catalog</p>
      <h2>{{ name }}</h2>
      <p>{{ priceLabel }}</p>
    </header>
    <nav aria-label="Card">
      <button type="button" @click="select">Details</button>
      <button type="button" @click="save">Save</button>
    </nav>
    <main>
      <section>
        <h3>Description</h3>
        <p>{{ description }}</p>
      </section>
      <section>
        <h3>Notes</h3>
        <ul>
          <li v-for="note in notes" :key="note.id">{{ note.body }}</li>
        </ul>
      </section>
    </main>
    <footer>
      <p v-if="loading">Saving the card.</p>
      <p v-else>Stock {{ stock }}</p>
    </footer>
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
  data() {
    return {
      loading: false,
      stock: 8,
      query: '',
      notes: [{ id: 'n1', body: 'Glazed lip, stamped base.' }],
    };
  },
  computed: {
    priceLabel() {
      return this.price + ' EUR';
    },
  },
  methods: {
    load(note) {
      this.loading = true;
      this.query = note || 'Load the current snapshot from the remote catalog.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    refresh(note) {
      this.loading = true;
      this.query = note || 'Refresh stale values without resetting the open view.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    save(note) {
      this.loading = true;
      this.query = note || 'Persist the draft and keep the previous revision.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    validate(note) {
      this.loading = true;
      this.query = note || 'Check required fields before the next transition.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    applyDiscount(note) {
      this.loading = true;
      this.query = note || 'Apply a percentage discount and round to cents.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    clearDiscount(note) {
      this.loading = true;
      this.query = note || 'Remove the active discount and restore list prices.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    select(note) {
      this.loading = true;
      this.query = note || 'Remember the row the operator last focused.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    deselect(note) {
      this.loading = true;
      this.query = note || 'Drop the current selection and return to the list.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    filter(note) {
      this.loading = true;
      this.query = note || 'Narrow the visible rows by the active query.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    sort(note) {
      this.loading = true;
      this.query = note || 'Order rows by the requested column and direction.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    page(note) {
      this.loading = true;
      this.query = note || 'Move the window to another slice of the result set.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    retry(note) {
      this.loading = true;
      this.query = note || 'Repeat the last failed request with the same payload.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    cancel(note) {
      this.loading = true;
      this.query = note || 'Abort the in-flight request and restore the idle flag.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    archive(note) {
      this.loading = true;
      this.query = note || 'Move a finished record out of the working set.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    restore(note) {
      this.loading = true;
      this.query = note || 'Bring an archived record back into the working set.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    duplicate(note) {
      this.loading = true;
      this.query = note || 'Copy a record and assign a fresh identifier.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    merge(note) {
      this.loading = true;
      this.query = note || 'Fold incoming changes into the local draft.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    split(note) {
      this.loading = true;
      this.query = note || 'Separate a combined line into independent entries.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    assign(note) {
      this.loading = true;
      this.query = note || 'Attach the record to the current operator.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    release(note) {
      this.loading = true;
      this.query = note || 'Detach the record so another operator can take it.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    notify(note) {
      this.loading = true;
      this.query = note || 'Queue a status message for the surrounding shell.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    audit(note) {
      this.loading = true;
      this.query = note || 'Append an audit note without changing business fields.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    exportRows(note) {
      this.loading = true;
      this.query = note || 'Build a flat export of the rows currently in view.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    importRows(note) {
      this.loading = true;
      this.query = note || 'Accept a flat import and reject unknown columns.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    summarize(note) {
      this.loading = true;
      this.query = note || 'Reduce the working set to totals and counts.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
    reset(note) {
      this.loading = true;
      this.query = note || 'Return the draft to the last confirmed snapshot.';
      this.notes = this.notes.concat({ id: note || this.query, body: this.query });
      this.loading = false;
    },
  },
};
</script>

<style>
.product-card { border: 1px solid #d0d7de; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.product-card header, .product-card nav, .product-card main, .product-card footer { padding: 8px 0; }
.product-card h2, .product-card h3 { margin: 0; }
.product-card--load { outline: 1px solid transparent; }
.product-card--refresh { outline: 1px solid transparent; }
.product-card--save { outline: 1px solid transparent; }
.product-card--validate { outline: 1px solid transparent; }
.product-card--applyDiscount { outline: 1px solid transparent; }
.product-card--clearDiscount { outline: 1px solid transparent; }
.product-card--select { outline: 1px solid transparent; }
.product-card--deselect { outline: 1px solid transparent; }
.product-card--filter { outline: 1px solid transparent; }
.product-card--sort { outline: 1px solid transparent; }
.product-card--page { outline: 1px solid transparent; }
.product-card--retry { outline: 1px solid transparent; }
.product-card--cancel { outline: 1px solid transparent; }
.product-card--archive { outline: 1px solid transparent; }
.product-card--restore { outline: 1px solid transparent; }
.product-card--duplicate { outline: 1px solid transparent; }
.product-card--merge { outline: 1px solid transparent; }
.product-card--split { outline: 1px solid transparent; }
.product-card--assign { outline: 1px solid transparent; }
.product-card--release { outline: 1px solid transparent; }
.product-card--notify { outline: 1px solid transparent; }
.product-card--audit { outline: 1px solid transparent; }
.product-card--exportRows { outline: 1px solid transparent; }
.product-card--importRows { outline: 1px solid transparent; }
.product-card--summarize { outline: 1px solid transparent; }
.product-card--reset { outline: 1px solid transparent; }
</style>
