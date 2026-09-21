use std::vec::Vec;

pub struct Product {
    pub name: String,
    pub price: f64,
}

impl Product {
    pub fn new(name: &str, price: f64) -> Self {
        Self {
            name: name.to_string(),
            price,
        }
    }
}

pub fn sample_products() -> Vec<Product> {
    vec![Product::new("Mug", 12.0), Product::new("Notebook", 8.5)]
}

fn main() {
    let products = sample_products();
    println!("{} products", products.len());
}
