from catalog import Catalog, Product, load_catalog, total


def test_load_catalog_contains_demo_products():
    catalog = load_catalog()
    assert len(catalog.items()) == 2


def test_total_sums_product_prices():
    catalog = Catalog([Product("Sticker", 2.0), Product("Pin", 3.0)])
    assert total(catalog) == 5.0
