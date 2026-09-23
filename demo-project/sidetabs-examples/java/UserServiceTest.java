package shop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UserServiceTest {
    private UserService service;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User("u-1", "Ada");
        service = new UserService("svc", "users");
    }

    @Test
    void loadKeepsTheSuppliedNote() {
        user.load("Load the current snapshot from the remote catalog.");
        assertEquals("Load the current snapshot from the remote catalog.", user.loadLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void refreshKeepsTheSuppliedNote() {
        user.refresh("Refresh stale values without resetting the open view.");
        assertEquals("Refresh stale values without resetting the open view.", user.refreshLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void saveKeepsTheSuppliedNote() {
        user.save("Persist the draft and keep the previous revision.");
        assertEquals("Persist the draft and keep the previous revision.", user.saveLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void validateKeepsTheSuppliedNote() {
        user.validate("Check required fields before the next transition.");
        assertEquals("Check required fields before the next transition.", user.validateLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void applyDiscountKeepsTheSuppliedNote() {
        user.applyDiscount("Apply a percentage discount and round to cents.");
        assertEquals("Apply a percentage discount and round to cents.", user.applyDiscountLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void clearDiscountKeepsTheSuppliedNote() {
        user.clearDiscount("Remove the active discount and restore list prices.");
        assertEquals("Remove the active discount and restore list prices.", user.clearDiscountLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void selectKeepsTheSuppliedNote() {
        user.select("Remember the row the operator last focused.");
        assertEquals("Remember the row the operator last focused.", user.selectLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void deselectKeepsTheSuppliedNote() {
        user.deselect("Drop the current selection and return to the list.");
        assertEquals("Drop the current selection and return to the list.", user.deselectLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void filterKeepsTheSuppliedNote() {
        user.filter("Narrow the visible rows by the active query.");
        assertEquals("Narrow the visible rows by the active query.", user.filterLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void sortKeepsTheSuppliedNote() {
        user.sort("Order rows by the requested column and direction.");
        assertEquals("Order rows by the requested column and direction.", user.sortLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void pageKeepsTheSuppliedNote() {
        user.page("Move the window to another slice of the result set.");
        assertEquals("Move the window to another slice of the result set.", user.pageLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void retryKeepsTheSuppliedNote() {
        user.retry("Repeat the last failed request with the same payload.");
        assertEquals("Repeat the last failed request with the same payload.", user.retryLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void cancelKeepsTheSuppliedNote() {
        user.cancel("Abort the in-flight request and restore the idle flag.");
        assertEquals("Abort the in-flight request and restore the idle flag.", user.cancelLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void archiveKeepsTheSuppliedNote() {
        user.archive("Move a finished record out of the working set.");
        assertEquals("Move a finished record out of the working set.", user.archiveLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void restoreKeepsTheSuppliedNote() {
        user.restore("Bring an archived record back into the working set.");
        assertEquals("Bring an archived record back into the working set.", user.restoreLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void duplicateKeepsTheSuppliedNote() {
        user.duplicate("Copy a record and assign a fresh identifier.");
        assertEquals("Copy a record and assign a fresh identifier.", user.duplicateLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void mergeKeepsTheSuppliedNote() {
        user.merge("Fold incoming changes into the local draft.");
        assertEquals("Fold incoming changes into the local draft.", user.mergeLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void splitKeepsTheSuppliedNote() {
        user.split("Separate a combined line into independent entries.");
        assertEquals("Separate a combined line into independent entries.", user.splitLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void assignKeepsTheSuppliedNote() {
        user.assign("Attach the record to the current operator.");
        assertEquals("Attach the record to the current operator.", user.assignLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void releaseKeepsTheSuppliedNote() {
        user.release("Detach the record so another operator can take it.");
        assertEquals("Detach the record so another operator can take it.", user.releaseLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void notifyKeepsTheSuppliedNote() {
        user.notify("Queue a status message for the surrounding shell.");
        assertEquals("Queue a status message for the surrounding shell.", user.notifyLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void auditKeepsTheSuppliedNote() {
        user.audit("Append an audit note without changing business fields.");
        assertEquals("Append an audit note without changing business fields.", user.auditLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void exportRowsKeepsTheSuppliedNote() {
        user.exportRows("Build a flat export of the rows currently in view.");
        assertEquals("Build a flat export of the rows currently in view.", user.exportRowsLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void importRowsKeepsTheSuppliedNote() {
        user.importRows("Accept a flat import and reject unknown columns.");
        assertEquals("Accept a flat import and reject unknown columns.", user.importRowsLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void summarizeKeepsTheSuppliedNote() {
        user.summarize("Reduce the working set to totals and counts.");
        assertEquals("Reduce the working set to totals and counts.", user.summarizeLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

    @Test
    void resetKeepsTheSuppliedNote() {
        user.reset("Return the draft to the last confirmed snapshot.");
        assertEquals("Return the draft to the last confirmed snapshot.", user.resetLabel().orElse(""));
        assertFalse(user.id().isBlank());
        assertFalse(service.id().isBlank());
    }

}
