package main

import (
	"fmt"
	"strings"
	"time"
)

type CatalogItem struct {
	ID         string
	Title      string
	Summary    string
	PriceCents int
	Stock      int
	Status     string
	UpdatedAt  time.Time
}

type Catalog struct {
	items []CatalogItem
}

func (catalog *Catalog) Load(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Load the current snapshot from the remote catalog."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Refresh(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Refresh stale values without resetting the open view."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Save(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Persist the draft and keep the previous revision."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Validate(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Check required fields before the next transition."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) ApplyDiscount(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Apply a percentage discount and round to cents."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) ClearDiscount(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Remove the active discount and restore list prices."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Select(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Remember the row the operator last focused."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Deselect(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Drop the current selection and return to the list."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Filter(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Narrow the visible rows by the active query."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Sort(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Order rows by the requested column and direction."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Page(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Move the window to another slice of the result set."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Retry(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Repeat the last failed request with the same payload."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Cancel(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Abort the in-flight request and restore the idle flag."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Archive(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Move a finished record out of the working set."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Restore(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Bring an archived record back into the working set."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Duplicate(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Copy a record and assign a fresh identifier."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Merge(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Fold incoming changes into the local draft."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Split(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Separate a combined line into independent entries."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Assign(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Attach the record to the current operator."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Release(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Detach the record so another operator can take it."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Notify(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Queue a status message for the surrounding shell."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Audit(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Append an audit note without changing business fields."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) ExportRows(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Build a flat export of the rows currently in view."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) ImportRows(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Accept a flat import and reject unknown columns."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Summarize(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Reduce the working set to totals and counts."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func (catalog *Catalog) Reset(id string, note string) CatalogItem {
	item := CatalogItem{ID: id, Title: note, Status: "draft", UpdatedAt: time.Now()}
	if strings.TrimSpace(note) == "" {
		item.Summary = "Return the draft to the last confirmed snapshot."
	} else {
		item.Summary = note
	}
	catalog.items = append(catalog.items, item)
	return item
}

func main() {
	catalog := &Catalog{}
	item := catalog.Load("demo", "Mug")
	fmt.Println(item.Title)
}
