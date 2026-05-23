package br.com.fiap.challenge.controller;

import br.com.fiap.challenge.dto.request.PetRequest;
import br.com.fiap.challenge.dto.response.PetHealthSummaryResponse;
import br.com.fiap.challenge.dto.response.PetResponse;
import br.com.fiap.challenge.enums.Species;
import br.com.fiap.challenge.service.PetHealthService;
import br.com.fiap.challenge.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
@Tag(name = "Pets", description = "Gerenciamento de pets e saúde")
public class PetController {

    private final PetService petService;
    private final PetHealthService petHealthService;

    @GetMapping
    @Operation(summary = "Listar pets", description = "Filtros: tutorId, species, breed, name")
    public ResponseEntity<Page<PetResponse>> findAll(
            @RequestParam(required = false) Long tutorId,
            @RequestParam(required = false) Species species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(petService.findAll(tutorId, species, breed, name, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pet por ID")
    public ResponseEntity<EntityModel<PetResponse>> findById(@PathVariable Long id) {
        PetResponse pet = petService.findById(id);
        EntityModel<PetResponse> model = EntityModel.of(pet);
        model.add(linkTo(methodOn(PetController.class).findById(id)).withSelfRel());
        model.add(linkTo(methodOn(PetController.class).getHealthSummary(id)).withRel("saude"));
        model.add(linkTo(methodOn(VaccineController.class).findByPet(id, Pageable.unpaged())).withRel("vacinas"));
        model.add(linkTo(methodOn(ClinicalRecordController.class).findByPet(id, null, Pageable.unpaged())).withRel("prontuarios"));
        model.add(linkTo(methodOn(ReminderController.class).findByPet(id, null, null, Pageable.unpaged())).withRel("lembretes"));
        model.add(linkTo(methodOn(ActivityCheckInController.class).getStreak(id)).withRel("streak"));
        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}/health")
    @Operation(summary = "Resumo de saúde do pet", description = "Score 0-100, streak, indicadores. Resultado cacheado.")
    public ResponseEntity<EntityModel<PetHealthSummaryResponse>> getHealthSummary(@PathVariable Long id) {
        PetHealthSummaryResponse health = petHealthService.getHealthSummary(id);
        EntityModel<PetHealthSummaryResponse> model = EntityModel.of(health);
        model.add(linkTo(methodOn(PetController.class).getHealthSummary(id)).withSelfRel());
        model.add(linkTo(methodOn(PetController.class).findById(id)).withRel("pet"));
        return ResponseEntity.ok(model);
    }

    @PostMapping
    @Operation(summary = "Cadastrar pet")
    public ResponseEntity<EntityModel<PetResponse>> create(@Valid @RequestBody PetRequest request) {
        PetResponse pet = petService.create(request);
        EntityModel<PetResponse> model = EntityModel.of(pet);
        model.add(linkTo(methodOn(PetController.class).findById(pet.id())).withSelfRel());
        model.add(linkTo(methodOn(PetController.class).getHealthSummary(pet.id())).withRel("saude"));
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar pet")
    public ResponseEntity<EntityModel<PetResponse>> update(@PathVariable Long id, @Valid @RequestBody PetRequest request) {
        PetResponse pet = petService.update(id, request);
        EntityModel<PetResponse> model = EntityModel.of(pet);
        model.add(linkTo(methodOn(PetController.class).findById(id)).withSelfRel());
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover pet")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        petService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
