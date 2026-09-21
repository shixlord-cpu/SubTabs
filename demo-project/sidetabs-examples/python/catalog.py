from dataclasses import dataclass


@dataclass
class Product:
    name: str
    price: float


class Catalog:
    def __init__(self, products: list[Product] | None = None) -> None:
        self._products = list(products or [])

    def add(self, product: Product) -> None:
        self._products.append(product)

    def items(self) -> list[Product]:
        return list(self._products)


def load_catalog() -> Catalog:
    catalog = Catalog()
    catalog.add(Product("Mug", 12.0))
    catalog.add(Product("Notebook", 8.5))
    return catalog


def total(catalog: Catalog) -> float:
    return sum(product.price for product in catalog.items())
