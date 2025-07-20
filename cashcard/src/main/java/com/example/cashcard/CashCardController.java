package com.example.cashcard;

import java.net.URI;
import java.security.Principal;
import java.util.List;
//import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

//import io.micrometer.core.annotation.TimedSet;

//import io.micrometer.core.ipc.http.HttpSender.Response;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestParam;

//import jakarta.websocket.server.PathParam;

@RestController
@RequestMapping("/cashcards")
public class CashCardController {

    // Inyectamos el repositorio
    private final CashCardRepository cashCardRepository;

    private CashCardController(CashCardRepository cashCardRepository) {
        this.cashCardRepository = cashCardRepository;
    }

    // Handler method for GET requests that match
    // cashcards/{requestedId}(IMPORTANTE: los bracelets hace que Spring lo entienda
    // como una variable)
    @GetMapping("/{requestedId}")
    /**
     * Con @PathVariable podremos trabajar dentro del metodo con el id suministrado
     * en la petición HTTP
     * 
     * Cuando implementamos seguridad se ha añadido el objeto Principal:
     * que guarda la información del usuario en cuanto a credenciales, autenticación y autorización
     * @param requestedId
     * @return
     */
    private ResponseEntity<CashCard> findById(@PathVariable Long requestedId, Principal principal) {
        /**
         * if (requestedId.equals(99L)) {
         * CashCard cashCard = new CashCard(99L, 123.45);
         * // Jackson convierte automaticamente el objeto a JSON antes de enviar la
         * // respuesta, el metodo devuelve un ResponseEntity<CashCard> pero la
         * respuesta es un JSON
         * return ResponseEntity.ok(cashCard);
         * } else {
         * return ResponseEntity.notFound().build();
         * }
         * Primera version en la que hemos pasados los datos en crudo desde el controlador
         * 
         * ahora pasamos a manejarlos a traves de repositorio
            Optional<CashCard> cashCardOptional = Optional.ofNullable(cashCardRepository.findByIdAndOwner(requestedId, principal.getName()));
            if (cashCardOptional.isPresent()) {
            return ResponseEntity.ok(cashCardOptional.get());
            } else {
            return ResponseEntity.notFound().build(); 
            }
            En esta nueva versión refactorizamos el código para no usar el objeto Optional
         */  
        CashCard cashCard = findCashCard(requestedId, principal);
        //Queda ademas implementado que si el usuario no es propietario, no la puede obtener
        if (cashCard != null) {
            return ResponseEntity.ok(cashCard);
        } else {
            return ResponseEntity.notFound().build();
        }
        
    }

    /**
     * Ahora si tenemos un metodo para manejar el verbo GET en el endpoint
     * "/cashcards"
     * ha sido comentado en una paso posterior ya que no puede haber dos metodos 
     * iguales mapeados por el mismo endpoint, este fallo se detecta en runtime(mvn test)
     */
    /*@GetMapping()
    private ResponseEntity<Iterable<CashCard>> findAll() {
        return ResponseEntity.ok(cashCardRepository.findAll());
    }*/


    /**El propio Spring Data Web se encarga de detectar un Pageable por 
     * los parametros de la query string y construye un objeto Page
     * a través de un objeto Pageable con ellos
     * endPoint que acepta GET para listas
      */
    @GetMapping
    private ResponseEntity<List<CashCard>> findAll(Pageable pageable, Principal principal) {
        Page<CashCard> page = cashCardRepository.findByOwner(principal.getName(),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),pageable.getSortOr(Sort.by(Sort.Direction.ASC,"amount"))));
        
        return ResponseEntity.ok(page.getContent());

    }

    /**
     * Handler method for POST request
     * 
     * @param newCashCardRequest
     * @param ucb
     * @return
     */
    @PostMapping
    private ResponseEntity<Void> createCashCard(@RequestBody CashCard newCashCardRequest, UriComponentsBuilder ucb, Principal principal) {
        // TODO: process POST request
        /**
         * savedCashCard tendrá ahora el id generado por la base de datos gracias al
         * metodo save,
         * que recibia una CashCard por parametro con el id igual a null
         */
        CashCard cashCardWithOwner = new CashCard(null, newCashCardRequest.amount(), principal.getName());
        //Ahora si guardamos la cashcard con el usuario propietario(que esta autenticado)
        CashCard savedCashCard = cashCardRepository.save(cashCardWithOwner);

        /** Ahora puedo construir la URI del recurso guardado */
        URI locationOfNewCashCard = ucb.path("cashcards/{id}").// plantilla con el placeholder que se cambiara en
                                                               // buildAndExpand
                buildAndExpand(savedCashCard.id()).// aqui se sustituye el valor del placeholder por el valor concreto
                toUri();// Convierte en objeto URI

        /**
         * Al usar .build(), no se envía ningún contenido en el cuerpo, estamos enviando
         * el estado
         * y el location como pedia el protocolo HTTP para metodos POST.
         */
        return ResponseEntity.created(locationOfNewCashCard).build();
    }

    /**PUT request method handler 
     * Con los parametros correspondientes, requestedId y principal, nos aseguramos
     * que solo el usuario autorizado y autenticado(principal es una representación
     * del usuario logeado en ese momento) puede modificar su tarjeta(en este caso
     * la modifica completamente)
     * En REST no se recomienda usar excepciones para control de flujo ("con try-catch").
     * En su lugar, se prefiere hacer una comprobación explícita con if.
     * No es lo mismo lanzar excepcion por la lógica de programación, que por no encontrar 
     * un recurso en la bbdd, estos ultimos se manejan con if en REST
    */
    @PutMapping("/{requestedId}")
    private ResponseEntity<Void> putCashCard(@PathVariable Long requestedId, @RequestBody CashCard cashCardUpdate, Principal principal){
        System.out.println("Usuario autenticado: " + principal.getName());
        CashCard cashCardToBeUpdated = findCashCard(requestedId, principal);
        if(cashCardToBeUpdated != null){
            CashCard updatedCashCard = new CashCard(cashCardToBeUpdated.id(), cashCardUpdate.amount(), principal.getName());
            cashCardRepository.save(updatedCashCard);
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.notFound().build();
        
    }

    /**
     * Delete endpoint
     * We use the @DeleteMapping with the "{id}" parameter,
     *  which Spring Web matches to the id method parameter
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    private ResponseEntity<Void> deleteCashCard(@PathVariable Long id, Principal principal) {
        // Si no existe el cashcard, forzando comprobación de pertenencia al usuario
        if (cashCardRepository.existsByIdAndOwner(id, principal.getName())) {
            cashCardRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {

            return ResponseEntity.notFound().build();
        }

    }



    /**
     * Con este metodo refactorizamos código y simplificamos 
     * @param requestedId
     * @param principal
     * @return
     */
    private CashCard findCashCard(Long requestedId,Principal principal){
        return cashCardRepository.findByIdAndOwner(requestedId, principal.getName());
    }

    
}
