import { Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import * as Actions from './catalog.actions';
import * as Selectors from './catalog.selectors';

@Injectable({ providedIn: 'root' })
export class CatalogFacade {
  readonly state$ = this.store.select(Selectors.selectCatalogState);

  constructor(private readonly store: Store) {}

  load(note: string): void {
    this.store.dispatch(Actions.loadCatalog({
      draft: {
        title: note,
        summary: 'Load the current snapshot from the remote catalog.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  refresh(note: string): void {
    this.store.dispatch(Actions.refreshCatalog({
      draft: {
        title: note,
        summary: 'Refresh stale values without resetting the open view.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  save(note: string): void {
    this.store.dispatch(Actions.saveCatalog({
      draft: {
        title: note,
        summary: 'Persist the draft and keep the previous revision.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  validate(note: string): void {
    this.store.dispatch(Actions.validateCatalog({
      draft: {
        title: note,
        summary: 'Check required fields before the next transition.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  applyDiscount(note: string): void {
    this.store.dispatch(Actions.applyDiscountCatalog({
      draft: {
        title: note,
        summary: 'Apply a percentage discount and round to cents.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  clearDiscount(note: string): void {
    this.store.dispatch(Actions.clearDiscountCatalog({
      draft: {
        title: note,
        summary: 'Remove the active discount and restore list prices.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  select(note: string): void {
    this.store.dispatch(Actions.selectCatalog({
      draft: {
        title: note,
        summary: 'Remember the row the operator last focused.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  deselect(note: string): void {
    this.store.dispatch(Actions.deselectCatalog({
      draft: {
        title: note,
        summary: 'Drop the current selection and return to the list.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  filter(note: string): void {
    this.store.dispatch(Actions.filterCatalog({
      draft: {
        title: note,
        summary: 'Narrow the visible rows by the active query.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  sort(note: string): void {
    this.store.dispatch(Actions.sortCatalog({
      draft: {
        title: note,
        summary: 'Order rows by the requested column and direction.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  page(note: string): void {
    this.store.dispatch(Actions.pageCatalog({
      draft: {
        title: note,
        summary: 'Move the window to another slice of the result set.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  retry(note: string): void {
    this.store.dispatch(Actions.retryCatalog({
      draft: {
        title: note,
        summary: 'Repeat the last failed request with the same payload.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  cancel(note: string): void {
    this.store.dispatch(Actions.cancelCatalog({
      draft: {
        title: note,
        summary: 'Abort the in-flight request and restore the idle flag.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  archive(note: string): void {
    this.store.dispatch(Actions.archiveCatalog({
      draft: {
        title: note,
        summary: 'Move a finished record out of the working set.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  restore(note: string): void {
    this.store.dispatch(Actions.restoreCatalog({
      draft: {
        title: note,
        summary: 'Bring an archived record back into the working set.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  duplicate(note: string): void {
    this.store.dispatch(Actions.duplicateCatalog({
      draft: {
        title: note,
        summary: 'Copy a record and assign a fresh identifier.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  merge(note: string): void {
    this.store.dispatch(Actions.mergeCatalog({
      draft: {
        title: note,
        summary: 'Fold incoming changes into the local draft.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  split(note: string): void {
    this.store.dispatch(Actions.splitCatalog({
      draft: {
        title: note,
        summary: 'Separate a combined line into independent entries.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  assign(note: string): void {
    this.store.dispatch(Actions.assignCatalog({
      draft: {
        title: note,
        summary: 'Attach the record to the current operator.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  release(note: string): void {
    this.store.dispatch(Actions.releaseCatalog({
      draft: {
        title: note,
        summary: 'Detach the record so another operator can take it.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  notify(note: string): void {
    this.store.dispatch(Actions.notifyCatalog({
      draft: {
        title: note,
        summary: 'Queue a status message for the surrounding shell.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  audit(note: string): void {
    this.store.dispatch(Actions.auditCatalog({
      draft: {
        title: note,
        summary: 'Append an audit note without changing business fields.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  exportRows(note: string): void {
    this.store.dispatch(Actions.exportRowsCatalog({
      draft: {
        title: note,
        summary: 'Build a flat export of the rows currently in view.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  importRows(note: string): void {
    this.store.dispatch(Actions.importRowsCatalog({
      draft: {
        title: note,
        summary: 'Accept a flat import and reject unknown columns.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  summarize(note: string): void {
    this.store.dispatch(Actions.summarizeCatalog({
      draft: {
        title: note,
        summary: 'Reduce the working set to totals and counts.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

  reset(note: string): void {
    this.store.dispatch(Actions.resetCatalog({
      draft: {
        title: note,
        summary: 'Return the draft to the last confirmed snapshot.',
        priceCents: 0,
        status: 'draft',
      },
      note,
    }));
  }

}

export function catalogFacadeNote(note: string): string {
  return note.trim();
}
