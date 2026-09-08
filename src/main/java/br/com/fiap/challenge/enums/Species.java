package br.com.fiap.challenge.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Especies aceitas no cadastro de pets.
 *
 * Persistido como texto (@Enumerated(EnumType.STRING) na entidade Pet), e nao
 * pela posicao: com ORDINAL, inserir uma especie no meio da lista mudaria o
 * significado dos registros ja gravados no banco.
 */
@Getter
@RequiredArgsConstructor
public enum Species {

    DOG("Cachorro"),
    CAT("Gato"),
    BIRD("Ave"),
    RODENT("Roedor"),
    REPTILE("Reptil"),
    RABBIT("Coelho"),
    OTHER("Outro");

    private final String label;
}
