CREATE TABLE catalog_item (
    id TEXT PRIMARY KEY,
    sku TEXT NOT NULL,
    title TEXT NOT NULL,
    summary TEXT,
    price_cents INTEGER NOT NULL DEFAULT 0,
    currency TEXT NOT NULL DEFAULT 'EUR',
    stock INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'draft',
    updated_at TEXT NOT NULL
);

CREATE TABLE catalog_note (
    id TEXT PRIMARY KEY,
    item_id TEXT NOT NULL,
    body TEXT NOT NULL,
    created_at TEXT NOT NULL
);

-- load
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Load the current snapshot from the remote catalog.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- refresh
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Refresh stale values without resetting the open view.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- save
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Persist the draft and keep the previous revision.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- validate
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Check required fields before the next transition.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- applyDiscount
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Apply a percentage discount and round to cents.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- clearDiscount
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Remove the active discount and restore list prices.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- select
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Remember the row the operator last focused.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- deselect
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Drop the current selection and return to the list.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- filter
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Narrow the visible rows by the active query.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- sort
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Order rows by the requested column and direction.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- page
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Move the window to another slice of the result set.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- retry
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Repeat the last failed request with the same payload.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- cancel
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Abort the in-flight request and restore the idle flag.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- archive
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Move a finished record out of the working set.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- restore
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Bring an archived record back into the working set.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- duplicate
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Copy a record and assign a fresh identifier.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- merge
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Fold incoming changes into the local draft.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- split
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Separate a combined line into independent entries.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- assign
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Attach the record to the current operator.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- release
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Detach the record so another operator can take it.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- notify
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Queue a status message for the surrounding shell.' OR item.title <> '')
ORDER BY item.updated_at DESC;

-- audit
SELECT
    item.id,
    item.sku,
    item.title,
    item.summary,
    item.price_cents,
    item.stock,
    item.status
FROM catalog_item AS item
LEFT JOIN catalog_note AS note ON note.item_id = item.id
WHERE item.status <> 'archived'
  AND (item.summary = 'Append an audit note without changing business fields.' OR item.title <> '')
ORDER BY item.updated_at DESC;
