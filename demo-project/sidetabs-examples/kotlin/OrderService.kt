package shop

import java.time.Instant

class OrderService(
    private val orders: MutableList<Order> = mutableListOf()
) {
    data class Order(
        val id: String,
        var title: String,
        var summary: String,
        var priceCents: Int,
        var status: String,
        var updatedAt: Instant
    )

    fun load(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Load the current snapshot from the remote catalog."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun refresh(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Refresh stale values without resetting the open view."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun save(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Persist the draft and keep the previous revision."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun validate(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Check required fields before the next transition."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun applyDiscount(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Apply a percentage discount and round to cents."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun clearDiscount(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Remove the active discount and restore list prices."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun select(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Remember the row the operator last focused."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun deselect(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Drop the current selection and return to the list."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun filter(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Narrow the visible rows by the active query."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun sort(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Order rows by the requested column and direction."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun page(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Move the window to another slice of the result set."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun retry(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Repeat the last failed request with the same payload."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun cancel(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Abort the in-flight request and restore the idle flag."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun archive(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Move a finished record out of the working set."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun restore(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Bring an archived record back into the working set."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun duplicate(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Copy a record and assign a fresh identifier."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun merge(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Fold incoming changes into the local draft."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun split(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Separate a combined line into independent entries."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun assign(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Attach the record to the current operator."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun release(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Detach the record so another operator can take it."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun notify(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Queue a status message for the surrounding shell."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun audit(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Append an audit note without changing business fields."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun exportRows(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Build a flat export of the rows currently in view."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun importRows(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Accept a flat import and reject unknown columns."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun summarize(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Reduce the working set to totals and counts."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

    fun reset(id: String, note: String): Order {
        val current = orders.find { it.id == id } ?: Order(id, note, "", 0, "draft", Instant.now())
        current.summary = note
        if (current.title.isBlank()) current.title = "Return the draft to the last confirmed snapshot."
        current.updatedAt = Instant.now()
        if (orders.none { it.id == id }) orders += current
        return current
    }

}
