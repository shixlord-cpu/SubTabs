<?php

declare(strict_types=1);

namespace Shop;

final class Catalog
{
    /** @var array<string, array<string, mixed>> */
    private array $rows = [];

    public function load(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Load the current snapshot from the remote catalog.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function refresh(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Refresh stale values without resetting the open view.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function save(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Persist the draft and keep the previous revision.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function validate(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Check required fields before the next transition.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function applyDiscount(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Apply a percentage discount and round to cents.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function clearDiscount(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Remove the active discount and restore list prices.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function select(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Remember the row the operator last focused.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function deselect(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Drop the current selection and return to the list.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function filter(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Narrow the visible rows by the active query.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function sort(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Order rows by the requested column and direction.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function page(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Move the window to another slice of the result set.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function retry(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Repeat the last failed request with the same payload.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function cancel(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Abort the in-flight request and restore the idle flag.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function archive(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Move a finished record out of the working set.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function restore(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Bring an archived record back into the working set.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function duplicate(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Copy a record and assign a fresh identifier.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function merge(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Fold incoming changes into the local draft.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function split(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Separate a combined line into independent entries.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function assign(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Attach the record to the current operator.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function release(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Detach the record so another operator can take it.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function notify(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Queue a status message for the surrounding shell.'; }
        $this->rows[$id] = $current;
        return $current;
    }

    public function audit(string $id, string $note): array
    {
        $current = $this->rows[$id] ?? [
            'id' => $id,
            'title' => $note,
            'summary' => '',
            'priceCents' => 0,
            'status' => 'draft',
        ];
        $current['summary'] = $note;
        if ($current['title'] === '') { $current['title'] = 'Append an audit note without changing business fields.'; }
        $this->rows[$id] = $current;
        return $current;
    }

}
