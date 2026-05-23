package br.com.fiap.challenge.controller;

import br.com.fiap.challenge.dto.request.VeterinarianRequest;
import br.com.fiap.challenge.dto.response.VeterinarianResponse;
import br.com.fiap.challenge.service.VeterinarianService;
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
@RequestMapping("/api/veterinarians")
@RequiredArgsConstructor
@Tag(name = "Veterinários", description = "Gerenciamento de veterinários")
public class VeterinarianController {

    private final VeterinarianService veterinarianService;

    @GetMapping
    @Operation(summary = "Listar veterinários", description = "Filtros: name, specialty. Resultado cacheado.")
    public ResponseEntity<Page<VeterinarianResponse>> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String specialty,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(veterinarianService.findAll(name, specialty, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar veterinário por ID")
    public ResponseEntity<EntityModel<VeterinarianResponse>> findById(@PathVariable Long id) {
        VeterinarianResponse vet = veterinarianService.findById(id);
        EntityModel<VeterinarianResponse> model = EntityModel.of(vet);
        model.add(linkTo(methodOn(VeterinarianController.class).findById(id)).withSelfRel());
        model.add(linkTo(methodOn(VeterinarianController.class).findAll(null, null, Pageable.unpaged())).withRel("veterinarios"));
        return ResponseEntity.ok(model);
    }

    @PostMapping
    @Operation(summary = "Cadastrar veterinário")
    public ResponseEntity<EntityModel<VeterinarianResponse>> create(@Valid @RequestBody VeterinarianRequest request) {
        VeterinarianResponse vet = veterinarianService.create(request);
        EntityModel<VeterinarianResponse> model = EntityModel.of(vet);
        model.add(linkTo(methodOn(VeterinarianController.class).findById(vet.id())).withSelfRel());
        model.add(linkTo(methodOn(VeterinarianController.class).findAll(null, null, Pageable.unpaged())).withRel("veterinarios"));
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar veterinário")
    public ResponseEntity<EntityModel<VeterinarianResponse>> update(@PathVariable Long id, @Valid @RequestBody VeterinarianRequest request) {
        VeterinarianResponse vet = veterinarianService.update(id, request);
        EntityModel<VeterinarianResponse> model = EntityModel.of(vet);
        model.add(linkTo(methodOn(VeterinarianController.class).findById(id)).withSelfRel());
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover veterinário")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        veterinarianService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
