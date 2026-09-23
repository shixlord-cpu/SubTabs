import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import * as Actions from './user.actions';

@Injectable()
export class UserEffects {
  readonly load$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.loadUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.loadUserSuccess({ items, note: 'Load the current snapshot from the remote catalog.' })),
          catchError((error: Error) => of(Actions.loadUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly refresh$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.refreshUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.refreshUserSuccess({ items, note: 'Refresh stale values without resetting the open view.' })),
          catchError((error: Error) => of(Actions.refreshUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly save$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.saveUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.saveUserSuccess({ items, note: 'Persist the draft and keep the previous revision.' })),
          catchError((error: Error) => of(Actions.saveUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly validate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.validateUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.validateUserSuccess({ items, note: 'Check required fields before the next transition.' })),
          catchError((error: Error) => of(Actions.validateUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly applyDiscount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.applyDiscountUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.applyDiscountUserSuccess({ items, note: 'Apply a percentage discount and round to cents.' })),
          catchError((error: Error) => of(Actions.applyDiscountUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly clearDiscount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.clearDiscountUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.clearDiscountUserSuccess({ items, note: 'Remove the active discount and restore list prices.' })),
          catchError((error: Error) => of(Actions.clearDiscountUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly select$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.selectUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.selectUserSuccess({ items, note: 'Remember the row the operator last focused.' })),
          catchError((error: Error) => of(Actions.selectUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly deselect$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.deselectUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.deselectUserSuccess({ items, note: 'Drop the current selection and return to the list.' })),
          catchError((error: Error) => of(Actions.deselectUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly filter$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.filterUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.filterUserSuccess({ items, note: 'Narrow the visible rows by the active query.' })),
          catchError((error: Error) => of(Actions.filterUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly sort$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.sortUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.sortUserSuccess({ items, note: 'Order rows by the requested column and direction.' })),
          catchError((error: Error) => of(Actions.sortUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly page$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.pageUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.pageUserSuccess({ items, note: 'Move the window to another slice of the result set.' })),
          catchError((error: Error) => of(Actions.pageUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly retry$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.retryUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.retryUserSuccess({ items, note: 'Repeat the last failed request with the same payload.' })),
          catchError((error: Error) => of(Actions.retryUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly cancel$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.cancelUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.cancelUserSuccess({ items, note: 'Abort the in-flight request and restore the idle flag.' })),
          catchError((error: Error) => of(Actions.cancelUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly archive$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.archiveUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.archiveUserSuccess({ items, note: 'Move a finished record out of the working set.' })),
          catchError((error: Error) => of(Actions.archiveUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly restore$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.restoreUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.restoreUserSuccess({ items, note: 'Bring an archived record back into the working set.' })),
          catchError((error: Error) => of(Actions.restoreUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly duplicate$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.duplicateUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.duplicateUserSuccess({ items, note: 'Copy a record and assign a fresh identifier.' })),
          catchError((error: Error) => of(Actions.duplicateUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly merge$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.mergeUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.mergeUserSuccess({ items, note: 'Fold incoming changes into the local draft.' })),
          catchError((error: Error) => of(Actions.mergeUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly split$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.splitUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.splitUserSuccess({ items, note: 'Separate a combined line into independent entries.' })),
          catchError((error: Error) => of(Actions.splitUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly assign$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.assignUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.assignUserSuccess({ items, note: 'Attach the record to the current operator.' })),
          catchError((error: Error) => of(Actions.assignUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly release$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.releaseUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.releaseUserSuccess({ items, note: 'Detach the record so another operator can take it.' })),
          catchError((error: Error) => of(Actions.releaseUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly notify$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.notifyUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.notifyUserSuccess({ items, note: 'Queue a status message for the surrounding shell.' })),
          catchError((error: Error) => of(Actions.notifyUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly audit$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.auditUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.auditUserSuccess({ items, note: 'Append an audit note without changing business fields.' })),
          catchError((error: Error) => of(Actions.auditUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly exportRows$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.exportRowsUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.exportRowsUserSuccess({ items, note: 'Build a flat export of the rows currently in view.' })),
          catchError((error: Error) => of(Actions.exportRowsUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly importRows$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.importRowsUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.importRowsUserSuccess({ items, note: 'Accept a flat import and reject unknown columns.' })),
          catchError((error: Error) => of(Actions.importRowsUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly summarize$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.summarizeUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.summarizeUserSuccess({ items, note: 'Reduce the working set to totals and counts.' })),
          catchError((error: Error) => of(Actions.summarizeUserFailure({ message: error.message })))
        )
      )
    )
  );

  readonly reset$ = createEffect(() =>
    this.actions$.pipe(
      ofType(Actions.resetUser),
      switchMap(({ note }) =>
        of([{ id: note, title: note, quantity: 1, priceCents: 0 }]).pipe(
          map((items) => Actions.resetUserSuccess({ items, note: 'Return the draft to the last confirmed snapshot.' })),
          catchError((error: Error) => of(Actions.resetUserFailure({ message: error.message })))
        )
      )
    )
  );

  constructor(private readonly actions$: Actions) {}
}

export function userEffectNote(note: string): string {
  return note.trim();
}
