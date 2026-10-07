package app.rekord.adapter.web.shared;

import jakarta.enterprise.context.RequestScoped;
import java.util.OptionalInt;

/**
 * Records the contract's documented success code for the current operation.
 *
 * <p>Quarkus reads JAX-RS metadata from the generated interface, so a status annotation on the implementing method
 * is not honoured. A resource therefore calls {@link #answer(int)} and {@link SuccessStatusFilter} applies the code
 * to the response (architecture-conventions §7.1, PIN-AC-0499). Only the success codes 200, 201, 202 and 204 are
 * accepted; an error status is the business of the error mapping, never of this helper.
 */
@RequestScoped
public class SuccessStatus {

    private Integer code;

    /** Names the success code this operation answers with. */
    public void answer(int code) {
        if (code != 200 && code != 201 && code != 202 && code != 204) {
            throw new IllegalArgumentException("not a success status of the contract: " + code);
        }
        this.code = code;
    }

    /** The recorded code, or empty when the operation did not name one. */
    public OptionalInt status() {
        return code == null ? OptionalInt.empty() : OptionalInt.of(code);
    }
}
