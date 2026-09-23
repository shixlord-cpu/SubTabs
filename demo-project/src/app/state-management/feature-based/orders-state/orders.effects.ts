import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import * as Actions from './orders.actions';

@Injectable()
export class OrdersEffects {
  readonly load$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.loadOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.loadOrdersSuccess({ items, note: 'Load the current snapshot from the remote catalog.' })),
          catchError((error: Error) => of(Actions.loadOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly refresh$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.refreshOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.refreshOrdersSuccess({ items, note: 'Refresh stale values without resetting the open view.' })),
          catchError((error: Error) => of(Actions.refreshOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly save$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.saveOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.saveOrdersSuccess({ items, note: 'Persist the draft and keep the previous revision.' })),
          catchError((error: Error) => of(Actions.saveOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly validate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.validateOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.validateOrdersSuccess({ items, note: 'Check required fields before the next transition.' })),
          catchError((error: Error) => of(Actions.validateOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly applyDiscount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.applyDiscountOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.applyDiscountOrdersSuccess({ items, note: 'Apply a percentage discount and round to cents.' })),
          catchError((error: Error) => of(Actions.applyDiscountOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly clearDiscount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.clearDiscountOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.clearDiscountOrdersSuccess({ items, note: 'Remove the active discount and restore list prices.' })),
          catchError((error: Error) => of(Actions.clearDiscountOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly select$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.selectOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.selectOrdersSuccess({ items, note: 'Remember the row the operator last focused.' })),
          catchError((error: Error) => of(Actions.selectOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly deselect$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.deselectOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.deselectOrdersSuccess({ items, note: 'Drop the current selection and return to the list.' })),
          catchError((error: Error) => of(Actions.deselectOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly filter$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.filterOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.filterOrdersSuccess({ items, note: 'Narrow the visible rows by the active query.' })),
          catchError((error: Error) => of(Actions.filterOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly sort$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.sortOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.sortOrdersSuccess({ items, note: 'Order rows by the requested column and direction.' })),
          catchError((error: Error) => of(Actions.sortOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly page$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.pageOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.pageOrdersSuccess({ items, note: 'Move the window to another slice of the result set.' })),
          catchError((error: Error) => of(Actions.pageOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly retry$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.retryOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.retryOrdersSuccess({ items, note: 'Repeat the last failed request with the same payload.' })),
          catchError((error: Error) => of(Actions.retryOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly cancel$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.cancelOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.cancelOrdersSuccess({ items, note: 'Abort the in-flight request and restore the idle flag.' })),
          catchError((error: Error) => of(Actions.cancelOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly archive$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.archiveOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.archiveOrdersSuccess({ items, note: 'Move a finished record out of the working set.' })),
          catchError((error: Error) => of(Actions.archiveOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly restore$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.restoreOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.restoreOrdersSuccess({ items, note: 'Bring an archived record back into the working set.' })),
          catchError((error: Error) => of(Actions.restoreOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly duplicate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.duplicateOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.duplicateOrdersSuccess({ items, note: 'Copy a record and assign a fresh identifier.' })),
          catchError((error: Error) => of(Actions.duplicateOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly merge$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.mergeOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.mergeOrdersSuccess({ items, note: 'Fold incoming changes into the local draft.' })),
          catchError((error: Error) => of(Actions.mergeOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly split$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.splitOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.splitOrdersSuccess({ items, note: 'Separate a combined line into independent entries.' })),
          catchError((error: Error) => of(Actions.splitOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly assign$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.assignOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.assignOrdersSuccess({ items, note: 'Attach the record to the current operator.' })),
          catchError((error: Error) => of(Actions.assignOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly release$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.releaseOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.releaseOrdersSuccess({ items, note: 'Detach the record so another operator can take it.' })),
          catchError((error: Error) => of(Actions.releaseOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly notify$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.notifyOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.notifyOrdersSuccess({ items, note: 'Queue a status message for the surrounding shell.' })),
          catchError((error: Error) => of(Actions.notifyOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly audit$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.auditOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.auditOrdersSuccess({ items, note: 'Append an audit note without changing business fields.' })),
          catchError((error: Error) => of(Actions.auditOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly exportRows$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.exportRowsOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.exportRowsOrdersSuccess({ items, note: 'Build a flat export of the rows currently in view.' })),
          catchError((error: Error) => of(Actions.exportRowsOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly importRows$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.importRowsOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.importRowsOrdersSuccess({ items, note: 'Accept a flat import and reject unknown columns.' })),
          catchError((error: Error) => of(Actions.importRowsOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly summarize$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.summarizeOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.summarizeOrdersSuccess({ items, note: 'Reduce the working set to totals and counts.' })),
          catchError((error: Error) => of(Actions.summarizeOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  readonly reset$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.resetOrders),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.resetOrdersSuccess({ items, note: 'Return the draft to the last confirmed snapshot.' })),
          catchError((error: Error) => of(Actions.resetOrdersFailure({ message: error.message })))
        )
      )
    )
  );

  constructor(private readonly actions$: Actions) {}
}

export function ordersEffectNote(note: string): string {
  return note.trim();
}
