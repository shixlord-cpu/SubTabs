import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import * as Actions from './cart.actions';

@Injectable()
export class CartEffects {
  readonly load$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.loadCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.loadCartSuccess({ items, note: 'Load the current snapshot from the remote catalog.' })),
          catchError((error: Error) => of(Actions.loadCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly refresh$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.refreshCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.refreshCartSuccess({ items, note: 'Refresh stale values without resetting the open view.' })),
          catchError((error: Error) => of(Actions.refreshCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly save$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.saveCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.saveCartSuccess({ items, note: 'Persist the draft and keep the previous revision.' })),
          catchError((error: Error) => of(Actions.saveCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly validate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.validateCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.validateCartSuccess({ items, note: 'Check required fields before the next transition.' })),
          catchError((error: Error) => of(Actions.validateCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly applyDiscount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.applyDiscountCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.applyDiscountCartSuccess({ items, note: 'Apply a percentage discount and round to cents.' })),
          catchError((error: Error) => of(Actions.applyDiscountCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly clearDiscount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.clearDiscountCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.clearDiscountCartSuccess({ items, note: 'Remove the active discount and restore list prices.' })),
          catchError((error: Error) => of(Actions.clearDiscountCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly select$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.selectCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.selectCartSuccess({ items, note: 'Remember the row the operator last focused.' })),
          catchError((error: Error) => of(Actions.selectCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly deselect$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.deselectCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.deselectCartSuccess({ items, note: 'Drop the current selection and return to the list.' })),
          catchError((error: Error) => of(Actions.deselectCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly filter$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.filterCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.filterCartSuccess({ items, note: 'Narrow the visible rows by the active query.' })),
          catchError((error: Error) => of(Actions.filterCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly sort$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.sortCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.sortCartSuccess({ items, note: 'Order rows by the requested column and direction.' })),
          catchError((error: Error) => of(Actions.sortCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly page$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.pageCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.pageCartSuccess({ items, note: 'Move the window to another slice of the result set.' })),
          catchError((error: Error) => of(Actions.pageCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly retry$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.retryCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.retryCartSuccess({ items, note: 'Repeat the last failed request with the same payload.' })),
          catchError((error: Error) => of(Actions.retryCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly cancel$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.cancelCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.cancelCartSuccess({ items, note: 'Abort the in-flight request and restore the idle flag.' })),
          catchError((error: Error) => of(Actions.cancelCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly archive$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.archiveCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.archiveCartSuccess({ items, note: 'Move a finished record out of the working set.' })),
          catchError((error: Error) => of(Actions.archiveCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly restore$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.restoreCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.restoreCartSuccess({ items, note: 'Bring an archived record back into the working set.' })),
          catchError((error: Error) => of(Actions.restoreCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly duplicate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.duplicateCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.duplicateCartSuccess({ items, note: 'Copy a record and assign a fresh identifier.' })),
          catchError((error: Error) => of(Actions.duplicateCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly merge$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.mergeCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.mergeCartSuccess({ items, note: 'Fold incoming changes into the local draft.' })),
          catchError((error: Error) => of(Actions.mergeCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly split$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.splitCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.splitCartSuccess({ items, note: 'Separate a combined line into independent entries.' })),
          catchError((error: Error) => of(Actions.splitCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly assign$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.assignCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.assignCartSuccess({ items, note: 'Attach the record to the current operator.' })),
          catchError((error: Error) => of(Actions.assignCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly release$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.releaseCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.releaseCartSuccess({ items, note: 'Detach the record so another operator can take it.' })),
          catchError((error: Error) => of(Actions.releaseCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly notify$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.notifyCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.notifyCartSuccess({ items, note: 'Queue a status message for the surrounding shell.' })),
          catchError((error: Error) => of(Actions.notifyCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly audit$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.auditCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.auditCartSuccess({ items, note: 'Append an audit note without changing business fields.' })),
          catchError((error: Error) => of(Actions.auditCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly exportRows$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.exportRowsCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.exportRowsCartSuccess({ items, note: 'Build a flat export of the rows currently in view.' })),
          catchError((error: Error) => of(Actions.exportRowsCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly importRows$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.importRowsCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.importRowsCartSuccess({ items, note: 'Accept a flat import and reject unknown columns.' })),
          catchError((error: Error) => of(Actions.importRowsCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly summarize$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.summarizeCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.summarizeCartSuccess({ items, note: 'Reduce the working set to totals and counts.' })),
          catchError((error: Error) => of(Actions.summarizeCartFailure({ message: error.message })))
        )
      )
    )
  );

  readonly reset$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.resetCart),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.resetCartSuccess({ items, note: 'Return the draft to the last confirmed snapshot.' })),
          catchError((error: Error) => of(Actions.resetCartFailure({ message: error.message })))
        )
      )
    )
  );

  constructor(private readonly actions$: Actions) {}
}

export function cartEffectNote(note: string): string {
  return note.trim();
}
