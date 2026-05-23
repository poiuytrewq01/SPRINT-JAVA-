package br.com.fiap.challenge.config;

import br.com.fiap.challenge.entity.*;
import br.com.fiap.challenge.enums.*;
import br.com.fiap.challenge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final TutorRepository tutorRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final PetRepository petRepository;
    private final VaccineRepository vaccineRepository;
    private final ClinicalRecordRepository clinicalRecordRepository;
    private final ReminderRepository reminderRepository;

    @Override
    public void run(String... args) {
        if (tutorRepository.count() > 0) return;

        log.info("Carregando dados iniciais...");

        Veterinarian vet1 = veterinarianRepository.save(Veterinarian.builder()
                .name("Dra. Ana Souza").crmv("SP-12345").email("ana@clinicapet.com")
                .phone("11987654321").specialty("Clínica Geral").build());

        Veterinarian vet2 = veterinarianRepository.save(Veterinarian.builder()
                .name("Dr. Carlos Lima").crmv("SP-67890").email("carlos@clinicapet.com")
                .phone("11976543210").specialty("Dermatologia").build());

        Tutor tutor1 = tutorRepository.save(Tutor.builder()
                .name("Maria Silva").email("maria@email.com")
                .phone("11912345678").cpf("12345678901").build());

        Tutor tutor2 = tutorRepository.save(Tutor.builder()
                .name("João Santos").email("joao@email.com")
                .phone("11998765432").cpf("98765432100").build());

        Pet pet1 = petRepository.save(Pet.builder()
                .name("Rex").species(Species.DOG).breed("Labrador")
                .birthDate(LocalDate.of(2020, 3, 15))
                .weight(new BigDecimal("28.5")).gender(Gender.MALE)
                .tutor(tutor1).veterinarian(vet1).build());

        Pet pet2 = petRepository.save(Pet.builder()
                .name("Mia").species(Species.CAT).breed("Siamês")
                .birthDate(LocalDate.of(2021, 7, 20))
                .weight(new BigDecimal("4.2")).gender(Gender.FEMALE)
                .tutor(tutor1).veterinarian(vet1).build());

        Pet pet3 = petRepository.save(Pet.builder()
                .name("Thor").species(Species.DOG).breed("Golden Retriever")
                .birthDate(LocalDate.of(2019, 11, 5))
                .weight(new BigDecimal("32.0")).gender(Gender.MALE)
                .tutor(tutor2).veterinarian(vet2).build());

        vaccineRepository.save(Vaccine.builder()
                .name("V10").manufacturer("MSD").applicationDate(LocalDate.now().minusMonths(6))
                .nextDoseDate(LocalDate.now().plusMonths(6))
                .certificateNumber("CERT-001").pet(pet1).veterinarian(vet1).build());

        vaccineRepository.save(Vaccine.builder()
                .name("Antirrábica").manufacturer("Zoetis").applicationDate(LocalDate.now().minusMonths(3))
                .nextDoseDate(LocalDate.now().plusMonths(9))
                .certificateNumber("CERT-002").pet(pet1).veterinarian(vet1).build());

        vaccineRepository.save(Vaccine.builder()
                .name("V4 Felina").manufacturer("Boehringer").applicationDate(LocalDate.now().minusMonths(4))
                .nextDoseDate(LocalDate.now().plusMonths(8))
                .certificateNumber("CERT-003").pet(pet2).veterinarian(vet1).build());

        clinicalRecordRepository.save(ClinicalRecord.builder()
                .date(LocalDate.now().minusMonths(2))
                .description("Consulta de rotina").diagnosis("Saudável")
                .treatment("Nenhum tratamento necessário").weight(new BigDecimal("28.5"))
                .pet(pet1).veterinarian(vet1).build());

        clinicalRecordRepository.save(ClinicalRecord.builder()
                .date(LocalDate.now().minusMonths(1))
                .description("Dermatite leve").diagnosis("Dermatite alérgica")
                .treatment("Shampoo medicamentoso por 30 dias").weight(new BigDecimal("32.0"))
                .pet(pet3).veterinarian(vet2).build());

        reminderRepository.save(Reminder.builder()
                .type(ReminderType.CHECKUP).dueDate(LocalDate.now().plusDays(15))
                .message("Check-up anual de Rex").pet(pet1).build());

        reminderRepository.save(Reminder.builder()
                .type(ReminderType.VACCINE).dueDate(LocalDate.now().plusDays(7))
                .message("Reforço vacina V4 Felina para Mia").pet(pet2).build());

        log.info("Dados iniciais carregados: {} tutores, {} veterinários, {} pets",
                tutorRepository.count(), veterinarianRepository.count(), petRepository.count());
    }
}
