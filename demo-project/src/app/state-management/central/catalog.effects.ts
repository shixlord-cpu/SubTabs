import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import * as Actions from './catalog.actions';

@Injectable()
export class CatalogEffects {
  readonly load$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.loadCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.loadCatalogSuccess({ items, note: 'Load the current snapshot from the remote catalog.' })),
          catchError((error: Error) => of(Actions.loadCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly refresh$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.refreshCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.refreshCatalogSuccess({ items, note: 'Refresh stale values without resetting the open view.' })),
          catchError((error: Error) => of(Actions.refreshCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly save$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.saveCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.saveCatalogSuccess({ items, note: 'Persist the draft and keep the previous revision.' })),
          catchError((error: Error) => of(Actions.saveCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly validate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.validateCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.validateCatalogSuccess({ items, note: 'Check required fields before the next transition.' })),
          catchError((error: Error) => of(Actions.validateCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly applyDiscount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.applyDiscountCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.applyDiscountCatalogSuccess({ items, note: 'Apply a percentage discount and round to cents.' })),
          catchError((error: Error) => of(Actions.applyDiscountCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly clearDiscount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.clearDiscountCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.clearDiscountCatalogSuccess({ items, note: 'Remove the active discount and restore list prices.' })),
          catchError((error: Error) => of(Actions.clearDiscountCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly select$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.selectCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.selectCatalogSuccess({ items, note: 'Remember the row the operator last focused.' })),
          catchError((error: Error) => of(Actions.selectCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly deselect$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.deselectCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.deselectCatalogSuccess({ items, note: 'Drop the current selection and return to the list.' })),
          catchError((error: Error) => of(Actions.deselectCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly filter$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.filterCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.filterCatalogSuccess({ items, note: 'Narrow the visible rows by the active query.' })),
          catchError((error: Error) => of(Actions.filterCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly sort$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.sortCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.sortCatalogSuccess({ items, note: 'Order rows by the requested column and direction.' })),
          catchError((error: Error) => of(Actions.sortCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly page$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.pageCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.pageCatalogSuccess({ items, note: 'Move the window to another slice of the result set.' })),
          catchError((error: Error) => of(Actions.pageCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly retry$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.retryCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.retryCatalogSuccess({ items, note: 'Repeat the last failed request with the same payload.' })),
          catchError((error: Error) => of(Actions.retryCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly cancel$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.cancelCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.cancelCatalogSuccess({ items, note: 'Abort the in-flight request and restore the idle flag.' })),
          catchError((error: Error) => of(Actions.cancelCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly archive$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.archiveCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.archiveCatalogSuccess({ items, note: 'Move a finished record out of the working set.' })),
          catchError((error: Error) => of(Actions.archiveCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly restore$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.restoreCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.restoreCatalogSuccess({ items, note: 'Bring an archived record back into the working set.' })),
          catchError((error: Error) => of(Actions.restoreCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly duplicate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.duplicateCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.duplicateCatalogSuccess({ items, note: 'Copy a record and assign a fresh identifier.' })),
          catchError((error: Error) => of(Actions.duplicateCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly merge$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.mergeCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.mergeCatalogSuccess({ items, note: 'Fold incoming changes into the local draft.' })),
          catchError((error: Error) => of(Actions.mergeCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly split$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.splitCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.splitCatalogSuccess({ items, note: 'Separate a combined line into independent entries.' })),
          catchError((error: Error) => of(Actions.splitCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly assign$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.assignCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.assignCatalogSuccess({ items, note: 'Attach the record to the current operator.' })),
          catchError((error: Error) => of(Actions.assignCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly release$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.releaseCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.releaseCatalogSuccess({ items, note: 'Detach the record so another operator can take it.' })),
          catchError((error: Error) => of(Actions.releaseCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly notify$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.notifyCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.notifyCatalogSuccess({ items, note: 'Queue a status message for the surrounding shell.' })),
          catchError((error: Error) => of(Actions.notifyCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly audit$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.auditCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.auditCatalogSuccess({ items, note: 'Append an audit note without changing business fields.' })),
          catchError((error: Error) => of(Actions.auditCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly exportRows$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.exportRowsCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.exportRowsCatalogSuccess({ items, note: 'Build a flat export of the rows currently in view.' })),
          catchError((error: Error) => of(Actions.exportRowsCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly importRows$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.importRowsCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.importRowsCatalogSuccess({ items, note: 'Accept a flat import and reject unknown columns.' })),
          catchError((error: Error) => of(Actions.importRowsCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly summarize$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.summarizeCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.summarizeCatalogSuccess({ items, note: 'Reduce the working set to totals and counts.' })),
          catchError((error: Error) => of(Actions.summarizeCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  readonly reset$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.resetCatalog),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.resetCatalogSuccess({ items, note: 'Return the draft to the last confirmed snapshot.' })),
          catchError((error: Error) => of(Actions.resetCatalogFailure({ message: error.message })))
        )
      )
    )
  );

  constructor(private readonly actions$: Actions) {}
}

export function catalogEffectNote(note: string): string {
  return note.trim();
}
