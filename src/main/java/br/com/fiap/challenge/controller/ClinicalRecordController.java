package br.com.fiap.challenge.controller;

import br.com.fiap.challenge.dto.request.ClinicalRecordRequest;
import br.com.fiap.challenge.dto.response.ClinicalRecordResponse;
import br.com.fiap.challenge.service.ClinicalRecordService;
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
@RequestMapping("/api/clinical-records")
@RequiredArgsConstructor
@Tag(name = "Prontuários", description = "Histórico clínico dos pets")
public class ClinicalRecordController {

    private final ClinicalRecordService clinicalRecordService;

    @GetMapping("/pet/{petId}")
    @Operation(summary = "Listar prontuários do pet", description = "Suporta busca por keyword em descrição e diagnóstico")
    public ResponseEntity<Page<ClinicalRecordResponse>> findByPet(
            @PathVariable Long petId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "date") Pageable pageable) {
        return ResponseEntity.ok(clinicalRecordService.findByPet(petId, keyword, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar prontuário por ID")
    public ResponseEntity<EntityModel<ClinicalRecordResponse>> findById(@PathVariable Long id) {
        ClinicalRecordResponse record = clinicalRecordService.findById(id);
        EntityModel<ClinicalRecordResponse> model = EntityModel.of(record);
        model.add(linkTo(methodOn(ClinicalRecordController.class).findById(id)).withSelfRel());
        model.add(linkTo(methodOn(PetController.class).findById(record.petId())).withRel("pet"));
        model.add(linkTo(methodOn(ClinicalRecordController.class).findByPet(record.petId(), null, Pageable.unpaged())).withRel("prontuarios-do-pet"));
        return ResponseEntity.ok(model);
    }

    @PostMapping
    @Operation(summary = "Criar prontuário", description = "Gera lembrete de retorno em 6 meses automaticamente")
    public ResponseEntity<EntityModel<ClinicalRecordResponse>> create(@Valid @RequestBody ClinicalRecordRequest request) {
        ClinicalRecordResponse record = clinicalRecordService.create(request);
        EntityModel<ClinicalRecordResponse> model = EntityModel.of(record);
        model.add(linkTo(methodOn(ClinicalRecordController.class).findById(record.id())).withSelfRel());
        model.add(linkTo(methodOn(PetController.class).findById(record.petId())).withRel("pet"));
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar prontuário")
    public ResponseEntity<EntityModel<ClinicalRecordResponse>> update(@PathVariable Long id, @Valid @RequestBody ClinicalRecordRequest request) {
        ClinicalRecordResponse record = clinicalRecordService.update(id, request);
        EntityModel<ClinicalRecordResponse> model = EntityModel.of(record);
        model.add(linkTo(methodOn(ClinicalRecordController.class).findById(id)).withSelfRel());
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover prontuário")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clinicalRecordService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
