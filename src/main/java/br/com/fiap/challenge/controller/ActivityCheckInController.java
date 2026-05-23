package br.com.fiap.challenge.controller;

import br.com.fiap.challenge.dto.request.ActivityCheckInRequest;
import br.com.fiap.challenge.dto.response.ActivityCheckInResponse;
import br.com.fiap.challenge.dto.response.PetStreakResponse;
import br.com.fiap.challenge.service.ActivityCheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/checkins")
@RequiredArgsConstructor
@Tag(name = "Gamificação - Check-ins", description = "Sistema de check-in diário e streak de atividades")
public class ActivityCheckInController {

    private final ActivityCheckInService checkInService;

    @GetMapping("/pet/{petId}")
    @Operation(summary = "Listar check-ins do pet", description = "Suporta filtro por intervalo de datas")
    public ResponseEntity<Page<ActivityCheckInResponse>> findByPet(
            @PathVariable Long petId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @PageableDefault(size = 10, sort = "date") Pageable pageable) {
        return ResponseEntity.ok(checkInService.findByPet(petId, start, end, pageable));
    }

    @GetMapping("/pet/{petId}/streak")
    @Operation(summary = "Consultar streak do pet", description = "Streak atual, recorde, nível e se já fez check-in hoje")
    public ResponseEntity<EntityModel<PetStreakResponse>> getStreak(@PathVariable Long petId) {
        PetStreakResponse streak = checkInService.getStreak(petId);
        EntityModel<PetStreakResponse> model = EntityModel.of(streak);
        model.add(linkTo(methodOn(ActivityCheckInController.class).getStreak(petId)).withSelfRel());
        model.add(linkTo(methodOn(PetController.class).findById(petId)).withRel("pet"));
        model.add(linkTo(methodOn(PetController.class).getHealthSummary(petId)).withRel("saude"));
        return ResponseEntity.ok(model);
    }

    @PostMapping("/pet/{petId}")
    @Operation(summary = "Realizar check-in diário", description = "Registra atividade e atualiza o streak. Apenas um check-in por dia.")
    public ResponseEntity<EntityModel<ActivityCheckInResponse>> checkIn(
            @PathVariable Long petId,
            @Valid @RequestBody ActivityCheckInRequest request) {
        ActivityCheckInResponse checkIn = checkInService.checkIn(petId, request);
        EntityModel<ActivityCheckInResponse> model = EntityModel.of(checkIn);
        model.add(linkTo(methodOn(ActivityCheckInController.class).getStreak(petId)).withRel("streak"));
        model.add(linkTo(methodOn(PetController.class).findById(petId)).withRel("pet"));
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover check-in")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        checkInService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
