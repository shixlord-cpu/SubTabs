import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import * as Actions from './checkout.actions';

@Injectable()
export class CheckoutEffects {
  readonly load$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.loadCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.loadCheckoutSuccess({ items, note: 'Load the current snapshot from the remote catalog.' })),
          catchError((error: Error) => of(Actions.loadCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly refresh$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.refreshCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.refreshCheckoutSuccess({ items, note: 'Refresh stale values without resetting the open view.' })),
          catchError((error: Error) => of(Actions.refreshCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly save$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.saveCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.saveCheckoutSuccess({ items, note: 'Persist the draft and keep the previous revision.' })),
          catchError((error: Error) => of(Actions.saveCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly validate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.validateCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.validateCheckoutSuccess({ items, note: 'Check required fields before the next transition.' })),
          catchError((error: Error) => of(Actions.validateCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly applyDiscount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.applyDiscountCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.applyDiscountCheckoutSuccess({ items, note: 'Apply a percentage discount and round to cents.' })),
          catchError((error: Error) => of(Actions.applyDiscountCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly clearDiscount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.clearDiscountCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.clearDiscountCheckoutSuccess({ items, note: 'Remove the active discount and restore list prices.' })),
          catchError((error: Error) => of(Actions.clearDiscountCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly select$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.selectCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.selectCheckoutSuccess({ items, note: 'Remember the row the operator last focused.' })),
          catchError((error: Error) => of(Actions.selectCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly deselect$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.deselectCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.deselectCheckoutSuccess({ items, note: 'Drop the current selection and return to the list.' })),
          catchError((error: Error) => of(Actions.deselectCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly filter$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.filterCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.filterCheckoutSuccess({ items, note: 'Narrow the visible rows by the active query.' })),
          catchError((error: Error) => of(Actions.filterCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly sort$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.sortCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.sortCheckoutSuccess({ items, note: 'Order rows by the requested column and direction.' })),
          catchError((error: Error) => of(Actions.sortCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly page$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.pageCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.pageCheckoutSuccess({ items, note: 'Move the window to another slice of the result set.' })),
          catchError((error: Error) => of(Actions.pageCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly retry$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.retryCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.retryCheckoutSuccess({ items, note: 'Repeat the last failed request with the same payload.' })),
          catchError((error: Error) => of(Actions.retryCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly cancel$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.cancelCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.cancelCheckoutSuccess({ items, note: 'Abort the in-flight request and restore the idle flag.' })),
          catchError((error: Error) => of(Actions.cancelCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly archive$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.archiveCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.archiveCheckoutSuccess({ items, note: 'Move a finished record out of the working set.' })),
          catchError((error: Error) => of(Actions.archiveCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly restore$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.restoreCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.restoreCheckoutSuccess({ items, note: 'Bring an archived record back into the working set.' })),
          catchError((error: Error) => of(Actions.restoreCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly duplicate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.duplicateCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.duplicateCheckoutSuccess({ items, note: 'Copy a record and assign a fresh identifier.' })),
          catchError((error: Error) => of(Actions.duplicateCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly merge$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.mergeCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.mergeCheckoutSuccess({ items, note: 'Fold incoming changes into the local draft.' })),
          catchError((error: Error) => of(Actions.mergeCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly split$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.splitCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.splitCheckoutSuccess({ items, note: 'Separate a combined line into independent entries.' })),
          catchError((error: Error) => of(Actions.splitCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly assign$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.assignCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.assignCheckoutSuccess({ items, note: 'Attach the record to the current operator.' })),
          catchError((error: Error) => of(Actions.assignCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly release$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.releaseCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.releaseCheckoutSuccess({ items, note: 'Detach the record so another operator can take it.' })),
          catchError((error: Error) => of(Actions.releaseCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly notify$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.notifyCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.notifyCheckoutSuccess({ items, note: 'Queue a status message for the surrounding shell.' })),
          catchError((error: Error) => of(Actions.notifyCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly audit$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.auditCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.auditCheckoutSuccess({ items, note: 'Append an audit note without changing business fields.' })),
          catchError((error: Error) => of(Actions.auditCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly exportRows$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.exportRowsCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.exportRowsCheckoutSuccess({ items, note: 'Build a flat export of the rows currently in view.' })),
          catchError((error: Error) => of(Actions.exportRowsCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly importRows$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.importRowsCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.importRowsCheckoutSuccess({ items, note: 'Accept a flat import and reject unknown columns.' })),
          catchError((error: Error) => of(Actions.importRowsCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly summarize$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.summarizeCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.summarizeCheckoutSuccess({ items, note: 'Reduce the working set to totals and counts.' })),
          catchError((error: Error) => of(Actions.summarizeCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  readonly reset$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.resetCheckout),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.resetCheckoutSuccess({ items, note: 'Return the draft to the last confirmed snapshot.' })),
          catchError((error: Error) => of(Actions.resetCheckoutFailure({ message: error.message })))
        )
      )
    )
  );

  constructor(private readonly actions$: Actions) {}
}

export function checkoutEffectNote(note: string): string {
  return note.trim();
}
