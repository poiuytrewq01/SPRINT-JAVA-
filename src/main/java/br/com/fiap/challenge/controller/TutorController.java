package br.com.fiap.challenge.controller;

import br.com.fiap.challenge.dto.request.TutorRequest;
import br.com.fiap.challenge.dto.response.TutorResponse;
import br.com.fiap.challenge.service.TutorService;
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
@RequestMapping("/api/tutors")
@RequiredArgsConstructor
@Tag(name = "Tutores", description = "Gerenciamento de tutores (donos de pets)")
public class TutorController {

    private final TutorService tutorService;

    @GetMapping
    @Operation(summary = "Listar tutores", description = "Retorna lista paginada com filtros opcionais por nome e e-mail")
    public ResponseEntity<Page<TutorResponse>> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(tutorService.findAll(name, email, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar tutor por ID")
    public ResponseEntity<EntityModel<TutorResponse>> findById(@PathVariable Long id) {
        TutorResponse tutor = tutorService.findById(id);
        EntityModel<TutorResponse> model = EntityModel.of(tutor);
        model.add(linkTo(methodOn(TutorController.class).findById(id)).withSelfRel());
        model.add(linkTo(methodOn(TutorController.class).findAll(null, null, Pageable.unpaged())).withRel("tutores"));
        model.add(linkTo(methodOn(PetController.class).findAll(id, null, null, null, Pageable.unpaged())).withRel("pets"));
        return ResponseEntity.ok(model);
    }

    @PostMapping
    @Operation(summary = "Cadastrar tutor")
    public ResponseEntity<EntityModel<TutorResponse>> create(@Valid @RequestBody TutorRequest request) {
        TutorResponse tutor = tutorService.create(request);
        EntityModel<TutorResponse> model = EntityModel.of(tutor);
        model.add(linkTo(methodOn(TutorController.class).findById(tutor.id())).withSelfRel());
        model.add(linkTo(methodOn(TutorController.class).findAll(null, null, Pageable.unpaged())).withRel("tutores"));
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar tutor")
    public ResponseEntity<EntityModel<TutorResponse>> update(@PathVariable Long id, @Valid @RequestBody TutorRequest request) {
        TutorResponse tutor = tutorService.update(id, request);
        EntityModel<TutorResponse> model = EntityModel.of(tutor);
        model.add(linkTo(methodOn(TutorController.class).findById(id)).withSelfRel());
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover tutor")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tutorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
