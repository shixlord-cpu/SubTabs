using System.Collections.Generic;
using System.Linq;

namespace Shop;

public sealed class InvoiceService
{
    private readonly List<Invoice> invoices = new();

    public void Add(Invoice invoice)
    {
        invoices.Add(invoice);
    }

    public decimal Total()
    {
        return invoices.Sum(invoice => invoice.Amount);
    }
}

public sealed record Invoice(string Id, decimal Amount);
