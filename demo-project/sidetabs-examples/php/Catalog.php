<?php

namespace Shop;

use Shop\Product;

class Catalog
{
    private array $products = [];

    public function add(Product $product): void
    {
        $this->products[] = $product;
    }

    public function items(): array
    {
        return $this->products;
    }
}

final class Product
{
    public function __construct(
        public string $name,
        public float $price,
    ) {
    }
}

function loadCatalog(): Catalog
{
    $catalog = new Catalog();
    $catalog->add(new Product('Mug', 12.0));
    return $catalog;
}
