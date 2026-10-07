package app.rekord.application.web;

import app.rekord.adapter.web.shared.SuccessStatus;
import app.rekord.api.model.Health;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Test-only: stands in for a generated-interface resource, which cannot set a status by annotation. */
@Path("/test-only/success-status")
@Produces(MediaType.APPLICATION_JSON)
public class SuccessStatusProbeResource {

    @Inject
    SuccessStatus successStatus;

    @POST
    @Path("/created")
    public Health created() {
        successStatus.answer(201);
        return new Health().ok(true);
    }

    @POST
    @Path("/no-content")
    public Health noContent() {
        successStatus.answer(204);
        return new Health().ok(true);
    }

    @GET
    @Path("/untouched")
    public Health untouched() {
        return new Health().ok(true);
    }
}
