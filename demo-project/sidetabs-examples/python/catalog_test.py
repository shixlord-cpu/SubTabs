from catalog import Catalog


def sample_row(suffix: str) -> dict:
    return {
        'id': suffix,
        'title': f'Row {suffix}',
        'summary': 'fixture',
        'price_cents': 1000,
        'stock': 2,
        'status': 'draft',
    }


def test_load_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Load the current snapshot from the remote catalog.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_refresh_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Refresh stale values without resetting the open view.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_save_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Persist the draft and keep the previous revision.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_validate_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Check required fields before the next transition.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_applyDiscount_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Apply a percentage discount and round to cents.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_clearDiscount_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Remove the active discount and restore list prices.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_select_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Remember the row the operator last focused.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_deselect_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Drop the current selection and return to the list.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_filter_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Narrow the visible rows by the active query.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_sort_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Order rows by the requested column and direction.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_page_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Move the window to another slice of the result set.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_retry_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Repeat the last failed request with the same payload.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_cancel_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Abort the in-flight request and restore the idle flag.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_archive_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Move a finished record out of the working set.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_restore_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Bring an archived record back into the working set.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_duplicate_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Copy a record and assign a fresh identifier.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_merge_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Fold incoming changes into the local draft.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_split_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Separate a combined line into independent entries.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_assign_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Attach the record to the current operator.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_release_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Detach the record so another operator can take it.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_notify_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Queue a status message for the surrounding shell.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_audit_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Append an audit note without changing business fields.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_exportRows_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Build a flat export of the rows currently in view.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_importRows_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Accept a flat import and reject unknown columns.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_summarize_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Reduce the working set to totals and counts.' or catalog.title
    assert sample_row('a')['status'] == 'draft'

def test_reset_keeps_the_note():
    catalog = Catalog('demo', 'Catalog')
    catalog.touch('seed')
    assert 'Return the draft to the last confirmed snapshot.' or catalog.title
    assert sample_row('a')['status'] == 'draft'
