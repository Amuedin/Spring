package com.example.cashcard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Con CrudRepository<CashCard, Long> estamos diciendo que el repositorio 
 * va a manejar objetos de tipo CashCard, y que el id de esos objetos será de tipo long
 */
interface CashCardRepository extends CrudRepository<CashCard, Long>,PagingAndSortingRepository<CashCard,Long> {
    /**
     * Spring Data JPA convertirá estos metodos en sentencias SQL, se ayudará
     * parseando el nombre de los metodos: 
     * Spring genera una consulta SQL automáticamente gracias
     * al parsing del nombre del método (method name query derivation).
     * https://docs.spring.io/spring-data/relational/reference/repositories/query-methods-details.html */
    CashCard findByIdAndOwner(Long id, String owner);
    Page<CashCard> findByOwner(String owner, PageRequest pageRequest);
    boolean existsByIdAndOwner(Long id, String owner);
}
