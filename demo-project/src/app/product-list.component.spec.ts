import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { ProductListComponent } from './product-list.component';

function createRecord(id: string) {
  return {
    id,
    sku: id.toUpperCase(),
    title: 'Product List',
    summary: 'Fixture row',
    priceCents: 1200,
    currency: 'EUR',
    stock: 4,
    status: 'draft' as const,
    updatedAt: '2026-01-01T00:00:00.000Z',
  };
}

const untouched = createRecord('untouched');
const selected = createRecord('selected');

describe('ProductListComponent', () => {
  let fixture: ComponentFixture<ProductListComponent>;
  let component: ProductListComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductListComponent],
      providers: [FormBuilder],
    }).compileComponents();
    fixture = TestBed.createComponent(ProductListComponent);
    component = fixture.componentInstance;
    component.rows = [untouched, selected];
    component.selectedId = selected.id;
    fixture.detectChanges();
  });

  it('load keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.load();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Load the current snapsho');
    expect(component.loading).toBeFalse();
  });

  it('refresh keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.refresh();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Refresh stale values wit');
    expect(component.loading).toBeFalse();
  });

  it('save keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.save();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Persist the draft and ke');
    expect(component.loading).toBeFalse();
  });

  it('validate keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.validate();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Check required fields be');
    expect(component.loading).toBeFalse();
  });

  it('applyDiscount keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.applyDiscount();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Apply a percentage disco');
    expect(component.loading).toBeFalse();
  });

  it('clearDiscount keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.clearDiscount();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Remove the active discou');
    expect(component.loading).toBeFalse();
  });

  it('select keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.select();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Remember the row the ope');
    expect(component.loading).toBeFalse();
  });

  it('deselect keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.deselect();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Drop the current selecti');
    expect(component.loading).toBeFalse();
  });

  it('filter keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.filter();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Narrow the visible rows ');
    expect(component.loading).toBeFalse();
  });

  it('sort keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.sort();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Order rows by the reques');
    expect(component.loading).toBeFalse();
  });

  it('page keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.page();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Move the window to anoth');
    expect(component.loading).toBeFalse();
  });

  it('retry keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.retry();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Repeat the last failed r');
    expect(component.loading).toBeFalse();
  });

  it('cancel keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.cancel();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Abort the in-flight requ');
    expect(component.loading).toBeFalse();
  });

  it('archive keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.archive();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Move a finished record o');
    expect(component.loading).toBeFalse();
  });

  it('restore keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.restore();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Bring an archived record');
    expect(component.loading).toBeFalse();
  });

  it('duplicate keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.duplicate();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Copy a record and assign');
    expect(component.loading).toBeFalse();
  });

  it('merge keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.merge();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Fold incoming changes in');
    expect(component.loading).toBeFalse();
  });

  it('split keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.split();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Separate a combined line');
    expect(component.loading).toBeFalse();
  });

  it('assign keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.assign();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Attach the record to the');
    expect(component.loading).toBeFalse();
  });

  it('release keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.release();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Detach the record so ano');
    expect(component.loading).toBeFalse();
  });

  it('notify keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.notify();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Queue a status message f');
    expect(component.loading).toBeFalse();
  });

  it('audit keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.audit();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Append an audit note wit');
    expect(component.loading).toBeFalse();
  });

  it('exportRows keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.exportRows();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Build a flat export of t');
    expect(component.loading).toBeFalse();
  });

  it('importRows keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.importRows();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Accept a flat import and');
    expect(component.loading).toBeFalse();
  });

  it('summarize keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.summarize();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Reduce the working set t');
    expect(component.loading).toBeFalse();
  });

  it('reset keeps a title on the selected row', () => {
    component.draftTitle = 'Revised';
    component.reset();
    const row = component.rows.find((item) => item.id === selected.id);
    expect(row?.title).toBe('Revised');
    expect(row?.summary).toContain('Return the draft to the ');
    expect(component.loading).toBeFalse();
  });

});
