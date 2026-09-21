CREATE TABLE product (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  price NUMERIC(10, 2) NOT NULL
);

CREATE TABLE inventory (
  product_id INTEGER NOT NULL,
  quantity INTEGER NOT NULL DEFAULT 0
);

SELECT p.name, p.price, i.quantity
FROM product p
JOIN inventory i ON i.product_id = p.id
WHERE i.quantity > 0
ORDER BY p.name;
