package shop;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class UserService {
    private final String id;
    private String title;
    private String summary;
    private int priceCents;
    private int stock;
    private String status;
    private Instant updatedAt;
    private final List<String> notes = new ArrayList<>();

    public UserService(String id, String title) {
        this.id = Objects.requireNonNull(id);
        this.title = title;
        this.summary = "";
        this.status = "draft";
        this.updatedAt = Instant.now();
    }

    public void load(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Load the current snapshot from the remote catalog."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> loadLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void refresh(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Refresh stale values without resetting the open view."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> refreshLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void save(String note) {
        // +SUB-Guards
        if (note == null || note.isBlank()) {
            this.summary = "missing note";
            return;
        }
        // +SUBEND
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Persist the draft and keep the previous revision."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> saveLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void validate(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Check required fields before the next transition."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> validateLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void applyDiscount(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Apply a percentage discount and round to cents."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> applyDiscountLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void clearDiscount(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Remove the active discount and restore list prices."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> clearDiscountLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void select(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Remember the row the operator last focused."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> selectLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void deselect(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Drop the current selection and return to the list."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> deselectLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void filter(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Narrow the visible rows by the active query."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> filterLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void sort(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Order rows by the requested column and direction."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> sortLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void page(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Move the window to another slice of the result set."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> pageLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void retry(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Repeat the last failed request with the same payload."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> retryLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void cancel(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Abort the in-flight request and restore the idle flag."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> cancelLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void archive(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Move a finished record out of the working set."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> archiveLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void restore(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Bring an archived record back into the working set."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> restoreLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void duplicate(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Copy a record and assign a fresh identifier."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> duplicateLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void merge(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Fold incoming changes into the local draft."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> mergeLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void split(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Separate a combined line into independent entries."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> splitLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void assign(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Attach the record to the current operator."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> assignLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void release(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Detach the record so another operator can take it."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> releaseLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void notify(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Queue a status message for the surrounding shell."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> notifyLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void audit(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Append an audit note without changing business fields."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> auditLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void exportRows(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Build a flat export of the rows currently in view."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> exportRowsLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void importRows(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Accept a flat import and reject unknown columns."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> importRowsLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void summarize(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Reduce the working set to totals and counts."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> summarizeLabel() {
        return Optional.ofNullable(this.summary);
    }

    public void reset(String note) {
        this.summary = note;
        if (this.title == null || this.title.isBlank()) { this.title = "Return the draft to the last confirmed snapshot."; }
        this.notes.add(note);
        this.updatedAt = Instant.now();
    }

    public Optional<String> resetLabel() {
        return Optional.ofNullable(this.summary);
    }

    public String id() {
        return id;
    }
}
