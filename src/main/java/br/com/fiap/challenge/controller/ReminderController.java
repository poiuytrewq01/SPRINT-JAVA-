package br.com.fiap.challenge.controller;

import br.com.fiap.challenge.dto.request.ReminderRequest;
import br.com.fiap.challenge.dto.response.ReminderResponse;
import br.com.fiap.challenge.enums.ReminderStatus;
import br.com.fiap.challenge.enums.ReminderType;
import br.com.fiap.challenge.service.ReminderService;
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
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
@Tag(name = "Lembretes", description = "Lembretes de vacinas, consultas e medicamentos")
public class ReminderController {

    private final ReminderService reminderService;

    @GetMapping("/pet/{petId}")
    @Operation(summary = "Listar lembretes do pet", description = "Filtros: status, type")
    public ResponseEntity<Page<ReminderResponse>> findByPet(
            @PathVariable Long petId,
            @RequestParam(required = false) ReminderStatus status,
            @RequestParam(required = false) ReminderType type,
            @PageableDefault(size = 10, sort = "dueDate") Pageable pageable) {
        return ResponseEntity.ok(reminderService.findByPet(petId, status, type, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar lembrete por ID")
    public ResponseEntity<EntityModel<ReminderResponse>> findById(@PathVariable Long id) {
        ReminderResponse reminder = reminderService.findById(id);
        EntityModel<ReminderResponse> model = EntityModel.of(reminder);
        model.add(linkTo(methodOn(ReminderController.class).findById(id)).withSelfRel());
        model.add(linkTo(methodOn(PetController.class).findById(reminder.petId())).withRel("pet"));
        model.add(linkTo(methodOn(ReminderController.class).findByPet(reminder.petId(), null, null, Pageable.unpaged())).withRel("lembretes-do-pet"));
        return ResponseEntity.ok(model);
    }

    @GetMapping("/pet/{petId}/upcoming")
    @Operation(summary = "Lembretes próximos", description = "Pendentes nos próximos N dias (padrão: 30)")
    public ResponseEntity<List<ReminderResponse>> findUpcoming(
            @PathVariable Long petId,
            @RequestParam(defaultValue = "30") int daysAhead) {
        return ResponseEntity.ok(reminderService.findUpcoming(petId, daysAhead));
    }

    @PostMapping
    @Operation(summary = "Criar lembrete")
    public ResponseEntity<EntityModel<ReminderResponse>> create(@Valid @RequestBody ReminderRequest request) {
        ReminderResponse reminder = reminderService.create(request);
        EntityModel<ReminderResponse> model = EntityModel.of(reminder);
        model.add(linkTo(methodOn(ReminderController.class).findById(reminder.id())).withSelfRel());
        model.add(linkTo(methodOn(PetController.class).findById(reminder.petId())).withRel("pet"));
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status do lembrete")
    public ResponseEntity<EntityModel<ReminderResponse>> updateStatus(@PathVariable Long id,
                                                                       @RequestParam ReminderStatus status) {
        ReminderResponse reminder = reminderService.updateStatus(id, status);
        EntityModel<ReminderResponse> model = EntityModel.of(reminder);
        model.add(linkTo(methodOn(ReminderController.class).findById(id)).withSelfRel());
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover lembrete")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reminderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
