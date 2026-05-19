package com.fiap.feedback.api;

import com.fiap.feedback.application.port.in.SubmitEvaluation;
import com.fiap.feedback.domain.Urgency;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Inbound HTTP adapter for the {@link SubmitEvaluation} use case.
 *
 * <p>This is intentionally thin: it validates the HTTP payload, builds a
 * use-case command, and translates the result into an HTTP response. No
 * Firestore, Pub/Sub, or email code lives here.</p>
 */
@Path("/avaliacao")
@Tag(name = "Avaliações", description = "Receives student feedback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EvaluationResource {

    private final SubmitEvaluation submitEvaluation;

    @Inject
    public EvaluationResource(SubmitEvaluation submitEvaluation) {
        this.submitEvaluation = submitEvaluation;
    }

    @POST
    @Operation(
            summary = "Submit an evaluation",
            description = "Persists an evaluation, classifies its urgency from the rating, "
                    + "and (when ALTA) publishes an urgent-feedback event for the "
                    + "notification handler."
    )
    @APIResponse(responseCode = "201", description = "Evaluation accepted",
            content = @Content(schema = @Schema(implementation = Response201.class)))
    @APIResponse(responseCode = "400", description = "Validation failure")
    public Response submit(@Valid @RequestBody(required = true) Request request) {
        SubmitEvaluation.Result result = submitEvaluation.handle(
                new SubmitEvaluation.Command(request.descricao(), request.nota()));
        return Response.status(Response.Status.CREATED)
                .entity(new Response201(result.id(), result.urgencia()))
                .build();
    }

    @Schema(name = "AvaliacaoRequest")
    public record Request(
            @NotBlank
            @Size(max = 4000)
            @Schema(description = "Descrição textual da avaliação", example = "Aula muito clara, gostei bastante")
            String descricao,

            @Min(0) @Max(10)
            @Schema(description = "Nota de 0 a 10", example = "8", minimum = "0", maximum = "10",
                    type = SchemaType.INTEGER)
            int nota
    ) {
    }

    @Schema(name = "AvaliacaoResponse")
    public record Response201(
            @Schema(description = "Identificador único atribuído à avaliação")
            String id,

            @Schema(description = "Urgência derivada da nota",
                    enumeration = {"ALTA", "MEDIA", "BAIXA"})
            Urgency urgencia
    ) {
    }
}
