package br.com.fiap.challenge.controller;

import br.com.fiap.challenge.dto.request.VaccineRequest;
import br.com.fiap.challenge.dto.response.VaccineResponse;
import br.com.fiap.challenge.service.VaccineService;
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

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/vaccines")
@RequiredArgsConstructor
@Tag(name = "Vacinas", description = "Registro e controle de vacinas")
public class VaccineController {

    private final VaccineService vaccineService;

    @GetMapping("/pet/{petId}")
    @Operation(summary = "Listar vacinas do pet")
    public ResponseEntity<Page<VaccineResponse>> findByPet(
            @PathVariable Long petId,
            @PageableDefault(size = 10, sort = "applicationDate") Pageable pageable) {
        return ResponseEntity.ok(vaccineService.findByPet(petId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar vacina por ID")
    public ResponseEntity<EntityModel<VaccineResponse>> findById(@PathVariable Long id) {
        VaccineResponse vaccine = vaccineService.findById(id);
        EntityModel<VaccineResponse> model = EntityModel.of(vaccine);
        model.add(linkTo(methodOn(VaccineController.class).findById(id)).withSelfRel());
        model.add(linkTo(methodOn(PetController.class).findById(vaccine.petId())).withRel("pet"));
        model.add(linkTo(methodOn(VaccineController.class).findByPet(vaccine.petId(), Pageable.unpaged())).withRel("vacinas-do-pet"));
        return ResponseEntity.ok(model);
    }

    @GetMapping("/pet/{petId}/overdue")
    @Operation(summary = "Vacinas atrasadas do pet")
    public ResponseEntity<List<VaccineResponse>> findOverdue(@PathVariable Long petId) {
        return ResponseEntity.ok(vaccineService.findOverdue(petId));
    }

    @GetMapping("/pet/{petId}/upcoming")
    @Operation(summary = "Próximas doses", description = "Vacinas com dose nos próximos N dias (padrão: 30)")
    public ResponseEntity<List<VaccineResponse>> findUpcoming(
            @PathVariable Long petId,
            @RequestParam(defaultValue = "30") int daysAhead) {
        return ResponseEntity.ok(vaccineService.findUpcoming(petId, daysAhead));
    }

    @PostMapping
    @Operation(summary = "Registrar vacina", description = "Se informar nextDoseDate, cria lembrete automático")
    public ResponseEntity<EntityModel<VaccineResponse>> create(@Valid @RequestBody VaccineRequest request) {
        VaccineResponse vaccine = vaccineService.create(request);
        EntityModel<VaccineResponse> model = EntityModel.of(vaccine);
        model.add(linkTo(methodOn(VaccineController.class).findById(vaccine.id())).withSelfRel());
        model.add(linkTo(methodOn(PetController.class).findById(vaccine.petId())).withRel("pet"));
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar vacina")
    public ResponseEntity<EntityModel<VaccineResponse>> update(@PathVariable Long id, @Valid @RequestBody VaccineRequest request) {
        VaccineResponse vaccine = vaccineService.update(id, request);
        EntityModel<VaccineResponse> model = EntityModel.of(vaccine);
        model.add(linkTo(methodOn(VaccineController.class).findById(id)).withSelfRel());
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover vacina")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vaccineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
