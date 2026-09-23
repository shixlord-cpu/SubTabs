using System;
using System.Collections.Generic;
using System.Linq;

namespace Shop;

public class InvoiceService
{
    private readonly List<Invoice> invoices = new();

    public record Invoice(string Id, string Title, string Summary, int PriceCents, string Status, DateTime UpdatedAt);

    public Invoice Load(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Load the current snapshot from the remote catalog." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Refresh(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Refresh stale values without resetting the open view." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Save(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Persist the draft and keep the previous revision." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Validate(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Check required fields before the next transition." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice ApplyDiscount(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Apply a percentage discount and round to cents." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice ClearDiscount(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Remove the active discount and restore list prices." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Select(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Remember the row the operator last focused." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Deselect(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Drop the current selection and return to the list." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Filter(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Narrow the visible rows by the active query." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Sort(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Order rows by the requested column and direction." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Page(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Move the window to another slice of the result set." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Retry(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Repeat the last failed request with the same payload." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Cancel(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Abort the in-flight request and restore the idle flag." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Archive(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Move a finished record out of the working set." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Restore(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Bring an archived record back into the working set." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Duplicate(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Copy a record and assign a fresh identifier." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Merge(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Fold incoming changes into the local draft." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Split(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Separate a combined line into independent entries." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Assign(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Attach the record to the current operator." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Release(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Detach the record so another operator can take it." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Notify(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Queue a status message for the surrounding shell." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

    public Invoice Audit(string id, string note)
    {
        var current = invoices.FirstOrDefault(item => item.Id == id)
            ?? new Invoice(id, note, string.Empty, 0, "draft", DateTime.UtcNow);
        var next = current with
        {
            Summary = note,
            Title = string.IsNullOrWhiteSpace(current.Title) ? "Append an audit note without changing business fields." : current.Title,
            UpdatedAt = DateTime.UtcNow
        };
        invoices.RemoveAll(item => item.Id == id);
        invoices.Add(next);
        return next;
    }

}
