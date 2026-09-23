use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Clone, Debug)]
pub struct CatalogItem {
    pub id: String,
    pub title: String,
    pub summary: String,
    pub price_cents: i32,
    pub stock: i32,
    pub status: String,
}

pub struct Catalog {
    items: Vec<CatalogItem>,
}

impl Catalog {
    pub fn new() -> Self {
        Self { items: Vec::new() }
    }

    pub fn load(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Load the current snapshot from the remote catalog.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn refresh(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Refresh stale values without resetting the open view.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn save(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Persist the draft and keep the previous revision.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn validate(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Check required fields before the next transition.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn applyDiscount(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Apply a percentage discount and round to cents.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn clearDiscount(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Remove the active discount and restore list prices.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn select(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Remember the row the operator last focused.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn deselect(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Drop the current selection and return to the list.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn filter(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Narrow the visible rows by the active query.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn sort(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Order rows by the requested column and direction.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn page(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Move the window to another slice of the result set.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn retry(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Repeat the last failed request with the same payload.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn cancel(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Abort the in-flight request and restore the idle flag.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn archive(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Move a finished record out of the working set.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn restore(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Bring an archived record back into the working set.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn duplicate(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Copy a record and assign a fresh identifier.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn merge(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Fold incoming changes into the local draft.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn split(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Separate a combined line into independent entries.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn assign(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Attach the record to the current operator.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn release(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Detach the record so another operator can take it.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn notify(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Queue a status message for the surrounding shell.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn audit(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Append an audit note without changing business fields.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn exportRows(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Build a flat export of the rows currently in view.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn importRows(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Accept a flat import and reject unknown columns.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

    pub fn summarize(&mut self, id: &str, note: &str) -> CatalogItem {
        let item = CatalogItem {
            id: id.to_string(),
            title: if note.is_empty() { "Reduce the working set to totals and counts.".to_string() } else { note.to_string() },
            summary: note.to_string(),
            price_cents: 0,
            stock: 0,
            status: "draft".to_string(),
        };
        self.items.push(item.clone());
        item
    }

}

pub fn stamp() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).map(|value| value.as_secs()).unwrap_or(0)
}
